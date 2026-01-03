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

@DisplayName("BalanceInquiryBiz 테스트")
class BalanceInquiryBizTest {

    private BalanceInquiryBiz biz;
    private MessageContext context;

    @BeforeEach
    void setUp() {
        biz = new BalanceInquiryBiz();
        context = BizTestHelper.createContext();
    }

    @Test
    @DisplayName("메시지 코드 확인")
    void getMessageCode() {
        assertThat(biz.getMessageCode()).isEqualTo(DemoMessageCodes.BALANCE_INQUIRY_REQ);
    }

    @Test
    @DisplayName("존재하는 계좌 잔액 조회 성공")
    void executeWithValidAccount() {
        // given
        Message request = BizTestHelper.createRequest(DemoMessageCodes.BALANCE_INQUIRY_REQ);
        request.setField("accountNo", "1234567890123456789");

        // when
        Message response = biz.execute(request, context);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getMessageCode()).isEqualTo(DemoMessageCodes.BALANCE_INQUIRY_RES);
        assertThat(response.getMessageType()).isEqualTo(MessageType.RESPONSE);
        assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);
        assertThat(response.getLong("balance")).isEqualTo(1000000L);
        assertThat(response.getString("accountNo")).isEqualTo("1234567890123456789");
    }

    @Test
    @DisplayName("존재하지 않는 계좌 조회시 에러 응답")
    void executeWithInvalidAccount() {
        // given
        Message request = BizTestHelper.createRequest(DemoMessageCodes.BALANCE_INQUIRY_REQ);
        request.setField("accountNo", "9999999999999999999");

        // when
        Message response = biz.execute(request, context);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_INVALID_ACCOUNT);
        assertThat(response.getLong("balance")).isEqualTo(0L);
    }

    @Test
    @DisplayName("요청 헤더가 응답에 복사됨")
    void headerCopied() {
        // given
        Message request = BizTestHelper.createRequest(DemoMessageCodes.BALANCE_INQUIRY_REQ);
        request.setField("accountNo", "1234567890123456789");
        request.setField("orgCode", "999");
        request.setField("seqNo", "0000000099");

        // when
        Message response = biz.execute(request, context);

        // then
        assertThat(response.getString("orgCode")).isEqualTo("999");
        assertThat(response.getString("seqNo")).isEqualTo("0000000099");
    }
}
