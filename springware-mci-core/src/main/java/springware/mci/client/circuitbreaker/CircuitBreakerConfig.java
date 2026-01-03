package springware.mci.client.circuitbreaker;

import lombok.Builder;
import lombok.Getter;

/**
 * 서킷 브레이커 설정
 */
@Getter
@Builder
public class CircuitBreakerConfig {

    /**
     * 서킷 브레이커 활성화 여부
     */
    @Builder.Default
    private final boolean enabled = true;

    /**
     * 실패 임계값 - 이 횟수만큼 실패하면 OPEN 상태로 전환
     */
    @Builder.Default
    private final int failureThreshold = 5;

    /**
     * 성공 임계값 - HALF_OPEN 상태에서 이 횟수만큼 성공하면 CLOSED로 전환
     */
    @Builder.Default
    private final int successThreshold = 3;

    /**
     * OPEN 상태 유지 시간 (밀리초) - 이 시간이 지나면 HALF_OPEN으로 전환
     */
    @Builder.Default
    private final long openTimeout = 30000;

    /**
     * HALF_OPEN 상태에서 허용할 테스트 요청 수
     */
    @Builder.Default
    private final int halfOpenPermittedCalls = 3;

    /**
     * 실패율 기반 임계값 사용 여부
     */
    @Builder.Default
    private final boolean failureRateThresholdEnabled = false;

    /**
     * 실패율 임계값 (0.0 ~ 1.0) - 이 비율 이상 실패하면 OPEN
     */
    @Builder.Default
    private final double failureRateThreshold = 0.5;

    /**
     * 실패율 계산을 위한 최소 호출 수
     */
    @Builder.Default
    private final int minimumNumberOfCalls = 10;

    /**
     * 슬라이딩 윈도우 크기 (호출 수 기반)
     */
    @Builder.Default
    private final int slidingWindowSize = 100;

    /**
     * 기본 설정
     */
    public static CircuitBreakerConfig defaultConfig() {
        return CircuitBreakerConfig.builder().build();
    }

    /**
     * 비활성화된 설정
     */
    public static CircuitBreakerConfig disabled() {
        return CircuitBreakerConfig.builder()
                .enabled(false)
                .build();
    }

    /**
     * 설정 유효성 검증
     */
    public void validate() {
        if (failureThreshold < 1) {
            throw new IllegalArgumentException("failureThreshold must be at least 1");
        }
        if (successThreshold < 1) {
            throw new IllegalArgumentException("successThreshold must be at least 1");
        }
        if (openTimeout < 0) {
            throw new IllegalArgumentException("openTimeout must be non-negative");
        }
        if (halfOpenPermittedCalls < 1) {
            throw new IllegalArgumentException("halfOpenPermittedCalls must be at least 1");
        }
        if (failureRateThreshold < 0 || failureRateThreshold > 1) {
            throw new IllegalArgumentException("failureRateThreshold must be between 0 and 1");
        }
    }
}
