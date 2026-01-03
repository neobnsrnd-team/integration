package springware.mci.client.circuitbreaker;

import springware.mci.common.exception.MciException;

/**
 * 서킷 브레이커가 열려있을 때 발생하는 예외
 */
public class CircuitBreakerOpenException extends MciException {

    private static final String ERROR_CODE = "CB_OPEN";

    private final CircuitBreakerState state;
    private final long remainingTime;

    public CircuitBreakerOpenException(String message) {
        super(ERROR_CODE, message);
        this.state = CircuitBreakerState.OPEN;
        this.remainingTime = 0;
    }

    public CircuitBreakerOpenException(String message, CircuitBreakerState state, long remainingTime) {
        super(ERROR_CODE, message);
        this.state = state;
        this.remainingTime = remainingTime;
    }

    /**
     * 서킷 브레이커 상태
     */
    public CircuitBreakerState getState() {
        return state;
    }

    /**
     * OPEN 상태 남은 시간 (밀리초)
     */
    public long getRemainingTime() {
        return remainingTime;
    }
}
