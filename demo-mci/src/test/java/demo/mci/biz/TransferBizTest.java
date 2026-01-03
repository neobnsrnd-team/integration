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

@DisplayName("TransferBiz 테스트")
class TransferBizTest {

    private TransferBiz biz;
    private MessageContext context;
    private AccountRepository accountRepository;

    @BeforeEach
    void setUp() {
        biz = new TransferBiz();
        context = BizTestHelper.createContext();
        accountRepository = AccountRepository.getInstance();

        // 테스트 데이터 리셋 (초기값으로 복원)
        accountRepository.setBalance("1234567890123456789", 1000000L);
        accountRepository.setBalance("9876543210987654321", 500000L);
    }

    @Test
    @DisplayName("메시지 코드 확인")
    void getMessageCode() {
        assertThat(biz.getMessageCode()).isEqualTo(DemoMessageCodes.TRANSFER_REQ);
    }

    @Test
    @DisplayName("이체 성공")
    void executeTransferSuccess() {
        // given
        Message request = createTransferRequest(
                "1234567890123456789",
                "9876543210987654321",
                100000L
        );

        // when
        Message response = biz.execute(request, context);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getMessageCode()).isEqualTo(DemoMessageCodes.TRANSFER_RES);
        assertThat(response.getMessageType()).isEqualTo(MessageType.RESPONSE);
        assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);
        assertThat(response.getLong("fromBalance")).isEqualTo(900000L);

        // 잔액 변경 확인
        assertThat(accountRepository.getBalance("1234567890123456789")).isEqualTo(900000L);
        assertThat(accountRepository.getBalance("9876543210987654321")).isEqualTo(600000L);
    }

    @Test
    @DisplayName("잔액 부족시 이체 실패")
    void executeInsufficientBalance() {
        // given
        Message request = createTransferRequest(
                "1234567890123456789",
                "9876543210987654321",
                2000000L // 잔액보다 큰 금액
        );

        // when
        Message response = biz.execute(request, context);

        // then
        assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_INSUFFICIENT_BALANCE);
        assertThat(response.getLong("fromBalance")).isEqualTo(1000000L); // 변경 없음

        // 잔액 변경 없음 확인
        assertThat(accountRepository.getBalance("1234567890123456789")).isEqualTo(1000000L);
    }

    @Test
    @DisplayName("존재하지 않는 출금계좌")
    void executeInvalidFromAccount() {
        // given
        Message request = createTransferRequest(
                "9999999999999999999",
                "9876543210987654321",
                100000L
        );

        // when
        Message response = biz.execute(request, context);

        // then
        assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_INVALID_ACCOUNT);
        assertThat(response.getLong("fromBalance")).isEqualTo(0L);
    }

    @Test
    @DisplayName("존재하지 않는 입금계좌로 이체 (출금만 수행)")
    void executeInvalidToAccount() {
        // given
        Message request = createTransferRequest(
                "1234567890123456789",
                "9999999999999999999", // 존재하지 않는 계좌
                100000L
        );

        // when
        Message response = biz.execute(request, context);

        // then
        assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);
        assertThat(response.getLong("fromBalance")).isEqualTo(900000L);

        // 출금계좌에서만 차감됨
        assertThat(accountRepository.getBalance("1234567890123456789")).isEqualTo(900000L);
    }

    @Test
    @DisplayName("요청 필드가 응답에 복사됨")
    void fieldsCopied() {
        // given
        Message request = createTransferRequest(
                "1234567890123456789",
                "9876543210987654321",
                50000L
        );

        // when
        Message response = biz.execute(request, context);

        // then
        assertThat(response.getString("fromAccount")).isEqualTo("1234567890123456789");
        assertThat(response.getString("toAccount")).isEqualTo("9876543210987654321");
        assertThat(response.getLong("amount")).isEqualTo(50000L);
    }

    private Message createTransferRequest(String fromAccount, String toAccount, Long amount) {
        Message request = BizTestHelper.createRequest(DemoMessageCodes.TRANSFER_REQ);
        request.setField("fromAccount", fromAccount);
        request.setField("toAccount", toAccount);
        request.setField("amount", amount);
        request.setField("memo", "Test Transfer");
        return request;
    }
}
