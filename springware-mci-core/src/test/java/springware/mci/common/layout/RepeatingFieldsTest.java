package springware.mci.common.layout;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import springware.mci.common.core.Message;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 반복부 필드 테스트 - 거래내역 조회 시나리오
 */
@DisplayName("반복부 필드 테스트")
class RepeatingFieldsTest {

    private static final Charset CHARSET = StandardCharsets.UTF_8;
    private MessageLayout transactionHistoryLayout;

    @BeforeEach
    void setUp() {
        // 거래내역 조회 응답 레이아웃 생성
        // 헤더: 메시지코드(4) + 응답코드(2) + 건수(3)
        // 반복부: 거래일자(8) + 거래시간(6) + 금액(15) + 적요(20)
        List<FieldDefinition> recordFields = List.of(
                FieldDefinition.string("transactionDate", 8),
                FieldDefinition.string("transactionTime", 6),
                FieldDefinition.number("amount", 15),
                FieldDefinition.string("description", 20)
        );

        transactionHistoryLayout = MessageLayout.builder("TXHST")
                .description("거래내역 조회 응답")
                .field(FieldDefinition.string("messageCode", 4))
                .field(FieldDefinition.string("responseCode", 2))
                .field(FieldDefinition.number("recordCount", 3))
                .field(FieldDefinition.repeating("records", "recordCount", recordFields))
                .build();
    }

    @Test
    @DisplayName("FieldDefinition 반복부 생성 테스트")
    void createRepeatingFieldDefinition() {
        // given
        List<FieldDefinition> children = List.of(
                FieldDefinition.string("field1", 10),
                FieldDefinition.number("field2", 5)
        );

        // when
        FieldDefinition repeating = FieldDefinition.repeating("records", "count", children);

        // then
        assertThat(repeating.isRepeating()).isTrue();
        assertThat(repeating.getRepeatCountField()).isEqualTo("count");
        assertThat(repeating.getChildren()).hasSize(2);
        assertThat(repeating.getRepeatingRecordLength()).isEqualTo(15);
        assertThat(repeating.getLength()).isEqualTo(0); // 동적 계산
    }

    @Test
    @DisplayName("반복부 인코딩 테스트")
    void encodeWithRepeatingFields() {
        // given
        Message message = Message.builder().messageCode("TXHST").build();
        message.setField("messageCode", "TXHST");
        message.setField("responseCode", "00");
        message.setField("recordCount", 2);

        List<Map<String, Object>> records = new ArrayList<>();

        Map<String, Object> record1 = new LinkedHashMap<>();
        record1.put("transactionDate", "20240115");
        record1.put("transactionTime", "143022");
        record1.put("amount", 50000L);
        record1.put("description", "ATM출금");
        records.add(record1);

        Map<String, Object> record2 = new LinkedHashMap<>();
        record2.put("transactionDate", "20240114");
        record2.put("transactionTime", "091530");
        record2.put("amount", 100000L);
        record2.put("description", "급여입금");
        records.add(record2);

        message.setField("records", records);

        // when
        byte[] encoded = transactionHistoryLayout.encode(message, CHARSET);

        // then
        // 헤더: 4 + 2 + 3 = 9바이트
        // 반복부: 2 * (8 + 6 + 15 + 20) = 2 * 49 = 98바이트
        // 총: 107바이트
        assertThat(encoded.length).isEqualTo(107);

        String encodedStr = new String(encoded, CHARSET);

        // 헤더 검증
        assertThat(encodedStr.substring(0, 4)).isEqualTo("TXHS");
        assertThat(encodedStr.substring(4, 6)).isEqualTo("00");
        assertThat(encodedStr.substring(6, 9)).isEqualTo("002");

        // 첫 번째 레코드 검증
        assertThat(encodedStr.substring(9, 17)).isEqualTo("20240115");
        assertThat(encodedStr.substring(17, 23)).isEqualTo("143022");
        assertThat(encodedStr.substring(23, 38)).isEqualTo("000000000050000");
    }

