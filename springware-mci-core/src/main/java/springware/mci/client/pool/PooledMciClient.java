package springware.mci.client.pool;

import lombok.extern.slf4j.Slf4j;
import springware.mci.client.config.ClientConfig;
import springware.mci.client.core.MciClient;
import springware.mci.client.tcp.TcpClient;
import springware.mci.common.core.Message;
import springware.mci.common.layout.LayoutManager;
import springware.mci.common.logging.MessageLogger;

import java.util.concurrent.CompletableFuture;

/**
 * 풀링을 지원하는 MCI 클라이언트
 * 매 요청마다 풀에서 연결을 획득하고 사용 후 반환
 */
@Slf4j
public class PooledMciClient implements MciClient {

    private final TcpConnectionPool pool;
    private final ClientConfig config;

    public PooledMciClient(ClientConfig clientConfig, PoolConfig poolConfig,
                           LayoutManager layoutManager, MessageLogger messageLogger) {
        this.config = clientConfig;
        this.pool = new TcpConnectionPool(clientConfig, poolConfig, layoutManager, messageLogger);
    }

    public PooledMciClient(ClientConfig clientConfig, PoolConfig poolConfig) {
        this.config = clientConfig;
        this.pool = new TcpConnectionPool(clientConfig, poolConfig);
    }

    public PooledMciClient(ClientConfig clientConfig) {
        this(clientConfig, PoolConfig.builder()
                .minSize(1)
                .maxSize(clientConfig.getPoolSize())
                .build());
    }

    @Override
    public void connect() {
        // 풀은 생성 시 이미 초기화됨
        log.debug("PooledMciClient is ready (pool already initialized)");
    }

    @Override
    public void disconnect() {
        pool.close();
    }

    @Override
    public boolean isConnected() {
        return pool.getAvailableCount() > 0 || pool.getActiveCount() > 0;
    }

    @Override
    public Message send(Message message) {
        TcpClient client = pool.acquire();
        try {
            return client.send(message);
        } finally {
            pool.release(client);
        }
    }

    @Override
    public Message send(Message message, long timeoutMillis) {
        TcpClient client = pool.acquire();
        try {
            return client.send(message, timeoutMillis);
        } finally {
            pool.release(client);
        }
    }

    @Override
    public CompletableFuture<Message> sendAsync(Message message) {
        TcpClient client = pool.acquire();
        return client.sendAsync(message)
                .whenComplete((response, error) -> pool.release(client));
    }

    @Override
    public void sendOneWay(Message message) {
        TcpClient client = pool.acquire();
        try {
            client.sendOneWay(message);
        } finally {
            pool.release(client);
        }
    }

    @Override
    public ClientConfig getConfig() {
        return config;
    }

    @Override
    public void close() {
        disconnect();
    }

    /**
     * 풀 상태 조회
     */
    public String getPoolStats() {
        return pool.getStats();
    }

    /**
     * 가용 연결 수
     */
    public int getAvailableCount() {
        return pool.getAvailableCount();
    }

    /**
     * 활성 연결 수
     */
    public int getActiveCount() {
        return pool.getActiveCount();
    }

    /**
     * 전체 연결 수
     */
    public int getPoolSize() {
        return pool.getPoolSize();
    }

    /**
     * 연결 풀 직접 접근 (고급 사용)
     */
    public TcpConnectionPool getPool() {
        return pool;
    }

    /**
     * 연결을 수동으로 획득 (try-with-resources 사용 권장)
     */
    public PooledConnection acquireConnection() {
        return new PooledConnection(pool.acquire(), pool);
    }

    /**
     * 수동 연결 관리를 위한 래퍼
     */
    public static class PooledConnection implements AutoCloseable {
        private final TcpClient client;
        private final TcpConnectionPool pool;
        private boolean released = false;

        PooledConnection(TcpClient client, TcpConnectionPool pool) {
            this.client = client;
            this.pool = pool;
        }

        public TcpClient getClient() {
            return client;
        }

        @Override
        public void close() {
            if (!released) {
                pool.release(client);
                released = true;
            }
        }
    }
}
