package springware.mci.common.logging;

import springware.mci.common.core.Message;
import springware.mci.common.layout.MessageLayout;

/**
 * 메시지 로거 인터페이스
 */
public interface MessageLogger {

    /**
     * 송신 메시지 로깅
     *
     * @param message  송신 메시지
     * @param layout   메시지 레이아웃
     * @param rawData  원본 바이트 데이터
     */
    void logSend(Message message, MessageLayout layout, byte[] rawData);

    /**
     * 수신 메시지 로깅
     *
     * @param message  수신 메시지
     * @param layout   메시지 레이아웃
     * @param rawData  원본 바이트 데이터
     */
    void logReceive(Message message, MessageLayout layout, byte[] rawData);

    /**
     * 헤더 로깅 (1단계)
     *
     * @param direction 방향 (SEND/RECV)
     * @param message   메시지
     * @param rawData   원본 데이터
     */
    void logHeader(String direction, Message message, byte[] rawData);

    /**
     * 상세 로깅 (2단계, 마스킹 적용)
     *
     * @param direction 방향 (SEND/RECV)
     * @param message   메시지
     * @param layout    레이아웃
     */
    void logDetail(String direction, Message message, MessageLayout layout);

    /**
     * 로깅 레벨 설정
     *
     * @param level 로깅 레벨
     */
    void setLogLevel(LogLevel level);

    /**
     * 현재 로깅 레벨 조회
     *
     * @return 로깅 레벨
     */
    LogLevel getLogLevel();

    /**
     * 마스킹 규칙 추가
     *
     * @param rule 마스킹 규칙
     */
    void addMaskingRule(MaskingRule rule);
}
