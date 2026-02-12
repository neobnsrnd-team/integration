package demo.mci.banking.biz;

import demo.mci.common.DemoConstants;
import demo.mci.common.DemoMessageCodes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import springware.mci.common.core.Message;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 계좌정보조회 Biz 테스트
 */
@DisplayName("계좌정보조회 Biz 테스트")
class AccountInquiryBizTest {

    private AccountInquiryBiz biz;

    @BeforeEach
    void setUp() {
        biz = new AccountInquiryBiz();
    }

    @Test
    @DisplayName("메시지 코드 확인")
    void getMessageCode() {
        assertThat(biz.getMessageCode()).isEqualTo(DemoMessageCodes.ACCOUNT_INFO_REQ);
    }

    @Test
    @DisplayName("보통예금 계좌정보 조회 성공")
    void executeSuccessForSavingsAccount() {
        // given
        Message request = createRequest("1234567890123456789");

        // when
        Message response = biz.execute(request, null);

        // then
        assertThat(response.getString("msgCode")).isEqualTo(DemoMessageCodes.ACCOUNT_INFO_RES);
        assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);
        assertThat(response.getString("accountNo")).isEqualTo("1234567890123456789");
        assertThat(response.getString("accountName")).isEqualTo("홍길동 보통예금");
        assertThat(response.getString("accountType")).isEqualTo("01");
        assertThat(response.getString("openDate")).isEqualTo("20200315");
        assertThat(response.getString("status")).isEqualTo("1");
        assertThat(response.getLong("balance")).isEqualTo(1000000L);
        assertThat(response.getLong("availableBalance")).isEqualTo(1000000L);
        assertThat(((Number) response.getField("interestRate")).intValue()).isEqualTo(150);
    }

    @Test
    @DisplayName("정기예금 계좌정보 조회 성공")
    void executeSuccessForFixedDeposit() {
        // given
        Message request = createRequest("1111222233334444555");

        // when
        Message response = biz.execute(request, null);

        // then
        assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);
        assertThat(response.getString("accountName")).isEqualTo("이영희 정기예금");
        assertThat(response.getString("accountType")).isEqualTo("02");
        assertThat(response.getLong("balance")).isEqualTo(2500000L);
        assertThat(response.getLong("availableBalance")).isEqualTo(0L);  // 정기예금은 출금 불가
        assertThat(((Number) response.getField("interestRate")).intValue()).isEqualTo(350);
    }

    @Test
    @DisplayName("존재하지 않는 계좌 조회")
    void executeInvalidAccount() {
        // given
        Message request = createRequest("0000000000000000000");

        // when
        Message response = biz.execute(request, null);

        // then
        assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_INVALID_ACCOUNT);
        assertThat(response.getString("accountName")).isEmpty();
        assertThat(response.getString("accountType")).isEqualTo("00");
        assertThat(response.getString("status")).isEqualTo("0");
        assertThat(response.getLong("balance")).isEqualTo(0L);
    }

    @Test
    @DisplayName("다른 계좌 정보 조회")
    void executeOtherAccount() {
        // given
        Message request = createRequest("9876543210987654321");

        // when
        Message response = biz.execute(request, null);

        // then
        assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);
        assertThat(response.getString("accountName")).isEqualTo("김철수 보통예금");
        assertThat(response.getString("accountType")).isEqualTo("01");
        assertThat(response.getString("openDate")).isEqualTo("20210620");
    }

    @Test
    @DisplayName("헤더 복사 확인")
    void headerCopy() {
        // given
        Message request = createRequest("1234567890123456789");
        request.setField("orgCode", "001");
        request.setField("txDate", "20240115");
        request.setField("txTime", "143022");
        request.setField("seqNo", "0000000001");

        // when
        Message response = biz.execute(request, null);

        // then
        assertThat(response.getString("orgCode")).isEqualTo("001");
        assertThat(response.getString("txDate")).isEqualTo("20240115");
        assertThat(response.getString("txTime")).isEqualTo("143022");
        assertThat(response.getString("seqNo")).isEqualTo("0000000001");
    }

    private Message createRequest(String accountNo) {
        Message request = Message.builder()
                .messageCode(DemoMessageCodes.ACCOUNT_INFO_REQ)
                .build();
        request.setField("msgCode", DemoMessageCodes.ACCOUNT_INFO_REQ);
        request.setField("orgCode", "001");
        request.setField("txDate", "20240115");
        request.setField("txTime", "143022");
        request.setField("seqNo", "0000000001");
        request.setField("rspCode", "0000");
        request.setField("filler", "");
        request.setField("accountNo", accountNo);
        return request;
    }
}
