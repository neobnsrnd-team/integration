package demo.mci.card.biz;

import springware.mci.common.core.Message;
import springware.mci.server.biz.Biz;

/**
 * 카드 Biz 추상 클래스
 * 공통 헬퍼 메서드 제공
 */
public abstract class AbstractCardBiz implements Biz {

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
     * 카드번호 마스킹
     */
    protected String maskCardNo(String cardNo) {
        if (cardNo == null || cardNo.length() < 8) {
            return "****";
        }
        return cardNo.substring(0, 4) + "****" + cardNo.substring(cardNo.length() - 4);
    }
}
