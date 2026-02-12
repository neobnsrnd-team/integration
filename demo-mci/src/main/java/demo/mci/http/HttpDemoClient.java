package demo.mci.http;

import demo.mci.common.DemoConstants;
import demo.mci.common.DemoLayoutRegistry;
import demo.mci.common.DemoMessageCodes;
import lombok.extern.slf4j.Slf4j;
import springware.mci.client.config.ClientConfig;
import springware.mci.client.http.HttpClient;
import springware.mci.common.core.Message;
import springware.mci.common.core.MessageType;
import springware.mci.common.core.TransportType;
import springware.mci.common.layout.LayoutManager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * HTTP 데모 클라이언트
 * REST API를 통한 메시지 송수신
 */
@Slf4j
public class HttpDemoClient {

    private final HttpClient client;
    private final AtomicLong sequenceNo = new AtomicLong(0);

    /**
     * 데모용 엔드포인트 매핑
     */
    private static final Map<String, String> DEMO_ENDPOINTS = Map.of(
            DemoMessageCodes.BALANCE_INQUIRY_REQ, "/api/balance",
            DemoMessageCodes.TRANSFER_REQ, "/api/transfer",
            DemoMessageCodes.TX_HISTORY_REQ, "/api/transactions",
            DemoMessageCodes.ACCOUNT_INFO_REQ, "/api/account",
            DemoMessageCodes.ECHO_REQ, "/api/echo",
            DemoMessageCodes.HEARTBEAT_REQ, "/api/heartbeat"
    );

    public HttpDemoClient(String host, int port) {
        // 레이아웃 등록
        DemoLayoutRegistry registry = new DemoLayoutRegistry();
        LayoutManager layoutManager = registry.getLayoutManager();

        // 클라이언트 설정
        ClientConfig config = ClientConfig.builder()
                .clientId("demo-http-client")
                .host(host)
                .port(port)
                .transportType(TransportType.HTTP)
                .connectTimeout(DemoConstants.CONNECT_TIMEOUT)
                .readTimeout(DemoConstants.READ_TIMEOUT)
                .build();

        client = new HttpClient(config, layoutManager, new springware.mci.common.logging.DefaultMessageLogger());

        // 데모용 엔드포인트 등록
        client.registerEndpoints(DEMO_ENDPOINTS);
    }

    /**
     * 연결
     */
    public void connect() {
        client.connect();
        log.info("HTTP client initialized");
    }

    /**
     * 연결 해제
     */
    public void disconnect() {
        client.disconnect();
        log.info("HTTP client disconnected");
    }

    /**
     * 잔액조회
     */
    public Message balanceInquiry(String accountNo) {
        Message request = Message.builder()
                .messageCode(DemoMessageCodes.BALANCE_INQUIRY_REQ)
                .messageType(MessageType.REQUEST)
                .build();

        request.setField("msgCode", DemoMessageCodes.BALANCE_INQUIRY_REQ);
        request.setField("orgCode", DemoConstants.ORG_CODE_BANK);
        request.setField("seqNo", String.format("%010d", sequenceNo.incrementAndGet()));
        request.setField("accountNo", accountNo);

        log.info("Sending balance inquiry for account: {}", maskAccount(accountNo));
        Message response = client.send(request);

        String rspCode = response.getString("rspCode");
        if (DemoConstants.RSP_SUCCESS.equals(rspCode)) {
            log.info("Balance: {}", response.getLong("balance"));
        } else {
            log.warn("Balance inquiry failed: {}", rspCode);
        }

        return response;
    }

    /**
     * 이체
     */
    public Message transfer(String fromAccount, String toAccount, long amount) {
        Message request = Message.builder()
                .messageCode(DemoMessageCodes.TRANSFER_REQ)
                .messageType(MessageType.REQUEST)
                .build();

        request.setField("msgCode", DemoMessageCodes.TRANSFER_REQ);
        request.setField("orgCode", DemoConstants.ORG_CODE_BANK);
        request.setField("seqNo", String.format("%010d", sequenceNo.incrementAndGet()));
        request.setField("fromAccount", fromAccount);
        request.setField("toAccount", toAccount);
        request.setField("amount", amount);
        request.setField("memo", "Transfer");

        log.info("Sending transfer: {} -> {} ({})",
                maskAccount(fromAccount), maskAccount(toAccount), amount);
        Message response = client.send(request);

        String rspCode = response.getString("rspCode");
        if (DemoConstants.RSP_SUCCESS.equals(rspCode)) {
            log.info("Transfer successful, remaining balance: {}", response.getLong("fromBalance"));
        } else {
            log.warn("Transfer failed: {}", rspCode);
        }

        return response;
    }

    /**
     * 거래내역조회
     */
    @SuppressWarnings("unchecked")
    public Message transactionHistory(String accountNo, String fromDate, String toDate) {
        Message request = Message.builder()
                .messageCode(DemoMessageCodes.TX_HISTORY_REQ)
                .messageType(MessageType.REQUEST)
                .build();

        request.setField("msgCode", DemoMessageCodes.TX_HISTORY_REQ);
        request.setField("orgCode", DemoConstants.ORG_CODE_BANK);
        request.setField("seqNo", String.format("%010d", sequenceNo.incrementAndGet()));
        request.setField("accountNo", accountNo);
        request.setField("fromDate", fromDate);
        request.setField("toDate", toDate);

        log.info("Sending transaction history inquiry: {} ({} ~ {})",
                maskAccount(accountNo), fromDate, toDate);
        Message response = client.send(request);

        String rspCode = response.getString("rspCode");
        if (DemoConstants.RSP_SUCCESS.equals(rspCode)) {
            Object countObj = response.getField("recordCount");
            int count = countObj instanceof Number ? ((Number) countObj).intValue() : 0;
            log.info("Found {} transactions", count);

            // 거래내역 출력
            List<Map<String, Object>> records =
                    (List<Map<String, Object>>) response.getField("records");
            if (records != null) {
                for (int i = 0; i < records.size(); i++) {
                    Map<String, Object> record = records.get(i);
                    String txType = "1".equals(String.valueOf(record.get("txType"))) ? "입금" : "출금";
                    log.info("  [{}] {} {} {} {:>15}원 (잔액: {})",
                            i + 1,
                            record.get("txDate"),
                            record.get("txTime"),
                            txType,
                            record.get("amount"),
                            record.get("balance"));
                }
            }
        } else {
            log.warn("Transaction history inquiry failed: {}", rspCode);
        }

        return response;
    }

