package springware.mci.server.biz;

import springware.mci.common.core.Message;
import springware.mci.server.core.MessageContext;

/**
 * 비즈니스 로직 처리 인터페이스
 * 메시지 코드별로 구현체를 생성하여 BizRegistry에 등록
 */
public interface Biz {

    /**
     * 비즈니스 로직 실행
     *
     * @param request 요청 메시지
     * @param context 메시지 컨텍스트 (클라이언트 정보 등)
     * @return 응답 메시지
     */
    Message execute(Message request, MessageContext context);

    /**
     * 처리 가능한 메시지 코드 반환
     *
     * @return 메시지 코드
     */
    String getMessageCode();
}
