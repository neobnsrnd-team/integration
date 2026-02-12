package demo.mci.banking.biz;

import demo.mci.banking.entity.AccountRepository;
import demo.mci.banking.entity.TransactionRepository;
import demo.mci.common.DemoConstants;
import demo.mci.common.DemoMessageCodes;
import lombok.extern.slf4j.Slf4j;
import springware.mci.common.core.Message;
import springware.mci.common.core.MessageType;
import springware.mci.server.core.MessageContext;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 거래내역조회 비즈니스 로직
 */
@Slf4j
public class TransactionHistoryBiz extends AbstractBankBiz {

    private final AccountRepository accountRepository = AccountRepository.getInstance();
    private final TransactionRepository transactionRepository = TransactionRepository.getInstance();

    @Override
    public String getMessageCode() {
        return DemoMessageCodes.TX_HISTORY_REQ;
    }

    @Override
    public Message execute(Message request, MessageContext context) {
        String accountNo = request.getString("accountNo");
        String fromDate = request.getString("fromDate");
        String toDate = request.getString("toDate");

        log.info("Transaction history inquiry - account: {}, period: {} ~ {}",
                maskAccount(accountNo), fromDate, toDate);

        Message response = Message.builder()
                .messageCode(DemoMessageCodes.TX_HISTORY_RES)
                .messageType(MessageType.RESPONSE)
                .build();

        // 요청 헤더 복사
        copyHeader(request, response);
        response.setField("msgCode", DemoMessageCodes.TX_HISTORY_RES);
        response.setField("accountNo", accountNo);

        // 계좌 존재 여부 확인
        if (!accountRepository.exists(accountNo)) {
            response.setField("rspCode", DemoConstants.RSP_INVALID_ACCOUNT);
            response.setField("recordCount", 0);
            response.setField("records", List.of());
            log.warn("Account not found: {}", maskAccount(accountNo));
            return response;
        }

        // 거래내역 조회
        List<TransactionRepository.Transaction> transactions =
                transactionRepository.getTransactions(accountNo, fromDate, toDate);

        // 응답 설정
        response.setField("rspCode", DemoConstants.RSP_SUCCESS);
        response.setField("recordCount", transactions.size());

        // 반복부 데이터 변환
        List<Map<String, Object>> records = transactions.stream()
                .map(TransactionRepository.Transaction::toMap)
                .collect(Collectors.toList());
        response.setField("records", records);

        log.info("Found {} transactions for account: {}", transactions.size(), maskAccount(accountNo));
        return response;
    }
}