    /**
     * 계좌정보조회
     */
    public Message accountInquiry(String accountNo) {
        Message request = Message.builder()
                .messageCode(DemoMessageCodes.ACCOUNT_INFO_REQ)
                .messageType(MessageType.REQUEST)
                .build();

        request.setField("msgCode", DemoMessageCodes.ACCOUNT_INFO_REQ);
        request.setField("orgCode", DemoConstants.ORG_CODE_BANK);
        request.setField("seqNo", String.format("%010d", sequenceNo.incrementAndGet()));
        request.setField("accountNo", accountNo);

        log.info("Sending account inquiry for account: {}", maskAccount(accountNo));
        Message response = client.send(request);

        String rspCode = response.getString("rspCode");
        if (DemoConstants.RSP_SUCCESS.equals(rspCode)) {
            log.info("Account: {} ({})",
                    response.getString("accountName"),
                    getAccountTypeName(response.getString("accountType")));
            log.info("  Open Date: {}, Status: {}",
                    response.getString("openDate"),
                    getStatusName(response.getString("status")));
            log.info("  Balance: {}, Available: {}",
                    response.getLong("balance"),
                    response.getLong("availableBalance"));
            log.info("  Interest Rate: {}%",
                    formatInterestRate(response.getField("interestRate")));
        } else {
            log.warn("Account inquiry failed: {}", rspCode);
        }

        return response;
    }

    private String getAccountTypeName(String accountType) {
        if (accountType == null) return "Unknown";
        switch (accountType) {
            case "01": return "보통예금";
            case "02": return "정기예금";
            case "03": return "적금";
            default: return "기타";
        }
    }

    private String getStatusName(String status) {
        if (status == null) return "Unknown";
        switch (status) {
            case "1": return "정상";
            case "2": return "정지";
            case "9": return "해지";
            default: return "Unknown";
        }
    }

    private String formatInterestRate(Object rate) {
        if (rate == null) return "0.00";
        int rateValue = rate instanceof Number ? ((Number) rate).intValue() : 0;
        return String.format("%.2f", rateValue / 100.0);
    }

    /**
     * 에코 테스트
     */
    public Message echo(String data) {
        Message request = Message.builder()
                .messageCode(DemoMessageCodes.ECHO_REQ)
                .messageType(MessageType.REQUEST)
                .build();

        request.setField("msgCode", DemoMessageCodes.ECHO_REQ);
        request.setField("orgCode", DemoConstants.ORG_CODE_BANK);
        request.setField("seqNo", String.format("%010d", sequenceNo.incrementAndGet()));
        request.setField("echoData", data);

        log.debug("Sending echo: {}", data);
        Message response = client.send(request);

        log.debug("Echo response: {}", response.getString("echoData"));
        return response;
    }

    /**
     * 하트비트
     */
    public Message heartbeat() {
        Message request = Message.builder()
                .messageCode(DemoMessageCodes.HEARTBEAT_REQ)
                .messageType(MessageType.REQUEST)
                .build();

        request.setField("msgCode", DemoMessageCodes.HEARTBEAT_REQ);
        request.setField("orgCode", DemoConstants.ORG_CODE_BANK);
        request.setField("seqNo", String.format("%010d", sequenceNo.incrementAndGet()));

        log.debug("Sending heartbeat");
        return client.send(request);
    }

    /**
     * 헬스 체크 (GET 요청)
     */
    public Message healthCheck() {
        log.debug("Sending health check");
        try {
            return client.sendGet(DemoMessageCodes.HEARTBEAT_REQ).get();
        } catch (Exception e) {
            log.error("Health check failed", e);
            Message errorResponse = Message.builder()
                    .messageCode("ERROR")
                    .messageType(MessageType.RESPONSE)
                    .build();
            errorResponse.setField("rspCode", "9999");
            errorResponse.setField("errorMessage", e.getMessage());
            return errorResponse;
        }
    }

    /**
     * 계좌번호 마스킹
     */
    private String maskAccount(String accountNo) {
        if (accountNo == null || accountNo.length() < 8) {
            return "****";
        }
        return accountNo.substring(0, 4) + "****" + accountNo.substring(accountNo.length() - 4);
    }

    /**
     * 메인 메서드 (테스트용)
     */
    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : DemoConstants.DEFAULT_HOST;
        int port = args.length > 1 ? Integer.parseInt(args[1]) : DemoConstants.DEFAULT_HTTP_PORT;

        HttpDemoClient demoClient = new HttpDemoClient(host, port);

        try {
            demoClient.connect();

            // 잔액조회
            demoClient.balanceInquiry("1234567890123456789");

            // 이체
            demoClient.transfer("1234567890123456789", "9876543210987654321", 50000);

            // 이체 후 잔액확인
            demoClient.balanceInquiry("1234567890123456789");
            demoClient.balanceInquiry("9876543210987654321");

            // 에코 테스트
            demoClient.echo("Hello, HTTP MCI!");

            // 하트비트
            demoClient.heartbeat();

        } finally {
            demoClient.disconnect();
        }
    }
}
