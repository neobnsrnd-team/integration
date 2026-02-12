package demo.mci.card.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 가상 카드 저장소 (데모용)
 */
public class CardRepository {

    private static final CardRepository INSTANCE = new CardRepository();

    private final Map<String, Card> cards = new ConcurrentHashMap<>();
    private final Map<String, List<Card>> cardsByCustomer = new ConcurrentHashMap<>();

    private CardRepository() {
        // 테스트 데이터 초기화
        addCard(Card.builder()
                .cardNo("1234567890123456")
                .cardName("신한카드 Deep Dream")
                .cardType("01")
                .holderName("홍길동")
                .expiryDate("1228")
                .status("1")
                .creditLimit(10000000L)
                .usedAmount(1500000L)
                .customerId("CUST001")
                .build());

        addCard(Card.builder()
                .cardNo("2345678901234567")
                .cardName("삼성카드 taptap O")
                .cardType("01")
                .holderName("홍길동")
                .expiryDate("0627")
                .status("1")
                .creditLimit(5000000L)
                .usedAmount(800000L)
                .customerId("CUST001")
                .build());

        addCard(Card.builder()
                .cardNo("3456789012345678")
                .cardName("현대카드 M")
                .cardType("02")
                .holderName("홍길동")
                .expiryDate("0329")
                .status("1")
                .creditLimit(0L)
                .usedAmount(0L)
                .customerId("CUST001")
                .build());

        addCard(Card.builder()
                .cardNo("4567890123456789")
                .cardName("KB국민카드 탄탄대로")
                .cardType("01")
                .holderName("김철수")
                .expiryDate("1126")
                .status("1")
                .creditLimit(20000000L)
                .usedAmount(5000000L)
                .customerId("CUST002")
                .build());

        addCard(Card.builder()
                .cardNo("5678901234567890")
                .cardName("롯데카드 LOCA")
                .cardType("01")
                .holderName("이영희")
                .expiryDate("0525")
                .status("2")
                .creditLimit(3000000L)
                .usedAmount(2900000L)
                .customerId("CUST003")
                .build());
    }

    private void addCard(Card card) {
        cards.put(card.getCardNo(), card);
        cardsByCustomer.computeIfAbsent(card.getCustomerId(), k -> new ArrayList<>()).add(card);
    }

    public static CardRepository getInstance() {
        return INSTANCE;
    }

    public Card getCard(String cardNo) {
        return cards.get(cardNo.trim());
    }

    public List<Card> getCardsByCustomerId(String customerId) {
        List<Card> customerCards = cardsByCustomer.get(customerId.trim());
        return customerCards != null ? new ArrayList<>(customerCards) : new ArrayList<>();
    }

    public boolean exists(String cardNo) {
        return cards.containsKey(cardNo.trim());
    }

    public void updateUsedAmount(String cardNo, long amount) {
        Card card = cards.get(cardNo.trim());
        if (card != null) {
            card.addUsedAmount(amount);
        }
    }
}
