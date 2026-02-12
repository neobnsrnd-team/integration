package demo.mci.card.https;

import demo.mci.card.biz.CardListBiz;
import demo.mci.card.biz.CardUsageHistoryBiz;
import demo.mci.common.DemoConstants;
import demo.mci.common.DemoLayoutRegistry;
import demo.mci.common.DemoMessageCodes;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import lombok.extern.slf4j.Slf4j;
import springware.mci.common.core.Message;
import springware.mci.common.core.MessageType;
import springware.mci.common.core.TransportType;
import springware.mci.common.layout.LayoutManager;
import springware.mci.server.biz.Biz;
import springware.mci.server.biz.BizRegistry;
import springware.mci.server.config.ServerConfig;
import springware.mci.server.core.MessageContext;
import springware.mci.server.http.HttpServer;
import springware.mci.server.http.RestEndpointRegistry;

/**
 * 카드 HTTPS 서버
 * 자체 서명 인증서를 사용한 SSL/TLS 통신
 */
@Slf4j
public class CardHttpsServer {

    private final HttpServer server;
    private final BizRegistry bizRegistry;
    private SelfSignedCertificate ssc;

    public CardHttpsServer(int port) {
        this(port, null, null);
    }

    public CardHttpsServer(int port, String keyStorePath, String keyStorePassword) {
        // 레이아웃 등록
        DemoLayoutRegistry registry = new DemoLayoutRegistry();
        LayoutManager layoutManager = registry.getLayoutManager();

        // REST 엔드포인트 레지스트리 초기화
        RestEndpointRegistry endpointRegistry = new RestEndpointRegistry();
        registerCustomEndpoints(endpointRegistry);

        // 서버 설정
        ServerConfig config;
        if (keyStorePath != null && !keyStorePath.isEmpty()) {
            // 지정된 KeyStore 사용
            config = ServerConfig.builder()
                    .serverId("card-https-server")
                    .port(port)
                    .transportType(TransportType.HTTP)
                    .sslEnabled(true)
                    .keyStorePath(keyStorePath)
                    .keyStorePassword(keyStorePassword)
                    .corsEnabled(true)
                    .healthCheckEnabled(true)
                    .healthCheckPath("/health")
                    .build();
            log.info("Using KeyStore: {}", keyStorePath);
        } else {
            // 자체 서명 인증서 생성 (데모용)
            try {
                ssc = new SelfSignedCertificate();
                config = ServerConfig.builder()
                        .serverId("card-https-server")
                        .port(port)
                        .transportType(TransportType.HTTP)
                        .sslEnabled(true)
                        .sslCertPath(ssc.certificate().getAbsolutePath())
                        .sslKeyPath(ssc.privateKey().getAbsolutePath())
                        .corsEnabled(true)
                        .healthCheckEnabled(true)
                        .healthCheckPath("/health")
                        .build();
                log.info("Using self-signed certificate for demo");
                log.warn("Self-signed certificates are for testing only. Do not use in production!");
            } catch (Exception e) {
                throw new RuntimeException("Failed to create self-signed certificate", e);
            }
        }

        server = new HttpServer(config, layoutManager,
                new springware.mci.common.logging.DefaultMessageLogger(), endpointRegistry);

        // Biz 레지스트리 초기화
        bizRegistry = new BizRegistry();
        registerBizComponents();

        // 핸들러 등록 (BizRegistry 기반)
        registerHandlers();
    }

    /**
     * 카드 엔드포인트 등록
     */
    private void registerCustomEndpoints(RestEndpointRegistry registry) {
        // 카드목록조회
        registry.register("/api/cards", DemoMessageCodes.CARD_LIST_REQ);
        // 카드사용내역조회
        registry.register("/api/card-history", DemoMessageCodes.CARD_USAGE_HISTORY_REQ);

        log.debug("Registered {} card endpoints", registry.size());
    }

    /**
     * Biz 컴포넌트 등록
     */
    private void registerBizComponents() {
        bizRegistry.register(new CardListBiz());
        bizRegistry.register(new CardUsageHistoryBiz());

        // 기본 Biz (등록되지 않은 메시지 코드 처리)
        bizRegistry.setDefaultBiz(new DefaultBiz());

        log.info("Registered {} Card Biz components", bizRegistry.size());
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
        String reqCode = request.getMessageCode();
        String resCode = reqCode != null && reqCode.length() >= 3
                ? reqCode.substring(0, 3) + "2"
                : "ERR2";

        Message response = Message.builder()
                .messageCode(resCode)
                .messageType(MessageType.RESPONSE)
                .build();

        response.setField("rspCode", DemoConstants.RSP_SYSTEM_ERROR);
        return response;
    }

    /**
     * 서버 시작
     */
    public void start() {
        server.start();
        log.info("Card HTTPS Server started on port {}", server.getConfig().getPort());
        log.info("Available endpoints:");
        server.getEndpointRegistry().getAllMappings().forEach((path, code) ->
                log.info("  {} -> {}", path, code));
        log.info("Health check: https://localhost:{}/health", server.getConfig().getPort());
    }

    /**
     * 서버 종료
     */
    public void stop() {
        server.stop();
        if (ssc != null) {
            ssc.delete();
        }
        log.info("Card HTTPS Server stopped");
    }

    /**
     * BizRegistry 반환 (테스트/확장용)
     */
    public BizRegistry getBizRegistry() {
        return bizRegistry;
    }

    /**
     * HttpServer 반환 (테스트/확장용)
     */
    public HttpServer getServer() {
        return server;
    }

    /**
     * 기본 Biz 구현 (등록되지 않은 메시지 코드 처리)
     */
    private static class DefaultBiz implements Biz {
        @Override
        public Message execute(Message request, MessageContext context) {
            log.warn("Unknown message code: {}", request.getMessageCode());

            String reqCode = request.getMessageCode();
            String resCode = reqCode != null && reqCode.length() >= 3
                    ? reqCode.substring(0, 3) + "2"
                    : "ERR2";

            Message response = Message.builder()
                    .messageCode(resCode)
                    .messageType(MessageType.RESPONSE)
                    .build();

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
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DemoConstants.DEFAULT_CARD_HTTPS_PORT;
        String keyStorePath = args.length > 1 ? args[1] : null;
        String keyStorePassword = args.length > 2 ? args[2] : null;

        CardHttpsServer cardServer = new CardHttpsServer(port, keyStorePath, keyStorePassword);
        cardServer.start();

        // 종료 훅
        Runtime.getRuntime().addShutdownHook(new Thread(cardServer::stop));

        log.info("Press Ctrl+C to stop the server");
    }
}
