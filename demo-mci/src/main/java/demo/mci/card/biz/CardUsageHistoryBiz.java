package demo.mci.card.biz;

import demo.mci.card.entity.CardRepository;
import demo.mci.card.entity.CardUsageHistory;
import demo.mci.card.entity.CardUsageHistoryRepository;
import demo.mci.common.DemoConstants;
import demo.mci.common.DemoMessageCodes;
import lombok.extern.slf4j.Slf4j;
import springware.mci.common.core.Message;
import springware.mci.common.core.MessageType;
import springware.mci.server.core.MessageContext;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 카드사용내역조회 비즈니스 로직
 */
@Slf4j
public class CardUsageHistoryBiz extends AbstractCardBiz {

    private final CardRepository cardRepository = CardRepository.getInstance();
    private final CardUsageHistoryRepository historyRepository = CardUsageHistoryRepository.getInstance();

    @Override
    public String getMessageCode() {
        return DemoMessageCodes.CARD_USAGE_HISTORY_REQ;
    }

    @Override
    public Message execute(Message request, MessageContext context) {
        String cardNo = request.getString("cardNo");
        String fromDate = request.getString("fromDate");
        String toDate = request.getString("toDate");

        log.info("Card usage history inquiry - card: {}, period: {} ~ {}",
                maskCardNo(cardNo), fromDate, toDate);

        Message response = Message.builder()
                .messageCode(DemoMessageCodes.CARD_USAGE_HISTORY_RES)
                .messageType(MessageType.RESPONSE)
                .build();

        // 요청 헤더 복사
        copyHeader(request, response);
        response.setField("msgCode", DemoMessageCodes.CARD_USAGE_HISTORY_RES);
        response.setField("cardNo", cardNo);

        // 카드 존재 여부 확인
        if (!cardRepository.exists(cardNo)) {
            response.setField("rspCode", DemoConstants.RSP_INVALID_CARD);
            response.setField("recordCount", 0);
            response.setField("records", List.of());
            log.warn("Card not found: {}", maskCardNo(cardNo));
            return response;
        }

        // 사용내역 조회
        List<CardUsageHistory> histories = historyRepository.getUsageHistory(cardNo, fromDate, toDate);

        // 응답 설정
        response.setField("rspCode", DemoConstants.RSP_SUCCESS);
        response.setField("recordCount", histories.size());

        // 반복부 데이터 변환
        List<Map<String, Object>> records = histories.stream()
                .map(CardUsageHistory::toMap)
                .collect(Collectors.toList());
        response.setField("records", records);

        log.info("Found {} usage records for card: {}", histories.size(), maskCardNo(cardNo));
        return response;
    }
}
