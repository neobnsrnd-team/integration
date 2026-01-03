package demo.mci.biz;

import demo.mci.common.DemoConstants;
import demo.mci.common.DemoMessageCodes;
import lombok.extern.slf4j.Slf4j;
import springware.mci.common.core.Message;
import springware.mci.common.core.MessageType;
import springware.mci.server.core.MessageContext;

/**
 * 이체 비즈니스 로직
 */
@Slf4j
public class TransferBiz extends AbstractDemoBiz {

    private final AccountRepository accountRepository = AccountRepository.getInstance();

    @Override
    public String getMessageCode() {
        return DemoMessageCodes.TRANSFER_REQ;
    }

    @Override
    public Message execute(Message request, MessageContext context) {
        String fromAccount = request.getString("fromAccount");
        String toAccount = request.getString("toAccount");
        Long amount = request.getLong("amount");

        log.info("Transfer: {} -> {} ({})", maskAccount(fromAccount), maskAccount(toAccount), amount);

        Message response = Message.builder()
                .messageCode(DemoMessageCodes.TRANSFER_RES)
                .messageType(MessageType.RESPONSE)
                .build();

        copyHeader(request, response);
        response.setField("msgCode", DemoMessageCodes.TRANSFER_RES);
        response.setField("fromAccount", fromAccount);
        response.setField("toAccount", toAccount);
        response.setField("amount", amount);

        Long fromBalance = accountRepository.getBalance(fromAccount);
        Long toBalance = accountRepository.getBalance(toAccount);

        if (fromBalance == null) {
            response.setField("rspCode", DemoConstants.RSP_INVALID_ACCOUNT);
            response.setField("fromBalance", 0L);
        } else if (fromBalance < amount) {
            response.setField("rspCode", DemoConstants.RSP_INSUFFICIENT_BALANCE);
            response.setField("fromBalance", fromBalance);
        } else {
            // 이체 수행
            accountRepository.setBalance(fromAccount, fromBalance - amount);
            if (toBalance != null) {
                accountRepository.setBalance(toAccount, toBalance + amount);
            }

            response.setField("rspCode", DemoConstants.RSP_SUCCESS);
            response.setField("fromBalance", fromBalance - amount);
        }

        return response;
    }
}
