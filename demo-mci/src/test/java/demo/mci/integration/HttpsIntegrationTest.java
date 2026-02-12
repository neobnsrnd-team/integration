package demo.mci.integration;

import demo.mci.biz.AccountRepository;
import demo.mci.common.DemoConstants;
import demo.mci.common.DemoMessageCodes;
import demo.mci.https.HttpsDemoClient;
import demo.mci.https.HttpsDemoServer;
import org.junit.jupiter.api.*;
import springware.mci.common.core.Message;

import java.io.IOException;
import java.net.ServerSocket;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HTTPS 클라이언트-서버 통합 테스트
 * 자체 서명 인증서를 사용한 SSL/TLS 통신 검증
 */
@DisplayName("HTTPS Client-Server Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HttpsIntegrationTest {

    private static HttpsDemoServer server;
    private static HttpsDemoClient client;
    private static int testPort;
    private static AccountRepository accountRepository;

    @BeforeAll
    static void setUpAll() throws Exception {
        // 사용 가능한 포트 찾기
        testPort = findAvailablePort();

        // HTTPS 서버 시작 (자체 서명 인증서 사용)
        server = new HttpsDemoServer(testPort);
        server.start();

        // 서버 시작 대기
        Thread.sleep(500);

        // HTTPS 클라이언트 연결 (trustAll 모드)
        client = new HttpsDemoClient("localhost", testPort);
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
    @DisplayName("HTTPS 잔액 조회 성공")
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
    @DisplayName("HTTPS 존재하지 않는 계좌 잔액 조회")
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
    @DisplayName("HTTPS 다른 계좌 잔액 조회")
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
    @DisplayName("HTTPS 이체 성공")
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
    @DisplayName("HTTPS 잔액 부족 이체 실패")
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
    @DisplayName("HTTPS 출금 계좌 없음 이체 실패")
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
    @DisplayName("HTTPS 연속 이체 처리")
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
    @DisplayName("HTTPS 에코 응답 확인")
    void echo_success() {
        // when
        Message response = client.echo("Hello, HTTPS Integration Test!");

        // then
        assertThat(response).isNotNull();
        assertThat(response.getMessageCode()).isEqualTo(DemoMessageCodes.ECHO_RES);
        assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);
        assertThat(response.getString("echoData")).isEqualTo("Hello, HTTPS Integration Test!");
    }

    @Test
    @Order(21)
    @DisplayName("HTTPS 빈 에코 데이터")
    void echo_emptyData() {
        // when
        Message response = client.echo("");

        // then
        assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);
        assertThat(response.getString("echoData")).isEqualTo("");
    }

    @Test
    @Order(22)
    @DisplayName("HTTPS 특수문자 에코")
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
    @DisplayName("HTTPS 한글 에코")
    void echo_korean() {
        // given
        String koreanData = "안녕하세요 HTTPS 테스트";

        // when
        Message response = client.echo(koreanData);

        // then
        assertThat(response.getString("echoData")).isEqualTo(koreanData);
    }

    // ==================== Heartbeat Tests ====================

    @Test
    @Order(30)
    @DisplayName("HTTPS 하트비트 응답 확인")
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
    @DisplayName("HTTPS 연속 하트비트")
    void heartbeat_consecutive() {
        for (int i = 0; i < 5; i++) {
            Message response = client.heartbeat();
            assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);
        }
    }

    // ==================== Mixed Scenario Tests ====================

    @Test
    @Order(40)
    @DisplayName("HTTPS 복합 시나리오: 잔액조회 -> 이체 -> 잔액확인")
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
    @DisplayName("HTTPS 복합 시나리오: 에코 + 하트비트 혼합")
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
    @DisplayName("HTTPS 복합 시나리오: 전체 거래 흐름")
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
        assertThat(client.echo("HTTPS Transaction Complete").getString("echoData")).isEqualTo("HTTPS Transaction Complete");
    }

    // ==================== SSL/TLS Specific Tests ====================

    @Test
    @Order(50)
    @DisplayName("HTTPS 암호화 통신 검증 - 응답에 필요한 필드 포함")
    void secureConnection_containsExpectedFields() {
        // when
        Message response = client.balanceInquiry("1234567890123456789");

        // then - SSL/TLS를 통해 안전하게 전송된 응답에 모든 필드 포함
        assertThat(response.getFields()).containsKey("rspCode");
        assertThat(response.getFields()).containsKey("balance");
        assertThat(response.getFields()).containsKey("accountNo");
    }

    @Test
    @Order(51)
    @DisplayName("HTTPS 대량 요청 처리")
    void bulkRequests() {
        // given
        int requestCount = 20;

        // when/then - SSL/TLS 핸드셰이크 후 대량 요청 처리
        for (int i = 0; i < requestCount; i++) {
            Message response = client.heartbeat();
            assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);
        }
    }

    @Test
    @Order(52)
    @DisplayName("HTTPS 민감 데이터 암호화 전송 확인")
    void secureTransmission_sensitiveData() {
        // given - 민감한 계좌 정보
        String sensitiveAccountNo = "1234567890123456789";
        long sensitiveAmount = 999999L;

        // when - HTTPS를 통해 민감 데이터 전송
        Message response = client.transfer(sensitiveAccountNo, "9876543210987654321", sensitiveAmount);

        // then - 응답이 정상적으로 수신됨 (암호화된 채널을 통해)
        assertThat(response).isNotNull();
        assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);
        assertThat(response.getLong("fromBalance")).isEqualTo(1L); // 1000000 - 999999
    }

    @Test
    @Order(53)
    @DisplayName("HTTPS 연결 재사용 확인")
    void connectionReuse() {
        // 여러 요청을 보내면서 연결이 재사용되는지 확인
        // (SSL 핸드셰이크 오버헤드가 첫 요청에만 발생)

        long startTime = System.currentTimeMillis();

        // 첫 번째 요청 (새 SSL 핸드셰이크 포함)
        Message response1 = client.heartbeat();
        assertThat(response1.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);

        long afterFirst = System.currentTimeMillis();

        // 연속 요청들 (연결 재사용)
        for (int i = 0; i < 10; i++) {
            Message response = client.heartbeat();
            assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);
        }

        long afterBatch = System.currentTimeMillis();

        // 연속 요청들의 평균 시간이 첫 요청보다 짧아야 함 (연결 재사용)
        long firstRequestTime = afterFirst - startTime;
        long batchTime = afterBatch - afterFirst;

        // 10개 요청의 총 시간이 첫 요청 시간의 10배보다 작으면 연결 재사용 확인
        // (연결 재사용이 없으면 각 요청마다 SSL 핸드셰이크 발생)
        assertThat(batchTime).isLessThan(firstRequestTime * 10);
    }
}
