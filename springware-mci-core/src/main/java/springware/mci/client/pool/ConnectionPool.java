package springware.mci.client.pool;

import springware.mci.client.core.MciClient;

/**
 * 연결 풀 인터페이스
 *
 * @param <T> 클라이언트 타입
 */
public interface ConnectionPool<T extends MciClient> extends AutoCloseable {

    /**
     * 풀에서 연결 획득
     *
     * @return 클라이언트 연결
     */
    T acquire();

    /**
     * 풀에서 연결 획득 (타임아웃 지정)
     *
     * @param timeoutMillis 대기 타임아웃 (밀리초)
     * @return 클라이언트 연결
     */
    T acquire(long timeoutMillis);

    /**
     * 연결 반환
     *
     * @param client 반환할 클라이언트
     */
    void release(T client);

    /**
     * 현재 사용 가능한 연결 수
     *
     * @return 가용 연결 수
     */
    int getAvailableCount();

    /**
     * 현재 사용 중인 연결 수
     *
     * @return 사용 중인 연결 수
     */
    int getActiveCount();

    /**
     * 풀 크기
     *
     * @return 총 연결 수
     */
    int getPoolSize();

    /**
     * 풀 종료
     */
    @Override
    void close();
}
