package springware.mci.client.pool;

import lombok.extern.slf4j.Slf4j;
import springware.mci.client.config.ClientConfig;
import springware.mci.client.tcp.TcpClient;
import springware.mci.common.exception.ConnectionException;
import springware.mci.common.layout.LayoutManager;
import springware.mci.common.logging.MessageLogger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TCP 연결 풀 구현
 */
@Slf4j
public class TcpConnectionPool implements ConnectionPool<TcpClient> {

    private final ClientConfig clientConfig;
    private final PoolConfig poolConfig;
    private final LayoutManager layoutManager;
    private final MessageLogger messageLogger;

    private final BlockingQueue<TcpClient> availableConnections;
    private final AtomicInteger totalConnections = new AtomicInteger(0);
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public TcpConnectionPool(ClientConfig clientConfig, PoolConfig poolConfig,
                             LayoutManager layoutManager, MessageLogger messageLogger) {
        this.clientConfig = clientConfig;
        this.poolConfig = poolConfig;
        this.layoutManager = layoutManager;
        this.messageLogger = messageLogger;
        this.availableConnections = new LinkedBlockingQueue<>(poolConfig.getMaxSize());

        poolConfig.validate();
        initializePool();
    }

    public TcpConnectionPool(ClientConfig clientConfig, PoolConfig poolConfig) {
        this(clientConfig, poolConfig, null, null);
    }

    public TcpConnectionPool(ClientConfig clientConfig) {
        this(clientConfig, PoolConfig.defaultConfig());
    }

    /**
     * 풀 초기화 - 최소 연결 수만큼 미리 생성
     */
    private void initializePool() {
        log.info("Initializing connection pool: minSize={}, maxSize={}",
                poolConfig.getMinSize(), poolConfig.getMaxSize());

        for (int i = 0; i < poolConfig.getMinSize(); i++) {
            try {
                TcpClient client = createConnection();
                availableConnections.offer(client);
                log.debug("Pre-created connection {}/{}", i + 1, poolConfig.getMinSize());
            } catch (Exception e) {
                log.warn("Failed to pre-create connection {}: {}", i + 1, e.getMessage());
            }
        }

        log.info("Connection pool initialized with {} connections", totalConnections.get());
    }

    /**
     * 새 연결 생성
     */
    private TcpClient createConnection() {
        TcpClient client;
        if (layoutManager != null && messageLogger != null) {
            client = new TcpClient(clientConfig, layoutManager, messageLogger);
        } else {
            client = new TcpClient(clientConfig);
        }

        client.connect();
        totalConnections.incrementAndGet();
        log.debug("Created new connection, total: {}", totalConnections.get());

        return client;
    }

    @Override
    public TcpClient acquire() {
        return acquire(poolConfig.getAcquireTimeout());
    }

    @Override
    public TcpClient acquire(long timeoutMillis) {
        if (closed.get()) {
            throw new ConnectionException("Connection pool is closed");
        }

        TcpClient client = null;

        // 1. 먼저 가용 연결에서 획득 시도
        client = availableConnections.poll();

        if (client != null) {
            // 연결 유효성 검증
            if (poolConfig.isValidateOnAcquire() && !isConnectionValid(client)) {
                log.debug("Invalid connection found, creating new one");
                closeConnection(client);
                client = null;
            }
        }

        // 2. 가용 연결이 없으면 새로 생성 (최대 크기 이내)
        if (client == null && totalConnections.get() < poolConfig.getMaxSize()) {
            try {
                client = createConnection();
            } catch (Exception e) {
                log.warn("Failed to create new connection: {}", e.getMessage());
            }
        }

        // 3. 여전히 없으면 대기
        if (client == null) {
            try {
                log.debug("Waiting for available connection (timeout: {}ms)", timeoutMillis);
                client = availableConnections.poll(timeoutMillis, TimeUnit.MILLISECONDS);

                if (client != null && poolConfig.isValidateOnAcquire() && !isConnectionValid(client)) {
                    closeConnection(client);
                    client = null;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ConnectionException("Interrupted while waiting for connection", e);
            }
        }

        if (client == null) {
            throw new ConnectionException(
                    String.format("Failed to acquire connection within %dms (pool: %d/%d, active: %d)",
                            timeoutMillis, totalConnections.get(), poolConfig.getMaxSize(),
                            activeConnections.get()));
        }

        activeConnections.incrementAndGet();
        log.debug("Connection acquired, active: {}, available: {}",
                activeConnections.get(), availableConnections.size());

        return client;
    }

    @Override
    public void release(TcpClient client) {
        if (client == null) {
            return;
        }

        if (closed.get()) {
            closeConnection(client);
            return;
        }

        activeConnections.decrementAndGet();

        // 연결이 유효하면 풀에 반환
        if (isConnectionValid(client)) {
            boolean offered = availableConnections.offer(client);
            if (!offered) {
                // 풀이 가득 찬 경우 (최대 크기 초과)
                closeConnection(client);
            } else {
                log.debug("Connection released, active: {}, available: {}",
                        activeConnections.get(), availableConnections.size());
            }
        } else {
            closeConnection(client);
        }
    }

    /**
     * 연결 유효성 검증
     */
    private boolean isConnectionValid(TcpClient client) {
        return client != null && client.isConnected();
    }

    /**
     * 연결 종료
     */
    private void closeConnection(TcpClient client) {
        try {
            client.disconnect();
        } catch (Exception e) {
            log.debug("Error closing connection: {}", e.getMessage());
        } finally {
            totalConnections.decrementAndGet();
        }
    }

    @Override
    public int getAvailableCount() {
        return availableConnections.size();
    }

    @Override
    public int getActiveCount() {
        return activeConnections.get();
    }

    @Override
    public int getPoolSize() {
        return totalConnections.get();
    }

    /**
     * 풀 상태 정보
     */
    public String getStats() {
        return String.format("Pool[total=%d, active=%d, available=%d, max=%d]",
                totalConnections.get(), activeConnections.get(),
                availableConnections.size(), poolConfig.getMaxSize());
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        log.info("Closing connection pool...");

        // 모든 가용 연결 종료
        TcpClient client;
        while ((client = availableConnections.poll()) != null) {
            closeConnection(client);
        }

        log.info("Connection pool closed");
    }
}
