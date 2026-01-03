package springware.mci.client.core;

import lombok.extern.slf4j.Slf4j;
import springware.mci.client.config.ClientConfig;
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

/**
 * MCI 클라이언트 추상 기본 클래스
 */
@Slf4j
public abstract class AbstractMciClient implements MciClient {

    protected final ClientConfig config;
    protected final LayoutManager layoutManager;
    protected final MessageLogger messageLogger;
    protected final AtomicReference<ConnectionState> state = new AtomicReference<>(ConnectionState.DISCONNECTED);

    protected AbstractMciClient(ClientConfig config) {
        this(config, new DefaultLayoutManager(), new DefaultMessageLogger());
    }

    protected AbstractMciClient(ClientConfig config, LayoutManager layoutManager, MessageLogger messageLogger) {
        this.config = config;
        this.layoutManager = layoutManager;
        this.messageLogger = messageLogger;

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

        try {
            doConnect();
            state.set(ConnectionState.CONNECTED);
            log.info("Connected to {}:{}", config.getHost(), config.getPort());
        } catch (Exception e) {
            state.set(ConnectionState.FAILED);
            throw new ConnectionException("Failed to connect to " + config.getHost() + ":" + config.getPort(), e);
        }
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
