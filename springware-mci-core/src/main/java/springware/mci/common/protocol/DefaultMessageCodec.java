package springware.mci.common.protocol;

import lombok.extern.slf4j.Slf4j;
import springware.mci.common.core.Message;
import springware.mci.common.core.MessageType;
import springware.mci.common.exception.ProtocolException;
import springware.mci.common.layout.LayoutManager;
import springware.mci.common.layout.MessageLayout;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * 기본 메시지 코덱 구현
 */
@Slf4j
public class DefaultMessageCodec implements MessageCodec {

    private final ProtocolConfig config;
    private final LayoutManager layoutManager;

    public DefaultMessageCodec(ProtocolConfig config, LayoutManager layoutManager) {
        this.config = config;
        this.layoutManager = layoutManager;
    }

    @Override
    public byte[] encode(Message message) {
        try {
            // 레이아웃 조회
            MessageLayout layout = layoutManager.getLayout(message.getMessageCode());
            if (layout == null) {
                throw new ProtocolException("Layout not found: " + message.getMessageCode());
            }

            // 레이아웃을 사용하여 바디 인코딩
            byte[] body = layout.encode(message, config.getCharset());

            // 길이 필드 생성
            byte[] lengthField = encodeLengthField(body.length);

            // 최종 메시지 조합
            byte[] result = new byte[lengthField.length + body.length];
            System.arraycopy(lengthField, 0, result, 0, lengthField.length);
            System.arraycopy(body, 0, result, lengthField.length, body.length);

            return result;
        } catch (ProtocolException e) {
            throw e;
        } catch (Exception e) {
            throw new ProtocolException("Failed to encode message: " + e.getMessage(), e);
        }
    }

    @Override
    public Message decode(byte[] data) {
        try {
            // 길이 필드 파싱
            int bodyLength = decodeLengthField(data);
            int bodyOffset = config.getLengthFieldOffset() + config.getLengthFieldLength();

            // 바디 추출
            byte[] body = new byte[bodyLength];
            System.arraycopy(data, bodyOffset, body, 0, bodyLength);

            // 헤더 레이아웃으로 메시지 코드 추출
            String messageCode = extractMessageCode(body);

            // 전체 레이아웃 조회
            MessageLayout layout = layoutManager.getLayout(messageCode);
            if (layout == null) {
                throw new ProtocolException("Layout not found: " + messageCode);
            }

            // 레이아웃을 사용하여 메시지 디코딩
            Message message = layout.decode(body, config.getCharset());
            message.setRawData(data);

            return message;
        } catch (ProtocolException e) {
            throw e;
        } catch (Exception e) {
            throw new ProtocolException("Failed to decode message: " + e.getMessage(), e);
        }
    }

    @Override
    public ProtocolConfig getConfig() {
        return config;
    }

    /**
     * 길이 필드 인코딩
     */
    private byte[] encodeLengthField(int length) {
        int fieldLength = config.getLengthFieldLength();
        int valueToEncode = config.isLengthIncludesHeader()
                ? length + fieldLength
                : length;
        valueToEncode += config.getLengthAdjustment();

        switch (config.getLengthFieldType()) {
            case NUMERIC_STRING:
                return encodeNumericString(valueToEncode, fieldLength, config.getCharset());
            case BINARY_BIG_ENDIAN:
                return encodeBinaryBigEndian(valueToEncode, fieldLength);
            case BINARY_LITTLE_ENDIAN:
                return encodeBinaryLittleEndian(valueToEncode, fieldLength);
            case BCD:
                return encodeBcd(valueToEncode, fieldLength);
            case NONE:
            default:
                return new byte[0];
        }
    }

    /**
     * 길이 필드 디코딩
     */
    private int decodeLengthField(byte[] data) {
        int offset = config.getLengthFieldOffset();
        int fieldLength = config.getLengthFieldLength();

        if (config.getLengthFieldType() == LengthFieldType.NONE) {
            return data.length - offset;
        }

        int rawLength;
        switch (config.getLengthFieldType()) {
            case NUMERIC_STRING:
                rawLength = decodeNumericString(data, offset, fieldLength, config.getCharset());
                break;
            case BINARY_BIG_ENDIAN:
                rawLength = decodeBinaryBigEndian(data, offset, fieldLength);
                break;
            case BINARY_LITTLE_ENDIAN:
                rawLength = decodeBinaryLittleEndian(data, offset, fieldLength);
                break;
            case BCD:
                rawLength = decodeBcd(data, offset, fieldLength);
                break;
            default:
                rawLength = data.length - offset - fieldLength;
        }

        // 길이 보정
        int bodyLength = rawLength - config.getLengthAdjustment();
        if (config.isLengthIncludesHeader()) {
            bodyLength -= fieldLength;
        }

        return bodyLength;
    }

    /**
     * 메시지 코드 추출 (헤더 레이아웃 사용)
     */
    private String extractMessageCode(byte[] body) {
        if (config.getHeaderLayoutId() != null) {
            MessageLayout headerLayout = layoutManager.getLayout(config.getHeaderLayoutId());
            if (headerLayout != null && config.getBodyLayoutField() != null) {
                Message headerMsg = headerLayout.decode(body, config.getCharset());
                return headerMsg.getString(config.getBodyLayoutField());
            }
        }
        // 기본: 처음 4바이트를 메시지 코드로 사용
        return new String(body, 0, Math.min(4, body.length), config.getCharset()).trim();
    }

    // 인코딩 헬퍼 메서드들
    private byte[] encodeNumericString(int value, int length, Charset charset) {
        String formatted = String.format("%0" + length + "d", value);
        return formatted.getBytes(charset);
    }

    private byte[] encodeBinaryBigEndian(int value, int length) {
        byte[] result = new byte[length];
        for (int i = length - 1; i >= 0; i--) {
            result[i] = (byte) (value & 0xFF);
            value >>= 8;
        }
        return result;
    }

    private byte[] encodeBinaryLittleEndian(int value, int length) {
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            result[i] = (byte) (value & 0xFF);
            value >>= 8;
        }
        return result;
    }

    private byte[] encodeBcd(int value, int length) {
        byte[] result = new byte[length];
        String str = String.format("%0" + (length * 2) + "d", value);
        for (int i = 0; i < length; i++) {
            int high = str.charAt(i * 2) - '0';
            int low = str.charAt(i * 2 + 1) - '0';
            result[i] = (byte) ((high << 4) | low);
        }
        return result;
    }

    // 디코딩 헬퍼 메서드들
    private int decodeNumericString(byte[] data, int offset, int length, Charset charset) {
        String str = new String(data, offset, length, charset).trim();
        return Integer.parseInt(str);
    }

    private int decodeBinaryBigEndian(byte[] data, int offset, int length) {
        int value = 0;
        for (int i = 0; i < length; i++) {
            value = (value << 8) | (data[offset + i] & 0xFF);
        }
        return value;
    }

    private int decodeBinaryLittleEndian(byte[] data, int offset, int length) {
        int value = 0;
        for (int i = length - 1; i >= 0; i--) {
            value = (value << 8) | (data[offset + i] & 0xFF);
        }
        return value;
    }

    private int decodeBcd(byte[] data, int offset, int length) {
        int value = 0;
        for (int i = 0; i < length; i++) {
            int high = (data[offset + i] >> 4) & 0x0F;
            int low = data[offset + i] & 0x0F;
            value = value * 100 + high * 10 + low;
        }
        return value;
    }
}
