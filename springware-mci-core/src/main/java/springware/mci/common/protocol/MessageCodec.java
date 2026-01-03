package springware.mci.common.protocol;

import springware.mci.common.core.Message;

/**
 * 메시지 인코딩/디코딩 인터페이스
 */
public interface MessageCodec {

    /**
     * 메시지를 바이트 배열로 인코딩
     *
     * @param message 인코딩할 메시지
     * @return 인코딩된 바이트 배열
     */
    byte[] encode(Message message);

    /**
     * 바이트 배열을 메시지로 디코딩
     *
     * @param data 디코딩할 바이트 배열
     * @return 디코딩된 메시지
     */
    Message decode(byte[] data);

    /**
     * 프로토콜 설정 조회
     *
     * @return 프로토콜 설정
     */
    ProtocolConfig getConfig();
}
