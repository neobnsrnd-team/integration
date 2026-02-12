package springware.mci.common.response;

import lombok.Builder;
import lombok.Getter;
import springware.mci.common.core.Message;
import springware.mci.common.exception.ErrorResponseException;

/**
 * 외부 제공자 응답을 통합된 형식으로 정규화한 응답 래퍼
 * <p>
 * 외부 시스템의 다양한 에러 코드를 래핑하며, 원본 에러 코드와 메시지를
 * 그대로 보존합니다. 매핑되지 않은 에러 코드는 {@link ErrorResponseException}으로
 * 래핑하여 원본 정보에 접근할 수 있습니다.
 */
@Getter
@Builder
public class NormalizedResponse {

    /**
     * 제공자 ID
     */
    private final String providerId;

    /**
     * 정규화된 응답 상태
     */
    private final ResponseStatus status;

    /**
     * 내부 에러 코드 (정규화됨, 예: "0000", "1001")
     */
    private final String errorCode;

    /**
     * 에러 메시지
     */
    private final String errorMessage;

    /**
     * 원본 응답 (디버깅용)
     */
    private final Message originalResponse;

    /**
     * 원본 제공자 에러 코드
     */
    private final String originalErrorCode;

    /**
     * 성공 여부 확인
     */
    public boolean isSuccess() {
        return status == ResponseStatus.SUCCESS;
    }

    /**
     * 실패 여부 확인
     */
    public boolean isFailure() {
        return status == ResponseStatus.FAILURE;
    }

    /**
     * 원본 응답에서 필드 조회
     */
    @SuppressWarnings("unchecked")
    public <T> T getField(String name) {
        if (originalResponse == null) {
            return null;
        }
        return originalResponse.getField(name);
    }

    /**
     * 원본 응답에서 문자열 필드 조회
     */
    public String getString(String name) {
        if (originalResponse == null) {
            return null;
        }
        return originalResponse.getString(name);
    }

    /**
     * 원본 응답에서 정수 필드 조회
     */
    public Integer getInt(String name) {
        if (originalResponse == null) {
            return null;
        }
        return originalResponse.getInt(name);
    }

    /**
     * 원본 응답에서 Long 필드 조회
     */
    public Long getLong(String name) {
        if (originalResponse == null) {
            return null;
        }
        return originalResponse.getLong(name);
    }

    /**
     * 실패시 예외 발생
     * <p>
     * 외부 시스템의 원본 에러 코드와 메시지를 포함한 {@link ErrorResponseException}을 발생시킵니다.
     * 예외의 getter 메서드를 통해 원본 에러 정보에 접근할 수 있습니다.
     *
     * @return 성공시 자기 자신 반환
     * @throws ErrorResponseException 실패시 발생 (외부 에러 코드/메시지 포함)
     */
    public NormalizedResponse orThrow() {
        if (isFailure()) {
            throw new ErrorResponseException(
                    providerId,
                    originalErrorCode,
                    errorMessage,
                    originalResponse
            );
        }
        return this;
    }

    /**
     * 실패시 커스텀 메시지로 예외 발생
     * <p>
     * 외부 시스템의 원본 에러 코드를 포함한 {@link ErrorResponseException}을 발생시킵니다.
     *
     * @param customMessage 커스텀 에러 메시지
     * @return 성공시 자기 자신 반환
     * @throws ErrorResponseException 실패시 발생 (외부 에러 코드 포함)
     */
    public NormalizedResponse orThrow(String customMessage) {
        if (isFailure()) {
            throw new ErrorResponseException(
                    providerId,
                    originalErrorCode,
                    customMessage != null ? customMessage : errorMessage,
                    originalResponse
            );
        }
        return this;
    }

    /**
     * 외부 에러 코드 조회 (원본)
     * <p>
     * 외부 시스템이 반환한 원본 에러 코드를 반환합니다.
     * 내부 매핑 코드가 아닌 원본 코드가 필요할 때 사용합니다.
     *
     * @return 외부 시스템 에러 코드
     */
    public String getExternalErrorCode() {
        return originalErrorCode;
    }

    /**
     * 외부 에러 메시지 조회 (원본)
     *
     * @return 외부 시스템 에러 메시지
     */
    public String getExternalErrorMessage() {
        return errorMessage;
    }

    /**
     * 성공 응답 생성 팩토리 메서드
     */
    public static NormalizedResponse success(String providerId, Message originalResponse, String originalErrorCode) {
        return NormalizedResponse.builder()
                .providerId(providerId)
                .status(ResponseStatus.SUCCESS)
                .errorCode("0000")
                .errorMessage(null)
                .originalResponse(originalResponse)
                .originalErrorCode(originalErrorCode)
                .build();
    }

    /**
     * 실패 응답 생성 팩토리 메서드
     */
    public static NormalizedResponse failure(String providerId, String errorCode, String errorMessage,
                                             Message originalResponse, String originalErrorCode) {
        return NormalizedResponse.builder()
                .providerId(providerId)
                .status(ResponseStatus.FAILURE)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .originalResponse(originalResponse)
                .originalErrorCode(originalErrorCode)
                .build();
    }

    /**
     * 알 수 없음 응답 생성 팩토리 메서드
     */
    public static NormalizedResponse unknown(String providerId, Message originalResponse, String originalErrorCode) {
        return NormalizedResponse.builder()
                .providerId(providerId)
                .status(ResponseStatus.UNKNOWN)
                .errorCode(originalErrorCode)
                .errorMessage("Unknown response status")
                .originalResponse(originalResponse)
                .originalErrorCode(originalErrorCode)
                .build();
    }
}
