package demo.mci.integration;

import demo.mci.biz.AccountRepository;
import demo.mci.common.DemoConstants;
import demo.mci.common.DemoMessageCodes;
import demo.mci.http.HttpDemoClient;
import demo.mci.http.HttpDemoServer;
import org.junit.jupiter.api.*;
import springware.mci.common.core.Message;

import java.io.IOException;
import java.net.ServerSocket;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HTTP 클라이언트-서버 통합 테스트
 */
@DisplayName("HTTP Client-Server Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HttpIntegrationTest {

    private static HttpDemoServer server;
    private static HttpDemoClient client;
    private static int testPort;
    private static AccountRepository accountRepository;

    @BeforeAll
    static void setUpAll() throws Exception {
        // 사용 가능한 포트 찾기
        testPort = findAvailablePort();

        // 서버 시작
        server = new HttpDemoServer(testPort);
        server.start();

        // 서버 시작 대기
        Thread.sleep(500);

        // 클라이언트 연결
        client = new HttpDemoClient("localhost", testPort);
        client.connect();

        // AccountRepository 참조
        accountRepository = AccountRepository.getInstance();
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

    @BeforeEach
    void setUp() {
        // 테스트 데이터 리셋
        accountRepository.setBalance("1234567890123456789", 1000000L);
        accountRepository.setBalance("9876543210987654321", 500000L);
        accountRepository.setBalance("1111222233334444555", 2500000L);
    }

    private static int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    // ==================== Balance Inquiry Tests ====================

    @Test
    @Order(1)
    @DisplayName("HTTP 잔액 조회 성공")
    void balanceInquiry_success() {
        // when
        Message response = client.balanceInquiry("1234567890123456789");

        // then
        assertThat(response).isNotNull();
        assertThat(response.getMessageCode()).isEqualTo(DemoMessageCodes.BALANCE_INQUIRY_RES);
        assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);
        assertThat(response.getLong("balance")).isEqualTo(1000000L);
        assertThat(response.getString("accountNo")).isEqualTo("1234567890123456789");
    }

    @Test
    @Order(2)
    @DisplayName("HTTP 존재하지 않는 계좌 잔액 조회")
    void balanceInquiry_invalidAccount() {
        // when
        Message response = client.balanceInquiry("9999999999999999999");

        // then
        assertThat(response).isNotNull();
        assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_INVALID_ACCOUNT);
        assertThat(response.getLong("balance")).isEqualTo(0L);
    }

    @Test
    @Order(3)
    @DisplayName("HTTP 다른 계좌 잔액 조회")
    void balanceInquiry_anotherAccount() {
        // when
        Message response = client.balanceInquiry("9876543210987654321");

        // then
        assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);
        assertThat(response.getLong("balance")).isEqualTo(500000L);
    }

    // ==================== Transfer Tests ====================

    @Test
    @Order(10)
    @DisplayName("HTTP 이체 성공")
    void transfer_success() {
        // when
        Message response = client.transfer("1234567890123456789", "9876543210987654321", 100000L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getMessageCode()).isEqualTo(DemoMessageCodes.TRANSFER_RES);
        assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);
        assertThat(response.getLong("fromBalance")).isEqualTo(900000L);

        // 잔액 확인
        assertThat(accountRepository.getBalance("1234567890123456789")).isEqualTo(900000L);
        assertThat(accountRepository.getBalance("9876543210987654321")).isEqualTo(600000L);
    }

    @Test
    @Order(11)
    @DisplayName("HTTP 잔액 부족 이체 실패")
    void transfer_insufficientBalance() {
        // when
        Message response = client.transfer("1234567890123456789", "9876543210987654321", 2000000L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_INSUFFICIENT_BALANCE);
        assertThat(response.getLong("fromBalance")).isEqualTo(1000000L);

        // 잔액 변경 없음
        assertThat(accountRepository.getBalance("1234567890123456789")).isEqualTo(1000000L);
    }

    @Test
    @Order(12)
    @DisplayName("HTTP 출금 계좌 없음 이체 실패")
    void transfer_invalidFromAccount() {
        // when
        Message response = client.transfer("9999999999999999999", "9876543210987654321", 100000L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_INVALID_ACCOUNT);
        assertThat(response.getLong("fromBalance")).isEqualTo(0L);
    }

    @Test
    @Order(13)
    @DisplayName("HTTP 연속 이체 처리")
    void transfer_consecutive() {
        // 첫 번째 이체
        Message response1 = client.transfer("1234567890123456789", "9876543210987654321", 100000L);
        assertThat(response1.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);
        assertThat(response1.getLong("fromBalance")).isEqualTo(900000L);

        // 두 번째 이체
        Message response2 = client.transfer("1234567890123456789", "9876543210987654321", 200000L);
        assertThat(response2.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);
        assertThat(response2.getLong("fromBalance")).isEqualTo(700000L);

        // 최종 잔액 확인
        assertThat(accountRepository.getBalance("1234567890123456789")).isEqualTo(700000L);
        assertThat(accountRepository.getBalance("9876543210987654321")).isEqualTo(800000L);
    }

    // ==================== Echo Tests ====================

    @Test
    @Order(20)
    @DisplayName("HTTP 에코 응답 확인")
    void echo_success() {
        // when
        Message response = client.echo("Hello, HTTP Integration Test!");

        // then
        assertThat(response).isNotNull();
        assertThat(response.getMessageCode()).isEqualTo(DemoMessageCodes.ECHO_RES);
        assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);
        assertThat(response.getString("echoData")).isEqualTo("Hello, HTTP Integration Test!");
    }

    @Test
    @Order(21)
    @DisplayName("HTTP 빈 에코 데이터")
    void echo_emptyData() {
        // when
        Message response = client.echo("");

        // then
        assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);
        assertThat(response.getString("echoData")).isEqualTo("");
    }

    @Test
    @Order(22)
    @DisplayName("HTTP 특수문자 에코")
    void echo_specialCharacters() {
        // given
        String specialData = "Test!@#$%^&*()";

        // when
        Message response = client.echo(specialData);

        // then
        assertThat(response.getString("echoData")).isEqualTo(specialData);
    }

    @Test
    @Order(23)
    @DisplayName("HTTP 한글 에코")
    void echo_korean() {
        // given
        String koreanData = "안녕하세요 HTTP 테스트";

        // when
        Message response = client.echo(koreanData);

        // then
        assertThat(response.getString("echoData")).isEqualTo(koreanData);
    }

    // ==================== Heartbeat Tests ====================

    @Test
    @Order(30)
    @DisplayName("HTTP 하트비트 응답 확인")
    void heartbeat_success() {
        // when
        Message response = client.heartbeat();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getMessageCode()).isEqualTo(DemoMessageCodes.HEARTBEAT_RES);
        assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);
    }

    @Test
    @Order(31)
    @DisplayName("HTTP 연속 하트비트")
    void heartbeat_consecutive() {
        for (int i = 0; i < 5; i++) {
            Message response = client.heartbeat();
            assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);
        }
    }

    // ==================== Mixed Scenario Tests ====================

    @Test
    @Order(40)
    @DisplayName("HTTP 복합 시나리오: 잔액조회 -> 이체 -> 잔액확인")
    void mixedScenario_balanceTransferBalance() {
        // 초기 잔액 조회
        Message balance1 = client.balanceInquiry("1234567890123456789");
        assertThat(balance1.getLong("balance")).isEqualTo(1000000L);

        // 이체 수행
        Message transfer = client.transfer("1234567890123456789", "9876543210987654321", 150000L);
        assertThat(transfer.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);

        // 이체 후 잔액 확인
        Message balance2 = client.balanceInquiry("1234567890123456789");
        assertThat(balance2.getLong("balance")).isEqualTo(850000L);

        // 수신 계좌 잔액 확인
        Message balance3 = client.balanceInquiry("9876543210987654321");
        assertThat(balance3.getLong("balance")).isEqualTo(650000L);
    }

    @Test
    @Order(41)
    @DisplayName("HTTP 복합 시나리오: 에코 + 하트비트 혼합")
    void mixedScenario_echoAndHeartbeat() {
        // 에코 -> 하트비트 -> 에코
        Message echo1 = client.echo("First");
        assertThat(echo1.getString("echoData")).isEqualTo("First");

        Message heartbeat = client.heartbeat();
        assertThat(heartbeat.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);

        Message echo2 = client.echo("Second");
        assertThat(echo2.getString("echoData")).isEqualTo("Second");
    }

    @Test
    @Order(42)
    @DisplayName("HTTP 복합 시나리오: 전체 거래 흐름")
    void mixedScenario_fullFlow() {
        // 1. 하트비트로 연결 확인
        assertThat(client.heartbeat().getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);

        // 2. 출금 계좌 잔액 조회
        assertThat(client.balanceInquiry("1234567890123456789").getLong("balance")).isEqualTo(1000000L);

        // 3. 입금 계좌 잔액 조회
        assertThat(client.balanceInquiry("9876543210987654321").getLong("balance")).isEqualTo(500000L);

        // 4. 이체 수행
        Message transfer = client.transfer("1234567890123456789", "9876543210987654321", 300000L);
        assertThat(transfer.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);
        assertThat(transfer.getLong("fromBalance")).isEqualTo(700000L);

        // 5. 양쪽 계좌 잔액 재확인
        assertThat(client.balanceInquiry("1234567890123456789").getLong("balance")).isEqualTo(700000L);
        assertThat(client.balanceInquiry("9876543210987654321").getLong("balance")).isEqualTo(800000L);

        // 6. 에코 확인
        assertThat(client.echo("HTTP Transaction Complete").getString("echoData")).isEqualTo("HTTP Transaction Complete");
    }

    // ==================== JSON/HTTP Specific Tests ====================

    @Test
    @Order(50)
    @DisplayName("HTTP 응답에 JSON 필드 정상 포함")
    void jsonResponse_containsExpectedFields() {
        // when
        Message response = client.balanceInquiry("1234567890123456789");

        // then - 응답에 필요한 모든 필드가 포함되어 있어야 함
        assertThat(response.getFields()).containsKey("rspCode");
        assertThat(response.getFields()).containsKey("balance");
        assertThat(response.getFields()).containsKey("accountNo");
    }

    @Test
    @Order(51)
    @DisplayName("HTTP 대량 요청 처리")
    void bulkRequests() {
        // given
        int requestCount = 20;

        // when/then
        for (int i = 0; i < requestCount; i++) {
            Message response = client.heartbeat();
            assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);
        }
    }
}
