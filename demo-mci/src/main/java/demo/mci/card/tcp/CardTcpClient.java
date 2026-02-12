package demo.mci.card.tcp;

import demo.mci.common.DemoConstants;
import demo.mci.common.DemoLayoutRegistry;
import demo.mci.common.DemoMessageCodes;
import lombok.extern.slf4j.Slf4j;
import springware.mci.client.config.ClientConfig;
import springware.mci.client.tcp.TcpClient;
import springware.mci.common.core.Message;
import springware.mci.common.core.MessageType;
import springware.mci.common.layout.LayoutManager;
import springware.mci.common.protocol.LengthFieldType;
import springware.mci.common.protocol.ProtocolConfig;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 카드 TCP 클라이언트
 */
@Slf4j
public class CardTcpClient {

    private final TcpClient client;
    private final AtomicLong sequenceNo = new AtomicLong(0);

    public CardTcpClient(String host, int port) {
        // 레이아웃 등록
        DemoLayoutRegistry registry = new DemoLayoutRegistry();
        LayoutManager layoutManager = registry.getLayoutManager();

        // 프로토콜 설정
        ProtocolConfig protocolConfig = ProtocolConfig.builder()
                .lengthFieldOffset(0)
                .lengthFieldLength(4)
                .lengthFieldType(LengthFieldType.BINARY_BIG_ENDIAN)
                .lengthIncludesHeader(false)
                .initialBytesToStrip(4)
                .build();

        // 클라이언트 설정
        ClientConfig config = ClientConfig.builder()
                .clientId("card-tcp-client")
                .host(host)
                .port(port)
                .protocolConfig(protocolConfig)
                .connectTimeout(DemoConstants.CONNECT_TIMEOUT)
                .readTimeout(DemoConstants.READ_TIMEOUT)
                .build();

        client = new TcpClient(config, layoutManager, new springware.mci.common.logging.DefaultMessageLogger());
    }

    /**
     * 연결
     */
    public void connect() {
        client.connect();
        log.info("Connected to card server");
    }

    /**
     * 연결 해제
     */
    public void disconnect() {
        client.disconnect();
        log.info("Disconnected from card server");
    }

    /**
     * 카드목록조회
     */
    @SuppressWarnings("unchecked")
    public Message cardList(String customerId) {
        Message request = Message.builder()
                .messageCode(DemoMessageCodes.CARD_LIST_REQ)
                .messageType(MessageType.REQUEST)
                .build();

        request.setField("msgCode", DemoMessageCodes.CARD_LIST_REQ);
        request.setField("orgCode", DemoConstants.ORG_CODE_CARD);
        request.setField("seqNo", String.format("%010d", sequenceNo.incrementAndGet()));
        request.setField("customerId", customerId);

        log.info("Sending card list inquiry for customer: {}", customerId);
        Message response = client.send(request);

        String rspCode = response.getString("rspCode");
        if (DemoConstants.RSP_SUCCESS.equals(rspCode)) {
            Object countObj = response.getField("cardCount");
            int count = countObj instanceof Number ? ((Number) countObj).intValue() : 0;
            log.info("Found {} cards", count);

            // 카드목록 출력
            List<Map<String, Object>> cards =
                    (List<Map<String, Object>>) response.getField("cards");
            if (cards != null) {
                for (int i = 0; i < cards.size(); i++) {
                    Map<String, Object> card = cards.get(i);
                    log.info("  [{}] {} ({}) - {} 한도: {}, 사용: {}",
                            i + 1,
                            maskCardNo(String.valueOf(card.get("cardNo"))),
                            card.get("cardName"),
                            getCardTypeName(String.valueOf(card.get("cardType"))),
                            card.get("creditLimit"),
                            card.get("usedAmount"));
                }
            }
        } else {
            log.warn("Card list inquiry failed: {}", rspCode);
        }

        return response;
    }

    /**
     * 카드사용내역조회
     */
    @SuppressWarnings("unchecked")
    public Message cardUsageHistory(String cardNo, String fromDate, String toDate) {
        Message request = Message.builder()
                .messageCode(DemoMessageCodes.CARD_USAGE_HISTORY_REQ)
                .messageType(MessageType.REQUEST)
                .build();

        request.setField("msgCode", DemoMessageCodes.CARD_USAGE_HISTORY_REQ);
        request.setField("orgCode", DemoConstants.ORG_CODE_CARD);
        request.setField("seqNo", String.format("%010d", sequenceNo.incrementAndGet()));
        request.setField("cardNo", cardNo);
        request.setField("fromDate", fromDate);
        request.setField("toDate", toDate);

        log.info("Sending card usage history inquiry: {} ({} ~ {})",
                maskCardNo(cardNo), fromDate, toDate);
        Message response = client.send(request);

        String rspCode = response.getString("rspCode");
        if (DemoConstants.RSP_SUCCESS.equals(rspCode)) {
            Object countObj = response.getField("recordCount");
            int count = countObj instanceof Number ? ((Number) countObj).intValue() : 0;
            log.info("Found {} usage records", count);

            // 사용내역 출력
            List<Map<String, Object>> records =
                    (List<Map<String, Object>>) response.getField("records");
            if (records != null) {
                for (int i = 0; i < records.size(); i++) {
                    Map<String, Object> record = records.get(i);
                    String usageType = "1".equals(String.valueOf(record.get("usageType"))) ? "일시불" : "할부";
                    log.info("  [{}] {} {} {} {} {:>12}원 (승인: {})",
                            i + 1,
                            record.get("usageDate"),
                            record.get("usageTime"),
                            record.get("merchantName"),
                            usageType,
                            record.get("amount"),
                            record.get("approvalNo"));
                }
            }
        } else {
            log.warn("Card usage history inquiry failed: {}", rspCode);
        }

        return response;
    }

    private String getCardTypeName(String cardType) {
        if (cardType == null) return "Unknown";
        switch (cardType) {
            case "01": return "신용카드";
            case "02": return "체크카드";
            case "03": return "선불카드";
            default: return "기타";
        }
    }

    /**
     * 카드번호 마스킹
     */
    private String maskCardNo(String cardNo) {
        if (cardNo == null || cardNo.length() < 8) {
            return "****";
        }
        return cardNo.substring(0, 4) + "****" + cardNo.substring(cardNo.length() - 4);
    }

    /**
     * 메인 메서드 (테스트용)
     */
    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : DemoConstants.DEFAULT_HOST;
        int port = args.length > 1 ? Integer.parseInt(args[1]) : DemoConstants.DEFAULT_CARD_TCP_PORT;

        CardTcpClient cardClient = new CardTcpClient(host, port);

        try {
            cardClient.connect();

            // 카드목록조회
            cardClient.cardList("CUST001");

            // 카드사용내역조회
            cardClient.cardUsageHistory("1234567890123456", "20240101", "20240131");

        } finally {
            cardClient.disconnect();
        }
    }
}
