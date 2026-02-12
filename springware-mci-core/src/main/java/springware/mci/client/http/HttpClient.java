package springware.mci.client.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import springware.mci.client.config.ClientConfig;
import springware.mci.client.core.AbstractMciClient;
import springware.mci.common.core.Message;
import springware.mci.common.core.MessageType;
import springware.mci.common.exception.ConnectionException;
import springware.mci.common.layout.LayoutManager;
import springware.mci.common.logging.MessageLogger;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HTTP/REST 기반 MCI 클라이언트
 * Java 11+ HttpClient 사용
 */
@Slf4j
public class HttpClient extends AbstractMciClient {

    private java.net.http.HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Map<String, String> endpointMap;

    /**
     * 기본 엔드포인트 매핑
     */
    private static final Map<String, String> DEFAULT_ENDPOINTS = Map.of(
            "BAL1", "/api/balance",
            "TRF1", "/api/transfer",
            "TXH1", "/api/transactions",
            "ACT1", "/api/account",
            "ECH1", "/api/echo",
            "HBT1", "/api/heartbeat"
    );

    public HttpClient(ClientConfig config) {
        super(config);
        this.objectMapper = new ObjectMapper();
        this.endpointMap = new ConcurrentHashMap<>(DEFAULT_ENDPOINTS);
    }

    public HttpClient(ClientConfig config, LayoutManager layoutManager, MessageLogger messageLogger) {
        super(config, layoutManager, messageLogger);
        this.objectMapper = new ObjectMapper();
        this.endpointMap = new ConcurrentHashMap<>(DEFAULT_ENDPOINTS);
    }

    /**
     * 엔드포인트 매핑 추가
     */
    public void registerEndpoint(String messageCode, String endpoint) {
        endpointMap.put(messageCode, endpoint);
        log.debug("Registered endpoint: {} -> {}", messageCode, endpoint);
    }

    /**
     * 엔드포인트 매핑 일괄 등록
     */
    public void registerEndpoints(Map<String, String> endpoints) {
        endpointMap.putAll(endpoints);
        log.debug("Registered {} endpoints", endpoints.size());
    }

    @Override
    protected void doConnect() {
        java.net.http.HttpClient.Builder builder = java.net.http.HttpClient.newBuilder()
                .version(java.net.http.HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(config.getConnectTimeout()));

        // followRedirects 설정
        if (config.isFollowRedirects()) {
            builder.followRedirects(java.net.http.HttpClient.Redirect.NORMAL);
        } else {
            builder.followRedirects(java.net.http.HttpClient.Redirect.NEVER);
        }

        httpClient = builder.build();
        log.debug("HTTP client initialized for {}:{}", config.getHost(), config.getPort());
    }

    @Override
    protected void doDisconnect() {
        // Java HttpClient는 명시적 close가 필요 없음
        httpClient = null;
        log.debug("HTTP client disconnected");
    }

