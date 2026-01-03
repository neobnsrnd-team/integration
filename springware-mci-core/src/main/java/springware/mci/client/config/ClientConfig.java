package springware.mci.client.config;

import lombok.Builder;
import lombok.Getter;
import springware.mci.common.core.TransportType;
import springware.mci.common.logging.LogLevel;
import springware.mci.common.protocol.ProtocolConfig;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

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
     * SSL 인증서 경로
     */
    private final String sslCertPath;

    /**
     * SSL 키 경로
     */
    private final String sslKeyPath;

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
