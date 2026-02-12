package demo.mci.banking.biz;

import springware.mci.common.core.Message;
import springware.mci.common.core.MessageType;
import springware.mci.server.core.MessageContext;

import java.net.InetSocketAddress;

/**
 * Biz 테스트용 헬퍼 클래스
 */
public class BizTestHelper {

    /**
     * 테스트용 MessageContext 생성
     */
    public static MessageContext createContext() {
        return MessageContext.builder()
                .remoteAddress(new InetSocketAddress("127.0.0.1", 12345))
                .build();
    }

    /**
     * 지정된 IP로 테스트용 MessageContext 생성
     */
    public static MessageContext createContext(String ip, int port) {
        return MessageContext.builder()
                .remoteAddress(new InetSocketAddress(ip, port))
                .build();
    }

    /**
     * 기본 헤더가 설정된 요청 메시지 생성
     */
    public static Message createRequest(String messageCode) {
        Message message = Message.builder()
                .messageCode(messageCode)
                .messageType(MessageType.REQUEST)
                .build();

        message.setField("msgCode", messageCode);
        message.setField("orgCode", "001");
        message.setField("txDate", "20260104");
        message.setField("txTime", "120000");
        message.setField("seqNo", "0000000001");
        message.setField("rspCode", "0000");
        message.setField("filler", "");

        return message;
    }
}
