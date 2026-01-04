package demo.mci.layout;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import springware.mci.common.core.Message;
import springware.mci.common.layout.*;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * YAML 레이아웃 로딩 테스트
 */
@DisplayName("YAML 레이아웃 로딩 테스트")
class YamlLayoutLoadingTest {

    private static final Charset CHARSET = StandardCharsets.UTF_8;
    private YamlLayoutLoader loader;
    private LayoutManager layoutManager;

    @BeforeEach
    void setUp() {
        loader = new YamlLayoutLoader();
        layoutManager = new DefaultLayoutManager();
    }

    @Test
    @DisplayName("단일 레이아웃 로드 - HEADER")
    void loadSingleLayout() {
        // when
        MessageLayout layout = loader.loadFromClasspath("/layouts/HEADER.yaml");

        // then
        assertThat(layout).isNotNull();
        assertThat(layout.getLayoutId()).isEqualTo("HEADER");
        assertThat(layout.getDescription()).isEqualTo("공통 헤더");
        assertThat(layout.getFields()).hasSize(7);
        assertThat(layout.getTotalLength()).isEqualTo(50);
    }

    @Test
    @DisplayName("모든 레이아웃 파일 로드")
    void loadAllLayouts() {
        // given
        Path layoutsDir = Paths.get("src/main/resources/layouts");

        // when
        List<MessageLayout> layouts = loader.loadAll(layoutsDir);

        // then
        assertThat(layouts).hasSize(13);

        // 레이아웃 ID 확인
        List<String> layoutIds = layouts.stream()
                .map(MessageLayout::getLayoutId)
                .toList();
        assertThat(layoutIds).contains(
                "HEADER", "BAL1", "BAL2", "TRF1", "TRF2",
                "TXH1", "TXH2", "ACT1", "ACT2",
                "ECH1", "ECH2", "HBT1", "HBT2"
        );
    }

    @Test
    @DisplayName("레이아웃 매니저에 등록")
    void loadAndRegister() {
        // given
        Path layoutsDir = Paths.get("src/main/resources/layouts");

        // when
        int count = loader.loadAndRegister(layoutsDir, layoutManager);

        // then
        assertThat(count).isEqualTo(13);
        assertThat(layoutManager.getLayout("BAL1")).isNotNull();
        assertThat(layoutManager.getLayout("TXH2")).isNotNull();
    }

    @Test
    @DisplayName("잔액조회 요청 레이아웃 검증")
    void verifyBalanceInquiryRequestLayout() {
        // when
        MessageLayout layout = loader.loadFromClasspath("/layouts/BAL1.yaml");

        // then
        assertThat(layout.getLayoutId()).isEqualTo("BAL1");
        assertThat(layout.getTotalLength()).isEqualTo(70); // 헤더 50 + 계좌번호 20

        // 필드 확인
        assertThat(layout.hasField("msgCode")).isTrue();
        assertThat(layout.hasField("accountNo")).isTrue();

        FieldDefinition accountNo = layout.getField("accountNo");
        assertThat(accountNo.getLength()).isEqualTo(20);
        assertThat(accountNo.isMasked()).isTrue();
    }

    @Test
    @DisplayName("잔액조회 요청 인코딩/디코딩 테스트")
    void encodeDecodeBalanceInquiry() {
        // given
        MessageLayout layout = loader.loadFromClasspath("/layouts/BAL1.yaml");

        Message request = Message.builder().messageCode("BAL1").build();
        request.setField("msgCode", "BAL1");
        request.setField("orgCode", "001");
        request.setField("txDate", "20240115");
        request.setField("txTime", "143022");
        request.setField("seqNo", "0000000001");
        request.setField("rspCode", "0000");
        request.setField("filler", "");
        request.setField("accountNo", "1234567890123456");

        // when
        byte[] encoded = layout.encode(request, CHARSET);
        Message decoded = layout.decode(encoded, CHARSET);

        // then
        assertThat(encoded.length).isEqualTo(70);
        assertThat((Object) decoded.getField("msgCode")).isEqualTo("BAL1");
        assertThat((Object) decoded.getField("orgCode")).isEqualTo("001");
        assertThat((Object) decoded.getField("accountNo")).isEqualTo("1234567890123456");
    }

