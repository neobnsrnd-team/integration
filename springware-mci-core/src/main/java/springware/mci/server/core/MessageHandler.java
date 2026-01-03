package springware.mci.server.core;

import springware.mci.common.core.Message;

/**
 * 메시지 처리 핸들러 인터페이스
 */
@FunctionalInterface
public interface MessageHandler {

    /**
     * 메시지 처리
     *
     * @param request 요청 메시지
     * @param context 처리 컨텍스트
     * @return 응답 메시지 (null이면 응답 없음)
     */
    Message handle(Message request, MessageContext context);
}
