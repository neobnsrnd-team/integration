package demo.mci.external.simulator;

import demo.mci.external.ExternalProviderConstants;
import lombok.extern.slf4j.Slf4j;
import springware.mci.common.core.Message;
import springware.mci.common.core.MessageType;
import springware.mci.server.biz.Biz;
import springware.mci.server.core.MessageContext;

/**
 * Provider A 응답 시뮬레이터
 * <p>
 * 응답 형식:
 * - 성공 코드: "00"
 * - 에러 코드: "99", "01", "02"
 * - 응답 코드 필드: resultCode
 * - 에러 메시지 필드: error_message
 */
@Slf4j
public class ProviderASimulator implements Biz {

    @Override
    public String getMessageCode() {
        return ExternalProviderConstants.PROVIDER_A_BALANCE_REQ;
    }

    @Override
    public Message execute(Message request, MessageContext context) {
        log.debug("[Provider A] Received request");

        String accountNo = request.getString("accountNo");
        Message response = Message.builder()
                .messageCode(ExternalProviderConstants.PROVIDER_A_BALANCE_RES)
                .messageType(MessageType.RESPONSE)
                .build();

        // 헤더 복사
        copyHeader(request, response);
        response.setField("msgCode", ExternalProviderConstants.PROVIDER_A_BALANCE_RES);

        // 시나리오별 응답 생성
        if (accountNo == null || accountNo.isEmpty()) {
            // 무효 계좌
            response.setField(ExternalProviderConstants.PROVIDER_A_CODE_FIELD,
                    ExternalProviderConstants.PROVIDER_A_INVALID_ACCOUNT);
            response.setField(ExternalProviderConstants.PROVIDER_A_ERROR_FIELD,
                    "Account number is required");
            log.debug("[Provider A] Response: Invalid account (01)");
        } else if (accountNo.startsWith("999")) {
            // 시스템 에러 시뮬레이션
            response.setField(ExternalProviderConstants.PROVIDER_A_CODE_FIELD,
                    ExternalProviderConstants.PROVIDER_A_SYSTEM_ERROR);
            response.setField(ExternalProviderConstants.PROVIDER_A_ERROR_FIELD,
                    "Internal system error");
            log.debug("[Provider A] Response: System error (99)");
        } else if (accountNo.startsWith("000")) {
            // 잔액 부족 시뮬레이션
            response.setField(ExternalProviderConstants.PROVIDER_A_CODE_FIELD,
                    ExternalProviderConstants.PROVIDER_A_INSUFFICIENT_BALANCE);
            response.setField(ExternalProviderConstants.PROVIDER_A_ERROR_FIELD,
                    "Insufficient balance");
            response.setField("balance", 0L);
            log.debug("[Provider A] Response: Insufficient balance (02)");
        } else {
            // 성공
            response.setField(ExternalProviderConstants.PROVIDER_A_CODE_FIELD,
                    ExternalProviderConstants.PROVIDER_A_SUCCESS);
            response.setField("balance", 1000000L);
            response.setField("accountName", "Test Account");
            log.debug("[Provider A] Response: Success (00)");
        }

        return response;
    }

    private void copyHeader(Message from, Message to) {
        to.setField("orgCode", from.getString("orgCode"));
        to.setField("txDate", from.getString("txDate"));
        to.setField("txTime", from.getString("txTime"));
        to.setField("seqNo", from.getString("seqNo"));
        to.setField("filler", "");
    }
}
