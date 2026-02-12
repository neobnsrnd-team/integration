package springware.mci.server.http;

import org.junit.jupiter.api.*;
import springware.mci.common.core.Message;
import springware.mci.common.core.MessageType;
import springware.mci.common.core.TransportType;
import springware.mci.server.config.ServerConfig;
import springware.mci.server.core.MessageContext;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HttpServer 테스트")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HttpServerTest {

    private static HttpServer server;
    private static int testPort;
    private static HttpClient httpClient;

    @BeforeAll
    static void setUpAll() throws Exception {
        testPort = findAvailablePort();

        ServerConfig config = ServerConfig.builder()
                .serverId("test-http-server")
                .port(testPort)
                .transportType(TransportType.HTTP)
                .corsEnabled(true)
                .healthCheckEnabled(true)
                .healthCheckPath("/health")
                .build();

        server = new HttpServer(config);

        // 테스트용 핸들러 등록
        server.registerHandler("TEST", (request, context) -> {
            Message response = Message.builder()
                    .messageCode("TEST_RES")
                    .messageType(MessageType.RESPONSE)
                    .build();
            response.setField("rspCode", "0000");
            response.setField("echoField", request.getString("inputField"));
            return response;
        });

        server.setDefaultHandler((request, context) -> {
            Message response = Message.builder()
                    .messageCode("DEFAULT_RES")
                    .messageType(MessageType.RESPONSE)
                    .build();
            response.setField("rspCode", "9999");
            return response;
        });

        // 커스텀 엔드포인트 등록
        server.registerEndpoint("/api/test", "TEST");

        server.start();
        Thread.sleep(300);

        httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @AfterAll
    static void tearDownAll() {
        if (server != null) {
            server.stop();
        }
    }

    private static int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    // ==================== Server Lifecycle Tests ====================

    @Test
    @Order(1)
    @DisplayName("서버 시작 확인")
    void serverStarted() {
        assertThat(server.isRunning()).isTrue();
    }

    @Test
    @Order(2)
    @DisplayName("서버 설정 확인")
    void serverConfig() {
        assertThat(server.getConfig().getPort()).isEqualTo(testPort);
        assertThat(server.getConfig().isCorsEnabled()).isTrue();
        assertThat(server.getConfig().isHealthCheckEnabled()).isTrue();
    }

    // ==================== Health Check Tests ====================

    @Test
    @Order(10)
    @DisplayName("헬스 체크 엔드포인트 응답")
    void healthCheck() throws Exception {
        // given
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + testPort + "/health"))
                .GET()
                .build();

        // when
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"UP\"");
    }

    // ==================== POST Request Tests ====================

    @Test
    @Order(20)
    @DisplayName("POST 요청 처리 - 등록된 핸들러")
    void postRequest_registeredHandler() throws Exception {
        // given
        String jsonBody = "{\"messageCode\":\"TEST\",\"fields\":{\"inputField\":\"hello\"}}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + testPort + "/api/test"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        // when
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"messageCode\":\"TEST_RES\"");
        assertThat(response.body()).contains("\"rspCode\":\"0000\"");
        assertThat(response.body()).contains("\"echoField\":\"hello\"");
    }

    @Test
    @Order(21)
    @DisplayName("POST 요청 처리 - 기본 핸들러")
    void postRequest_defaultHandler() throws Exception {
        // given
        String jsonBody = "{\"messageCode\":\"UNKNOWN\",\"fields\":{}}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + testPort + "/api/unknown"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        // when
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"messageCode\":\"DEFAULT_RES\"");
    }

    @Test
    @Order(22)
    @DisplayName("POST 요청 - 빈 바디 오류")
    void postRequest_emptyBody() throws Exception {
        // given
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + testPort + "/api/test"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(""))
                .build();

        // when
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // then
        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    @Order(23)
    @DisplayName("POST 요청 - 잘못된 JSON 오류")
    void postRequest_invalidJson() throws Exception {
        // given
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + testPort + "/api/test"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("not a valid json"))
                .build();

        // when
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // then
        assertThat(response.statusCode()).isEqualTo(400);
    }

    // ==================== CORS Tests ====================

    @Test
    @Order(30)
    @DisplayName("CORS 헤더 포함")
    void corsHeaders() throws Exception {
        // given
        String jsonBody = "{\"messageCode\":\"TEST\",\"fields\":{}}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + testPort + "/api/test"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        // when
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // then
        assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).isPresent();
    }

    @Test
    @Order(31)
    @DisplayName("CORS Preflight 요청 처리")
    void corsPreflightRequest() throws Exception {
        // given
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + testPort + "/api/test"))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .build();

        // when
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Access-Control-Allow-Methods")).isPresent();
    }

    // ==================== Method Tests ====================

    @Test
    @Order(40)
    @DisplayName("GET 요청 - 등록된 엔드포인트")
    void getRequest() throws Exception {
        // given
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + testPort + "/api/test"))
                .GET()
                .build();

        // when
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // then - GET 요청도 핸들러로 라우팅됨
        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    @Order(41)
    @DisplayName("지원하지 않는 HTTP 메서드")
    void unsupportedMethod() throws Exception {
        // given
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + testPort + "/api/test"))
                .method("DELETE", HttpRequest.BodyPublishers.noBody())
                .build();

        // when
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // then
        assertThat(response.statusCode()).isEqualTo(405);
    }

    // ==================== Endpoint Registry Tests ====================

    @Test
    @Order(50)
    @DisplayName("엔드포인트 레지스트리 확인")
    void endpointRegistry() {
        RestEndpointRegistry registry = server.getEndpointRegistry();
        assertThat(registry).isNotNull();
        assertThat(registry.hasPath("/api/test")).isTrue();
        assertThat(registry.getMessageCode("/api/test")).isEqualTo("TEST");
    }

    // ==================== Content-Type Tests ====================

    @Test
    @Order(60)
    @DisplayName("응답 Content-Type 확인")
    void responseContentType() throws Exception {
        // given
        String jsonBody = "{\"messageCode\":\"TEST\",\"fields\":{}}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + testPort + "/api/test"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        // when
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // then
        assertThat(response.headers().firstValue("Content-Type"))
                .isPresent()
                .get().asString().contains("application/json");
    }

    // ==================== SSL Configuration Tests ====================

    @Test
    @Order(70)
    @DisplayName("ServerConfig.httpsServer() 팩토리 메서드")
    void httpsServerFactoryMethod() {
        // when
        ServerConfig config = ServerConfig.httpsServer(8443);

        // then
        assertThat(config.getPort()).isEqualTo(8443);
        assertThat(config.isSslEnabled()).isTrue();
        assertThat(config.isCorsEnabled()).isTrue();
        assertThat(config.isHealthCheckEnabled()).isTrue();
    }

    @Test
    @Order(71)
    @DisplayName("ServerConfig.httpsServer(keyStore) 팩토리 메서드")
    void httpsServerKeyStoreFactoryMethod() {
        // when
        ServerConfig config = ServerConfig.httpsServer(8443, "/path/to/keystore.p12", "password");

        // then
        assertThat(config.getPort()).isEqualTo(8443);
        assertThat(config.isSslEnabled()).isTrue();
        assertThat(config.getKeyStorePath()).isEqualTo("/path/to/keystore.p12");
        assertThat(config.getKeyStorePassword()).isEqualTo("password");
    }

    @Test
    @Order(72)
    @DisplayName("ServerConfig.httpsServerWithClientAuth() 팩토리 메서드")
    void httpsServerWithClientAuthFactoryMethod() {
        // when
        ServerConfig config = ServerConfig.httpsServerWithClientAuth(
                8443,
                "/path/to/keystore.p12", "keypass",
                "/path/to/truststore.p12", "trustpass"
        );

        // then
        assertThat(config.isSslEnabled()).isTrue();
        assertThat(config.isClientAuthRequired()).isTrue();
        assertThat(config.getKeyStorePath()).isEqualTo("/path/to/keystore.p12");
        assertThat(config.getKeyStorePassword()).isEqualTo("keypass");
        assertThat(config.getTrustStorePath()).isEqualTo("/path/to/truststore.p12");
        assertThat(config.getTrustStorePassword()).isEqualTo("trustpass");
    }

    @Test
    @Order(73)
    @DisplayName("SSL 설정 옵션")
    void sslConfigurationOptions() {
        // when
        ServerConfig config = ServerConfig.builder()
                .serverId("ssl-test-server")
                .port(8443)
                .sslEnabled(true)
                .keyStorePath("/path/to/keystore.p12")
                .keyStorePassword("password")
                .keyStoreType("PKCS12")
                .trustStorePath("/path/to/truststore.p12")
                .trustStorePassword("password")
                .trustStoreType("PKCS12")
                .clientAuthRequired(true)
                .sslProtocol("TLSv1.3")
                .build();

        // then
        assertThat(config.isSslEnabled()).isTrue();
        assertThat(config.getKeyStorePath()).isEqualTo("/path/to/keystore.p12");
        assertThat(config.getKeyStorePassword()).isEqualTo("password");
        assertThat(config.getKeyStoreType()).isEqualTo("PKCS12");
        assertThat(config.getTrustStorePath()).isEqualTo("/path/to/truststore.p12");
        assertThat(config.getTrustStorePassword()).isEqualTo("password");
        assertThat(config.getTrustStoreType()).isEqualTo("PKCS12");
        assertThat(config.isClientAuthRequired()).isTrue();
        assertThat(config.getSslProtocol()).isEqualTo("TLSv1.3");
    }

    @Test
    @Order(74)
    @DisplayName("SSL 기본값")
    void sslDefaultValues() {
        // when
        ServerConfig config = ServerConfig.builder()
                .serverId("ssl-default-test")
                .port(8443)
                .sslEnabled(true)
                .build();

        // then
        assertThat(config.getKeyStoreType()).isEqualTo("PKCS12");
        assertThat(config.getTrustStoreType()).isEqualTo("PKCS12");
        assertThat(config.getSslProtocol()).isEqualTo("TLS");
        assertThat(config.isClientAuthRequired()).isFalse();
    }

    @Test
    @Order(75)
    @DisplayName("PEM 인증서 설정")
    void pemCertificateConfiguration() {
        // when
        ServerConfig config = ServerConfig.builder()
                .serverId("pem-test-server")
                .port(8443)
                .sslEnabled(true)
                .sslCertPath("/path/to/cert.pem")
                .sslKeyPath("/path/to/key.pem")
                .build();

        // then
        assertThat(config.isSslEnabled()).isTrue();
        assertThat(config.getSslCertPath()).isEqualTo("/path/to/cert.pem");
        assertThat(config.getSslKeyPath()).isEqualTo("/path/to/key.pem");
    }
}
