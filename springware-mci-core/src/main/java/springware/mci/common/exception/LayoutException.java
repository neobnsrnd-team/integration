package springware.mci.common.exception;

/**
 * 레이아웃 처리 관련 예외
 */
public class LayoutException extends MciException {

    public LayoutException(String errorMessage) {
        super("MCI_LAYOUT_001", errorMessage);
    }

    public LayoutException(String errorMessage, Throwable cause) {
        super("MCI_LAYOUT_001", errorMessage, cause);
    }

    public LayoutException(String errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }

    public LayoutException(String errorCode, String errorMessage, Throwable cause) {
        super(errorCode, errorMessage, cause);
    }
}
