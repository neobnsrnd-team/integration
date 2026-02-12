package springware.mci.common.exception;

import lombok.Getter;
import springware.mci.common.core.Message;

/**
 * 외부 제공자 호출 실패 예외
 */
@Getter
public class ExternalProviderException extends MciException {

    private static final String DEFAULT_ERROR_CODE = "MCI_PROVIDER_001";

    /**
     * 제공자 ID
     */
    private final String providerId;

    /**
     * 원본 응답 메시지
     */
    private final Message originalResponse;

    public ExternalProviderException(String providerId, String errorCode, String errorMessage) {
        super(errorCode != null ? errorCode : DEFAULT_ERROR_CODE, errorMessage);
        this.providerId = providerId;
        this.originalResponse = null;
    }

    public ExternalProviderException(String providerId, String errorCode, String errorMessage, Message originalResponse) {
        super(errorCode != null ? errorCode : DEFAULT_ERROR_CODE, errorMessage);
        this.providerId = providerId;
        this.originalResponse = originalResponse;
    }

    public ExternalProviderException(String providerId, String errorCode, String errorMessage, Throwable cause) {
        super(errorCode != null ? errorCode : DEFAULT_ERROR_CODE, errorMessage, cause);
        this.providerId = providerId;
        this.originalResponse = null;
    }

    public ExternalProviderException(String providerId, String errorCode, String errorMessage,
                                     Message originalResponse, Throwable cause) {
        super(errorCode != null ? errorCode : DEFAULT_ERROR_CODE, errorMessage, cause);
        this.providerId = providerId;
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
}
