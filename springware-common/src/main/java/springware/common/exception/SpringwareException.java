package springware.common.exception;

import lombok.Getter;

/**
 * Springware 프레임워크 기본 예외 클래스
 */
@Getter
public class SpringwareException extends RuntimeException {

    private final String errorCode;
    private final String errorMessage;

    public SpringwareException(String errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public SpringwareException(String errorCode, String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return String.format("SpringwareException[code=%s, message=%s]", errorCode, errorMessage);
    }
}
