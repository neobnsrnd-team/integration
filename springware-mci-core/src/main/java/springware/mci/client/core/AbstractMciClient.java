package springware.mci.client.core;

import lombok.extern.slf4j.Slf4j;
import springware.mci.client.circuitbreaker.CircuitBreaker;
import springware.mci.client.circuitbreaker.CircuitBreakerConfig;
import springware.mci.client.circuitbreaker.CircuitBreakerOpenException;
import springware.mci.client.config.ClientConfig;
import springware.mci.client.healthcheck.HealthCheckConfig;
import springware.mci.client.healthcheck.HealthChecker;
import springware.mci.common.core.Message;
import springware.mci.common.exception.ConnectionException;
import springware.mci.common.exception.TimeoutException;
import springware.mci.common.layout.DefaultLayoutManager;
import springware.mci.common.layout.LayoutManager;
import springware.mci.common.layout.YamlLayoutLoader;
import springware.mci.common.logging.DefaultMessageLogger;
import springware.mci.common.logging.MessageLogger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * MCI 클라이언트 추상 기본 클래스
 */
@Slf4j
public abstract class AbstractMciClient implements MciClient {

    protected final ClientConfig config;
    protected final LayoutManager layoutManager;
    protected final MessageLogger messageLogger;
    protected final AtomicReference<ConnectionState> state = new AtomicReference<>(ConnectionState.DISCONNECTED);
    protected final CircuitBreaker circuitBreaker;

    protected AbstractMciClient(ClientConfig config) {
        this(config, new DefaultLayoutManager(), new DefaultMessageLogger());
    }

    protected AbstractMciClient(ClientConfig config, LayoutManager layoutManager, MessageLogger messageLogger) {
        this.config = config;
        this.layoutManager = layoutManager;
        this.messageLogger = messageLogger;

        // 서킷 브레이커 초기화
        CircuitBreakerConfig cbConfig = config.getCircuitBreakerConfig();
        this.circuitBreaker = new CircuitBreaker(
                config.getClientId() != null ? config.getClientId() : "mci-client",
                cbConfig != null ? cbConfig : CircuitBreakerConfig.defaultConfig()
        );

        // 설정 검증
        config.validate();

        // 레이아웃 로드
        if (config.getLayoutPath() != null) {
            loadLayouts(config.getLayoutPath());
        }

        // 로깅 레벨 설정
        messageLogger.setLogLevel(config.getLogLevel());
    }

    /**
     * 레이아웃 파일 로드
     */
    protected void loadLayouts(String layoutPath) {
        try {
            Path path = Paths.get(layoutPath);
            YamlLayoutLoader loader = new YamlLayoutLoader();
            int count = loader.loadAndRegister(path, layoutManager);
            log.info("Loaded {} layouts from {}", count, layoutPath);
        } catch (Exception e) {
            log.warn("Failed to load layouts from {}: {}", layoutPath, e.getMessage());
        }
    }

    @Override
    public void connect() {
        if (!state.compareAndSet(ConnectionState.DISCONNECTED, ConnectionState.CONNECTING) &&
            !state.compareAndSet(ConnectionState.FAILED, ConnectionState.CONNECTING)) {
            throw new ConnectionException("Cannot connect: current state is " + state.get());
        }

        // 서킷 브레이커 확인
        if (!circuitBreaker.allowRequest()) {
            state.set(ConnectionState.FAILED);
            throw new CircuitBreakerOpenException(
                    String.format("CircuitBreaker is OPEN for %s:%d, remaining time: %dms",
                            config.getHost(), config.getPort(), circuitBreaker.getRemainingOpenTime()),
                    circuitBreaker.getState(),
                    circuitBreaker.getRemainingOpenTime());
        }

        try {
            if (config.isRetryEnabled()) {
                connectWithRetry();
            } else {
                connectOnce();
            }
            // 연결 성공시 서킷 브레이커에 알림
            circuitBreaker.onSuccess();
        } catch (Exception e) {
            // 연결 실패시 서킷 브레이커에 알림
            circuitBreaker.onFailure(e);
            throw e;
        }
    }

    /**
     * 재시도 없이 한 번만 연결 시도
     */
    private void connectOnce() {
        try {
            doConnect();
            state.set(ConnectionState.CONNECTED);
            log.info("Connected to {}:{}", config.getHost(), config.getPort());
        } catch (Exception e) {
            state.set(ConnectionState.FAILED);
            throw new ConnectionException("Failed to connect to " + config.getHost() + ":" + config.getPort(), e);
        }
    }

