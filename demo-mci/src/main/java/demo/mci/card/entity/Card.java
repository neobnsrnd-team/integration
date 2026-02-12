package demo.mci.card.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * 카드 정보 클래스 (데모용)
 */
@Getter
@Builder
public class Card {

    private final String cardNo;          // 16자리 카드번호
    private final String cardName;        // 카드명 (예: "신한카드 Deep Dream")
    private final String cardType;        // 01:신용, 02:체크, 03:선불
    private final String holderName;      // 카드 소유자명
    private final String expiryDate;      // 유효기간 (MMYY)
    private final String status;          // 1:정상, 2:정지, 9:해지
    private final long creditLimit;       // 신용한도
    @Setter
    private long usedAmount;              // 사용금액
    private final String customerId;      // 고객ID (계좌와 연동)

    public long getAvailableLimit() {
        return creditLimit - usedAmount;
    }

    public void addUsedAmount(long amount) {
        this.usedAmount += amount;
    }
}
