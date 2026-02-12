package demo.mci.external;

import demo.mci.external.normalizer.ProviderANormalizer;
import demo.mci.external.normalizer.ProviderBNormalizer;
import demo.mci.external.normalizer.ProviderCNormalizer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import springware.mci.common.protocol.normalize.DefaultProtocolNormalizer;
import springware.mci.common.protocol.normalize.ProtocolNormalizerRegistry;

/**
 * 외부 제공자 정규화기 레지스트리
 * <p>
 * 모든 외부 제공자 정규화기를 등록하고 관리합니다.
 */
@Slf4j
@Getter
public class ExternalProviderRegistry {

    private final ProtocolNormalizerRegistry normalizerRegistry;

    public ExternalProviderRegistry() {
        this.normalizerRegistry = new ProtocolNormalizerRegistry();
        registerAll();
    }

    private void registerAll() {
        // Provider A 등록
        normalizerRegistry.register(new ProviderANormalizer());

        // Provider B 등록
        normalizerRegistry.register(new ProviderBNormalizer());

        // Provider C 등록
        normalizerRegistry.register(new ProviderCNormalizer());

        // 기본 정규화기 설정 (내부 형식)
        normalizerRegistry.setDefaultNormalizer(new DefaultProtocolNormalizer());

        log.info("Registered {} ExternalProvider normalizers", normalizerRegistry.size());
    }
}
