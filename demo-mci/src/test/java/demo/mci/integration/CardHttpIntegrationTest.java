package demo.mci.integration;

import demo.mci.card.entity.CardRepository;
import demo.mci.card.http.CardHttpClient;
import demo.mci.card.http.CardHttpServer;
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
 * 카드 HTTP 클라이언트-서버 통합 테스트
 */
@DisplayName("Card HTTP Client-Server Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CardHttpIntegrationTest {

    private static CardHttpServer server;
    private static CardHttpClient client;
    private static int testPort;
    private static CardRepository cardRepository;

    @BeforeAll
    static void setUpAll() throws Exception {
        // 사용 가능한 포트 찾기
        testPort = findAvailablePort();

        // 서버 시작
        server = new CardHttpServer(testPort);
        server.start();

        // 서버 시작 대기
        Thread.sleep(500);

        // 클라이언트 연결
        client = new CardHttpClient("localhost", testPort);
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
    @DisplayName("HTTP 카드목록 조회 성공 - CUST001")
    @SuppressWarnings("unchecked")
    void cardList_success_cust001() {
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
    @DisplayName("HTTP 카드목록 조회 성공 - CUST002")
    @SuppressWarnings("unchecked")
    void cardList_success_cust002() {
        // when
        Message response = client.cardList("CUST002");

        // then
        assertThat(response).isNotNull();
        assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);

        Object countObj = response.getField("cardCount");
        int count = countObj instanceof Number ? ((Number) countObj).intValue() : 0;
        assertThat(count).isEqualTo(1);
    }

    @Test
    @Order(3)
    @DisplayName("HTTP 존재하지 않는 고객 카드목록 조회")
    void cardList_invalidCustomer() {
        // when
        Message response = client.cardList("CUST999");

        // then
        assertThat(response).isNotNull();
        assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_INVALID_CARD);

        Object countObj = response.getField("cardCount");
        int count = countObj instanceof Number ? ((Number) countObj).intValue() : 0;
        assertThat(count).isEqualTo(0);
    }

    // ==================== Card Usage History Tests ====================

    @Test
    @Order(10)
    @DisplayName("HTTP 카드사용내역 조회 성공")
    @SuppressWarnings("unchecked")
    void cardUsageHistory_success() {
        // when
        Message response = client.cardUsageHistory("1234567890123456", "20240101", "20240131");

        // then
        assertThat(response).isNotNull();
        assertThat(response.getMessageCode()).isEqualTo(DemoMessageCodes.CARD_USAGE_HISTORY_RES);
        assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);
        assertThat(response.getString("cardNo")).isEqualTo("1234567890123456");

        Object countObj = response.getField("recordCount");
        int count = countObj instanceof Number ? ((Number) countObj).intValue() : 0;
        assertThat(count).isGreaterThan(0);

        List<Map<String, Object>> records = (List<Map<String, Object>>) response.getField("records");
        assertThat(records).isNotEmpty();
    }

    @Test
    @Order(11)
    @DisplayName("HTTP 존재하지 않는 카드 사용내역 조회")
    void cardUsageHistory_invalidCard() {
        // when
        Message response = client.cardUsageHistory("9999999999999999", "20240101", "20240131");

        // then
        assertThat(response).isNotNull();
        assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_INVALID_CARD);

        Object countObj = response.getField("recordCount");
        int count = countObj instanceof Number ? ((Number) countObj).intValue() : 0;
        assertThat(count).isEqualTo(0);
    }

    // ==================== Mixed Scenario Tests ====================

    @Test
    @Order(20)
    @DisplayName("HTTP 복합 시나리오: 카드목록 -> 사용내역 조회")
    @SuppressWarnings("unchecked")
    void mixedScenario_cardListThenHistory() {
        // 1. 고객의 카드 목록 조회
        Message listResponse = client.cardList("CUST001");
        assertThat(listResponse.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);

        List<Map<String, Object>> cards = (List<Map<String, Object>>) listResponse.getField("cards");
        assertThat(cards).isNotEmpty();

        // 2. 첫 번째 카드로 사용내역 조회
        String cardNo = String.valueOf(cards.get(0).get("cardNo"));
        Message historyResponse = client.cardUsageHistory(cardNo, "20240101", "20240131");
        assertThat(historyResponse.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);
    }

    @Test
    @Order(21)
    @DisplayName("HTTP 대량 요청 처리")
    void bulkRequests() {
        // given
        int requestCount = 20;

        // when/then
        for (int i = 0; i < requestCount; i++) {
            Message response = client.cardList("CUST001");
            assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);
        }
    }

    @Test
    @Order(22)
    @DisplayName("HTTP JSON 응답 필드 포함 확인")
    void jsonResponse_containsExpectedFields() {
        // when
        Message response = client.cardList("CUST001");

        // then
        assertThat(response.getFields()).containsKey("rspCode");
        assertThat(response.getFields()).containsKey("cardCount");
        assertThat(response.getFields()).containsKey("cards");
    }
}