    @Test
    @DisplayName("반복부 디코딩 테스트")
    @SuppressWarnings("unchecked")
    void decodeWithRepeatingFields() {
        // given - 인코딩된 데이터 생성
        StringBuilder sb = new StringBuilder();
        sb.append("TXHS");                       // messageCode (4)
        sb.append("00");                         // responseCode (2)
        sb.append("003");                        // recordCount (3)

        // 첫 번째 레코드 (ASCII만 사용하여 바이트 길이 명확하게)
        sb.append("20240115");                   // transactionDate (8)
        sb.append("143022");                     // transactionTime (6)
        sb.append("000000000050000");            // amount (15)
        sb.append("ATM WITHDRAWAL      ");       // description (20)

        // 두 번째 레코드
        sb.append("20240114");
        sb.append("091530");
        sb.append("000000000100000");
        sb.append("SALARY DEPOSIT      ");       // description (20)

        // 세 번째 레코드
        sb.append("20240113");
        sb.append("180000");
        sb.append("000000000025000");
        sb.append("CARD PAYMENT        ");       // description (20)

        byte[] data = sb.toString().getBytes(CHARSET);

        // when
        Message decoded = transactionHistoryLayout.decode(data, CHARSET);

        // then
        assertThat((Object) decoded.getField("messageCode")).isEqualTo("TXHS");
        assertThat((Object) decoded.getField("responseCode")).isEqualTo("00");
        assertThat((Object) decoded.getField("recordCount")).isEqualTo(3L);

        List<Map<String, Object>> records = (List<Map<String, Object>>) decoded.getField("records");
        assertThat(records).hasSize(3);

        // 첫 번째 레코드 검증
        Map<String, Object> firstRecord = records.get(0);
        assertThat((Object) firstRecord.get("transactionDate")).isEqualTo("20240115");
        assertThat((Object) firstRecord.get("transactionTime")).isEqualTo("143022");
        assertThat((Object) firstRecord.get("amount")).isEqualTo(50000L);

        // 두 번째 레코드 검증
        Map<String, Object> secondRecord = records.get(1);
        assertThat((Object) secondRecord.get("transactionDate")).isEqualTo("20240114");
        assertThat((Object) secondRecord.get("amount")).isEqualTo(100000L);
    }

    @Test
    @DisplayName("반복 횟수 0인 경우 테스트")
    @SuppressWarnings("unchecked")
    void decodeWithZeroRecords() {
        // given
        StringBuilder sb = new StringBuilder();
        sb.append("TXHS");   // messageCode (4)
        sb.append("00");     // responseCode (2)
        sb.append("000");    // recordCount = 0 (3)

        byte[] data = sb.toString().getBytes(CHARSET);

        // when
        Message decoded = transactionHistoryLayout.decode(data, CHARSET);

        // then
        assertThat((Object) decoded.getField("recordCount")).isEqualTo(0L);
        List<Map<String, Object>> records = (List<Map<String, Object>>) decoded.getField("records");
        assertThat(records).isEmpty();
    }

