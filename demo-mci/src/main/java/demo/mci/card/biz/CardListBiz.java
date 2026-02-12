package demo.mci.card.biz;

import demo.mci.card.entity.Card;
import demo.mci.card.entity.CardRepository;
import demo.mci.common.DemoConstants;
import demo.mci.common.DemoMessageCodes;
import lombok.extern.slf4j.Slf4j;
import springware.mci.common.core.Message;
import springware.mci.common.core.MessageType;
import springware.mci.server.core.MessageContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 카드목록조회 비즈니스 로직
 */
@Slf4j
public class CardListBiz extends AbstractCardBiz {

    private final CardRepository cardRepository = CardRepository.getInstance();

    @Override
    public String getMessageCode() {
        return DemoMessageCodes.CARD_LIST_REQ;
    }

    @Override
    public Message execute(Message request, MessageContext context) {
        String customerId = request.getString("customerId");
        log.info("Card list inquiry for customer: {}", customerId);

        Message response = Message.builder()
                .messageCode(DemoMessageCodes.CARD_LIST_RES)
                .messageType(MessageType.RESPONSE)
                .build();

        // 요청 헤더 복사
        copyHeader(request, response);
        response.setField("msgCode", DemoMessageCodes.CARD_LIST_RES);

        // 고객의 카드 목록 조회
        List<Card> cards = cardRepository.getCardsByCustomerId(customerId);

        if (cards.isEmpty()) {
            response.setField("rspCode", DemoConstants.RSP_INVALID_CARD);
            response.setField("cardCount", 0);
            response.setField("cards", List.of());
            log.warn("No cards found for customer: {}", customerId);
            return response;
        }

        response.setField("rspCode", DemoConstants.RSP_SUCCESS);
        response.setField("cardCount", cards.size());

        // 반복부 데이터 변환
        List<Map<String, Object>> cardList = cards.stream()
                .map(this::cardToMap)
                .collect(Collectors.toList());
        response.setField("cards", cardList);

        log.info("Found {} cards for customer: {}", cards.size(), customerId);
        return response;
    }

    private Map<String, Object> cardToMap(Card card) {
        Map<String, Object> map = new HashMap<>();
        map.put("cardNo", card.getCardNo());
        map.put("cardName", card.getCardName());
        map.put("cardType", card.getCardType());
        map.put("status", card.getStatus());
        map.put("creditLimit", card.getCreditLimit());
        map.put("usedAmount", card.getUsedAmount());
        return map;
    }
}
