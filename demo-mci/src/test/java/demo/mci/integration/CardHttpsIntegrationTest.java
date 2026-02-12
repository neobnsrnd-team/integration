package demo.mci.integration;

import demo.mci.card.entity.CardRepository;
import demo.mci.card.https.CardHttpsClient;
import demo.mci.card.https.CardHttpsServer;
import demo.mci.common.DemoConstants;
import demo.mci.common.DemoMessageCodes;
import org.junit.jupiter.api.*;
import springware.mci.common.core.Message;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 카드 HTTPS 클라이언트-서버 통합 테스트
 */
@DisplayName("Card HTTPS Client-Server Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CardHttpsIntegrationTest {

    private static CardHttpsServer server;
    private static CardHttpsClient client;
    private static int testPort;
    private static CardRepository cardRepository;

    @BeforeAll
    static void setUpAll() throws Exception {
        // 사용 가능한 포트 찾기
        testPort = findAvailablePort();

        // 서버 시작 (자체 서명 인증서 사용)
        server = new CardHttpsServer(testPort);
        server.start();

        // 서버 시작 대기
        Thread.sleep(500);

        // 클라이언트 연결 (trustAll 모드)
        client = new CardHttpsClient("localhost", testPort, true);
        client.connect();

        // CardRepository 참조
        cardRepository = CardRepository.getInstance();
    }

    @AfterAll
    static void tearDownAll() {
        if (client != null) {
            client.disconnect();
        }
        if (server != null) {
            server.stop();
        }
    }

    private static int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    // ==================== Card List Tests ====================

    @Test
    @Order(1)
    @DisplayName("HTTPS 카드목록 조회 성공")
    @SuppressWarnings("unchecked")
    void cardList_success() {
        // when
        Message response = client.cardList("CUST001");

        // then
        assertThat(response).isNotNull();
        assertThat(response.getMessageCode()).isEqualTo(DemoMessageCodes.CARD_LIST_RES);
        assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);

        Object countObj = response.getField("cardCount");
        int count = countObj instanceof Number ? ((Number) countObj).intValue() : 0;
        assertThat(count).isEqualTo(3);

        List<Map<String, Object>> cards = (List<Map<String, Object>>) response.getField("cards");
        assertThat(cards).hasSize(3);
    }

    @Test
    @Order(2)
    @DisplayName("HTTPS 존재하지 않는 고객 카드목록 조회")
    void cardList_invalidCustomer() {
        // when
        Message response = client.cardList("CUST999");

        // then
        assertThat(response).isNotNull();
        assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_INVALID_CARD);
    }

    // ==================== Card Usage History Tests ====================

    @Test
    @Order(10)
    @DisplayName("HTTPS 카드사용내역 조회 성공")
    @SuppressWarnings("unchecked")
    void cardUsageHistory_success() {
        // when
        Message response = client.cardUsageHistory("1234567890123456", "20240101", "20240131");

        // then
        assertThat(response).isNotNull();
        assertThat(response.getMessageCode()).isEqualTo(DemoMessageCodes.CARD_USAGE_HISTORY_RES);
        assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);

        Object countObj = response.getField("recordCount");
        int count = countObj instanceof Number ? ((Number) countObj).intValue() : 0;
        assertThat(count).isGreaterThan(0);
    }

    @Test
    @Order(11)
    @DisplayName("HTTPS 존재하지 않는 카드 사용내역 조회")
    void cardUsageHistory_invalidCard() {
        // when
        Message response = client.cardUsageHistory("9999999999999999", "20240101", "20240131");

        // then
        assertThat(response).isNotNull();
        assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_INVALID_CARD);
    }

    // ==================== SSL/TLS Tests ====================

    @Test
    @Order(20)
    @DisplayName("HTTPS 암호화 통신 복합 시나리오")
    @SuppressWarnings("unchecked")
    void sslCommunication_mixedScenario() {
        // 1. 카드 목록 조회
        Message listResponse = client.cardList("CUST001");
        assertThat(listResponse.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);

        List<Map<String, Object>> cards = (List<Map<String, Object>>) listResponse.getField("cards");
        assertThat(cards).isNotEmpty();

        // 2. 사용내역 조회
        String cardNo = String.valueOf(cards.get(0).get("cardNo"));
        Message historyResponse = client.cardUsageHistory(cardNo, "20240101", "20240131");
        assertThat(historyResponse.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);
    }

    @Test
    @Order(21)
    @DisplayName("HTTPS 연속 요청 처리")
    void consecutiveRequests() {
        for (int i = 0; i < 10; i++) {
            Message response = client.cardList("CUST001");
            assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);
        }
    }
}
