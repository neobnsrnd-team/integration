package springware.mci.client.pool;

import org.junit.jupiter.api.*;
import springware.mci.client.config.ClientConfig;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PooledMciClient Tests")
class PooledMciClientTest {

    private static ServerSocket serverSocket;
    private static int testPort;
    private static ExecutorService serverExecutor;
    private static volatile boolean serverRunning;

    @BeforeAll
    static void setUpServer() throws IOException {
        serverSocket = new ServerSocket(0);
        testPort = serverSocket.getLocalPort();
        serverRunning = true;

        serverExecutor = Executors.newCachedThreadPool();
        serverExecutor.submit(() -> {
            while (serverRunning) {
                try {
                    Socket client = serverSocket.accept();
                    serverExecutor.submit(() -> {
                        try {
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
                .poolSize(3)
                .retryEnabled(false)
                .build();
    }

    @Test
    @DisplayName("풀 클라이언트 생성")
    void createPooledClient() {
        // given
        ClientConfig config = createConfig();
        PoolConfig poolConfig = PoolConfig.builder()
                .minSize(2)
                .maxSize(5)
                .build();

        // when
        PooledMciClient client = new PooledMciClient(config, poolConfig);

        // then
        assertThat(client.getPoolSize()).isEqualTo(2);
        assertThat(client.isConnected()).isTrue();

        client.close();
    }

    @Test
    @DisplayName("ClientConfig의 poolSize 사용")
    void usesClientConfigPoolSize() {
        // given
        ClientConfig config = createConfig(); // poolSize = 3

        // when
        PooledMciClient client = new PooledMciClient(config);

        // then
        assertThat(client.getPool().getPoolSize()).isGreaterThanOrEqualTo(1);

        client.close();
    }

    @Test
    @DisplayName("수동 연결 획득 (try-with-resources)")
    void manualConnectionAcquisition() {
        // given
        PooledMciClient client = new PooledMciClient(createConfig(),
                PoolConfig.builder().minSize(1).maxSize(3).build());

        int initialAvailable = client.getAvailableCount();

        // when
        try (var connection = client.acquireConnection()) {
            assertThat(connection.getClient()).isNotNull();
            assertThat(connection.getClient().isConnected()).isTrue();
            assertThat(client.getActiveCount()).isEqualTo(1);
        }

        // then - connection returned
        assertThat(client.getActiveCount()).isEqualTo(0);
        assertThat(client.getAvailableCount()).isEqualTo(initialAvailable);

        client.close();
    }

    @Test
    @DisplayName("풀 상태 조회")
    void getPoolStats() {
        // given
        PooledMciClient client = new PooledMciClient(createConfig(),
                PoolConfig.builder().minSize(2).maxSize(5).build());

        // when
        String stats = client.getPoolStats();

        // then
        assertThat(stats).contains("total=2");
        assertThat(stats).contains("max=5");

        client.close();
    }

    @Test
    @DisplayName("close() 호출시 풀 종료")
    void closeClosesPool() {
        // given
        PooledMciClient client = new PooledMciClient(createConfig(),
                PoolConfig.builder().minSize(2).maxSize(3).build());

        assertThat(client.getPoolSize()).isEqualTo(2);

        // when
        client.close();

        // then
        assertThat(client.getPoolSize()).isEqualTo(0);
    }
}
