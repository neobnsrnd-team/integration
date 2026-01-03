package springware.mci.client.healthcheck;

import lombok.extern.slf4j.Slf4j;
import springware.mci.client.core.MciClient;
import springware.mci.common.core.Message;
import springware.mci.common.core.MessageType;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * 클라이언트 연결 상태를 모니터링하는 헬스 체커
 */
@Slf4j
public class HealthChecker implements AutoCloseable {

    private final String name;
    private final HealthCheckConfig config;
    private final MciClient client;
    private final Supplier<Message> heartbeatMessageSupplier;
    private final List<HealthCheckListener> listeners = new CopyOnWriteArrayList<>();

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledTask;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean healthy = new AtomicBoolean(true);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong lastSuccessTime = new AtomicLong(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private final AtomicLong totalChecks = new AtomicLong(0);
    private final AtomicLong totalFailures = new AtomicLong(0);

    /**
     * 기본 생성자 (Ping 메시지 사용)
     */
    public HealthChecker(String name, MciClient client, HealthCheckConfig config) {
        this(name, client, config, () -> Message.builder()
                .messageCode("PING")
                .messageType(MessageType.REQUEST)
                .build());
    }

    /**
     * 커스텀 하트비트 메시지를 사용하는 생성자
     */
    public HealthChecker(String name, MciClient client, HealthCheckConfig config,
                         Supplier<Message> heartbeatMessageSupplier) {
        this.name = name;
        this.client = client;
        this.config = config;
        this.heartbeatMessageSupplier = heartbeatMessageSupplier;

        config.validate();
    }

    /**
     * 헬스 체크 시작
     */
    public void start() {
        if (!config.isEnabled()) {
            log.debug("HealthChecker '{}' is disabled", name);
            return;
        }

        if (!running.compareAndSet(false, true)) {
            log.warn("HealthChecker '{}' is already running", name);
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "health-checker-" + name);
            t.setDaemon(true);
            return t;
        });

        scheduledTask = scheduler.scheduleAtFixedRate(
                this::performHealthCheck,
                config.getInitialDelayMillis(),
                config.getIntervalMillis(),
                TimeUnit.MILLISECONDS
        );

        log.info("HealthChecker '{}' started with interval {}ms", name, config.getIntervalMillis());
    }

    /**
     * 헬스 체크 중지
     */
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            scheduledTask = null;
        }

        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            scheduler = null;
        }

        log.info("HealthChecker '{}' stopped", name);
    }

    /**
     * 헬스 체크 수행
     */
    private void performHealthCheck() {
        if (!client.isConnected()) {
            handleFailure(new IllegalStateException("Client is not connected"));
            return;
        }

        totalChecks.incrementAndGet();
        long startTime = System.currentTimeMillis();

        try {
            Message heartbeat = heartbeatMessageSupplier.get();
            Message response = client.send(heartbeat, config.getTimeoutMillis());

            long latency = System.currentTimeMillis() - startTime;
            handleSuccess(latency);

        } catch (Exception e) {
            handleFailure(e);
        }
    }

    /**
     * 헬스 체크 성공 처리
     */
    private void handleSuccess(long latencyMillis) {
        lastSuccessTime.set(System.currentTimeMillis());
        int previousFailures = consecutiveFailures.getAndSet(0);

        log.debug("HealthCheck '{}' succeeded, latency: {}ms", name, latencyMillis);

        // 이전에 unhealthy 상태였다가 복구된 경우
        if (!healthy.getAndSet(true) || previousFailures >= config.getFailureThreshold()) {
            log.info("HealthChecker '{}' connection recovered", name);
            notifyRecovered();
        }

        notifySuccess(latencyMillis);
    }

    /**
     * 헬스 체크 실패 처리
     */
    private void handleFailure(Throwable cause) {
        lastFailureTime.set(System.currentTimeMillis());
        totalFailures.incrementAndGet();
        int failures = consecutiveFailures.incrementAndGet();

        log.warn("HealthCheck '{}' failed (consecutive: {}): {}",
                name, failures, cause.getMessage());

        notifyFailure(failures, cause);

        // 실패 임계값 도달
        if (failures >= config.getFailureThreshold()) {
            if (healthy.getAndSet(false)) {
                log.error("HealthChecker '{}' detected unhealthy connection after {} consecutive failures",
                        name, failures);
                notifyUnhealthy();

                // 자동 재연결 시도
                if (config.isAutoReconnect()) {
                    tryReconnect();
                }
            }
        }
    }

    /**
     * 재연결 시도
     */
    private void tryReconnect() {
        try {
            log.info("HealthChecker '{}' attempting reconnection...", name);
            client.disconnect();
            client.connect();
            log.info("HealthChecker '{}' reconnection successful", name);
        } catch (Exception e) {
            log.error("HealthChecker '{}' reconnection failed: {}", name, e.getMessage());
        }
    }

    /**
     * 리스너 등록
     */
    public void addListener(HealthCheckListener listener) {
        listeners.add(listener);
    }

    /**
     * 리스너 제거
     */
    public void removeListener(HealthCheckListener listener) {
        listeners.remove(listener);
    }

    private void notifySuccess(long latencyMillis) {
        for (HealthCheckListener listener : listeners) {
            try {
                listener.onHealthCheckSuccess(latencyMillis);
            } catch (Exception e) {
                log.warn("Error notifying listener", e);
            }
        }
    }

    private void notifyFailure(int failures, Throwable cause) {
        for (HealthCheckListener listener : listeners) {
            try {
                listener.onHealthCheckFailure(failures, cause);
            } catch (Exception e) {
                log.warn("Error notifying listener", e);
            }
        }
    }

    private void notifyUnhealthy() {
        for (HealthCheckListener listener : listeners) {
            try {
                listener.onConnectionUnhealthy();
            } catch (Exception e) {
                log.warn("Error notifying listener", e);
            }
        }
    }

    private void notifyRecovered() {
        for (HealthCheckListener listener : listeners) {
            try {
                listener.onConnectionRecovered();
            } catch (Exception e) {
                log.warn("Error notifying listener", e);
            }
        }
    }

    // Getters

    public String getName() {
        return name;
    }

    public boolean isRunning() {
        return running.get();
    }

    public boolean isHealthy() {
        return healthy.get();
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures.get();
    }

    public long getLastSuccessTime() {
        return lastSuccessTime.get();
    }

    public long getLastFailureTime() {
        return lastFailureTime.get();
    }

    /**
     * 통계 정보
     */
    public Stats getStats() {
        return new Stats(
                totalChecks.get(),
                totalFailures.get(),
                consecutiveFailures.get(),
                healthy.get(),
                lastSuccessTime.get(),
                lastFailureTime.get()
        );
    }

    @Override
    public void close() {
        stop();
    }

    /**
     * 헬스 체크 통계
     */
    public record Stats(
            long totalChecks,
            long totalFailures,
            int consecutiveFailures,
            boolean healthy,
            long lastSuccessTime,
            long lastFailureTime
    ) {
        public double getSuccessRate() {
            if (totalChecks == 0) return 1.0;
            return (double) (totalChecks - totalFailures) / totalChecks;
        }

        @Override
        public String toString() {
            return String.format("Stats[healthy=%s, checks=%d, failures=%d, consecutiveFailures=%d, successRate=%.2f%%]",
                    healthy, totalChecks, totalFailures, consecutiveFailures, getSuccessRate() * 100);
        }
    }
}
