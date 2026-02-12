package demo.mci.banking.biz;

import demo.mci.banking.entity.AccountRepository;
import demo.mci.common.DemoConstants;
import demo.mci.common.DemoMessageCodes;
import lombok.extern.slf4j.Slf4j;
import springware.mci.common.core.Message;
import springware.mci.common.core.MessageType;
import springware.mci.server.core.MessageContext;

/**
 * 잔액조회 비즈니스 로직
 */
@Slf4j
public class BalanceInquiryBiz extends AbstractBankBiz {

    private final AccountRepository accountRepository = AccountRepository.getInstance();

    @Override
    public String getMessageCode() {
        return DemoMessageCodes.BALANCE_INQUIRY_REQ;
    }

    @Override
    public Message execute(Message request, MessageContext context) {
        String accountNo = request.getString("accountNo");
        log.info("Balance inquiry for account: {}", maskAccount(accountNo));

        Message response = Message.builder()
                .messageCode(DemoMessageCodes.BALANCE_INQUIRY_RES)
                .messageType(MessageType.RESPONSE)
                .build();

        // 요청 헤더 복사
        copyHeader(request, response);
        response.setField("msgCode", DemoMessageCodes.BALANCE_INQUIRY_RES);
        response.setField("accountNo", accountNo);

        Long balance = accountRepository.getBalance(accountNo);
        if (balance != null) {
            response.setField("rspCode", DemoConstants.RSP_SUCCESS);
            response.setField("balance", balance);
        } else {
            response.setField("rspCode", DemoConstants.RSP_INVALID_ACCOUNT);
            response.setField("balance", 0L);
        }

        return response;
    }
}
