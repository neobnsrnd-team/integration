package springware.mci.common.exception;

/**
 * 타임아웃 관련 예외
 */
public class TimeoutException extends MciException {

    public TimeoutException(String errorMessage) {
        super("MCI_TIMEOUT_001", errorMessage);
    }

    public TimeoutException(String errorMessage, Throwable cause) {
        super("MCI_TIMEOUT_001", errorMessage, cause);
    }

    public TimeoutException(long timeoutMillis) {
        super("MCI_TIMEOUT_001", "Request timed out after " + timeoutMillis + "ms");
    }
}
