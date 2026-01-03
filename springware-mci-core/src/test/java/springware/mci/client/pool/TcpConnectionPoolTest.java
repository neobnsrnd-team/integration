package springware.mci.client.pool;

import org.junit.jupiter.api.*;
import springware.mci.client.config.ClientConfig;
import springware.mci.common.exception.ConnectionException;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TcpConnectionPool Tests")
class TcpConnectionPoolTest {

    private static ServerSocket serverSocket;
    private static int testPort;
    private static ExecutorService serverExecutor;
    private static volatile boolean serverRunning;

    @BeforeAll
    static void setUpServer() throws IOException {
        serverSocket = new ServerSocket(0);
        testPort = serverSocket.getLocalPort();
        serverRunning = true;

        // Simple echo server
        serverExecutor = Executors.newCachedThreadPool();
        serverExecutor.submit(() -> {
            while (serverRunning) {
                try {
                    Socket client = serverSocket.accept();
                    serverExecutor.submit(() -> {
                        try {
                            // Keep connection alive
                            while (!client.isClosed() && serverRunning) {
                                Thread.sleep(100);
                            }
                        } catch (Exception e) {
                            // ignore
                        }
                    });
                } catch (Exception e) {
                    if (serverRunning) {
                        // ignore
                    }
                }
            }
        });
    }

    @AfterAll
    static void tearDownServer() throws IOException {
        serverRunning = false;
        serverExecutor.shutdownNow();
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
    }

    private ClientConfig createConfig() {
        return ClientConfig.builder()
                .clientId("test-client")
                .host("localhost")
                .port(testPort)
                .connectTimeout(5000)
                .retryEnabled(false)
                .build();
    }

    @Test
    @DisplayName("풀 초기화시 최소 연결 생성")
    void initializesWithMinConnections() {
        // given
        PoolConfig poolConfig = PoolConfig.builder()
                .minSize(2)
                .maxSize(5)
                .build();

        // when
        TcpConnectionPool pool = new TcpConnectionPool(createConfig(), poolConfig);

        // then
        assertThat(pool.getPoolSize()).isEqualTo(2);
        assertThat(pool.getAvailableCount()).isEqualTo(2);
        assertThat(pool.getActiveCount()).isEqualTo(0);

        pool.close();
    }

    @Test
    @DisplayName("연결 획득 및 반환")
    void acquireAndRelease() {
        // given
        PoolConfig poolConfig = PoolConfig.builder()
                .minSize(1)
                .maxSize(3)
                .build();
        TcpConnectionPool pool = new TcpConnectionPool(createConfig(), poolConfig);

        // when
        var client = pool.acquire();

        // then
        assertThat(client).isNotNull();
        assertThat(client.isConnected()).isTrue();
        assertThat(pool.getActiveCount()).isEqualTo(1);
        assertThat(pool.getAvailableCount()).isEqualTo(0);

        // when - release
        pool.release(client);

        // then
        assertThat(pool.getActiveCount()).isEqualTo(0);
        assertThat(pool.getAvailableCount()).isEqualTo(1);

        pool.close();
    }

    @Test
    @DisplayName("풀이 비었을 때 새 연결 생성")
    void createsNewConnectionWhenPoolEmpty() {
        // given
        PoolConfig poolConfig = PoolConfig.builder()
                .minSize(0)
                .maxSize(3)
                .build();
        TcpConnectionPool pool = new TcpConnectionPool(createConfig(), poolConfig);

        assertThat(pool.getPoolSize()).isEqualTo(0);

        // when
        var client = pool.acquire();

        // then
        assertThat(client).isNotNull();
        assertThat(pool.getPoolSize()).isEqualTo(1);
        assertThat(pool.getActiveCount()).isEqualTo(1);

        pool.release(client);
        pool.close();
    }

