package springware.mci.client.circuitbreaker;

/**
 * 서킷 브레이커 상태
 */
public enum CircuitBreakerState {

    /**
     * 닫힘 - 정상 동작, 요청 통과
     */
    CLOSED,

    /**
     * 열림 - 요청 차단, 빠른 실패 반환
     */
    OPEN,

    /**
     * 반열림 - 테스트 요청 허용, 성공시 CLOSED로 전환
     */
    HALF_OPEN
}
