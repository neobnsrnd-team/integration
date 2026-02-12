package demo.mci.external.normalizer;

import demo.mci.external.ExternalProviderConstants;
import springware.mci.common.protocol.normalize.AbstractProtocolNormalizer;

import java.util.Set;

/**
 * Provider A 프로토콜 정규화기
 * <p>
 * 응답 형식:
 * - 성공 코드: "00"
 * - 에러 코드: "99", "01", "02"
 * - 응답 코드 필드: resultCode
 * - 에러 메시지 필드: error_message
 */
public class ProviderANormalizer extends AbstractProtocolNormalizer {

    public ProviderANormalizer() {
        super(
                ExternalProviderConstants.PROVIDER_A,
                ExternalProviderConstants.PROVIDER_A_CODE_FIELD,
                ExternalProviderConstants.PROVIDER_A_ERROR_FIELD,
                Set.of(ExternalProviderConstants.PROVIDER_A_SUCCESS),
                ExternalProviderConstants.PROVIDER_A_ERROR_MAPPING
        );
    }
}
