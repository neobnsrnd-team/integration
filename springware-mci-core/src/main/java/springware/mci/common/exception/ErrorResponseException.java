package springware.mci.common.exception;

import lombok.Getter;
import springware.mci.common.core.Message;

/**
 * 외부 시스템 에러 응답 예외
 * <p>
 * 외부 시스템은 다양한 에러 코드를 반환하며, 모든 코드를 내부 코드로 매핑하는 것은 불가능합니다.
 * 이 예외는 외부 시스템의 원본 에러 코드와 메시지를 그대로 래핑하여 제공합니다.
 */
@Getter
public class ErrorResponseException extends MciException {

    private static final String DEFAULT_ERROR_CODE = "MCI_EXT_ERROR";

    /**
     * 제공자 ID
     */
    private final String providerId;

    /**
     * 외부 시스템 에러 코드 (원본)
     */
    private final String externalErrorCode;

    /**
     * 외부 시스템 에러 메시지 (원본)
     */
    private final String externalErrorMessage;

    /**
     * 원본 응답 메시지
     */
    private final Message originalResponse;

    public ErrorResponseException(String providerId, String externalErrorCode, String externalErrorMessage) {
        super(DEFAULT_ERROR_CODE, formatMessage(providerId, externalErrorCode, externalErrorMessage));
        this.providerId = providerId;
        this.externalErrorCode = externalErrorCode;
        this.externalErrorMessage = externalErrorMessage;
        this.originalResponse = null;
    }

    public ErrorResponseException(String providerId, String externalErrorCode, String externalErrorMessage,
                                  Message originalResponse) {
        super(DEFAULT_ERROR_CODE, formatMessage(providerId, externalErrorCode, externalErrorMessage));
        this.providerId = providerId;
        this.externalErrorCode = externalErrorCode;
        this.externalErrorMessage = externalErrorMessage;
        this.originalResponse = originalResponse;
    }

    public ErrorResponseException(String providerId, String externalErrorCode, String externalErrorMessage,
                                  Throwable cause) {
        super(DEFAULT_ERROR_CODE, formatMessage(providerId, externalErrorCode, externalErrorMessage), cause);
        this.providerId = providerId;
        this.externalErrorCode = externalErrorCode;
        this.externalErrorMessage = externalErrorMessage;
        this.originalResponse = null;
    }

    public ErrorResponseException(String providerId, String externalErrorCode, String externalErrorMessage,
                                  Message originalResponse, Throwable cause) {
        super(DEFAULT_ERROR_CODE, formatMessage(providerId, externalErrorCode, externalErrorMessage), cause);
        this.providerId = providerId;
        this.externalErrorCode = externalErrorCode;
        this.externalErrorMessage = externalErrorMessage;
        this.originalResponse = originalResponse;
    }

    /**
     * 원본 응답에서 필드 조회
     */
    @SuppressWarnings("unchecked")
    public <T> T getOriginalField(String name) {
        if (originalResponse == null) {
            return null;
        }
        return originalResponse.getField(name);
    }

    /**
     * 원본 응답에서 문자열 필드 조회
     */
    public String getOriginalString(String name) {
        if (originalResponse == null) {
            return null;
        }
        return originalResponse.getString(name);
    }

    /**
     * 에러 코드가 특정 값과 일치하는지 확인
     */
    public boolean hasExternalErrorCode(String code) {
        return externalErrorCode != null && externalErrorCode.equals(code);
    }

    private static String formatMessage(String providerId, String externalErrorCode, String externalErrorMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(providerId).append("] ");
        sb.append("External error: ").append(externalErrorCode);
        if (externalErrorMessage != null && !externalErrorMessage.isEmpty()) {
            sb.append(" - ").append(externalErrorMessage);
        }
        return sb.toString();
    }
}
