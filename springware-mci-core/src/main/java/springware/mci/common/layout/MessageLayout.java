package springware.mci.common.layout;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import springware.common.util.DateUtils;
import springware.common.util.StringUtils;
import springware.mci.common.core.Message;
import springware.mci.common.core.MessageType;
import springware.mci.common.exception.LayoutException;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 메시지 레이아웃 정의
 */
@Slf4j
@Getter
public class MessageLayout {

    /**
     * 레이아웃 ID (메시지 코드)
     */
    private final String layoutId;

    /**
     * 레이아웃 설명
     */
    private final String description;

    /**
     * 필드 목록 (순서 유지)
     */
    private final List<FieldDefinition> fields;

    /**
     * 필드명으로 빠른 조회
     */
    private final Map<String, FieldDefinition> fieldMap;

    /**
     * 전체 메시지 길이 (바이트)
     */
    private final int totalLength;

    /**
     * 반복부 포함 여부
     */
    private final boolean hasRepeatingFields;

    private MessageLayout(String layoutId, String description, List<FieldDefinition> fields) {
        this.layoutId = layoutId;
        this.description = description;
        this.fields = new ArrayList<>(fields);
        this.fieldMap = new LinkedHashMap<>();

        // 오프셋 계산 및 맵 구성
        int offset = 0;
        boolean hasRepeating = false;
        for (FieldDefinition field : this.fields) {
            field.setOffset(offset);
            fieldMap.put(field.getName(), field);
            if (field.isRepeating()) {
                hasRepeating = true;
                // 반복부는 길이가 0으로 설정되어 있으므로 동적 계산 필요
            } else {
                offset += field.getLength();
            }
        }
        this.totalLength = offset;
        this.hasRepeatingFields = hasRepeating;
    }

    /**
     * 빌더 생성
     */
    public static Builder builder(String layoutId) {
        return new Builder(layoutId);
    }

    /**
     * 필드 정의 조회
     */
    public FieldDefinition getField(String name) {
        return fieldMap.get(name);
    }

    /**
     * 필드 존재 여부
     */
    public boolean hasField(String name) {
        return fieldMap.containsKey(name);
    }

    /**
     * 메시지 인코딩
     */
    public byte[] encode(Message message, Charset charset) {
        if (!hasRepeatingFields) {
            return encodeSimple(message, charset);
        }
        return encodeWithRepeating(message, charset);
    }

    /**
     * 단순 인코딩 (반복부 없는 경우)
     */
    private byte[] encodeSimple(Message message, Charset charset) {
        byte[] result = new byte[totalLength];
        java.util.Arrays.fill(result, (byte) ' ');

        for (FieldDefinition field : fields) {
            Object value = message.getField(field.getName());
            String strValue = formatFieldValue(field, value);
            byte[] fieldBytes = strValue.getBytes(charset);

            if (fieldBytes.length > field.getLength()) {
                fieldBytes = StringUtils.truncateByBytes(strValue, field.getLength(), charset).getBytes(charset);
            }

            System.arraycopy(fieldBytes, 0, result, field.getOffset(), fieldBytes.length);
        }

        return result;
    }

    /**
     * 반복부 포함 인코딩
     */
    @SuppressWarnings("unchecked")
    private byte[] encodeWithRepeating(Message message, Charset charset) {
        // 먼저 전체 길이 계산
        int dynamicLength = calculateDynamicLength(message);
        byte[] result = new byte[dynamicLength];
        java.util.Arrays.fill(result, (byte) ' ');

        int currentOffset = 0;
        for (FieldDefinition field : fields) {
            if (field.isRepeating()) {
                // 반복 횟수 조회
                Object countValue = message.getField(field.getRepeatCountField());
                int repeatCount = countValue instanceof Number ? ((Number) countValue).intValue() : 0;

                // 반복부 데이터 조회
                Object repeatingData = message.getField(field.getName());
                List<Map<String, Object>> records = (List<Map<String, Object>>) repeatingData;

                // 각 레코드 인코딩
                for (int i = 0; i < repeatCount && records != null && i < records.size(); i++) {
                    Map<String, Object> record = records.get(i);
                    for (FieldDefinition childField : field.getChildren()) {
                        Object value = record.get(childField.getName());
                        String strValue = formatFieldValue(childField, value);
                        byte[] fieldBytes = strValue.getBytes(charset);

                        if (fieldBytes.length > childField.getLength()) {
                            fieldBytes = StringUtils.truncateByBytes(strValue, childField.getLength(), charset).getBytes(charset);
                        }

                        System.arraycopy(fieldBytes, 0, result, currentOffset, fieldBytes.length);
                        currentOffset += childField.getLength();
                    }
                }
            } else {
                Object value = message.getField(field.getName());
                String strValue = formatFieldValue(field, value);
                byte[] fieldBytes = strValue.getBytes(charset);

                if (fieldBytes.length > field.getLength()) {
                    fieldBytes = StringUtils.truncateByBytes(strValue, field.getLength(), charset).getBytes(charset);
                }

                System.arraycopy(fieldBytes, 0, result, currentOffset, fieldBytes.length);
                currentOffset += field.getLength();
            }
        }

        return result;
    }

    /**
     * 동적 길이 계산 (반복부 포함)
     */
    private int calculateDynamicLength(Message message) {
        int length = 0;
        for (FieldDefinition field : fields) {
            if (field.isRepeating()) {
                Object countValue = message.getField(field.getRepeatCountField());
                int repeatCount = countValue instanceof Number ? ((Number) countValue).intValue() : 0;
                length += repeatCount * field.getRepeatingRecordLength();
            } else {
                length += field.getLength();
            }
        }
        return length;
    }

