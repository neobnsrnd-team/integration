package springware.mci.server.biz;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Biz 구현체 레지스트리
 * 메시지 코드별로 Biz 구현체를 관리
 */
@Slf4j
public class BizRegistry {

    private final Map<String, Biz> bizMap = new ConcurrentHashMap<>();
    private Biz defaultBiz;

    /**
     * Biz 구현체 등록
     *
     * @param biz Biz 구현체
     */
    public void register(Biz biz) {
        String messageCode = biz.getMessageCode();
        bizMap.put(messageCode, biz);
        log.debug("Registered Biz for message code: {}", messageCode);
    }

    /**
     * 메시지 코드로 Biz 구현체 조회
     *
     * @param messageCode 메시지 코드
     * @return Biz 구현체 (없으면 defaultBiz, defaultBiz도 없으면 null)
     */
    public Biz getBiz(String messageCode) {
        Biz biz = bizMap.get(messageCode);
        if (biz == null && defaultBiz != null) {
            return defaultBiz;
        }
        return biz;
    }

    /**
     * 기본 Biz 구현체 설정
     * 등록되지 않은 메시지 코드에 대해 사용
     *
     * @param defaultBiz 기본 Biz 구현체
     */
    public void setDefaultBiz(Biz defaultBiz) {
        this.defaultBiz = defaultBiz;
        log.debug("Set default Biz");
    }

    /**
     * 등록된 Biz 구현체 존재 여부 확인
     *
     * @param messageCode 메시지 코드
     * @return 존재 여부
     */
    public boolean hasBiz(String messageCode) {
        return bizMap.containsKey(messageCode);
    }

    /**
     * 모든 Biz 구현체 제거
     */
    public void clear() {
        bizMap.clear();
        defaultBiz = null;
    }

    /**
     * 등록된 Biz 구현체 수 반환
     *
     * @return 등록된 Biz 수
     */
    public int size() {
        return bizMap.size();
    }
}
