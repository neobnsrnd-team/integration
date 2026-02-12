package springware.mci.common.protocol.normalize;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * 기본 프로토콜 정규화기 (내부 형식)
 * <p>
 * 내부 표준 형식을 사용하는 응답을 정규화합니다.
 * - 성공 코드: "0000"
 * - 응답 코드 필드: "rspCode"
 * - 에러 메시지 필드: "errMsg"
 */
public class DefaultProtocolNormalizer extends AbstractProtocolNormalizer {

    /**
     * 기본 제공자 ID
     */
    public static final String PROVIDER_ID = "DEFAULT";

    /**
     * 성공 코드
     */
    public static final String SUCCESS_CODE = "0000";

    /**
     * 응답 코드 필드명
     */
    public static final String RESPONSE_CODE_FIELD = "rspCode";

    /**
     * 에러 메시지 필드명
     */
    public static final String ERROR_MESSAGE_FIELD = "errMsg";

    public DefaultProtocolNormalizer() {
        super(
                PROVIDER_ID,
                RESPONSE_CODE_FIELD,
                ERROR_MESSAGE_FIELD,
                Set.of(SUCCESS_CODE),
                Collections.emptyMap()  // 내부 형식은 매핑 불필요
        );
    }

    public DefaultProtocolNormalizer(String providerId) {
        super(
                providerId,
                RESPONSE_CODE_FIELD,
                ERROR_MESSAGE_FIELD,
                Set.of(SUCCESS_CODE),
                Collections.emptyMap()
        );
    }

    public DefaultProtocolNormalizer(String providerId, Map<String, String> additionalMapping) {
        super(
                providerId,
                RESPONSE_CODE_FIELD,
                ERROR_MESSAGE_FIELD,
                Set.of(SUCCESS_CODE),
                additionalMapping
        );
    }

    @Override
    protected String getDefaultErrorCode(String externalErrorCode) {
        // 내부 형식이므로 원본 코드를 그대로 반환
        return externalErrorCode;
    }
}
