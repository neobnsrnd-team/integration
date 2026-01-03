package demo.mci.tcp;

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

import java.util.concurrent.atomic.AtomicLong;

/**
 * TCP 데모 클라이언트
 */
@Slf4j
public class TcpDemoClient {

    private final TcpClient client;
    private final AtomicLong sequenceNo = new AtomicLong(0);

    public TcpDemoClient(String host, int port) {
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
                .clientId("demo-tcp-client")
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
        log.info("Connected to server");
    }

    /**
     * 연결 해제
     */
    public void disconnect() {
        client.disconnect();
        log.info("Disconnected from server");
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
        int port = args.length > 1 ? Integer.parseInt(args[1]) : DemoConstants.DEFAULT_TCP_PORT;

        TcpDemoClient demoClient = new TcpDemoClient(host, port);

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
            demoClient.echo("Hello, MCI!");

            // 하트비트
            demoClient.heartbeat();

        } finally {
            demoClient.disconnect();
        }
    }
}
