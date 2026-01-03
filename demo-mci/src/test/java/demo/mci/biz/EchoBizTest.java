package demo.mci.biz;

import demo.mci.common.DemoConstants;
import demo.mci.common.DemoMessageCodes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import springware.mci.common.core.Message;
import springware.mci.common.core.MessageType;
import springware.mci.server.core.MessageContext;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EchoBiz 테스트")
class EchoBizTest {

    private EchoBiz biz;
    private MessageContext context;

    @BeforeEach
    void setUp() {
        biz = new EchoBiz();
        context = BizTestHelper.createContext();
    }

    @Test
    @DisplayName("메시지 코드 확인")
    void getMessageCode() {
        assertThat(biz.getMessageCode()).isEqualTo(DemoMessageCodes.ECHO_REQ);
    }

    @Test
    @DisplayName("에코 데이터 반환")
    void executeEcho() {
        // given
        Message request = BizTestHelper.createRequest(DemoMessageCodes.ECHO_REQ);
        request.setField("echoData", "Hello, World!");

        // when
        Message response = biz.execute(request, context);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getMessageCode()).isEqualTo(DemoMessageCodes.ECHO_RES);
        assertThat(response.getMessageType()).isEqualTo(MessageType.RESPONSE);
        assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);
        assertThat(response.getString("echoData")).isEqualTo("Hello, World!");
    }

    @Test
    @DisplayName("빈 에코 데이터 처리")
    void executeEmptyEcho() {
        // given
        Message request = BizTestHelper.createRequest(DemoMessageCodes.ECHO_REQ);
        request.setField("echoData", "");

        // when
        Message response = biz.execute(request, context);

        // then
        assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);
        assertThat(response.getString("echoData")).isEqualTo("");
    }

    @Test
    @DisplayName("긴 에코 데이터 처리")
    void executeLongEcho() {
        // given
        String longData = "A".repeat(100);
        Message request = BizTestHelper.createRequest(DemoMessageCodes.ECHO_REQ);
        request.setField("echoData", longData);

        // when
        Message response = biz.execute(request, context);

        // then
        assertThat(response.getString("echoData")).isEqualTo(longData);
    }

    @Test
    @DisplayName("특수문자 포함 에코 데이터 처리")
    void executeSpecialCharEcho() {
        // given
        String specialData = "Hello! @#$%^&*() 한글테스트 日本語";
        Message request = BizTestHelper.createRequest(DemoMessageCodes.ECHO_REQ);
        request.setField("echoData", specialData);

        // when
        Message response = biz.execute(request, context);

        // then
        assertThat(response.getString("echoData")).isEqualTo(specialData);
    }

    @Test
    @DisplayName("요청 헤더가 응답에 복사됨")
    void headerCopied() {
        // given
        Message request = BizTestHelper.createRequest(DemoMessageCodes.ECHO_REQ);
        request.setField("echoData", "test");
        request.setField("orgCode", "888");
        request.setField("txDate", "20260115");

        // when
        Message response = biz.execute(request, context);

        // then
        assertThat(response.getString("orgCode")).isEqualTo("888");
        assertThat(response.getString("txDate")).isEqualTo("20260115");
    }
}
