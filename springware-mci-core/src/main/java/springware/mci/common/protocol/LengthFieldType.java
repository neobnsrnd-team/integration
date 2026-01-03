package springware.mci.common.protocol;

/**
 * 길이 필드 타입
 */
public enum LengthFieldType {
    /**
     * 길이 필드 없음 (고정 길이 메시지)
     */
    NONE,

    /**
     * 숫자 문자열 (예: "0100")
     */
    NUMERIC_STRING,

    /**
     * 바이너리 빅엔디안
     */
    BINARY_BIG_ENDIAN,

    /**
     * 바이너리 리틀엔디안
     */
    BINARY_LITTLE_ENDIAN,

    /**
     * BCD (Binary-Coded Decimal)
     */
    BCD
}
