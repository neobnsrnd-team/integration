package springware.mci.common.exception;

import springware.common.exception.SpringwareException;

/**
 * MCI 기본 예외 클래스
 */
public class MciException extends SpringwareException {

    public MciException(String errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }

    public MciException(String errorCode, String errorMessage, Throwable cause) {
        super(errorCode, errorMessage, cause);
    }
}
