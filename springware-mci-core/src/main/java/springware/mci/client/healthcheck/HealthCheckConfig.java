package springware.mci.client.healthcheck;

import lombok.Builder;
import lombok.Getter;

/**
 * 헬스 체크 설정
 */
@Getter
@Builder
public class HealthCheckConfig {

    /**
     * 헬스 체크 활성화 여부
     */
    @Builder.Default
    private final boolean enabled = true;

    /**
     * 헬스 체크 주기 (밀리초)
     */
    @Builder.Default
    private final long intervalMillis = 30000;

    /**
     * 헬스 체크 타임아웃 (밀리초)
     */
    @Builder.Default
    private final long timeoutMillis = 5000;

    /**
     * 연속 실패 허용 횟수
     */
    @Builder.Default
    private final int failureThreshold = 3;

    /**
     * 실패시 자동 재연결 여부
     */
    @Builder.Default
    private final boolean autoReconnect = true;

    /**
     * 초기 지연 시간 (밀리초)
     */
    @Builder.Default
    private final long initialDelayMillis = 5000;

    /**
     * 기본 설정
     */
    public static HealthCheckConfig defaultConfig() {
        return HealthCheckConfig.builder().build();
    }

    /**
     * 비활성화 설정
     */
    public static HealthCheckConfig disabled() {
        return HealthCheckConfig.builder()
                .enabled(false)
                .build();
    }

    /**
     * 빠른 헬스 체크 설정 (테스트용)
     */
    public static HealthCheckConfig fast() {
        return HealthCheckConfig.builder()
                .intervalMillis(5000)
                .timeoutMillis(2000)
                .failureThreshold(2)
                .initialDelayMillis(1000)
                .build();
    }

    /**
     * 설정 검증
     */
    public void validate() {
        if (enabled) {
            if (intervalMillis <= 0) {
                throw new IllegalArgumentException("Interval must be positive");
            }
            if (timeoutMillis <= 0) {
                throw new IllegalArgumentException("Timeout must be positive");
            }
            if (timeoutMillis >= intervalMillis) {
                throw new IllegalArgumentException("Timeout must be less than interval");
            }
            if (failureThreshold <= 0) {
                throw new IllegalArgumentException("Failure threshold must be positive");
            }
        }
    }
}
