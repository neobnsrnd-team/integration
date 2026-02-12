package demo.mci.external.normalizer;

import demo.mci.external.ExternalProviderConstants;
import springware.mci.common.protocol.normalize.AbstractProtocolNormalizer;

import java.util.Collections;
import java.util.Set;

/**
 * Provider C 프로토콜 정규화기
 * <p>
 * 응답 형식 (내부 형식과 동일):
 * - 성공 코드: "0000"
 * - 에러 코드: "9999", "1001", "1002"
 * - 응답 코드 필드: rspCode
 * - 에러 메시지 필드: errMsg
 */
public class ProviderCNormalizer extends AbstractProtocolNormalizer {

    public ProviderCNormalizer() {
        super(
                ExternalProviderConstants.PROVIDER_C,
                ExternalProviderConstants.PROVIDER_C_CODE_FIELD,
                ExternalProviderConstants.PROVIDER_C_ERROR_FIELD,
                Set.of(ExternalProviderConstants.PROVIDER_C_SUCCESS),
                Collections.emptyMap()  // 내부 형식과 동일하므로 매핑 불필요
        );
    }

    @Override
    protected String getDefaultErrorCode(String externalErrorCode) {
        // 내부 형식과 동일하므로 원본 코드를 그대로 반환
        return externalErrorCode;
    }
}
