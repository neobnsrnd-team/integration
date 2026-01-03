package springware.mci.server.core;

import springware.mci.server.config.ServerConfig;

/**
 * MCI 서버 인터페이스
 */
public interface MciServer extends AutoCloseable {

    /**
     * 서버 시작
     */
    void start();

    /**
     * 서버 종료
     */
    void stop();

    /**
     * 서버 실행 상태 확인
     *
     * @return 실행 중 여부
     */
    boolean isRunning();

    /**
     * 메시지 핸들러 등록
     *
     * @param messageCode 메시지 코드
     * @param handler     핸들러
     */
    void registerHandler(String messageCode, MessageHandler handler);

    /**
     * 기본 핸들러 설정
     *
     * @param handler 기본 핸들러
     */
    void setDefaultHandler(MessageHandler handler);

    /**
     * 서버 설정 조회
     *
     * @return 서버 설정
     */
    ServerConfig getConfig();

    /**
     * 리소스 해제
     */
    @Override
    void close();
}