    @Test
    @DisplayName("이체 요청 레이아웃 검증")
    void verifyTransferRequestLayout() {
        // when
        MessageLayout layout = loader.loadFromClasspath("/layouts/TRF1.yaml");

        // then
        assertThat(layout.getLayoutId()).isEqualTo("TRF1");
        assertThat(layout.getTotalLength()).isEqualTo(135); // 헤더 50 + 본문 85

        // 필드 확인
        assertThat(layout.hasField("fromAccount")).isTrue();
        assertThat(layout.hasField("toAccount")).isTrue();
        assertThat(layout.hasField("amount")).isTrue();
        assertThat(layout.hasField("memo")).isTrue();

        FieldDefinition fromAccount = layout.getField("fromAccount");
        assertThat(fromAccount.isMasked()).isTrue();
    }

    @Test
    @DisplayName("거래내역조회 응답 레이아웃 - 반복부 검증")
    void verifyTransactionHistoryResponseLayout() {
        // when
        MessageLayout layout = loader.loadFromClasspath("/layouts/TXH2.yaml");

        // then
        assertThat(layout.getLayoutId()).isEqualTo("TXH2");
        assertThat(layout.hasField("recordCount")).isTrue();
        assertThat(layout.hasField("records")).isTrue();

        FieldDefinition records = layout.getField("records");
        assertThat(records.isRepeating()).isTrue();
        assertThat(records.getRepeatCountField()).isEqualTo("recordCount");
        assertThat(records.getChildren()).hasSize(6);
        assertThat(records.getRepeatingRecordLength()).isEqualTo(49); // 8+6+1+15+15+4
    }

    @Test
    @DisplayName("거래내역조회 응답 인코딩/디코딩 테스트 - 반복부 포함")
    @SuppressWarnings("unchecked")
    void encodeDecodeTransactionHistory() {
        // given
        MessageLayout layout = loader.loadFromClasspath("/layouts/TXH2.yaml");

        Message response = Message.builder().messageCode("TXH2").build();
        response.setField("msgCode", "TXH2");
        response.setField("orgCode", "001");
        response.setField("txDate", "20240115");
        response.setField("txTime", "143022");
        response.setField("seqNo", "0000000001");
        response.setField("rspCode", "0000");
        response.setField("filler", "");
        response.setField("accountNo", "1234567890123456");
        response.setField("recordCount", 2);

        List<Map<String, Object>> records = new ArrayList<>();

        Map<String, Object> record1 = new LinkedHashMap<>();
        record1.put("txDate", "20240114");
        record1.put("txTime", "100000");
        record1.put("txType", "1");
        record1.put("amount", 50000L);
        record1.put("balance", 150000L);
        record1.put("memo", "DPST");
        records.add(record1);

        Map<String, Object> record2 = new LinkedHashMap<>();
        record2.put("txDate", "20240113");
        record2.put("txTime", "153000");
        record2.put("txType", "2");
        record2.put("amount", 30000L);
        record2.put("balance", 100000L);
        record2.put("memo", "WDRL");
        records.add(record2);

        response.setField("records", records);

        // when
        byte[] encoded = layout.encode(response, CHARSET);
        Message decoded = layout.decode(encoded, CHARSET);

        // then
        // 헤더 50 + 계좌번호 20 + 건수 3 + 반복부 2*49 = 171
        assertThat(encoded.length).isEqualTo(171);

        assertThat((Object) decoded.getField("msgCode")).isEqualTo("TXH2");
        assertThat((Object) decoded.getField("accountNo")).isEqualTo("1234567890123456");
        assertThat((Object) decoded.getField("recordCount")).isEqualTo(2L);

        List<Map<String, Object>> decodedRecords = (List<Map<String, Object>>) decoded.getField("records");
        assertThat(decodedRecords).hasSize(2);

        assertThat((Object) decodedRecords.get(0).get("txDate")).isEqualTo("20240114");
        assertThat((Object) decodedRecords.get(0).get("txType")).isEqualTo("1");
        assertThat((Object) decodedRecords.get(0).get("amount")).isEqualTo(50000L);

        assertThat((Object) decodedRecords.get(1).get("txDate")).isEqualTo("20240113");
        assertThat((Object) decodedRecords.get(1).get("txType")).isEqualTo("2");
        assertThat((Object) decodedRecords.get(1).get("amount")).isEqualTo(30000L);
    }

