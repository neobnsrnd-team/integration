package springware.mci.common.logging;

import lombok.extern.slf4j.Slf4j;
import springware.mci.common.core.Message;
import springware.mci.common.layout.MessageLayout;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 비동기 메시지 로거
 *
 * 서버측 2단계 로깅에서 상세 로깅을 비동기로 처리하여
 * 메시지 처리 성능에 영향을 최소화
 */
@Slf4j
public class AsyncMessageLogger implements MessageLogger {

    private final MessageLogger delegate;
    private final ExecutorService executorService;
    private volatile boolean shutdown = false;

    public AsyncMessageLogger(MessageLogger delegate) {
        this(delegate, Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "async-message-logger");
            t.setDaemon(true);
            return t;
        }));
    }

    public AsyncMessageLogger(MessageLogger delegate, ExecutorService executorService) {
        this.delegate = delegate;
        this.executorService = executorService;
    }

    @Override
    public void logSend(Message message, MessageLayout layout, byte[] rawData) {
        // 1단계: 헤더는 동기 로깅 (즉시)
        delegate.logHeader("SEND", message, rawData);

        // 2단계: 상세는 비동기 로깅
        if (!shutdown && delegate.getLogLevel().isEnabled(LogLevel.DETAIL_MASKED)) {
            executorService.submit(() -> {
                try {
                    delegate.logDetail("SEND", message, layout);
                } catch (Exception e) {
                    log.warn("Failed to log message detail", e);
                }
            });
        }
    }

    @Override
    public void logReceive(Message message, MessageLayout layout, byte[] rawData) {
        // 1단계: 헤더는 동기 로깅 (즉시)
        delegate.logHeader("RECV", message, rawData);

        // 2단계: 상세는 비동기 로깅
        if (!shutdown && delegate.getLogLevel().isEnabled(LogLevel.DETAIL_MASKED)) {
            executorService.submit(() -> {
                try {
                    delegate.logDetail("RECV", message, layout);
                } catch (Exception e) {
                    log.warn("Failed to log message detail", e);
                }
            });
        }
    }

    @Override
    public void logHeader(String direction, Message message, byte[] rawData) {
        // 헤더는 항상 동기 로깅
        delegate.logHeader(direction, message, rawData);
    }

    @Override
    public void logDetail(String direction, Message message, MessageLayout layout) {
        // 상세는 비동기 로깅
        if (!shutdown) {
            executorService.submit(() -> {
                try {
                    delegate.logDetail(direction, message, layout);
                } catch (Exception e) {
                    log.warn("Failed to log message detail", e);
                }
            });
        }
    }

    @Override
    public void setLogLevel(LogLevel level) {
        delegate.setLogLevel(level);
    }

    @Override
    public LogLevel getLogLevel() {
        return delegate.getLogLevel();
    }

    @Override
    public void addMaskingRule(MaskingRule rule) {
        delegate.addMaskingRule(rule);
    }

    /**
     * 로거 종료
     */
    public void shutdown() {
        shutdown = true;
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 대기 중인 로그 플러시 (테스트용)
     */
    public void flush() {
        try {
            executorService.submit(() -> {}).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to flush async logger", e);
        }
    }
}