    /**
     * 메시지 디코딩
     */
    public Message decode(byte[] data, Charset charset) {
        if (!hasRepeatingFields) {
            return decodeSimple(data, charset);
        }
        return decodeWithRepeating(data, charset);
    }

    /**
     * 단순 디코딩 (반복부 없는 경우)
     */
    private Message decodeSimple(byte[] data, Charset charset) {
        if (data.length < totalLength) {
            throw new LayoutException(
                    String.format("Data length %d is less than layout length %d", data.length, totalLength));
        }

        Message.MessageBuilder builder = Message.builder()
                .messageCode(layoutId)
                .messageType(MessageType.REQUEST);

        Message message = builder.build();

        for (FieldDefinition field : fields) {
            String rawValue = new String(data, field.getOffset(), field.getLength(), charset);
            Object parsedValue = parseFieldValue(field, rawValue);
            message.setField(field.getName(), parsedValue);
        }

        message.setRawData(data);
        return message;
    }

    /**
     * 반복부 포함 디코딩
     */
    private Message decodeWithRepeating(byte[] data, Charset charset) {
        Message.MessageBuilder builder = Message.builder()
                .messageCode(layoutId)
                .messageType(MessageType.REQUEST);

        Message message = builder.build();

        int currentOffset = 0;
        for (FieldDefinition field : fields) {
            if (field.isRepeating()) {
                // 반복 횟수 조회 (이미 파싱된 필드에서)
                Object countValue = message.getField(field.getRepeatCountField());
                int repeatCount = countValue instanceof Number ? ((Number) countValue).intValue() : 0;

                // 반복부 데이터 디코딩
                List<Map<String, Object>> records = new ArrayList<>();
                int recordLength = field.getRepeatingRecordLength();

                for (int i = 0; i < repeatCount; i++) {
                    if (currentOffset + recordLength > data.length) {
                        log.warn("Insufficient data for repeating record {}/{}", i + 1, repeatCount);
                        break;
                    }

                    Map<String, Object> record = new LinkedHashMap<>();
                    int recordOffset = currentOffset;
                    for (FieldDefinition childField : field.getChildren()) {
                        String rawValue = new String(data, recordOffset, childField.getLength(), charset);
                        Object parsedValue = parseFieldValue(childField, rawValue);
                        record.put(childField.getName(), parsedValue);
                        recordOffset += childField.getLength();
                    }
                    records.add(record);
                    currentOffset += recordLength;
                }

                message.setField(field.getName(), records);
            } else {
                if (currentOffset + field.getLength() > data.length) {
                    throw new LayoutException(
                            String.format("Insufficient data at offset %d for field %s (length %d)",
                                    currentOffset, field.getName(), field.getLength()));
                }

                String rawValue = new String(data, currentOffset, field.getLength(), charset);
                Object parsedValue = parseFieldValue(field, rawValue);
                message.setField(field.getName(), parsedValue);
                currentOffset += field.getLength();
            }
        }

        message.setRawData(data);
        return message;
    }

    /**
     * 필드 값 포맷팅
     */
    private String formatFieldValue(FieldDefinition field, Object value) {
        String strValue;

        // 표현식 처리
        if (value == null && field.getExpression() != null) {
            strValue = DateUtils.evaluateDateExpression(field.getExpression());
        } else if (value == null) {
            strValue = field.getDefaultValue() != null ? field.getDefaultValue() : "";
        } else {
            strValue = value.toString();
        }

        // 타입에 따른 패딩
        if (field.getType().isRightAligned()) {
            // 우측 정렬, 제로 패딩
            return StringUtils.leftPad(strValue, field.getLength(), '0');
        } else {
            // 좌측 정렬, 공백 패딩
            return StringUtils.rightPad(strValue, field.getLength(), ' ');
        }
    }

    /**
     * 필드 값 파싱
     */
    private Object parseFieldValue(FieldDefinition field, String rawValue) {
        String trimmed = rawValue.trim();

        switch (field.getType()) {
            case NUMBER:
                if (trimmed.isEmpty()) {
                    return 0L;
                }
                try {
                    return Long.parseLong(trimmed);
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse number field {}: {}", field.getName(), rawValue);
                    return 0L;
                }

            case AMOUNT:
                if (trimmed.isEmpty()) {
                    return 0.0;
                }
                try {
                    long longValue = Long.parseLong(trimmed);
                    if (field.getDecimalPlaces() > 0) {
                        return longValue / Math.pow(10, field.getDecimalPlaces());
                    }
                    return (double) longValue;
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse amount field {}: {}", field.getName(), rawValue);
                    return 0.0;
                }

            case NUMERIC_STRING:
            case DATE:
            case TIME:
            case DATETIME:
            case STRING:
            case VARCHAR:
            default:
                return trimmed;
        }
    }

    /**
     * 레이아웃 빌더
     */
    public static class Builder {
        private final String layoutId;
        private String description;
        private final List<FieldDefinition> fields = new ArrayList<>();

        private Builder(String layoutId) {
            this.layoutId = layoutId;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder field(FieldDefinition field) {
            this.fields.add(field);
            return this;
        }

        public Builder field(String name, int length, FieldType type) {
            this.fields.add(FieldDefinition.builder()
                    .name(name)
                    .length(length)
                    .type(type)
                    .build());
            return this;
        }

        public Builder stringField(String name, int length) {
            return field(name, length, FieldType.STRING);
        }

        public Builder numberField(String name, int length) {
            return field(name, length, FieldType.NUMBER);
        }

        public Builder maskedField(String name, int length, FieldType type) {
            this.fields.add(FieldDefinition.builder()
                    .name(name)
                    .length(length)
                    .type(type)
                    .masked(true)
                    .build());
            return this;
        }

        public MessageLayout build() {
            return new MessageLayout(layoutId, description, fields);
        }
    }
}
