package springware.mci.client.healthcheck;

/**
 * 헬스 체크 이벤트 리스너
 */
public interface HealthCheckListener {

    /**
     * 헬스 체크 성공시 호출
     *
     * @param latencyMillis 응답 시간 (밀리초)
     */
    default void onHealthCheckSuccess(long latencyMillis) {}

    /**
     * 헬스 체크 실패시 호출
     *
     * @param consecutiveFailures 연속 실패 횟수
     * @param cause 실패 원인
     */
    default void onHealthCheckFailure(int consecutiveFailures, Throwable cause) {}

    /**
     * 연결이 불건전(unhealthy) 상태로 판단되었을 때 호출
     * (연속 실패 횟수가 임계값에 도달)
     */
    default void onConnectionUnhealthy() {}

    /**
     * 연결이 복구되었을 때 호출
     */
    default void onConnectionRecovered() {}
}