    @Override
    public CompletableFuture<Message> sendAsync(Message message) {
        ensureConnected();

        String endpoint = getEndpoint(message.getMessageCode());
        String url = buildUrl(endpoint);

        try {
            // JSON 요청 바디 생성
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("messageCode", message.getMessageCode());
            requestBody.put("messageId", message.getMessageId());
            requestBody.put("fields", message.getFields());

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            byte[] bodyBytes = jsonBody.getBytes(config.getCharset());

            // HTTP 요청 빌드
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(config.getReadTimeout()))
                    .header("Content-Type", "application/json; charset=" + config.getCharset().name())
                    .header("Accept", "application/json");

            // 추가 헤더 설정
            if (config.getHttpHeaders() != null) {
                config.getHttpHeaders().forEach(requestBuilder::header);
            }

            HttpRequest request = requestBuilder
                    .POST(HttpRequest.BodyPublishers.ofByteArray(bodyBytes))
                    .build();

            // 송신 로깅
            messageLogger.logSend(message, layoutManager.getLayout(message.getMessageCode()), bodyBytes);

            log.debug("Sending HTTP request to {}: {}", url, message.getMessageId());

            // 비동기 전송
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> processResponse(message, response));

        } catch (Exception e) {
            CompletableFuture<Message> future = new CompletableFuture<>();
            future.completeExceptionally(new ConnectionException("Failed to send HTTP request", e));
            return future;
        }
    }

    @Override
    protected void doSendOneWay(Message message) {
        // HTTP는 기본적으로 요청-응답 모델이므로 응답을 무시
        sendAsync(message).whenComplete((response, error) -> {
            if (error != null) {
                log.warn("One-way HTTP request failed: {}", error.getMessage());
            }
        });
    }

    /**
     * HTTP 응답 처리
     */
    private Message processResponse(Message request, HttpResponse<String> response) {
        try {
            int statusCode = response.statusCode();
            String body = response.body();

            log.debug("Received HTTP response: status={}, body length={}", statusCode, body.length());

            if (statusCode >= 200 && statusCode < 300) {
                // JSON 응답 파싱
                Map<String, Object> responseBody = objectMapper.readValue(
                        body, new TypeReference<Map<String, Object>>() {});

                String messageCode = (String) responseBody.get("messageCode");
                String messageId = (String) responseBody.getOrDefault("messageId", request.getMessageId());

                @SuppressWarnings("unchecked")
                Map<String, Object> fields = (Map<String, Object>) responseBody.getOrDefault("fields", new HashMap<>());

                Message responseMessage = Message.builder()
                        .messageId(messageId)
                        .messageCode(messageCode)
                        .messageType(MessageType.RESPONSE)
                        .build();

                fields.forEach(responseMessage::setField);

                // 수신 로깅
                byte[] responseBytes = body.getBytes(config.getCharset());
                responseMessage.setRawData(responseBytes);
                messageLogger.logReceive(responseMessage, layoutManager.getLayout(messageCode), responseBytes);

                return responseMessage;

            } else {
                // 오류 응답
                log.warn("HTTP error response: status={}, body={}", statusCode, body);

                Message errorResponse = Message.builder()
                        .messageCode(request.getMessageCode())
                        .messageType(MessageType.RESPONSE)
                        .build();

                errorResponse.setField("rspCode", "9999");
                errorResponse.setField("httpStatus", statusCode);
                errorResponse.setField("errorMessage", body);

                return errorResponse;
            }

        } catch (Exception e) {
            log.error("Failed to process HTTP response", e);

            Message errorResponse = Message.builder()
                    .messageCode(request.getMessageCode())
                    .messageType(MessageType.RESPONSE)
                    .build();

            errorResponse.setField("rspCode", "9999");
            errorResponse.setField("errorMessage", e.getMessage());

            return errorResponse;
        }
    }

    /**
     * 메시지 코드에 해당하는 엔드포인트 조회
     */
    private String getEndpoint(String messageCode) {
        String endpoint = endpointMap.get(messageCode);
        if (endpoint == null) {
            // 기본 엔드포인트: /api/{messageCode}
            endpoint = config.getApiBasePath() + "/" + messageCode.toLowerCase();
        }
        return endpoint;
    }

    /**
     * 전체 URL 생성
     */
    private String buildUrl(String endpoint) {
        String baseUrl = config.getBaseUrl();
        if (baseUrl != null && !baseUrl.isEmpty()) {
            return baseUrl + endpoint;
        }

        String scheme = config.isSslEnabled() ? "https" : "http";
        return String.format("%s://%s:%d%s", scheme, config.getHost(), config.getPort(), endpoint);
    }

    /**
     * HTTP GET 요청 (하트비트 등)
     */
    public CompletableFuture<Message> sendGet(String messageCode) {
        ensureConnected();

        String endpoint = getEndpoint(messageCode);
        String url = buildUrl(endpoint);

        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(config.getReadTimeout()))
                    .header("Accept", "application/json");

            if (config.getHttpHeaders() != null) {
                config.getHttpHeaders().forEach(requestBuilder::header);
            }

            HttpRequest request = requestBuilder.GET().build();

            log.debug("Sending HTTP GET request to {}", url);

            Message dummyRequest = Message.builder()
                    .messageCode(messageCode)
                    .messageType(MessageType.REQUEST)
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> processResponse(dummyRequest, response));

        } catch (Exception e) {
            CompletableFuture<Message> future = new CompletableFuture<>();
            future.completeExceptionally(new ConnectionException("Failed to send HTTP GET request", e));
            return future;
        }
    }
}
