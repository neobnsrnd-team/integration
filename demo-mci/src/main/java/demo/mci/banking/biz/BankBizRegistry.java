package demo.mci.banking.biz;

import lombok.extern.slf4j.Slf4j;
import springware.mci.server.biz.BizRegistry;

/**
 * 뱅킹 Biz 컴포넌트 레지스트리
 * 뱅킹 도메인의 모든 Biz 컴포넌트를 등록
 */
@Slf4j
public class BankBizRegistry {

    private final BizRegistry bizRegistry;

    public BankBizRegistry() {
        this.bizRegistry = new BizRegistry();
        registerAll();
    }

    public BankBizRegistry(BizRegistry bizRegistry) {
        this.bizRegistry = bizRegistry;
        registerAll();
    }

    private void registerAll() {
        bizRegistry.register(new BalanceInquiryBiz());
        bizRegistry.register(new TransferBiz());
        bizRegistry.register(new TransactionHistoryBiz());
        bizRegistry.register(new AccountInquiryBiz());
        bizRegistry.register(new EchoBiz());
        bizRegistry.register(new HeartbeatBiz());

        log.info("Registered {} Bank Biz components", bizRegistry.size());
    }

    public BizRegistry getBizRegistry() {
        return bizRegistry;
    }
}
