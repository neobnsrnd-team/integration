package springware.mci.common.core;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 통신 메시지 객체
 */
@Getter
@ToString
@Builder
public class Message {

    /**
     * 메시지 고유 ID
     */
    @Builder.Default
    private final String messageId = UUID.randomUUID().toString();

    /**
     * 전문 코드 (거래 식별자)
     */
    private final String messageCode;

    /**
     * 메시지 타입
     */
    private final MessageType messageType;

    /**
     * 전송 프로토콜
     */
    private final TransportType transportType;

    /**
     * 원본 바이트 데이터
     */
    private byte[] rawData;

    /**
     * 파싱된 필드 데이터
     */
    @Builder.Default
    private final Map<String, Object> fields = new HashMap<>();

    /**
     * 메시지 생성 시각
     */
    @Builder.Default
    private final LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 응답 대기 타임아웃 (밀리초)
     */
    @Builder.Default
    private long timeoutMillis = 30000;

    /**
     * 필드 값 설정
     */
    public void setField(String name, Object value) {
        fields.put(name, value);
    }

    /**
     * 필드 값 조회
     */
    @SuppressWarnings("unchecked")
    public <T> T getField(String name) {
        return (T) fields.get(name);
    }

    /**
     * 필드 값 조회 (기본값 포함)
     */
    @SuppressWarnings("unchecked")
    public <T> T getField(String name, T defaultValue) {
        Object value = fields.get(name);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * 필드 존재 여부 확인
     */
    public boolean hasField(String name) {
        return fields.containsKey(name);
    }

    /**
     * 문자열 필드 값 조회
     */
    public String getString(String name) {
        Object value = fields.get(name);
        return value != null ? value.toString() : null;
    }

    /**
     * 정수 필드 값 조회
     */
    public Integer getInt(String name) {
        Object value = fields.get(name);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(value.toString().trim());
    }

    /**
     * Long 필드 값 조회
     */
    public Long getLong(String name) {
        Object value = fields.get(name);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString().trim());
    }

    /**
     * 원본 데이터 설정
     */
    public void setRawData(byte[] rawData) {
        this.rawData = rawData;
    }

    /**
     * 요청 메시지 여부
     */
    public boolean isRequest() {
        return messageType == MessageType.REQUEST;
    }

    /**
     * 응답 메시지 여부
     */
    public boolean isResponse() {
        return messageType == MessageType.RESPONSE;
    }

    /**
     * 복사본 생성 (응답 메시지용)
     */
    public Message toResponse() {
        return Message.builder()
                .messageCode(this.messageCode)
                .messageType(MessageType.RESPONSE)
                .transportType(this.transportType)
                .timeoutMillis(this.timeoutMillis)
                .build();
    }
}