    @Test
    @DisplayName("반복부 인코딩/디코딩 왕복 테스트")
    @SuppressWarnings("unchecked")
    void roundtripEncodeDecode() {
        // given
        Message original = Message.builder().messageCode("TXHST").build();
        original.setField("messageCode", "TXHST");
        original.setField("responseCode", "00");
        original.setField("recordCount", 2);

        List<Map<String, Object>> originalRecords = new ArrayList<>();

        Map<String, Object> record1 = new LinkedHashMap<>();
        record1.put("transactionDate", "20240115");
        record1.put("transactionTime", "143022");
        record1.put("amount", 50000L);
        record1.put("description", "ATM");
        originalRecords.add(record1);

        Map<String, Object> record2 = new LinkedHashMap<>();
        record2.put("transactionDate", "20240114");
        record2.put("transactionTime", "091530");
        record2.put("amount", 100000L);
        record2.put("description", "SALARY");
        originalRecords.add(record2);

        original.setField("records", originalRecords);

        // when
        byte[] encoded = transactionHistoryLayout.encode(original, CHARSET);
        Message decoded = transactionHistoryLayout.decode(encoded, CHARSET);

        // then
        assertThat((Object) decoded.getField("recordCount")).isEqualTo(2L);

        List<Map<String, Object>> decodedRecords = (List<Map<String, Object>>) decoded.getField("records");
        assertThat(decodedRecords).hasSize(2);

        assertThat((Object) decodedRecords.get(0).get("transactionDate")).isEqualTo("20240115");
        assertThat((Object) decodedRecords.get(0).get("amount")).isEqualTo(50000L);
        assertThat((Object) decodedRecords.get(1).get("transactionDate")).isEqualTo("20240114");
        assertThat((Object) decodedRecords.get(1).get("amount")).isEqualTo(100000L);
    }

    @Test
    @DisplayName("반복부 레코드 길이 계산 테스트")
    void repeatingRecordLength() {
        // given
        List<FieldDefinition> children = List.of(
                FieldDefinition.string("date", 8),
                FieldDefinition.string("time", 6),
                FieldDefinition.number("amount", 15),
                FieldDefinition.string("desc", 20)
        );

        FieldDefinition repeating = FieldDefinition.repeating("records", "count", children);

        // when/then
        assertThat(repeating.getRepeatingRecordLength()).isEqualTo(49);
    }

    @Test
    @DisplayName("일반 필드와 반복부 필드 혼합 레이아웃 테스트")
    @SuppressWarnings("unchecked")
    void mixedFieldsLayout() {
        // 헤더부 + 반복부 + 꼬리부 형태의 레이아웃
        List<FieldDefinition> recordFields = List.of(
                FieldDefinition.string("itemCode", 10),
                FieldDefinition.number("quantity", 5),
                FieldDefinition.number("price", 10)
        );

        MessageLayout orderLayout = MessageLayout.builder("ORDER")
                .description("주문 전문")
                .field(FieldDefinition.string("orderId", 10))       // 헤더부
                .field(FieldDefinition.string("orderDate", 8))
                .field(FieldDefinition.number("itemCount", 3))
                .field(FieldDefinition.repeating("items", "itemCount", recordFields))  // 반복부
                .build();

        // given
        Message order = Message.builder().messageCode("ORDER").build();
        order.setField("orderId", "ORD001");
        order.setField("orderDate", "20240115");
        order.setField("itemCount", 2);

        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> item1 = new LinkedHashMap<>();
        item1.put("itemCode", "ITEM001");
        item1.put("quantity", 5L);
        item1.put("price", 10000L);
        items.add(item1);

        Map<String, Object> item2 = new LinkedHashMap<>();
        item2.put("itemCode", "ITEM002");
        item2.put("quantity", 3L);
        item2.put("price", 25000L);
        items.add(item2);

        order.setField("items", items);

        // when
        byte[] encoded = orderLayout.encode(order, CHARSET);
        Message decoded = orderLayout.decode(encoded, CHARSET);

        // then
        assertThat((Object) decoded.getField("orderId")).isEqualTo("ORD001");
        assertThat((Object) decoded.getField("orderDate")).isEqualTo("20240115");
        assertThat((Object) decoded.getField("itemCount")).isEqualTo(2L);

        List<Map<String, Object>> decodedItems = (List<Map<String, Object>>) decoded.getField("items");
        assertThat(decodedItems).hasSize(2);
        assertThat((Object) decodedItems.get(0).get("itemCode")).isEqualTo("ITEM001");
        assertThat((Object) decodedItems.get(0).get("quantity")).isEqualTo(5L);
        assertThat((Object) decodedItems.get(1).get("itemCode")).isEqualTo("ITEM002");
        assertThat((Object) decodedItems.get(1).get("price")).isEqualTo(25000L);
    }
}
