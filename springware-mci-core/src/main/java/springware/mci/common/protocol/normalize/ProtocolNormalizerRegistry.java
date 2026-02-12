package springware.mci.common.protocol.normalize;

import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 프로토콜 정규화기 레지스트리
 * <p>
 * 제공자 ID별로 정규화기를 등록하고 조회합니다.
 * BizRegistry 패턴을 따릅니다.
 */
@Slf4j
public class ProtocolNormalizerRegistry {

    /**
     * 정규화기 맵 (제공자 ID -> 정규화기)
     */
    private final Map<String, ProtocolNormalizer> normalizerMap = new ConcurrentHashMap<>();

    /**
     * 기본 정규화기 (제공자 ID가 없는 경우 사용)
     */
    private ProtocolNormalizer defaultNormalizer;

    /**
     * 정규화기 등록
     *
     * @param normalizer 등록할 정규화기
     */
    public void register(ProtocolNormalizer normalizer) {
        if (normalizer == null) {
            throw new IllegalArgumentException("Normalizer cannot be null");
        }
        String providerId = normalizer.getProviderId();
        if (providerId == null || providerId.isEmpty()) {
            throw new IllegalArgumentException("Provider ID cannot be null or empty");
        }

        normalizerMap.put(providerId, normalizer);
        log.info("Registered ProtocolNormalizer: {}", providerId);
    }

    /**
     * 정규화기 조회
     *
     * @param providerId 제공자 ID
     * @return 정규화기 (없으면 기본 정규화기 반환)
     */
    public ProtocolNormalizer getNormalizer(String providerId) {
        ProtocolNormalizer normalizer = normalizerMap.get(providerId);
        if (normalizer == null && defaultNormalizer != null) {
            log.debug("Normalizer not found for provider '{}', using default", providerId);
            return defaultNormalizer;
        }
        return normalizer;
    }

    /**
     * 정규화기 존재 여부 확인
     *
     * @param providerId 제공자 ID
     * @return 존재 여부
     */
    public boolean hasNormalizer(String providerId) {
        return normalizerMap.containsKey(providerId);
    }

    /**
     * 정규화기 제거
     *
     * @param providerId 제공자 ID
     * @return 제거된 정규화기 (없으면 null)
     */
    public ProtocolNormalizer unregister(String providerId) {
        ProtocolNormalizer removed = normalizerMap.remove(providerId);
        if (removed != null) {
            log.info("Unregistered ProtocolNormalizer: {}", providerId);
        }
        return removed;
    }

    /**
     * 기본 정규화기 설정
     *
     * @param defaultNormalizer 기본 정규화기
     */
    public void setDefaultNormalizer(ProtocolNormalizer defaultNormalizer) {
        this.defaultNormalizer = defaultNormalizer;
        if (defaultNormalizer != null) {
            log.info("Set default ProtocolNormalizer: {}", defaultNormalizer.getProviderId());
        }
    }

    /**
     * 기본 정규화기 조회
     *
     * @return 기본 정규화기
     */
    public ProtocolNormalizer getDefaultNormalizer() {
        return defaultNormalizer;
    }

    /**
     * 등록된 모든 정규화기 조회
     *
     * @return 정규화기 컬렉션
     */
    public Collection<ProtocolNormalizer> getAllNormalizers() {
        return normalizerMap.values();
    }

    /**
     * 등록된 정규화기 수 조회
     *
     * @return 정규화기 수
     */
    public int size() {
        return normalizerMap.size();
    }

    /**
     * 모든 정규화기 제거
     */
    public void clear() {
        normalizerMap.clear();
        defaultNormalizer = null;
        log.info("Cleared all ProtocolNormalizers");
    }
}
