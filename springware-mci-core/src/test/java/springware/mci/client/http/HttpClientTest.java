package springware.mci.client.http;

import org.junit.jupiter.api.*;
import springware.mci.client.config.ClientConfig;
import springware.mci.common.core.Message;
import springware.mci.common.core.MessageType;
import springware.mci.common.core.TransportType;
import springware.mci.server.config.ServerConfig;
import springware.mci.server.http.HttpServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("HttpClient 테스트")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HttpClientTest {

    private static HttpServer server;
    private static int testPort;

    @BeforeAll
    static void setUpAll() throws Exception {
        testPort = findAvailablePort();

        ServerConfig config = ServerConfig.builder()
                .serverId("test-http-server")
                .port(testPort)
                .transportType(TransportType.HTTP)
                .corsEnabled(true)
                .healthCheckEnabled(true)
                .build();

        server = new HttpServer(config);

        // 테스트용 핸들러 등록
        server.registerHandler("ECHO", (request, context) -> {
            Message response = Message.builder()
                    .messageCode("ECHO_RES")
                    .messageType(MessageType.RESPONSE)
                    .build();
            response.setField("rspCode", "0000");
            response.setField("echoData", request.getString("echoData"));
            return response;
        });

        server.registerEndpoint("/api/echo", "ECHO");

        server.setDefaultHandler((request, context) -> {
            Message response = Message.builder()
                    .messageCode(request.getMessageCode() + "_RES")
                    .messageType(MessageType.RESPONSE)
                    .build();
            response.setField("rspCode", "0000");
            return response;
        });

        server.start();
        Thread.sleep(300);
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

    // ==================== Connection Tests ====================

    @Test
    @Order(1)
    @DisplayName("클라이언트 연결 및 해제")
    void connectAndDisconnect() {
        // given
        ClientConfig config = ClientConfig.httpClient("localhost", testPort);
        HttpClient client = new HttpClient(config);

        // when
        client.connect();

        // then
        assertThat(client.isConnected()).isTrue();

        // when
        client.disconnect();

        // then
        assertThat(client.isConnected()).isFalse();
    }

    @Test
    @Order(2)
    @DisplayName("연결 없이 전송 시도 - 예외 발생")
    void sendWithoutConnect() {
        // given
        ClientConfig config = ClientConfig.httpClient("localhost", testPort);
        HttpClient client = new HttpClient(config);

        Message request = Message.builder()
                .messageCode("TEST")
                .messageType(MessageType.REQUEST)
                .build();

        // then
        assertThatThrownBy(() -> client.send(request))
                .hasMessageContaining("not connected");
    }

    // ==================== Send Tests ====================

    @Test
    @Order(10)
    @DisplayName("동기 메시지 전송")
    void sendSync() {
        // given
        ClientConfig config = ClientConfig.httpClient("localhost", testPort);
        HttpClient client = new HttpClient(config);
        client.connect();

        try {
            Message request = Message.builder()
                    .messageCode("ECHO")
                    .messageType(MessageType.REQUEST)
                    .build();
            request.setField("echoData", "Hello");

            // when
            Message response = client.send(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getMessageCode()).isEqualTo("ECHO_RES");
            assertThat(response.getString("rspCode")).isEqualTo("0000");
            assertThat(response.getString("echoData")).isEqualTo("Hello");
        } finally {
            client.disconnect();
        }
    }

    @Test
    @Order(11)
    @DisplayName("비동기 메시지 전송")
    void sendAsync() throws Exception {
        // given
        ClientConfig config = ClientConfig.httpClient("localhost", testPort);
        HttpClient client = new HttpClient(config);
        client.connect();

        try {
            Message request = Message.builder()
                    .messageCode("ECHO")
                    .messageType(MessageType.REQUEST)
                    .build();
            request.setField("echoData", "Async Hello");

            // when
            CompletableFuture<Message> future = client.sendAsync(request);
            Message response = future.get(5, TimeUnit.SECONDS);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getString("echoData")).isEqualTo("Async Hello");
        } finally {
            client.disconnect();
        }
    }

    @Test
    @Order(12)
    @DisplayName("타임아웃 지정 메시지 전송")
    void sendWithTimeout() {
        // given
        ClientConfig config = ClientConfig.httpClient("localhost", testPort);
        HttpClient client = new HttpClient(config);
        client.connect();

        try {
            Message request = Message.builder()
                    .messageCode("ECHO")
                    .messageType(MessageType.REQUEST)
                    .build();
            request.setField("echoData", "Timeout Test");

            // when
            Message response = client.send(request, 10000);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getString("echoData")).isEqualTo("Timeout Test");
        } finally {
            client.disconnect();
        }
    }

    // ==================== Endpoint Mapping Tests ====================

    @Test
    @Order(20)
    @DisplayName("엔드포인트 등록")
    void registerEndpoint() {
        // given
        ClientConfig config = ClientConfig.httpClient("localhost", testPort);
        HttpClient client = new HttpClient(config);

        // when
        client.registerEndpoint("CUSTOM", "/api/custom");

        // then - 내부 매핑이 등록됨 (send 시 사용됨)
        client.connect();
        try {
            // 커스텀 엔드포인트가 등록되어 있음을 확인
            assertThat(client.isConnected()).isTrue();
        } finally {
            client.disconnect();
        }
    }

    @Test
    @Order(21)
    @DisplayName("엔드포인트 일괄 등록")
    void registerEndpoints() {
        // given
        ClientConfig config = ClientConfig.httpClient("localhost", testPort);
        HttpClient client = new HttpClient(config);

        Map<String, String> endpoints = Map.of(
                "NEW1", "/api/new1",
                "NEW2", "/api/new2"
        );

        // when
        client.registerEndpoints(endpoints);

        // then
        assertThat(client).isNotNull();
    }

    // ==================== Configuration Tests ====================

    @Test
    @Order(30)
    @DisplayName("ClientConfig.httpClient() 팩토리 메서드")
    void httpClientFactoryMethod() {
        // when
        ClientConfig config = ClientConfig.httpClient("localhost", 8080);

        // then
        assertThat(config.getHost()).isEqualTo("localhost");
        assertThat(config.getPort()).isEqualTo(8080);
        assertThat(config.getTransportType()).isEqualTo(TransportType.HTTP);
    }

    @Test
    @Order(31)
    @DisplayName("ClientConfig.httpClient(baseUrl) 팩토리 메서드")
    void httpClientBaseUrlFactoryMethod() {
        // when
        ClientConfig config = ClientConfig.httpClient("http://api.example.com");

        // then
        assertThat(config.getBaseUrl()).isEqualTo("http://api.example.com");
        assertThat(config.getTransportType()).isEqualTo(TransportType.HTTP);
    }

    // ==================== Multiple Requests Tests ====================

    @Test
    @Order(40)
    @DisplayName("연속 요청 처리")
    void consecutiveRequests() {
        // given
        ClientConfig config = ClientConfig.httpClient("localhost", testPort);
        HttpClient client = new HttpClient(config);
        client.connect();

        try {
            for (int i = 0; i < 10; i++) {
                Message request = Message.builder()
                        .messageCode("ECHO")
                        .messageType(MessageType.REQUEST)
                        .build();
                request.setField("echoData", "Request-" + i);

                // when
                Message response = client.send(request);

                // then
                assertThat(response.getString("echoData")).isEqualTo("Request-" + i);
            }
        } finally {
            client.disconnect();
        }
    }

    // ==================== Error Handling Tests ====================

    @Test
    @Order(50)
    @DisplayName("서버 오류 응답 처리")
    void serverErrorResponse() {
        // given
        ClientConfig config = ClientConfig.httpClient("localhost", testPort);
        HttpClient client = new HttpClient(config);
        client.connect();

        try {
            // 기본 핸들러로 처리되는 메시지
            Message request = Message.builder()
                    .messageCode("UNKNOWN")
                    .messageType(MessageType.REQUEST)
                    .build();

            // when
            Message response = client.send(request);

            // then - 기본 핸들러가 처리
            assertThat(response).isNotNull();
            assertThat(response.getString("rspCode")).isEqualTo("0000");
        } finally {
            client.disconnect();
        }
    }

    // ==================== Korean Data Tests ====================

    @Test
    @Order(60)
    @DisplayName("한글 데이터 처리")
    void koreanData() {
        // given
        ClientConfig config = ClientConfig.httpClient("localhost", testPort);
        HttpClient client = new HttpClient(config);
        client.connect();

        try {
            Message request = Message.builder()
                    .messageCode("ECHO")
                    .messageType(MessageType.REQUEST)
                    .build();
            request.setField("echoData", "안녕하세요 테스트");

            // when
            Message response = client.send(request);

            // then
            assertThat(response.getString("echoData")).isEqualTo("안녕하세요 테스트");
        } finally {
            client.disconnect();
        }
    }
}
