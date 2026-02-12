package springware.mci.client.config;

import lombok.Builder;
import lombok.Getter;
import springware.mci.client.circuitbreaker.CircuitBreakerConfig;
import springware.mci.client.healthcheck.HealthCheckConfig;
import springware.mci.common.core.TransportType;
import springware.mci.common.logging.LogLevel;
import springware.mci.common.protocol.ProtocolConfig;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 클라이언트 설정
 */
@Getter
@Builder
public class ClientConfig {

    /**
     * 클라이언트 ID
     */
    private final String clientId;

    /**
     * 서버 호스트
     */
    private final String host;

    /**
     * 서버 포트
     */
    private final int port;

    /**
     * 전송 프로토콜
     */
    @Builder.Default
    private final TransportType transportType = TransportType.TCP;

    /**
     * 연결 타임아웃 (밀리초)
     */
    @Builder.Default
    private final int connectTimeout = 10000;

    /**
     * 응답 대기 타임아웃 (밀리초)
     */
    @Builder.Default
    private final int readTimeout = 30000;

    /**
     * 쓰기 타임아웃 (밀리초)
     */
    @Builder.Default
    private final int writeTimeout = 10000;

    /**
     * 유휴 연결 타임아웃 (밀리초)
     */
    @Builder.Default
    private final int idleTimeout = 60000;

    /**
     * 재연결 시도 횟수
     */
    @Builder.Default
    private final int reconnectAttempts = 3;

    /**
     * 재연결 대기 시간 (밀리초)
     */
    @Builder.Default
    private final int reconnectDelay = 1000;

    /**
     * 초기 연결시 재시도 사용 여부
     */
    @Builder.Default
    private final boolean retryEnabled = true;

    /**
     * 초기 연결 재시도 횟수
     */
    @Builder.Default
    private final int retryAttempts = 3;

    /**
     * 초기 연결 재시도 대기 시간 (밀리초)
     */
    @Builder.Default
    private final int retryDelay = 1000;

    /**
     * 재시도 대기 시간 증가 배율 (exponential backoff)
     */
    @Builder.Default
    private final double retryBackoffMultiplier = 1.5;

    /**
     * 연결 풀 크기 (TCP용)
     */
    @Builder.Default
    private final int poolSize = 1;

    /**
     * Keep-Alive 사용 여부
     */
    @Builder.Default
    private final boolean keepAlive = true;

    /**
     * TCP_NODELAY 사용 여부
     */
    @Builder.Default
    private final boolean tcpNoDelay = true;

    /**
     * 프로토콜 설정
     */
    @Builder.Default
    private final ProtocolConfig protocolConfig = ProtocolConfig.defaultConfig();

    /**
     * 문자셋
     */
    @Builder.Default
    private final Charset charset = StandardCharsets.UTF_8;

    /**
     * 로깅 레벨
     */
    @Builder.Default
    private final LogLevel logLevel = LogLevel.DETAIL_MASKED;

    /**
     * 레이아웃 파일 경로
     */
    private final String layoutPath;

    /**
     * SSL/TLS 사용 여부
     */
    @Builder.Default
    private final boolean sslEnabled = false;

    /**
     * SSL 인증서 경로 (클라이언트 인증서)
     */
    private final String sslCertPath;

    /**
     * SSL 키 경로 (클라이언트 키)
     */
    private final String sslKeyPath;

    /**
     * KeyStore 경로 (PKCS12/JKS)
     */
    private final String keyStorePath;

    /**
     * KeyStore 비밀번호
     */
    private final String keyStorePassword;

    /**
     * KeyStore 타입 (PKCS12, JKS)
     */
    @Builder.Default
    private final String keyStoreType = "PKCS12";

    /**
     * TrustStore 경로
     */
    private final String trustStorePath;

    /**
     * TrustStore 비밀번호
     */
    private final String trustStorePassword;

    /**
     * TrustStore 타입 (PKCS12, JKS)
     */
    @Builder.Default
    private final String trustStoreType = "PKCS12";

    /**
     * 모든 인증서 신뢰 여부 (개발용, 프로덕션에서 사용 금지)
     */
    @Builder.Default
    private final boolean trustAllCertificates = false;

