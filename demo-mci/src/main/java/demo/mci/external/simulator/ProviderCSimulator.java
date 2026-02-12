package demo.mci.external.simulator;

import demo.mci.external.ExternalProviderConstants;
import lombok.extern.slf4j.Slf4j;
import springware.mci.common.core.Message;
import springware.mci.common.core.MessageType;
import springware.mci.server.biz.Biz;
import springware.mci.server.core.MessageContext;

/**
 * Provider C 응답 시뮬레이터
 * <p>
 * 응답 형식 (내부 형식과 동일):
 * - 성공 코드: "0000"
 * - 에러 코드: "9999", "1001", "1002"
 * - 응답 코드 필드: rspCode
 * - 에러 메시지 필드: errMsg
 */
@Slf4j
public class ProviderCSimulator implements Biz {

    @Override
    public String getMessageCode() {
        return ExternalProviderConstants.PROVIDER_C_BALANCE_REQ;
    }

    @Override
    public Message execute(Message request, MessageContext context) {
        log.debug("[Provider C] Received request");

        String accountNo = request.getString("accountNo");
        Message response = Message.builder()
                .messageCode(ExternalProviderConstants.PROVIDER_C_BALANCE_RES)
                .messageType(MessageType.RESPONSE)
                .build();

        // 헤더 복사
        copyHeader(request, response);
        response.setField("msgCode", ExternalProviderConstants.PROVIDER_C_BALANCE_RES);

        // 시나리오별 응답 생성
        if (accountNo == null || accountNo.isEmpty()) {
            // 무효 계좌
            response.setField(ExternalProviderConstants.PROVIDER_C_CODE_FIELD,
                    ExternalProviderConstants.PROVIDER_C_INVALID_ACCOUNT);
            response.setField(ExternalProviderConstants.PROVIDER_C_ERROR_FIELD,
                    "Invalid account number");
            log.debug("[Provider C] Response: Invalid account (1001)");
        } else if (accountNo.startsWith("999")) {
            // 시스템 에러 시뮬레이션
            response.setField(ExternalProviderConstants.PROVIDER_C_CODE_FIELD,
                    ExternalProviderConstants.PROVIDER_C_SYSTEM_ERROR);
            response.setField(ExternalProviderConstants.PROVIDER_C_ERROR_FIELD,
                    "System error occurred");
            log.debug("[Provider C] Response: System error (9999)");
        } else if (accountNo.startsWith("000")) {
            // 잔액 부족 시뮬레이션
            response.setField(ExternalProviderConstants.PROVIDER_C_CODE_FIELD,
                    ExternalProviderConstants.PROVIDER_C_INSUFFICIENT_BALANCE);
            response.setField(ExternalProviderConstants.PROVIDER_C_ERROR_FIELD,
                    "Insufficient balance for this operation");
            response.setField("balance", 0L);
            log.debug("[Provider C] Response: Insufficient balance (1002)");
        } else {
            // 성공
            response.setField(ExternalProviderConstants.PROVIDER_C_CODE_FIELD,
                    ExternalProviderConstants.PROVIDER_C_SUCCESS);
            response.setField("balance", 3000000L);
            response.setField("accountName", "Provider C Account");
            response.setField("lastTxDate", "20260212");
            log.debug("[Provider C] Response: Success (0000)");
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
