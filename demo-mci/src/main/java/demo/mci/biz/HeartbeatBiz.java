package demo.mci.biz;

import demo.mci.common.DemoConstants;
import demo.mci.common.DemoMessageCodes;
import lombok.extern.slf4j.Slf4j;
import springware.mci.common.core.Message;
import springware.mci.common.core.MessageType;
import springware.mci.server.core.MessageContext;

/**
 * 하트비트 비즈니스 로직
 */
@Slf4j
public class HeartbeatBiz extends AbstractDemoBiz {

    @Override
    public String getMessageCode() {
        return DemoMessageCodes.HEARTBEAT_REQ;
    }

    @Override
    public Message execute(Message request, MessageContext context) {
        log.debug("Heartbeat received from {}", context.getRemoteIp());

        Message response = Message.builder()
                .messageCode(DemoMessageCodes.HEARTBEAT_RES)
                .messageType(MessageType.RESPONSE)
                .build();

        copyHeader(request, response);
        response.setField("msgCode", DemoMessageCodes.HEARTBEAT_RES);
        response.setField("rspCode", DemoConstants.RSP_SUCCESS);

        return response;
    }
}