    @Test
    @DisplayName("계좌정보조회 응답 레이아웃 검증")
    void verifyAccountInfoResponseLayout() {
        // when
        MessageLayout layout = loader.loadFromClasspath("/layouts/ACT2.yaml");

        // then
        assertThat(layout.getLayoutId()).isEqualTo("ACT2");
        assertThat(layout.getTotalLength()).isEqualTo(145); // 헤더 50 + 본문 95

        // 필드 확인
        assertThat(layout.hasField("accountName")).isTrue();
        assertThat(layout.hasField("accountType")).isTrue();
        assertThat(layout.hasField("balance")).isTrue();
        assertThat(layout.hasField("availableBalance")).isTrue();
        assertThat(layout.hasField("interestRate")).isTrue();
    }

    @Test
    @DisplayName("에코 레이아웃 인코딩/디코딩 테스트")
    void encodeDecodeEcho() {
        // given
        MessageLayout layout = loader.loadFromClasspath("/layouts/ECH1.yaml");

        Message request = Message.builder().messageCode("ECH1").build();
        request.setField("msgCode", "ECH1");
        request.setField("orgCode", "001");
        request.setField("txDate", "20240115");
        request.setField("txTime", "143022");
        request.setField("seqNo", "0000000001");
        request.setField("rspCode", "0000");
        request.setField("filler", "");
        request.setField("echoData", "Hello, MCI!");

        // when
        byte[] encoded = layout.encode(request, CHARSET);
        Message decoded = layout.decode(encoded, CHARSET);

        // then
        assertThat(encoded.length).isEqualTo(150);
        assertThat((Object) decoded.getField("msgCode")).isEqualTo("ECH1");
        assertThat((Object) decoded.getField("echoData")).isEqualTo("Hello, MCI!");
    }

    @Test
    @DisplayName("하트비트 레이아웃 검증")
    void verifyHeartbeatLayout() {
        // when
        MessageLayout request = loader.loadFromClasspath("/layouts/HBT1.yaml");
        MessageLayout response = loader.loadFromClasspath("/layouts/HBT2.yaml");

        // then
        assertThat(request.getLayoutId()).isEqualTo("HBT1");
        assertThat(request.getTotalLength()).isEqualTo(50);
        assertThat(request.getFields()).hasSize(7);

        assertThat(response.getLayoutId()).isEqualTo("HBT2");
        assertThat(response.getTotalLength()).isEqualTo(50);
    }

    @Test
    @DisplayName("필드 타입 확인")
    void verifyFieldTypes() {
        // when
        MessageLayout layout = loader.loadFromClasspath("/layouts/HEADER.yaml");

        // then
        assertThat(layout.getField("msgCode").getType()).isEqualTo(FieldType.STRING);
        assertThat(layout.getField("txDate").getType()).isEqualTo(FieldType.DATE);
        assertThat(layout.getField("txTime").getType()).isEqualTo(FieldType.TIME);
        assertThat(layout.getField("seqNo").getType()).isEqualTo(FieldType.NUMERIC_STRING);
    }

    @Test
    @DisplayName("기본값 및 표현식 확인")
    void verifyDefaultValuesAndExpressions() {
        // when
        MessageLayout layout = loader.loadFromClasspath("/layouts/BAL1.yaml");

        // then
        FieldDefinition rspCode = layout.getField("rspCode");
        assertThat(rspCode.getDefaultValue()).isEqualTo("0000");

        FieldDefinition txDate = layout.getField("txDate");
        assertThat(txDate.getExpression()).isEqualTo("${DATE:yyyyMMdd}");

        FieldDefinition txTime = layout.getField("txTime");
        assertThat(txTime.getExpression()).isEqualTo("${TIME:HHmmss}");
    }
}
