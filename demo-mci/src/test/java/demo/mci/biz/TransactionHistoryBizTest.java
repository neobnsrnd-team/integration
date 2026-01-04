package demo.mci.biz;

import demo.mci.common.DemoConstants;
import demo.mci.common.DemoMessageCodes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import springware.mci.common.core.Message;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 거래내역조회 Biz 테스트
 */
@DisplayName("거래내역조회 Biz 테스트")
class TransactionHistoryBizTest {

    private TransactionHistoryBiz biz;

    @BeforeEach
    void setUp() {
        biz = new TransactionHistoryBiz();
    }

    @Test
    @DisplayName("메시지 코드 확인")
    void getMessageCode() {
        assertThat(biz.getMessageCode()).isEqualTo(DemoMessageCodes.TX_HISTORY_REQ);
    }

    @Test
    @DisplayName("거래내역 조회 성공")
    @SuppressWarnings("unchecked")
    void executeSuccess() {
        // given
        Message request = createRequest("1234567890123456789", "20240110", "20240115");

        // when
        Message response = biz.execute(request, null);

        // then
        assertThat(response.getString("msgCode")).isEqualTo(DemoMessageCodes.TX_HISTORY_RES);
        assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);
        assertThat(response.getString("accountNo")).isEqualTo("1234567890123456789");

        // 반복부 검증
        Object recordCount = response.getField("recordCount");
        assertThat(((Number) recordCount).intValue()).isGreaterThan(0);

        List<Map<String, Object>> records = (List<Map<String, Object>>) response.getField("records");
        assertThat(records).isNotEmpty();

        // 첫 번째 레코드 검증 (최신 거래)
        Map<String, Object> firstRecord = records.get(0);
        assertThat(firstRecord).containsKey("txDate");
        assertThat(firstRecord).containsKey("txTime");
        assertThat(firstRecord).containsKey("txType");
        assertThat(firstRecord).containsKey("amount");
        assertThat(firstRecord).containsKey("balance");
        assertThat(firstRecord).containsKey("memo");
    }

    @Test
    @DisplayName("존재하지 않는 계좌 조회")
    @SuppressWarnings("unchecked")
    void executeInvalidAccount() {
        // given
        Message request = createRequest("0000000000000000000", "20240101", "20240115");

        // when
        Message response = biz.execute(request, null);

        // then
        assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_INVALID_ACCOUNT);

        Object recordCount = response.getField("recordCount");
        assertThat(((Number) recordCount).intValue()).isEqualTo(0);

        List<Map<String, Object>> records = (List<Map<String, Object>>) response.getField("records");
        assertThat(records).isEmpty();
    }

    @Test
    @DisplayName("기간 내 거래 없음")
    @SuppressWarnings("unchecked")
    void executeNoTransactionsInPeriod() {
        // given - 과거 기간으로 조회
        Message request = createRequest("1234567890123456789", "20230101", "20230131");

        // when
        Message response = biz.execute(request, null);

        // then
        assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);

        Object recordCount = response.getField("recordCount");
        assertThat(((Number) recordCount).intValue()).isEqualTo(0);

        List<Map<String, Object>> records = (List<Map<String, Object>>) response.getField("records");
        assertThat(records).isEmpty();
    }

    @Test
    @DisplayName("다른 계좌 거래내역 조회")
    @SuppressWarnings("unchecked")
    void executeOtherAccount() {
        // given
        Message request = createRequest("9876543210987654321", "20240113", "20240115");

        // when
        Message response = biz.execute(request, null);

        // then
        assertThat(response.getString("rspCode")).isEqualTo(DemoConstants.RSP_SUCCESS);

        List<Map<String, Object>> records = (List<Map<String, Object>>) response.getField("records");
        assertThat(records).hasSize(3);  // 20240113 ~ 20240115 사이 3건
    }

    @Test
    @DisplayName("헤더 복사 확인")
    void headerCopy() {
        // given
        Message request = createRequest("1234567890123456789", "20240101", "20240115");
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

    @Test
    @DisplayName("거래내역 정렬 확인 (최신순)")
    @SuppressWarnings("unchecked")
    void transactionsSortedByDateDesc() {
        // given
        Message request = createRequest("1234567890123456789", "20240110", "20240115");

        // when
        Message response = biz.execute(request, null);

        // then
        List<Map<String, Object>> records = (List<Map<String, Object>>) response.getField("records");
        assertThat(records.size()).isGreaterThanOrEqualTo(2);

        // 최신 거래가 먼저 나와야 함
        String firstDate = (String) records.get(0).get("txDate");
        String lastDate = (String) records.get(records.size() - 1).get("txDate");
        assertThat(firstDate.compareTo(lastDate)).isGreaterThanOrEqualTo(0);
    }

    private Message createRequest(String accountNo, String fromDate, String toDate) {
        Message request = Message.builder()
                .messageCode(DemoMessageCodes.TX_HISTORY_REQ)
                .build();
        request.setField("msgCode", DemoMessageCodes.TX_HISTORY_REQ);
        request.setField("orgCode", "001");
        request.setField("txDate", "20240115");
        request.setField("txTime", "143022");
        request.setField("seqNo", "0000000001");
        request.setField("rspCode", "0000");
        request.setField("filler", "");
        request.setField("accountNo", accountNo);
        request.setField("fromDate", fromDate);
        request.setField("toDate", toDate);
        return request;
    }
}
