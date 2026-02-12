package springware.mci.common.protocol.normalize;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import springware.mci.common.core.Message;
import springware.mci.common.response.NormalizedResponse;
import springware.mci.common.response.ResponseStatus;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 프로토콜 정규화기 추상 기본 클래스
 * <p>
 * 공통 정규화 로직을 제공하며, 하위 클래스에서 제공자별
 * 성공 코드, 에러 코드 필드명, 에러 메시지 필드명을 정의합니다.
 */
@Slf4j
@Getter
public abstract class AbstractProtocolNormalizer implements ProtocolNormalizer {

    /**
     * 제공자 ID
     */
    private final String providerId;

    /**
     * 응답 코드 필드명 (예: "resultCode", "status", "rspCode")
     */
    private final String responseCodeFieldName;

    /**
     * 에러 메시지 필드명 (예: "error_message", "error_reason", "errMsg")
     */
    private final String errorMessageFieldName;

    /**
     * 성공 코드 집합 (예: {"00"}, {"SUCCESS"}, {"0000"})
     */
    private final Set<String> successCodes;

    /**
     * 에러 코드 매핑 (외부 코드 -> 내부 코드)
     */
    private final Map<String, String> errorCodeMapping;

    protected AbstractProtocolNormalizer(String providerId,
                                         String responseCodeFieldName,
                                         String errorMessageFieldName,
                                         Set<String> successCodes,
                                         Map<String, String> errorCodeMapping) {
        this.providerId = providerId;
        this.responseCodeFieldName = responseCodeFieldName;
        this.errorMessageFieldName = errorMessageFieldName;
        this.successCodes = successCodes != null ? new HashSet<>(successCodes) : Collections.emptySet();
        this.errorCodeMapping = errorCodeMapping != null ? new HashMap<>(errorCodeMapping) : Collections.emptyMap();
    }

    @Override
    public NormalizedResponse normalize(Message externalResponse) {
        if (externalResponse == null) {
            log.warn("[{}] External response is null", providerId);
            return NormalizedResponse.builder()
                    .providerId(providerId)
                    .status(ResponseStatus.UNKNOWN)
                    .errorCode("9999")
                    .errorMessage("External response is null")
                    .originalResponse(null)
                    .originalErrorCode(null)
                    .build();
        }

        String originalErrorCode = extractErrorCode(externalResponse);
        String internalErrorCode = mapToInternalErrorCode(originalErrorCode);
        String errorMessage = extractErrorMessage(externalResponse);

        if (isSuccess(externalResponse)) {
            log.debug("[{}] Response normalized as SUCCESS. Original code: {}", providerId, originalErrorCode);
            return NormalizedResponse.success(providerId, externalResponse, originalErrorCode);
        }

        if (originalErrorCode == null) {
            log.warn("[{}] Response code is null, treating as UNKNOWN", providerId);
            return NormalizedResponse.unknown(providerId, externalResponse, originalErrorCode);
        }

        log.debug("[{}] Response normalized as FAILURE. Original code: {}, Internal code: {}, Message: {}",
                providerId, originalErrorCode, internalErrorCode, errorMessage);

        return NormalizedResponse.failure(
                providerId,
                internalErrorCode,
                errorMessage,
                externalResponse,
                originalErrorCode
        );
    }

    @Override
    public boolean isSuccess(Message externalResponse) {
        if (externalResponse == null) {
            return false;
        }
        String responseCode = extractErrorCode(externalResponse);
        return responseCode != null && successCodes.contains(responseCode);
    }

    @Override
    public String extractErrorCode(Message externalResponse) {
        if (externalResponse == null || responseCodeFieldName == null) {
            return null;
        }
        return externalResponse.getString(responseCodeFieldName);
    }

    @Override
    public String extractErrorMessage(Message externalResponse) {
        if (externalResponse == null || errorMessageFieldName == null) {
            return null;
        }
        return externalResponse.getString(errorMessageFieldName);
    }

    @Override
    public String mapToInternalErrorCode(String externalErrorCode) {
        if (externalErrorCode == null) {
            return "9999";
        }
        // 성공 코드는 "0000"으로 매핑
        if (successCodes.contains(externalErrorCode)) {
            return "0000";
        }
        // 매핑 테이블에서 찾기
        String internalCode = errorCodeMapping.get(externalErrorCode);
        if (internalCode != null) {
            return internalCode;
        }
        // 매핑이 없으면 기본값 또는 원본 반환
        return getDefaultErrorCode(externalErrorCode);
    }

    /**
     * 매핑되지 않은 에러 코드에 대한 기본값 반환
     * 하위 클래스에서 오버라이드 가능
     *
     * @param externalErrorCode 외부 에러 코드
     * @return 기본 내부 에러 코드
     */
    protected String getDefaultErrorCode(String externalErrorCode) {
        return "9999";
    }
}