    @Test
    @DisplayName("최대 연결 수 제한")
    void respectsMaxPoolSize() throws Exception {
        // given
        PoolConfig poolConfig = PoolConfig.builder()
                .minSize(1)
                .maxSize(2)
                .acquireTimeout(500)
                .build();
        TcpConnectionPool pool = new TcpConnectionPool(createConfig(), poolConfig);

        // when - acquire all available
        var client1 = pool.acquire();
        var client2 = pool.acquire();

        // then
        assertThat(pool.getActiveCount()).isEqualTo(2);
        assertThat(pool.getPoolSize()).isEqualTo(2);

        // when - try to acquire third (should timeout)
        assertThatThrownBy(() -> pool.acquire(500))
                .isInstanceOf(ConnectionException.class)
                .hasMessageContaining("Failed to acquire connection");

        pool.release(client1);
        pool.release(client2);
        pool.close();
    }

    @Test
    @DisplayName("반환된 연결 재사용")
    void reusesReleasedConnections() {
        // given
        PoolConfig poolConfig = PoolConfig.builder()
                .minSize(1)
                .maxSize(1)
                .build();
        TcpConnectionPool pool = new TcpConnectionPool(createConfig(), poolConfig);

        // when
        var client1 = pool.acquire();
        pool.release(client1);
        var client2 = pool.acquire();

        // then - same connection reused
        assertThat(client2).isSameAs(client1);
        assertThat(pool.getPoolSize()).isEqualTo(1);

        pool.release(client2);
        pool.close();
    }

    @Test
    @DisplayName("멀티스레드 동시 접근")
    void handlesConcurrentAccess() throws Exception {
        // given
        PoolConfig poolConfig = PoolConfig.builder()
                .minSize(2)
                .maxSize(5)
                .acquireTimeout(5000)
                .build();
        TcpConnectionPool pool = new TcpConnectionPool(createConfig(), poolConfig);

        int threadCount = 10;
        int iterationsPerThread = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Exception> errors = new CopyOnWriteArrayList<>();

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < iterationsPerThread; j++) {
                        var client = pool.acquire();
                        Thread.sleep(10); // simulate work
                        pool.release(client);
                    }
                } catch (Exception e) {
                    errors.add(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // then
        assertThat(errors).isEmpty();
        assertThat(pool.getActiveCount()).isEqualTo(0);
        assertThat(pool.getPoolSize()).isLessThanOrEqualTo(5);

        pool.close();
    }

    @Test
    @DisplayName("풀 종료시 모든 연결 정리")
    void closesAllConnectionsOnShutdown() {
        // given
        PoolConfig poolConfig = PoolConfig.builder()
                .minSize(3)
                .maxSize(5)
                .build();
        TcpConnectionPool pool = new TcpConnectionPool(createConfig(), poolConfig);

        assertThat(pool.getPoolSize()).isEqualTo(3);

        // when
        pool.close();

        // then
        assertThat(pool.getPoolSize()).isEqualTo(0);
        assertThat(pool.getAvailableCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("종료된 풀에서 연결 획득 시도시 예외")
    void throwsExceptionWhenAcquiringFromClosedPool() {
        // given
        TcpConnectionPool pool = new TcpConnectionPool(createConfig(), PoolConfig.defaultConfig());
        pool.close();

        // when & then
        assertThatThrownBy(pool::acquire)
                .isInstanceOf(ConnectionException.class)
                .hasMessageContaining("closed");
    }

    @Test
    @DisplayName("풀 상태 조회")
    void getStats() {
        // given
        PoolConfig poolConfig = PoolConfig.builder()
                .minSize(2)
                .maxSize(5)
                .build();
        TcpConnectionPool pool = new TcpConnectionPool(createConfig(), poolConfig);

        // when
        var client = pool.acquire();
        String stats = pool.getStats();

        // then
        assertThat(stats).contains("total=2");
        assertThat(stats).contains("active=1");
        assertThat(stats).contains("available=1");
        assertThat(stats).contains("max=5");

        pool.release(client);
        pool.close();
    }
}
