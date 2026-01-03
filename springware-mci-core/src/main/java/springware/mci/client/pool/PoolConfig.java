package springware.mci.client.pool;

import lombok.Builder;
import lombok.Getter;

/**
 * 연결 풀 설정
 */
@Getter
@Builder
public class PoolConfig {

    /**
     * 최소 연결 수
     */
    @Builder.Default
    private final int minSize = 1;

    /**
     * 최대 연결 수
     */
    @Builder.Default
    private final int maxSize = 10;

    /**
     * 연결 획득 대기 타임아웃 (밀리초)
     */
    @Builder.Default
    private final long acquireTimeout = 30000;

    /**
     * 유휴 연결 최대 유지 시간 (밀리초)
     */
    @Builder.Default
    private final long maxIdleTime = 300000;

    /**
     * 연결 유효성 검증 여부
     */
    @Builder.Default
    private final boolean validateOnAcquire = true;

    /**
     * 연결 유효성 검증 간격 (밀리초)
     */
    @Builder.Default
    private final long validationInterval = 30000;

    /**
     * 기본 설정
     */
    public static PoolConfig defaultConfig() {
        return PoolConfig.builder().build();
    }

    /**
     * 설정 유효성 검증
     */
    public void validate() {
        if (minSize < 0) {
            throw new IllegalArgumentException("minSize must be non-negative");
        }
        if (maxSize < 1) {
            throw new IllegalArgumentException("maxSize must be at least 1");
        }
        if (minSize > maxSize) {
            throw new IllegalArgumentException("minSize cannot be greater than maxSize");
        }
        if (acquireTimeout < 0) {
            throw new IllegalArgumentException("acquireTimeout must be non-negative");
        }
    }
}
