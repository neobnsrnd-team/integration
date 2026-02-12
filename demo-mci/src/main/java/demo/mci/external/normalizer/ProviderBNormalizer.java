package demo.mci.external.normalizer;

import demo.mci.external.ExternalProviderConstants;
import springware.mci.common.protocol.normalize.AbstractProtocolNormalizer;

import java.util.Set;

/**
 * Provider B 프로토콜 정규화기
 * <p>
 * 응답 형식:
 * - 성공 코드: "SUCCESS"
 * - 에러 코드: "FAIL", "ERROR", "ACCT_ERR"
 * - 응답 코드 필드: status
 * - 에러 메시지 필드: error_reason
 */
public class ProviderBNormalizer extends AbstractProtocolNormalizer {

    public ProviderBNormalizer() {
        super(
                ExternalProviderConstants.PROVIDER_B,
                ExternalProviderConstants.PROVIDER_B_CODE_FIELD,
                ExternalProviderConstants.PROVIDER_B_ERROR_FIELD,
                Set.of(ExternalProviderConstants.PROVIDER_B_SUCCESS),
                ExternalProviderConstants.PROVIDER_B_ERROR_MAPPING
        );
    }
}
