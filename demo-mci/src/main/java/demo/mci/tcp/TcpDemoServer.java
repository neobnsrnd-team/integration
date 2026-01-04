package demo.mci.tcp;

import demo.mci.biz.AccountInquiryBiz;
import demo.mci.biz.BalanceInquiryBiz;
import demo.mci.biz.EchoBiz;
import demo.mci.biz.HeartbeatBiz;
import demo.mci.biz.TransactionHistoryBiz;
import demo.mci.biz.TransferBiz;
import demo.mci.common.DemoConstants;
import demo.mci.common.DemoLayoutRegistry;
import lombok.extern.slf4j.Slf4j;
import springware.mci.common.core.Message;
import springware.mci.common.core.MessageType;
import springware.mci.common.layout.LayoutManager;
import springware.mci.common.protocol.LengthFieldType;
import springware.mci.common.protocol.ProtocolConfig;
import springware.mci.server.biz.Biz;
import springware.mci.server.biz.BizRegistry;
import springware.mci.server.config.ServerConfig;
import springware.mci.server.core.MessageContext;
import springware.mci.server.tcp.TcpServer;

/**
 * TCP 데모 서버
 * Biz 패턴을 사용하여 메시지 코드별 비즈니스 로직 처리
 */
@Slf4j
public class TcpDemoServer {

    private final TcpServer server;
    private final BizRegistry bizRegistry;

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

        // Biz 레지스트리 초기화
        bizRegistry = new BizRegistry();
        registerBizComponents();

        // 핸들러 등록 (BizRegistry 기반)
            registerHandlers();
    }

    /**
     * Biz 컴포넌트 등록
     */
    private void registerBizComponents() {
        bizRegistry.register(new BalanceInquiryBiz());
        bizRegistry.register(new TransferBiz());
        bizRegistry.register(new TransactionHistoryBiz());
        bizRegistry.register(new AccountInquiryBiz());
        bizRegistry.register(new EchoBiz());
        bizRegistry.register(new HeartbeatBiz());

        // 기본 Biz (등록되지 않은 메시지 코드 처리)
        bizRegistry.setDefaultBiz(new DefaultBiz());

        log.info("Registered {} Biz components", bizRegistry.size());
    }

    /**
     * 메시지 핸들러 등록
     * BizRegistry를 통해 비즈니스 로직 실행
     */
    private void registerHandlers() {
        // 공통 핸들러 - BizRegistry에서 Biz를 찾아 실행
        server.setDefaultHandler((request, context) -> {
            String messageCode = request.getMessageCode();
            Biz biz = bizRegistry.getBiz(messageCode);

            if (biz != null) {
                log.debug("Executing Biz for message code: {}", messageCode);
                return biz.execute(request, context);
            } else {
                log.warn("No Biz found for message code: {}", messageCode);
                return createErrorResponse(request);
            }
        });
    }

    /**
     * 에러 응답 생성
     */
    private Message createErrorResponse(Message request) {
        Message response = Message.builder()
                .messageCode(request.getMessageCode())
                .messageType(MessageType.RESPONSE)
                .build();

        response.setField("msgCode", request.getMessageCode().substring(0, 3) + "2");
        response.setField("rspCode", DemoConstants.RSP_SYSTEM_ERROR);
        return response;
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
     * BizRegistry 반환 (테스트/확장용)
     */
    public BizRegistry getBizRegistry() {
        return bizRegistry;
    }

    /**
     * 기본 Biz 구현 (등록되지 않은 메시지 코드 처리)
     */
    private static class DefaultBiz implements Biz {
        @Override
        public Message execute(Message request, MessageContext context) {
            log.warn("Unknown message code: {}", request.getMessageCode());

            Message response = Message.builder()
                    .messageCode(request.getMessageCode())
                    .messageType(MessageType.RESPONSE)
                    .build();

            response.setField("msgCode", request.getMessageCode().substring(0, 3) + "2");
            response.setField("rspCode", DemoConstants.RSP_SYSTEM_ERROR);
            return response;
        }

        @Override
        public String getMessageCode() {
            return "*"; // 와일드카드 - 기본 처리용
        }
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
