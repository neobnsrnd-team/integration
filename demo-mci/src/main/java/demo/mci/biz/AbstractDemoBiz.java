package demo.mci.biz;

import springware.mci.common.core.Message;
import springware.mci.server.biz.Biz;

/**
 * 데모 Biz 추상 클래스
 * 공통 헬퍼 메서드 제공
 */
public abstract class AbstractDemoBiz implements Biz {

    /**
     * 헤더 복사
     */
    protected void copyHeader(Message from, Message to) {
        to.setField("orgCode", from.getString("orgCode"));
        to.setField("txDate", from.getString("txDate"));
        to.setField("txTime", from.getString("txTime"));
        to.setField("seqNo", from.getString("seqNo"));
        to.setField("filler", "");
    }

    /**
     * 계좌번호 마스킹
     */
    protected String maskAccount(String accountNo) {
        if (accountNo == null || accountNo.length() < 8) {
            return "****";
        }
        return accountNo.substring(0, 4) + "****" + accountNo.substring(accountNo.length() - 4);
    }
}
