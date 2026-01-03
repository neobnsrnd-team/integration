package springware.mci.server.config;

import lombok.Builder;
import lombok.Getter;
import springware.mci.common.core.TransportType;
import springware.mci.common.logging.LogLevel;
import springware.mci.common.protocol.ProtocolConfig;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 서버 설정
 */
@Getter
@Builder
public class ServerConfig {

    /**
     * 서버 ID
     */
    private final String serverId;

    /**
     * 바인딩 호스트 (기본: 0.0.0.0)
     */
    @Builder.Default
    private final String host = "0.0.0.0";

    /**
     * 바인딩 포트
     */
    private final int port;

    /**
     * 전송 프로토콜
     */
    @Builder.Default
    private final TransportType transportType = TransportType.TCP;

    /**
     * Boss 스레드 수 (TCP용)
     */
    @Builder.Default
    private final int bossThreads = 1;

    /**
     * Worker 스레드 수
     */
    @Builder.Default
    private final int workerThreads = 0; // 0 = default (CPU cores * 2)

    /**
     * 연결 백로그
     */
    @Builder.Default
    private final int backlog = 128;

    /**
     * 유휴 연결 타임아웃 (밀리초)
     */
    @Builder.Default
    private final int idleTimeout = 60000;

    /**
     * 읽기 타임아웃 (밀리초)
     */
    @Builder.Default
    private final int readTimeout = 30000;

    /**
     * 쓰기 타임아웃 (밀리초)
     */
    @Builder.Default
    private final int writeTimeout = 10000;

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
     * SO_REUSEADDR 사용 여부
     */
    @Builder.Default
    private final boolean reuseAddress = true;

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
     * 기본 TCP 서버 설정
     */
    public static ServerConfig tcpServer(int port) {
        return ServerConfig.builder()
                .serverId("tcp-server-" + System.currentTimeMillis())
                .port(port)
                .transportType(TransportType.TCP)
                .build();
    }

    /**
     * 기본 UDP 서버 설정
     */
    public static ServerConfig udpServer(int port) {
        return ServerConfig.builder()
                .serverId("udp-server-" + System.currentTimeMillis())
                .port(port)
                .transportType(TransportType.UDP)
                .build();
    }

    /**
     * 설정 유효성 검증
     */
    public void validate() {
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
        if (bossThreads < 0) {
            throw new IllegalArgumentException("Boss threads must be non-negative");
        }
        if (workerThreads < 0) {
            throw new IllegalArgumentException("Worker threads must be non-negative");
        }
    }
}
