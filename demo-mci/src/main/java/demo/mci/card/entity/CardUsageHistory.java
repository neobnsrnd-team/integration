package demo.mci.card.entity;

import lombok.Builder;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 카드 사용내역 클래스 (데모용)
 */
@Getter
@Builder
public class CardUsageHistory {

    private final String cardNo;          // 카드번호
    private final String usageDate;       // 사용일자 (YYYYMMDD)
    private final String usageTime;       // 사용시간 (HHmmss)
    private final String merchantName;    // 가맹점명
    private final String merchantCode;    // 가맹점코드
    private final String usageType;       // 1:일시불, 2:할부
    private final long amount;            // 사용금액
    private final String currency;        // 통화 (KRW, USD)
    private final String approvalNo;      // 승인번호
    private final String installmentMonths; // 할부개월수 (00: 일시불)

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("usageDate", usageDate);
        map.put("usageTime", usageTime);
        map.put("merchantName", merchantName);
        map.put("usageType", usageType);
        map.put("amount", amount);
        map.put("approvalNo", approvalNo);
        return map;
    }
}
