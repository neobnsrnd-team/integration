package springware.mci.common.exception;

/**
 * 프로토콜 처리 관련 예외
 */
public class ProtocolException extends MciException {

    public ProtocolException(String errorMessage) {
        super("MCI_PROTOCOL_001", errorMessage);
    }

    public ProtocolException(String errorMessage, Throwable cause) {
        super("MCI_PROTOCOL_001", errorMessage, cause);
    }

    public ProtocolException(String errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }
}
