package demo.mci.banking.biz;

import demo.mci.common.DemoConstants;
import demo.mci.common.DemoMessageCodes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import springware.mci.common.core.Message;
import springware.mci.common.core.MessageType;
import springware.mci.server.core.MessageContext;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HeartbeatBiz 테스트")
class HeartbeatBizTest {

    private HeartbeatBiz biz;
    private MessageContext context;

    @BeforeEach
    void setUp() {
        biz = new HeartbeatBiz();
        context = BizTestHelper.createContext();
    }

    @Test
    @DisplayName("메시지 코드 확인")
    void getMessageCode() {
        assertThat(biz.getMessageCode()).isEqualTo(DemoMessageCodes.HEARTBEAT_REQ);
    }

    @Test
    @DisplayName("하트비트 응답 성공")
    void executeHeartbeat() {
        // given
        Message request = BizTestHelper.createRequest(DemoMessageCodes.HEARTBEAT_REQ);

        // when
        Message response = biz.execute(request, context);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getMessageCode()).isEqualTo(DemoMessageCodes.HEARTBEAT_RES);
        assertThat(response.getMessageType()).isEqualTo(MessageType.RESPONSE);
        assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);
        assertThat(response.getString("msgCode")).isEqualTo(DemoMessageCodes.HEARTBEAT_RES);
    }

    @Test
    @DisplayName("요청 헤더가 응답에 복사됨")
    void headerCopied() {
        // given
        Message request = BizTestHelper.createRequest(DemoMessageCodes.HEARTBEAT_REQ);
        request.setField("orgCode", "777");
        request.setField("seqNo", "0000000123");
        request.setField("txDate", "20260201");
        request.setField("txTime", "235959");

        // when
        Message response = biz.execute(request, context);

        // then
        assertThat(response.getString("orgCode")).isEqualTo("777");
        assertThat(response.getString("seqNo")).isEqualTo("0000000123");
        assertThat(response.getString("txDate")).isEqualTo("20260201");
        assertThat(response.getString("txTime")).isEqualTo("235959");
    }

    @Test
    @DisplayName("다른 클라이언트 IP에서 하트비트")
    void executeFromDifferentIp() {
        // given
        MessageContext otherContext = BizTestHelper.createContext("192.168.1.100", 54321);
        Message request = BizTestHelper.createRequest(DemoMessageCodes.HEARTBEAT_REQ);

        // when
        Message response = biz.execute(request, otherContext);

        // then
        assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);
    }

    @Test
    @DisplayName("연속 하트비트 요청 처리")
    void executeMultipleHeartbeats() {
        // given
        Message request = BizTestHelper.createRequest(DemoMessageCodes.HEARTBEAT_REQ);

        // when & then
        for (int i = 0; i < 10; i++) {
            Message response = biz.execute(request, context);
            assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);
        }
    }
}
