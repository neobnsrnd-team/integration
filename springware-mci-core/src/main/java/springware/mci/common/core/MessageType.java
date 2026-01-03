package springware.mci.common.core;

/**
 * 메시지 타입
 */
public enum MessageType {
    REQUEST("REQ", "요청 메시지"),
    RESPONSE("RES", "응답 메시지"),
    ACK("ACK", "확인 메시지"),
    NACK("NAK", "거부 메시지"),
    HEARTBEAT("HBT", "하트비트 메시지");

    private final String code;
    private final String description;

    MessageType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static MessageType fromCode(String code) {
        for (MessageType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown message type: " + code);
    }
}
