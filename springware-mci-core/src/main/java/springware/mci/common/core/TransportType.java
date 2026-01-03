package springware.mci.common.core;

/**
 * 전송 프로토콜 타입
 */
public enum TransportType {
    TCP("TCP", "Transmission Control Protocol"),
    UDP("UDP", "User Datagram Protocol"),
    HTTP("HTTP", "HyperText Transfer Protocol");

    private final String code;
    private final String description;

    TransportType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static TransportType fromCode(String code) {
        for (TransportType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown transport type: " + code);
    }
}
