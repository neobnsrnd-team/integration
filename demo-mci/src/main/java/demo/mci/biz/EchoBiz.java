package demo.mci.biz;

import demo.mci.common.DemoConstants;
import demo.mci.common.DemoMessageCodes;
import lombok.extern.slf4j.Slf4j;
import springware.mci.common.core.Message;
import springware.mci.common.core.MessageType;
import springware.mci.server.core.MessageContext;

/**
 * 에코 비즈니스 로직
 */
@Slf4j
public class EchoBiz extends AbstractDemoBiz {

    @Override
    public String getMessageCode() {
        return DemoMessageCodes.ECHO_REQ;
    }

    @Override
    public Message execute(Message request, MessageContext context) {
        log.debug("Echo request received");

        Message response = Message.builder()
                .messageCode(DemoMessageCodes.ECHO_RES)
                .messageType(MessageType.RESPONSE)
                .build();

        copyHeader(request, response);
        response.setField("msgCode", DemoMessageCodes.ECHO_RES);
        response.setField("rspCode", DemoConstants.RSP_SUCCESS);
        response.setField("echoData", request.getString("echoData"));

        return response;
    }
}
