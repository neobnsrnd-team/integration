package springware.mci.client.core;

/**
 * 연결 상태
 */
public enum ConnectionState {
    /**
     * 연결 안됨
     */
    DISCONNECTED,

    /**
     * 연결 중
     */
    CONNECTING,

    /**
     * 연결됨
     */
    CONNECTED,

    /**
     * 재연결 중
     */
    RECONNECTING,

    /**
     * 연결 종료 중
     */
    DISCONNECTING,

    /**
     * 연결 실패
     */
    FAILED
}
