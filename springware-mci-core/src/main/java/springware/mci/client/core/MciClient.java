package springware.mci.client.core;

import springware.mci.client.config.ClientConfig;
import springware.mci.common.core.Message;

import java.util.concurrent.CompletableFuture;

/**
 * MCI 클라이언트 인터페이스
 */
public interface MciClient extends AutoCloseable {

    /**
     * 서버에 연결
     */
    void connect();

    /**
     * 연결 해제
     */
    void disconnect();

    /**
     * 연결 상태 확인
     *
     * @return 연결 여부
     */
    boolean isConnected();

    /**
     * 동기 메시지 전송 (응답 대기)
     *
     * @param message 전송할 메시지
     * @return 응답 메시지
     */
    Message send(Message message);

    /**
     * 동기 메시지 전송 (커스텀 타임아웃)
     *
     * @param message       전송할 메시지
     * @param timeoutMillis 응답 대기 타임아웃 (밀리초)
     * @return 응답 메시지
     */
    Message send(Message message, long timeoutMillis);

    /**
     * 비동기 메시지 전송
     *
     * @param message 전송할 메시지
     * @return 응답 Future
     */
    CompletableFuture<Message> sendAsync(Message message);

    /**
     * 단방향 메시지 전송 (응답 없음)
     *
     * @param message 전송할 메시지
     */
    void sendOneWay(Message message);

    /**
     * 클라이언트 설정 조회
     *
     * @return 클라이언트 설정
     */
    ClientConfig getConfig();

    /**
     * 리소스 해제
     */
    @Override
    void close();
}
