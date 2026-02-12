package springware.mci.common.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import springware.mci.common.core.Message;
import springware.mci.common.core.MessageType;
import springware.mci.common.core.TransportType;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP JSON 메시지 변환기
 * Message <-> JSON 변환 처리
 *
 * 클라이언트와 서버 모두에서 사용
 */
@Slf4j
public class HttpMessageConverter {

    private final ObjectMapper objectMapper;
    private final Charset charset;

    public HttpMessageConverter() {
        this(StandardCharsets.UTF_8);
    }

    public HttpMessageConverter(Charset charset) {
        this.charset = charset;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    /**
     * JSON 문자열을 Message로 변환
     *
     * @param json JSON 문자열
     * @return Message 객체
     */
    public Message fromJson(String json) {
        return fromJson(json, null);
    }

    /**
     * JSON 문자열을 Message로 변환 (메시지 코드 지정)
     *
     * @param json JSON 문자열
     * @param defaultMessageCode 기본 메시지 코드 (JSON에 없을 경우 사용)
     * @return Message 객체
     */
    public Message fromJson(String json, String defaultMessageCode) {
        return fromJson(json, defaultMessageCode, MessageType.REQUEST);
    }

    /**
     * JSON 문자열을 Message로 변환 (메시지 코드, 타입 지정)
     *
     * @param json JSON 문자열
     * @param defaultMessageCode 기본 메시지 코드 (JSON에 없을 경우 사용)
     * @param messageType 메시지 타입
     * @return Message 객체
     */
    public Message fromJson(String json, String defaultMessageCode, MessageType messageType) {
        try {
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});

            String messageCode = (String) map.getOrDefault("messageCode", defaultMessageCode);
            String messageId = (String) map.get("messageId");

            @SuppressWarnings("unchecked")
            Map<String, Object> fields = (Map<String, Object>) map.getOrDefault("fields", new HashMap<>());

            Message.MessageBuilder builder = Message.builder()
                    .messageCode(messageCode)
                    .messageType(messageType)
                    .transportType(TransportType.HTTP);

            if (messageId != null) {
                builder.messageId(messageId);
            }

            Message message = builder.build();

            // 필드 복사
            fields.forEach(message::setField);

            // 원본 데이터 저장
            message.setRawData(json.getBytes(charset));

            return message;

        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid JSON format", e);
        }
    }

    /**
     * Message를 JSON 문자열로 변환
     *
     * @param message Message 객체
     * @return JSON 문자열
     */
    public String toJson(Message message) {
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("messageCode", message.getMessageCode());
            map.put("messageId", message.getMessageId());
            map.put("fields", message.getFields());

            return objectMapper.writeValueAsString(map);

        } catch (JsonProcessingException e) {
            log.error("Failed to convert message to JSON: {}", e.getMessage());
            throw new IllegalStateException("Failed to serialize message", e);
        }
    }

    /**
     * Message를 JSON 바이트 배열로 변환
     *
     * @param message Message 객체
     * @return JSON 바이트 배열
     */
    public byte[] toJsonBytes(Message message) {
        return toJson(message).getBytes(charset);
    }

    /**
     * 에러 응답 JSON 생성
     *
     * @param errorCode 에러 코드
     * @param errorMessage 에러 메시지
     * @return JSON 문자열
     */
    public String createErrorJson(String errorCode, String errorMessage) {
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("messageCode", "ERROR");
            map.put("fields", Map.of(
                    "rspCode", errorCode,
                    "errorMessage", errorMessage
            ));

            return objectMapper.writeValueAsString(map);

        } catch (JsonProcessingException e) {
            return "{\"messageCode\":\"ERROR\",\"fields\":{\"rspCode\":\"9999\",\"errorMessage\":\"Internal server error\"}}";
        }
    }

    /**
     * 헬스 체크 응답 JSON 생성
     *
     * @param status 상태 (UP, DOWN 등)
     * @return JSON 문자열
     */
    public String createHealthCheckJson(String status) {
        try {
            Map<String, Object> map = Map.of(
                    "status", status,
                    "timestamp", System.currentTimeMillis()
            );

            return objectMapper.writeValueAsString(map);

        } catch (JsonProcessingException e) {
            return "{\"status\":\"" + status + "\"}";
        }
    }

    /**
     * Charset 반환
     */
    public Charset getCharset() {
        return charset;
    }

    /**
     * ObjectMapper 직접 접근
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
