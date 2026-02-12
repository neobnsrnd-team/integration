package demo.mci.banking.biz;

import demo.mci.banking.entity.Account;
import demo.mci.banking.entity.AccountRepository;
import demo.mci.common.DemoConstants;
import demo.mci.common.DemoMessageCodes;
import lombok.extern.slf4j.Slf4j;
import springware.mci.common.core.Message;
import springware.mci.common.core.MessageType;
import springware.mci.server.core.MessageContext;

/**
 * 계좌정보조회 비즈니스 로직
 */
@Slf4j
public class AccountInquiryBiz extends AbstractBankBiz {

    private final AccountRepository accountRepository = AccountRepository.getInstance();

    @Override
    public String getMessageCode() {
        return DemoMessageCodes.ACCOUNT_INFO_REQ;
    }

    @Override
    public Message execute(Message request, MessageContext context) {
        String accountNo = request.getString("accountNo");
        log.info("Account inquiry for account: {}", maskAccount(accountNo));

        Message response = Message.builder()
                .messageCode(DemoMessageCodes.ACCOUNT_INFO_RES)
                .messageType(MessageType.RESPONSE)
                .build();

        // 요청 헤더 복사
        copyHeader(request, response);
        response.setField("msgCode", DemoMessageCodes.ACCOUNT_INFO_RES);
        response.setField("accountNo", accountNo);

        Account account = accountRepository.getAccount(accountNo);
        if (account != null) {
            response.setField("rspCode", DemoConstants.RSP_SUCCESS);
            response.setField("accountName", account.getAccountName());
            response.setField("accountType", account.getAccountType());
            response.setField("openDate", account.getOpenDate());
            response.setField("status", account.getStatus());
            response.setField("balance", account.getBalance());
            response.setField("availableBalance", account.getAvailableBalance());
            response.setField("interestRate", account.getInterestRate());

            log.info("Account found: {} ({})", account.getAccountName(), getAccountTypeName(account.getAccountType()));
        } else {
            response.setField("rspCode", DemoConstants.RSP_INVALID_ACCOUNT);
            response.setField("accountName", "");
            response.setField("accountType", "00");
            response.setField("openDate", "00000000");
            response.setField("status", "0");
            response.setField("balance", 0L);
            response.setField("availableBalance", 0L);
            response.setField("interestRate", 0);

            log.warn("Account not found: {}", maskAccount(accountNo));
        }

        return response;
    }

    private String getAccountTypeName(String accountType) {
        switch (accountType) {
            case "01": return "보통예금";
            case "02": return "정기예금";
            case "03": return "적금";
            default: return "기타";
        }
    }
}