    /**
     * 재시도 로직으로 연결
     */
    private void connectWithRetry() {
        int maxAttempts = config.getRetryAttempts();
        long delay = config.getRetryDelay();
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.debug("Connection attempt {}/{} to {}:{}",
                        attempt, maxAttempts, config.getHost(), config.getPort());

                doConnect();
                state.set(ConnectionState.CONNECTED);

                if (attempt > 1) {
                    log.info("Connected to {}:{} after {} attempts",
                            config.getHost(), config.getPort(), attempt);
                } else {
                    log.info("Connected to {}:{}", config.getHost(), config.getPort());
                }
                return;

            } catch (Exception e) {
                lastException = e;
                log.warn("Connection attempt {}/{} failed: {}",
                        attempt, maxAttempts, e.getMessage());

                if (attempt < maxAttempts) {
                    try {
                        log.debug("Waiting {}ms before next retry...", delay);
                        Thread.sleep(delay);
                        // Exponential backoff
                        delay = (long) (delay * config.getRetryBackoffMultiplier());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        state.set(ConnectionState.FAILED);
                        throw new ConnectionException("Connection retry interrupted", ie);
                    }
                }
            }
        }

        state.set(ConnectionState.FAILED);
        throw new ConnectionException(
                String.format("Failed to connect to %s:%d after %d attempts",
                        config.getHost(), config.getPort(), maxAttempts),
                lastException);
    }

    @Override
    public void disconnect() {
        if (state.get() == ConnectionState.DISCONNECTED) {
            return;
        }

        state.set(ConnectionState.DISCONNECTING);
        try {
            doDisconnect();
        } finally {
            state.set(ConnectionState.DISCONNECTED);
            log.info("Disconnected from {}:{}", config.getHost(), config.getPort());
        }
    }

    @Override
    public boolean isConnected() {
        return state.get() == ConnectionState.CONNECTED;
    }

    @Override
    public Message send(Message message) {
        return send(message, config.getReadTimeout());
    }

    @Override
    public Message send(Message message, long timeoutMillis) {
        ensureConnected();

        try {
            CompletableFuture<Message> future = sendAsync(message);
            return future.get(timeoutMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new TimeoutException(timeoutMillis);
        } catch (Exception e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw new ConnectionException("Failed to send message", e);
        }
    }

    @Override
    public void sendOneWay(Message message) {
        ensureConnected();
        doSendOneWay(message);
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
     * 연결 상태 확인
     */
    protected void ensureConnected() {
        if (!isConnected()) {
            throw new ConnectionException("Client is not connected");
        }
    }

    /**
     * 재연결 시도
     */
    protected void tryReconnect() {
        if (!state.compareAndSet(ConnectionState.CONNECTED, ConnectionState.RECONNECTING) &&
            !state.compareAndSet(ConnectionState.FAILED, ConnectionState.RECONNECTING)) {
            return;
        }

        for (int i = 0; i < config.getReconnectAttempts(); i++) {
            try {
                Thread.sleep(config.getReconnectDelay());
                doConnect();
                state.set(ConnectionState.CONNECTED);
                log.info("Reconnected to {}:{} (attempt {})", config.getHost(), config.getPort(), i + 1);
                return;
            } catch (Exception e) {
                log.warn("Reconnection attempt {} failed: {}", i + 1, e.getMessage());
            }
        }

        state.set(ConnectionState.FAILED);
        log.error("All reconnection attempts failed");
    }

    /**
     * 레이아웃 매니저 조회
     */
    public LayoutManager getLayoutManager() {
        return layoutManager;
    }

    /**
     * 메시지 로거 조회
     */
    public MessageLogger getMessageLogger() {
        return messageLogger;
    }

    /**
     * 서킷 브레이커 조회
     */
    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }

    /**
     * 헬스 체커 생성 (기본 PING 메시지 사용)
     */
    public HealthChecker createHealthChecker() {
        HealthCheckConfig hcConfig = config.getHealthCheckConfig();
        return new HealthChecker(
                config.getClientId() != null ? config.getClientId() : "mci-client",
                this,
                hcConfig != null ? hcConfig : HealthCheckConfig.defaultConfig()
        );
    }

    /**
     * 헬스 체커 생성 (커스텀 하트비트 메시지)
     */
    public HealthChecker createHealthChecker(Supplier<Message> heartbeatMessageSupplier) {
        HealthCheckConfig hcConfig = config.getHealthCheckConfig();
        return new HealthChecker(
                config.getClientId() != null ? config.getClientId() : "mci-client",
                this,
                hcConfig != null ? hcConfig : HealthCheckConfig.defaultConfig(),
                heartbeatMessageSupplier
        );
    }

    // 하위 클래스에서 구현할 메서드들

    /**
     * 실제 연결 수행
     */
    protected abstract void doConnect();

    /**
     * 실제 연결 해제 수행
     */
    protected abstract void doDisconnect();

    /**
     * 단방향 메시지 전송 수행
     */
    protected abstract void doSendOneWay(Message message);
}
