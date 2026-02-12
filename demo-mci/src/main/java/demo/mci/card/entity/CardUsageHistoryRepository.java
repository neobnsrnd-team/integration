package demo.mci.card.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 가상 카드 사용내역 저장소 (데모용)
 */
public class CardUsageHistoryRepository {

    private static final CardUsageHistoryRepository INSTANCE = new CardUsageHistoryRepository();

    private final Map<String, List<CardUsageHistory>> historyByCard = new ConcurrentHashMap<>();

    private CardUsageHistoryRepository() {
        // 테스트 데이터 초기화 - 카드번호 "1234567890123456"
        String cardNo1 = "1234567890123456";
        addHistory(CardUsageHistory.builder()
                .cardNo(cardNo1)
                .usageDate("20240115")
                .usageTime("143025")
                .merchantName("스타벅스 강남점")
                .merchantCode("MC001")
                .usageType("1")
                .amount(6500L)
                .currency("KRW")
                .approvalNo("A123456789")
                .installmentMonths("00")
                .build());

        addHistory(CardUsageHistory.builder()
                .cardNo(cardNo1)
                .usageDate("20240116")
                .usageTime("120530")
                .merchantName("이마트 역삼점")
                .merchantCode("MC002")
                .usageType("1")
                .amount(87500L)
                .currency("KRW")
                .approvalNo("A123456790")
                .installmentMonths("00")
                .build());

        addHistory(CardUsageHistory.builder()
                .cardNo(cardNo1)
                .usageDate("20240118")
                .usageTime("184522")
                .merchantName("CGV 코엑스")
                .merchantCode("MC003")
                .usageType("1")
                .amount(28000L)
                .currency("KRW")
                .approvalNo("A123456791")
                .installmentMonths("00")
                .build());

        addHistory(CardUsageHistory.builder()
                .cardNo(cardNo1)
                .usageDate("20240120")
                .usageTime("153040")
                .merchantName("삼성전자 온라인몰")
                .merchantCode("MC004")
                .usageType("2")
                .amount(1200000L)
                .currency("KRW")
                .approvalNo("A123456792")
                .installmentMonths("06")
                .build());

        // 테스트 데이터 - 카드번호 "2345678901234567"
        String cardNo2 = "2345678901234567";
        addHistory(CardUsageHistory.builder()
                .cardNo(cardNo2)
                .usageDate("20240117")
                .usageTime("091512")
                .merchantName("GS25 삼성점")
                .merchantCode("MC005")
                .usageType("1")
                .amount(3200L)
                .currency("KRW")
                .approvalNo("B234567890")
                .installmentMonths("00")
                .build());

        addHistory(CardUsageHistory.builder()
                .cardNo(cardNo2)
                .usageDate("20240119")
                .usageTime("201030")
                .merchantName("배달의민족")
                .merchantCode("MC006")
                .usageType("1")
                .amount(32000L)
                .currency("KRW")
                .approvalNo("B234567891")
                .installmentMonths("00")
                .build());

        // 테스트 데이터 - 카드번호 "4567890123456789"
        String cardNo3 = "4567890123456789";
        addHistory(CardUsageHistory.builder()
                .cardNo(cardNo3)
                .usageDate("20240110")
                .usageTime("103000")
                .merchantName("현대백화점 무역센터점")
                .merchantCode("MC007")
                .usageType("2")
                .amount(2500000L)
                .currency("KRW")
                .approvalNo("C345678901")
                .installmentMonths("12")
                .build());

        addHistory(CardUsageHistory.builder()
                .cardNo(cardNo3)
                .usageDate("20240112")
                .usageTime("142200")
                .merchantName("SSG닷컴")
                .merchantCode("MC008")
                .usageType("1")
                .amount(156000L)
                .currency("KRW")
                .approvalNo("C345678902")
                .installmentMonths("00")
                .build());
    }

    private void addHistory(CardUsageHistory history) {
        historyByCard.computeIfAbsent(history.getCardNo(), k -> new ArrayList<>()).add(history);
    }

    public static CardUsageHistoryRepository getInstance() {
        return INSTANCE;
    }

    public List<CardUsageHistory> getUsageHistory(String cardNo, String fromDate, String toDate) {
        List<CardUsageHistory> histories = historyByCard.get(cardNo.trim());
        if (histories == null) {
            return new ArrayList<>();
        }

        return histories.stream()
                .filter(h -> h.getUsageDate().compareTo(fromDate) >= 0 && h.getUsageDate().compareTo(toDate) <= 0)
                .sorted((a, b) -> {
                    int dateCompare = b.getUsageDate().compareTo(a.getUsageDate());
                    return dateCompare != 0 ? dateCompare : b.getUsageTime().compareTo(a.getUsageTime());
                })
                .collect(Collectors.toList());
    }

    public void addUsageHistory(CardUsageHistory history) {
        historyByCard.computeIfAbsent(history.getCardNo(), k -> new ArrayList<>()).add(history);
    }
}
