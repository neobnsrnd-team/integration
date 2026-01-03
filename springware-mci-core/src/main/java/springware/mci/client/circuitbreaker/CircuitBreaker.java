package springware.mci.client.circuitbreaker;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * 서킷 브레이커 구현
 *
 * 상태 전이:
 * - CLOSED -> OPEN: 실패 횟수가 임계값 도달
 * - OPEN -> HALF_OPEN: openTimeout 경과
 * - HALF_OPEN -> CLOSED: 성공 횟수가 임계값 도달
 * - HALF_OPEN -> OPEN: 실패 발생
 */
@Slf4j
public class CircuitBreaker {

    private final String name;
    private final CircuitBreakerConfig config;

    private final AtomicReference<CircuitBreakerState> state = new AtomicReference<>(CircuitBreakerState.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger halfOpenCallCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private final AtomicLong stateChangedTime = new AtomicLong(System.currentTimeMillis());

    // 통계
    private final AtomicLong totalCalls = new AtomicLong(0);
    private final AtomicLong totalFailures = new AtomicLong(0);
    private final AtomicLong totalSuccess = new AtomicLong(0);
    private final AtomicLong rejectedCalls = new AtomicLong(0);

    public CircuitBreaker(String name, CircuitBreakerConfig config) {
        this.name = name;
        this.config = config;
        config.validate();
    }

    public CircuitBreaker(String name) {
        this(name, CircuitBreakerConfig.defaultConfig());
    }

    /**
     * 서킷 브레이커를 통해 작업 실행
     */
    public <T> T execute(Supplier<T> supplier) {
        if (!config.isEnabled()) {
            return supplier.get();
        }

        if (!allowRequest()) {
            rejectedCalls.incrementAndGet();
            long remaining = getRemainingOpenTime();
            throw new CircuitBreakerOpenException(
                    String.format("CircuitBreaker '%s' is OPEN, remaining time: %dms", name, remaining),
                    state.get(), remaining);
        }

        totalCalls.incrementAndGet();

        try {
            T result = supplier.get();
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure(e);
            throw e;
        }
    }

    /**
     * 서킷 브레이커를 통해 Runnable 실행
     */
    public void execute(Runnable runnable) {
        execute(() -> {
            runnable.run();
            return null;
        });
    }

    /**
     * 요청 허용 여부 확인
     */
    public boolean allowRequest() {
        if (!config.isEnabled()) {
            return true;
        }

        CircuitBreakerState currentState = state.get();

        switch (currentState) {
            case CLOSED:
                return true;

            case OPEN:
                // 타임아웃 경과 확인
                if (isOpenTimeoutExpired()) {
                    if (transitionTo(CircuitBreakerState.OPEN, CircuitBreakerState.HALF_OPEN)) {
                        log.info("CircuitBreaker '{}' transitioned from OPEN to HALF_OPEN", name);
                        halfOpenCallCount.set(0);
                        successCount.set(0);
                    }
                    return allowHalfOpenRequest();
                }
                return false;

            case HALF_OPEN:
                return allowHalfOpenRequest();

            default:
                return false;
        }
    }

    /**
     * HALF_OPEN 상태에서 요청 허용 여부
     */
    private boolean allowHalfOpenRequest() {
        int currentCalls = halfOpenCallCount.incrementAndGet();
        return currentCalls <= config.getHalfOpenPermittedCalls();
    }

    /**
     * 성공 처리
     */
    public void onSuccess() {
        totalSuccess.incrementAndGet();

        CircuitBreakerState currentState = state.get();

        if (currentState == CircuitBreakerState.CLOSED) {
            failureCount.set(0);
        } else if (currentState == CircuitBreakerState.HALF_OPEN) {
            int successes = successCount.incrementAndGet();
            if (successes >= config.getSuccessThreshold()) {
                if (transitionTo(CircuitBreakerState.HALF_OPEN, CircuitBreakerState.CLOSED)) {
                    log.info("CircuitBreaker '{}' transitioned from HALF_OPEN to CLOSED after {} successes",
                            name, successes);
                    reset();
                }
            }
        }
    }

    /**
     * 실패 처리
     */
    public void onFailure(Throwable throwable) {
        totalFailures.incrementAndGet();
        lastFailureTime.set(System.currentTimeMillis());

        CircuitBreakerState currentState = state.get();

        if (currentState == CircuitBreakerState.CLOSED) {
            int failures = failureCount.incrementAndGet();
            if (failures >= config.getFailureThreshold()) {
                if (transitionTo(CircuitBreakerState.CLOSED, CircuitBreakerState.OPEN)) {
                    log.warn("CircuitBreaker '{}' transitioned from CLOSED to OPEN after {} failures: {}",
                            name, failures, throwable.getMessage());
                }
            }
        } else if (currentState == CircuitBreakerState.HALF_OPEN) {
            if (transitionTo(CircuitBreakerState.HALF_OPEN, CircuitBreakerState.OPEN)) {
                log.warn("CircuitBreaker '{}' transitioned from HALF_OPEN to OPEN due to failure: {}",
                        name, throwable.getMessage());
            }
        }
    }

    /**
     * 상태 전이
     */
    private boolean transitionTo(CircuitBreakerState from, CircuitBreakerState to) {
        boolean success = state.compareAndSet(from, to);
        if (success) {
            stateChangedTime.set(System.currentTimeMillis());
        }
        return success;
    }

    /**
     * OPEN 타임아웃 경과 확인
     */
    private boolean isOpenTimeoutExpired() {
        return System.currentTimeMillis() - stateChangedTime.get() >= config.getOpenTimeout();
    }

    /**
     * OPEN 상태 남은 시간
     */
    public long getRemainingOpenTime() {
        if (state.get() != CircuitBreakerState.OPEN) {
            return 0;
        }
        long elapsed = System.currentTimeMillis() - stateChangedTime.get();
        return Math.max(0, config.getOpenTimeout() - elapsed);
    }

    /**
     * 상태 초기화
     */
    public void reset() {
        failureCount.set(0);
        successCount.set(0);
        halfOpenCallCount.set(0);
        state.set(CircuitBreakerState.CLOSED);
        stateChangedTime.set(System.currentTimeMillis());
        log.info("CircuitBreaker '{}' has been reset", name);
    }

    /**
     * 강제로 OPEN 상태로 전환
     */
    public void forceOpen() {
        state.set(CircuitBreakerState.OPEN);
        stateChangedTime.set(System.currentTimeMillis());
        log.info("CircuitBreaker '{}' forced to OPEN", name);
    }

    /**
     * 강제로 CLOSED 상태로 전환
     */
    public void forceClosed() {
        reset();
    }

    // Getters

    public String getName() {
        return name;
    }

    public CircuitBreakerState getState() {
        // OPEN 상태에서 타임아웃 확인
        if (state.get() == CircuitBreakerState.OPEN && isOpenTimeoutExpired()) {
            transitionTo(CircuitBreakerState.OPEN, CircuitBreakerState.HALF_OPEN);
        }
        return state.get();
    }

    public CircuitBreakerConfig getConfig() {
        return config;
    }

    public int getFailureCount() {
        return failureCount.get();
    }

    public int getSuccessCount() {
        return successCount.get();
    }

    public long getLastFailureTime() {
        return lastFailureTime.get();
    }

    /**
     * 통계 정보
     */
    public Stats getStats() {
        return new Stats(
                totalCalls.get(),
                totalSuccess.get(),
                totalFailures.get(),
                rejectedCalls.get(),
                state.get(),
                failureCount.get()
        );
    }

    /**
     * 서킷 브레이커 통계
     */
    public record Stats(
            long totalCalls,
            long totalSuccess,
            long totalFailures,
            long rejectedCalls,
            CircuitBreakerState currentState,
            int currentFailureCount
    ) {
        public double getFailureRate() {
            if (totalCalls == 0) return 0.0;
            return (double) totalFailures / totalCalls;
        }

        @Override
        public String toString() {
            return String.format("Stats[state=%s, calls=%d, success=%d, failures=%d, rejected=%d, failureRate=%.2f%%]",
                    currentState, totalCalls, totalSuccess, totalFailures, rejectedCalls, getFailureRate() * 100);
        }
    }
}
