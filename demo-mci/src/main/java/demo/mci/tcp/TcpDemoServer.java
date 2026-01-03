package demo.mci.tcp;

import demo.mci.common.DemoConstants;
import demo.mci.common.DemoLayoutRegistry;
import demo.mci.common.DemoMessageCodes;
import lombok.extern.slf4j.Slf4j;
import springware.mci.common.core.Message;
import springware.mci.common.core.MessageType;
import springware.mci.common.layout.LayoutManager;
import springware.mci.common.protocol.LengthFieldType;
import springware.mci.common.protocol.ProtocolConfig;
import springware.mci.server.config.ServerConfig;
import springware.mci.server.core.MessageContext;
import springware.mci.server.core.MessageHandler;
import springware.mci.server.tcp.TcpServer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TCP 데모 서버
 */
@Slf4j
public class TcpDemoServer {

    private final TcpServer server;
    private final AtomicLong sequenceNo = new AtomicLong(0);

    // 가상 계좌 데이터
    private final Map<String, Long> accountBalances = new HashMap<>();

    public TcpDemoServer(int port) {
        // 레이아웃 등록
        DemoLayoutRegistry registry = new DemoLayoutRegistry();
        LayoutManager layoutManager = registry.getLayoutManager();

        // 프로토콜 설정 (4바이트 바이너리 길이 필드)
        ProtocolConfig protocolConfig = ProtocolConfig.builder()
                .lengthFieldOffset(0)
                .lengthFieldLength(4)
                .lengthFieldType(LengthFieldType.BINARY_BIG_ENDIAN)
                .lengthIncludesHeader(false)
                .initialBytesToStrip(4)
                .build();

        // 서버 설정
        ServerConfig config = ServerConfig.builder()
                .serverId("demo-tcp-server")
                .port(port)
                .protocolConfig(protocolConfig)
                .build();

        server = new TcpServer(config, layoutManager, new springware.mci.common.logging.DefaultMessageLogger());

        // 핸들러 등록
        registerHandlers();

        // 테스트 데이터 초기화
        initTestData();
    }

    /**
     * 핸들러 등록
     */
    private void registerHandlers() {
        // 잔액조회 핸들러
        server.registerHandler(DemoMessageCodes.BALANCE_INQUIRY_REQ, this::handleBalanceInquiry);

        // 이체 핸들러
        server.registerHandler(DemoMessageCodes.TRANSFER_REQ, this::handleTransfer);

        // 에코 핸들러
        server.registerHandler(DemoMessageCodes.ECHO_REQ, this::handleEcho);

        // 하트비트 핸들러
        server.registerHandler(DemoMessageCodes.HEARTBEAT_REQ, this::handleHeartbeat);

        // 기본 핸들러
        server.setDefaultHandler((request, context) -> {
            log.warn("Unknown message code: {}", request.getMessageCode());
            Message response = request.toResponse();
            response.setField("msgCode", request.getMessageCode().substring(0, 3) + "2");
            response.setField("rspCode", DemoConstants.RSP_SYSTEM_ERROR);
            return response;
        });
    }

    /**
     * 테스트 데이터 초기화
     */
    private void initTestData() {
        accountBalances.put("1234567890123456789", 1000000L);
        accountBalances.put("9876543210987654321", 500000L);
        accountBalances.put("1111222233334444555", 2500000L);
    }

    /**
     * 잔액조회 처리
     */
    private Message handleBalanceInquiry(Message request, MessageContext context) {
        String accountNo = request.getString("accountNo");
        log.info("Balance inquiry for account: {}", maskAccount(accountNo));

        Message response = Message.builder()
                .messageCode(DemoMessageCodes.BALANCE_INQUIRY_RES)
                .messageType(MessageType.RESPONSE)
                .build();

        // 요청 헤더 복사
        copyHeader(request, response);
        response.setField("msgCode", DemoMessageCodes.BALANCE_INQUIRY_RES);
        response.setField("accountNo", accountNo);

        Long balance = accountBalances.get(accountNo.trim());
        if (balance != null) {
            response.setField("rspCode", DemoConstants.RSP_SUCCESS);
            response.setField("balance", balance);
        } else {
            response.setField("rspCode", DemoConstants.RSP_INVALID_ACCOUNT);
            response.setField("balance", 0L);
        }

        return response;
    }

