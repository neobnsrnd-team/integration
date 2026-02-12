package demo.mci.card.biz;

import lombok.extern.slf4j.Slf4j;
import springware.mci.server.biz.BizRegistry;

/**
 * 카드 Biz 컴포넌트 레지스트리
 * 카드 도메인의 모든 Biz 컴포넌트를 등록
 */
@Slf4j
public class CardBizRegistry {

    private final BizRegistry bizRegistry;

    public CardBizRegistry() {
        this.bizRegistry = new BizRegistry();
        registerAll();
    }

    public CardBizRegistry(BizRegistry bizRegistry) {
        this.bizRegistry = bizRegistry;
        registerAll();
    }

    private void registerAll() {
        bizRegistry.register(new CardListBiz());
        bizRegistry.register(new CardUsageHistoryBiz());

        log.info("Registered {} Card Biz components", bizRegistry.size());
    }

    public BizRegistry getBizRegistry() {
        return bizRegistry;
    }
}
