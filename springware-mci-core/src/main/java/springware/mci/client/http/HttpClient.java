package springware.mci.client.http;

import lombok.extern.slf4j.Slf4j;
import springware.mci.client.config.ClientConfig;
import springware.mci.client.core.AbstractMciClient;
import springware.mci.common.core.Message;
import springware.mci.common.core.MessageType;
import springware.mci.common.exception.ConnectionException;
import springware.mci.common.http.HttpMessageConverter;
import springware.mci.common.layout.LayoutManager;
import springware.mci.common.logging.MessageLogger;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
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
    private final HttpMessageConverter messageConverter;
    private final Map<String, String> endpointMap;

    public HttpClient(ClientConfig config) {
        super(config);
        this.messageConverter = new HttpMessageConverter(config.getCharset());
        this.endpointMap = new ConcurrentHashMap<>();
    }

    public HttpClient(ClientConfig config, LayoutManager layoutManager, MessageLogger messageLogger) {
        super(config, layoutManager, messageLogger);
        this.messageConverter = new HttpMessageConverter(config.getCharset());
        this.endpointMap = new ConcurrentHashMap<>();
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
        try {
            java.net.http.HttpClient.Builder builder = java.net.http.HttpClient.newBuilder()
                    .version(java.net.http.HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofMillis(config.getConnectTimeout()));

            // followRedirects 설정
            if (config.isFollowRedirects()) {
                builder.followRedirects(java.net.http.HttpClient.Redirect.NORMAL);
            } else {
                builder.followRedirects(java.net.http.HttpClient.Redirect.NEVER);
            }

            // SSL/TLS 설정
            if (config.isSslEnabled()) {
                SSLContext sslContext = createSslContext();
                builder.sslContext(sslContext);

                // 호스트명 검증 스킵 (개발용)
                if (config.isSkipHostnameVerification()) {
                    System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");
                    log.warn("SSL hostname verification is disabled. Do not use in production!");
                }

                log.debug("SSL/TLS enabled with protocol: {}", config.getSslProtocol());
            }

            httpClient = builder.build();

            String scheme = config.isSslEnabled() ? "HTTPS" : "HTTP";
            log.debug("{} client initialized for {}:{}", scheme, config.getHost(), config.getPort());

        } catch (Exception e) {
            throw new ConnectionException("Failed to initialize HTTP client with SSL", e);
        }
    }

    /**
     * SSL Context 생성
     */
    private SSLContext createSslContext() throws Exception {
        KeyManager[] keyManagers = null;
        TrustManager[] trustManagers = null;

        // KeyStore 설정 (클라이언트 인증서)
        if (config.getKeyStorePath() != null && !config.getKeyStorePath().isEmpty()) {
            KeyStore keyStore = KeyStore.getInstance(config.getKeyStoreType());
            char[] password = config.getKeyStorePassword() != null
                    ? config.getKeyStorePassword().toCharArray()
                    : new char[0];

            try (FileInputStream fis = new FileInputStream(config.getKeyStorePath())) {
                keyStore.load(fis, password);
            }

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, password);
            keyManagers = kmf.getKeyManagers();

            log.debug("Loaded KeyStore from: {}", config.getKeyStorePath());
        }

        // TrustStore 설정 (서버 인증서 검증)
        if (config.isTrustAllCertificates()) {
            // 모든 인증서 신뢰 (개발용)
            trustManagers = new TrustManager[]{new TrustAllCertificatesManager()};
            log.warn("Trusting all SSL certificates. Do not use in production!");

        } else if (config.getTrustStorePath() != null && !config.getTrustStorePath().isEmpty()) {
            // 커스텀 TrustStore 사용
            KeyStore trustStore = KeyStore.getInstance(config.getTrustStoreType());
            char[] password = config.getTrustStorePassword() != null
                    ? config.getTrustStorePassword().toCharArray()
                    : new char[0];

            try (FileInputStream fis = new FileInputStream(config.getTrustStorePath())) {
                trustStore.load(fis, password);
            }

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
            trustManagers = tmf.getTrustManagers();

            log.debug("Loaded TrustStore from: {}", config.getTrustStorePath());
        }
        // trustManagers가 null이면 기본 TrustManager 사용 (시스템 CA 인증서)

        SSLContext sslContext = SSLContext.getInstance(config.getSslProtocol());
        sslContext.init(keyManagers, trustManagers, new SecureRandom());

        return sslContext;
    }

    /**
     * 모든 인증서를 신뢰하는 TrustManager (개발용)
     * 주의: 프로덕션에서 절대 사용 금지
     */
    private static class TrustAllCertificatesManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
            // 모든 클라이언트 인증서 신뢰
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
            // 모든 서버 인증서 신뢰
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
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
            // Message를 JSON으로 변환
            String jsonBody = messageConverter.toJson(message);
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
                // JSON 응답을 Message로 변환
                Message responseMessage = messageConverter.fromJson(body, request.getMessageCode(), MessageType.RESPONSE);

                // 수신 로깅
                byte[] responseBytes = body.getBytes(config.getCharset());
                responseMessage.setRawData(responseBytes);
                messageLogger.logReceive(responseMessage, layoutManager.getLayout(responseMessage.getMessageCode()), responseBytes);

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

    /**
     * 메시지 변환기 조회
     */
    public HttpMessageConverter getMessageConverter() {
        return messageConverter;
    }
}
