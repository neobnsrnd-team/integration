package springware.mci.common.exception;

/**
 * 연결 관련 예외
 */
public class ConnectionException extends MciException {

    public ConnectionException(String errorMessage) {
        super("MCI_CONN_001", errorMessage);
    }

    public ConnectionException(String errorMessage, Throwable cause) {
        super("MCI_CONN_001", errorMessage, cause);
    }

    public ConnectionException(String errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }

    public ConnectionException(String errorCode, String errorMessage, Throwable cause) {
        super(errorCode, errorMessage, cause);
    }
}
