package springware.mci.server.core;

import lombok.extern.slf4j.Slf4j;
import springware.mci.common.layout.DefaultLayoutManager;
import springware.mci.common.layout.LayoutManager;
import springware.mci.common.layout.YamlLayoutLoader;
import springware.mci.common.logging.AsyncMessageLogger;
import springware.mci.common.logging.DefaultMessageLogger;
import springware.mci.common.logging.MessageLogger;
import springware.mci.server.config.ServerConfig;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MCI 서버 추상 기본 클래스
 */
@Slf4j
public abstract class AbstractMciServer implements MciServer {

    protected final ServerConfig config;
    protected final LayoutManager layoutManager;
    protected final MessageLogger messageLogger;
    protected final Map<String, MessageHandler> handlers = new ConcurrentHashMap<>();
    protected MessageHandler defaultHandler;
    protected final AtomicBoolean running = new AtomicBoolean(false);

    protected AbstractMciServer(ServerConfig config) {
        this(config, new DefaultLayoutManager(), createAsyncLogger());
    }

    protected AbstractMciServer(ServerConfig config, LayoutManager layoutManager, MessageLogger messageLogger) {
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

        // 기본 마스킹 규칙 등록
        if (messageLogger instanceof DefaultMessageLogger) {
            ((DefaultMessageLogger) messageLogger).registerDefaultMaskingRules();
        }
    }

    /**
     * 비동기 메시지 로거 생성
     */
    private static MessageLogger createAsyncLogger() {
        DefaultMessageLogger syncLogger = new DefaultMessageLogger();
        syncLogger.registerDefaultMaskingRules();
        return new AsyncMessageLogger(syncLogger);
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
    public void start() {
        if (!running.compareAndSet(false, true)) {
            log.warn("Server is already running");
            return;
        }

        try {
            doStart();
            log.info("Server started on port {}", config.getPort());
        } catch (Exception e) {
            running.set(false);
            throw new RuntimeException("Failed to start server", e);
        }
    }

    @Override
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        try {
            doStop();
            log.info("Server stopped");
        } catch (Exception e) {
            log.error("Error stopping server", e);
        }

        // 비동기 로거 종료
        if (messageLogger instanceof AsyncMessageLogger) {
            ((AsyncMessageLogger) messageLogger).shutdown();
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public void registerHandler(String messageCode, MessageHandler handler) {
        handlers.put(messageCode, handler);
        log.debug("Registered handler for message code: {}", messageCode);
    }

    @Override
    public void setDefaultHandler(MessageHandler handler) {
        this.defaultHandler = handler;
        log.debug("Set default handler");
    }

    @Override
    public ServerConfig getConfig() {
        return config;
    }

    @Override
    public void close() {
        stop();
    }

    /**
     * 메시지 코드로 핸들러 조회
     */
    protected MessageHandler getHandler(String messageCode) {
        MessageHandler handler = handlers.get(messageCode);
        return handler != null ? handler : defaultHandler;
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

    // 하위 클래스에서 구현

    /**
     * 실제 서버 시작
     */
    protected abstract void doStart();

    /**
     * 실제 서버 종료
     */
    protected abstract void doStop();
}
