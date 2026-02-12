package springware.mci.server.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import springware.mci.common.core.Message;
import springware.mci.common.core.MessageType;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("HttpMessageConverter 테스트")
class HttpMessageConverterTest {

    private HttpMessageConverter converter;

    @BeforeEach
    void setUp() {
        converter = new HttpMessageConverter();
    }

    // ==================== fromJson Tests ====================

    @Test
    @DisplayName("JSON을 Message로 변환 - 기본")
    void fromJson_basic() {
        // given
        String json = "{\"messageCode\":\"BAL1\",\"messageId\":\"test-id-123\",\"fields\":{\"accountNo\":\"1234567890\"}}";

        // when
        Message message = converter.fromJson(json);

        // then
        assertThat(message).isNotNull();
        assertThat(message.getMessageCode()).isEqualTo("BAL1");
        assertThat(message.getMessageId()).isEqualTo("test-id-123");
        assertThat(message.getString("accountNo")).isEqualTo("1234567890");
    }

    @Test
    @DisplayName("JSON을 Message로 변환 - 기본 메시지 코드 사용")
    void fromJson_withDefaultMessageCode() {
        // given
        String json = "{\"fields\":{\"accountNo\":\"1234567890\"}}";

        // when
        Message message = converter.fromJson(json, "DEFAULT");

        // then
        assertThat(message.getMessageCode()).isEqualTo("DEFAULT");
    }

    @Test
    @DisplayName("JSON을 Message로 변환 - 다양한 필드 타입")
    void fromJson_variousFieldTypes() {
        // given
        String json = "{\"messageCode\":\"TRF1\",\"fields\":{\"amount\":50000,\"memo\":\"이체\",\"flag\":true}}";

        // when
        Message message = converter.fromJson(json);

        // then
        assertThat((Integer) message.getField("amount")).isEqualTo(50000);
        assertThat(message.getString("memo")).isEqualTo("이체");
        assertThat((Boolean) message.getField("flag")).isTrue();
    }

    @Test
    @DisplayName("JSON을 Message로 변환 - 필드 없음")
    void fromJson_noFields() {
        // given
        String json = "{\"messageCode\":\"HBT1\"}";

        // when
        Message message = converter.fromJson(json);

        // then
        assertThat(message.getMessageCode()).isEqualTo("HBT1");
        assertThat(message.getFields()).isEmpty();
    }

    @Test
    @DisplayName("잘못된 JSON 형식 - 예외 발생")
    void fromJson_invalidJson() {
        // given
        String invalidJson = "not a valid json";

        // then
        assertThatThrownBy(() -> converter.fromJson(invalidJson))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid JSON format");
    }

    // ==================== toJson Tests ====================

    @Test
    @DisplayName("Message를 JSON으로 변환 - 기본")
    void toJson_basic() {
        // given
        Message message = Message.builder()
                .messageCode("BAL2")
                .messageType(MessageType.RESPONSE)
                .build();
        message.setField("rspCode", "0000");
        message.setField("balance", 1000000L);

        // when
        String json = converter.toJson(message);

        // then
        assertThat(json).contains("\"messageCode\":\"BAL2\"");
        assertThat(json).contains("\"rspCode\":\"0000\"");
        assertThat(json).contains("\"balance\":1000000");
    }

    @Test
    @DisplayName("Message를 JSON으로 변환 - 필드 없음")
    void toJson_noFields() {
        // given
        Message message = Message.builder()
                .messageCode("HBT2")
                .build();

        // when
        String json = converter.toJson(message);

        // then
        assertThat(json).contains("\"messageCode\":\"HBT2\"");
        assertThat(json).contains("\"fields\":{}");
    }

    @Test
    @DisplayName("Message를 JSON 바이트 배열로 변환")
    void toJsonBytes() {
        // given
        Message message = Message.builder()
                .messageCode("ECH2")
                .build();
        message.setField("echoData", "test");

        // when
        byte[] bytes = converter.toJsonBytes(message);

        // then
        assertThat(bytes).isNotNull();
        String json = new String(bytes, StandardCharsets.UTF_8);
        assertThat(json).contains("\"messageCode\":\"ECH2\"");
        assertThat(json).contains("\"echoData\":\"test\"");
    }

    // ==================== createErrorJson Tests ====================

    @Test
    @DisplayName("에러 응답 JSON 생성")
    void createErrorJson() {
        // when
        String json = converter.createErrorJson("9999", "Internal server error");

        // then
        assertThat(json).contains("\"messageCode\":\"ERROR\"");
        assertThat(json).contains("\"rspCode\":\"9999\"");
        assertThat(json).contains("\"errorMessage\":\"Internal server error\"");
    }

    // ==================== createHealthCheckJson Tests ====================

    @Test
    @DisplayName("헬스 체크 응답 JSON 생성")
    void createHealthCheckJson() {
        // when
        String json = converter.createHealthCheckJson("UP");

        // then
        assertThat(json).contains("\"status\":\"UP\"");
        assertThat(json).contains("\"timestamp\":");
    }

    @Test
    @DisplayName("헬스 체크 응답 JSON - DOWN 상태")
    void createHealthCheckJson_down() {
        // when
        String json = converter.createHealthCheckJson("DOWN");

        // then
        assertThat(json).contains("\"status\":\"DOWN\"");
    }

    // ==================== Round-trip Tests ====================

    @Test
    @DisplayName("JSON -> Message -> JSON 왕복 변환")
    void roundTrip() {
        // given
        String originalJson = "{\"messageCode\":\"BAL1\",\"messageId\":\"round-trip-test\",\"fields\":{\"accountNo\":\"1234567890\",\"amount\":50000}}";

        // when
        Message message = converter.fromJson(originalJson);
        String resultJson = converter.toJson(message);
        Message resultMessage = converter.fromJson(resultJson);

        // then
        assertThat(resultMessage.getMessageCode()).isEqualTo("BAL1");
        assertThat(resultMessage.getMessageId()).isEqualTo("round-trip-test");
        assertThat(resultMessage.getString("accountNo")).isEqualTo("1234567890");
        assertThat((Integer) resultMessage.getField("amount")).isEqualTo(50000);
    }

    // ==================== Charset Tests ====================

    @Test
    @DisplayName("UTF-8 문자셋으로 한글 처리")
    void charset_korean() {
        // given
        HttpMessageConverter utf8Converter = new HttpMessageConverter(StandardCharsets.UTF_8);
        String json = "{\"messageCode\":\"ECH1\",\"fields\":{\"echoData\":\"안녕하세요\"}}";

        // when
        Message message = utf8Converter.fromJson(json);
        String resultJson = utf8Converter.toJson(message);

        // then
        assertThat(message.getString("echoData")).isEqualTo("안녕하세요");
        assertThat(resultJson).contains("안녕하세요");
    }
}