    /**
     * 이체 처리
     */
    private Message handleTransfer(Message request, MessageContext context) {
        String fromAccount = request.getString("fromAccount");
        String toAccount = request.getString("toAccount");
        Long amount = request.getLong("amount");

        log.info("Transfer: {} -> {} ({})", maskAccount(fromAccount), maskAccount(toAccount), amount);

        Message response = Message.builder()
                .messageCode(DemoMessageCodes.TRANSFER_RES)
                .messageType(MessageType.RESPONSE)
                .build();

        copyHeader(request, response);
        response.setField("msgCode", DemoMessageCodes.TRANSFER_RES);
        response.setField("fromAccount", fromAccount);
        response.setField("toAccount", toAccount);
        response.setField("amount", amount);

        Long fromBalance = accountBalances.get(fromAccount.trim());
        Long toBalance = accountBalances.get(toAccount.trim());

        if (fromBalance == null) {
            response.setField("rspCode", DemoConstants.RSP_INVALID_ACCOUNT);
            response.setField("fromBalance", 0L);
        } else if (fromBalance < amount) {
            response.setField("rspCode", DemoConstants.RSP_INSUFFICIENT_BALANCE);
            response.setField("fromBalance", fromBalance);
        } else {
            // 이체 수행
            accountBalances.put(fromAccount.trim(), fromBalance - amount);
            if (toBalance != null) {
                accountBalances.put(toAccount.trim(), toBalance + amount);
            }

            response.setField("rspCode", DemoConstants.RSP_SUCCESS);
            response.setField("fromBalance", fromBalance - amount);
        }

        return response;
    }

    /**
     * 에코 처리
     */
    private Message handleEcho(Message request, MessageContext context) {
        log.debug("Echo request received");

        Message response = Message.builder()
                .messageCode(DemoMessageCodes.ECHO_RES)
                .messageType(MessageType.RESPONSE)
                .build();

        copyHeader(request, response);
        response.setField("msgCode", DemoMessageCodes.ECHO_RES);
        response.setField("rspCode", DemoConstants.RSP_SUCCESS);
        response.setField("echoData", request.getString("echoData"));

        return response;
    }

    /**
     * 하트비트 처리
     */
    private Message handleHeartbeat(Message request, MessageContext context) {
        log.debug("Heartbeat received from {}", context.getRemoteIp());

        Message response = Message.builder()
                .messageCode(DemoMessageCodes.HEARTBEAT_RES)
                .messageType(MessageType.RESPONSE)
                .build();

        copyHeader(request, response);
        response.setField("msgCode", DemoMessageCodes.HEARTBEAT_RES);
        response.setField("rspCode", DemoConstants.RSP_SUCCESS);

        return response;
    }

    /**
     * 헤더 복사
     */
    private void copyHeader(Message from, Message to) {
        to.setField("orgCode", from.getString("orgCode"));
        to.setField("txDate", from.getString("txDate"));
        to.setField("txTime", from.getString("txTime"));
        to.setField("seqNo", from.getString("seqNo"));
        to.setField("filler", "");
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
     * 서버 시작
     */
    public void start() {
        server.start();
        log.info("Demo TCP Server started on port {}", server.getConfig().getPort());
    }

    /**
     * 서버 종료
     */
    public void stop() {
        server.stop();
        log.info("Demo TCP Server stopped");
    }

    /**
     * 메인 메서드
     */
    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DemoConstants.DEFAULT_TCP_PORT;

        TcpDemoServer demoServer = new TcpDemoServer(port);
        demoServer.start();

        // 종료 훅
        Runtime.getRuntime().addShutdownHook(new Thread(demoServer::stop));

        log.info("Press Ctrl+C to stop the server");
    }
}
