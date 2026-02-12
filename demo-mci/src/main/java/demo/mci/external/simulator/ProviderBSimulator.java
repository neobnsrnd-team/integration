package demo.mci.external.simulator;

import demo.mci.external.ExternalProviderConstants;
import lombok.extern.slf4j.Slf4j;
import springware.mci.common.core.Message;
import springware.mci.common.core.MessageType;
import springware.mci.server.biz.Biz;
import springware.mci.server.core.MessageContext;

/**
 * Provider B 응답 시뮬레이터
 * <p>
 * 응답 형식:
 * - 성공 코드: "SUCCESS"
 * - 에러 코드: "FAIL", "ERROR", "ACCT_ERR"
 * - 응답 코드 필드: status
 * - 에러 메시지 필드: error_reason
 */
@Slf4j
public class ProviderBSimulator implements Biz {

    @Override
    public String getMessageCode() {
        return ExternalProviderConstants.PROVIDER_B_BALANCE_REQ;
    }

    @Override
    public Message execute(Message request, MessageContext context) {
        log.debug("[Provider B] Received request");

        String accountNo = request.getString("accountNo");
        Message response = Message.builder()
                .messageCode(ExternalProviderConstants.PROVIDER_B_BALANCE_RES)
                .messageType(MessageType.RESPONSE)
                .build();

        // 헤더 복사
        copyHeader(request, response);
        response.setField("msgCode", ExternalProviderConstants.PROVIDER_B_BALANCE_RES);

        // 시나리오별 응답 생성
        if (accountNo == null || accountNo.isEmpty()) {
            // 계좌 에러
            response.setField(ExternalProviderConstants.PROVIDER_B_CODE_FIELD,
                    ExternalProviderConstants.PROVIDER_B_ACCT_ERR);
            response.setField(ExternalProviderConstants.PROVIDER_B_ERROR_FIELD,
                    "Account validation failed");
            log.debug("[Provider B] Response: Account error (ACCT_ERR)");
        } else if (accountNo.startsWith("999")) {
            // 시스템 에러 시뮬레이션
            response.setField(ExternalProviderConstants.PROVIDER_B_CODE_FIELD,
                    ExternalProviderConstants.PROVIDER_B_ERROR);
            response.setField(ExternalProviderConstants.PROVIDER_B_ERROR_FIELD,
                    "System processing error occurred");
            log.debug("[Provider B] Response: Error (ERROR)");
        } else if (accountNo.startsWith("000")) {
            // 일반 실패 시뮬레이션
            response.setField(ExternalProviderConstants.PROVIDER_B_CODE_FIELD,
                    ExternalProviderConstants.PROVIDER_B_FAIL);
            response.setField(ExternalProviderConstants.PROVIDER_B_ERROR_FIELD,
                    "Transaction failed due to business rule");
            log.debug("[Provider B] Response: Fail (FAIL)");
        } else {
            // 성공
            response.setField(ExternalProviderConstants.PROVIDER_B_CODE_FIELD,
                    ExternalProviderConstants.PROVIDER_B_SUCCESS);
            response.setField("balance", 2000000L);
            response.setField("accountName", "Provider B Account");
            response.setField("currency", "KRW");
            log.debug("[Provider B] Response: Success (SUCCESS)");
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