    /**
     * 호스트명 검증 스킵 여부 (개발용, 프로덕션에서 사용 금지)
     */
    @Builder.Default
    private final boolean skipHostnameVerification = false;

    /**
     * SSL/TLS 프로토콜 버전 (TLSv1.2, TLSv1.3)
     */
    @Builder.Default
    private final String sslProtocol = "TLS";

    // ========== HTTP 전용 설정 ==========

    /**
     * Base URL (HTTP 클라이언트용, 예: http://api.example.com)
     */
    private final String baseUrl;

    /**
     * HTTP 헤더 (HTTP 클라이언트용)
     */
    private final Map<String, String> httpHeaders;

    /**
     * 리다이렉트 따르기 여부 (HTTP 클라이언트용)
     */
    @Builder.Default
    private final boolean followRedirects = true;

    /**
     * API 기본 경로 (HTTP 클라이언트용)
     */
    @Builder.Default
    private final String apiBasePath = "/api";

    /**
     * 서킷 브레이커 설정
     */
    @Builder.Default
    private final CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.defaultConfig();

    /**
     * 헬스 체크 설정
     */
    @Builder.Default
    private final HealthCheckConfig healthCheckConfig = HealthCheckConfig.disabled();

    /**
     * 기본 TCP 클라이언트 설정
     */
    public static ClientConfig tcpClient(String host, int port) {
        return ClientConfig.builder()
                .clientId("tcp-client-" + System.currentTimeMillis())
                .host(host)
                .port(port)
                .transportType(TransportType.TCP)
                .build();
    }

    /**
     * 기본 UDP 클라이언트 설정
     */
    public static ClientConfig udpClient(String host, int port) {
        return ClientConfig.builder()
                .clientId("udp-client-" + System.currentTimeMillis())
                .host(host)
                .port(port)
                .transportType(TransportType.UDP)
                .build();
    }

    /**
     * 기본 HTTP 클라이언트 설정
     */
    public static ClientConfig httpClient(String host, int port) {
        return ClientConfig.builder()
                .clientId("http-client-" + System.currentTimeMillis())
                .host(host)
                .port(port)
                .transportType(TransportType.HTTP)
                .build();
    }

    /**
     * HTTP 클라이언트 설정 (Base URL 사용)
     */
    public static ClientConfig httpClient(String baseUrl) {
        return ClientConfig.builder()
                .clientId("http-client-" + System.currentTimeMillis())
                .host("localhost")  // dummy - baseUrl이 우선
                .port(80)           // dummy - baseUrl이 우선
                .baseUrl(baseUrl)
                .transportType(TransportType.HTTP)
                .build();
    }

    /**
     * HTTPS 클라이언트 설정 (SSL 활성화)
     */
    public static ClientConfig httpsClient(String host, int port) {
        return ClientConfig.builder()
                .clientId("https-client-" + System.currentTimeMillis())
                .host(host)
                .port(port)
                .transportType(TransportType.HTTP)
                .sslEnabled(true)
                .build();
    }

    /**
     * HTTPS 클라이언트 설정 (Base URL 사용)
     */
    public static ClientConfig httpsClient(String baseUrl) {
        return ClientConfig.builder()
                .clientId("https-client-" + System.currentTimeMillis())
                .host("localhost")  // dummy - baseUrl이 우선
                .port(443)          // dummy - baseUrl이 우선
                .baseUrl(baseUrl)
                .transportType(TransportType.HTTP)
                .sslEnabled(true)
                .build();
    }

    /**
     * HTTPS 클라이언트 설정 (개발용 - 모든 인증서 신뢰)
     * 주의: 프로덕션에서 사용 금지
     */
    public static ClientConfig httpsClientTrustAll(String host, int port) {
        return ClientConfig.builder()
                .clientId("https-client-" + System.currentTimeMillis())
                .host(host)
                .port(port)
                .transportType(TransportType.HTTP)
                .sslEnabled(true)
                .trustAllCertificates(true)
                .skipHostnameVerification(true)
                .build();
    }

    /**
     * 설정 유효성 검증
     */
    public void validate() {
        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("Host must not be empty");
        }
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
        if (connectTimeout <= 0) {
            throw new IllegalArgumentException("Connect timeout must be positive");
        }
        if (readTimeout <= 0) {
            throw new IllegalArgumentException("Read timeout must be positive");
        }
    }
}
