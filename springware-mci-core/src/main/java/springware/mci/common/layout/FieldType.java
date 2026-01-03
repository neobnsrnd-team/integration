package springware.mci.common.layout;

/**
 * 필드 데이터 타입
 */
public enum FieldType {
    /**
     * 문자열 (좌측 정렬, 공백 패딩)
     */
    STRING("S"),

    /**
     * 숫자 (우측 정렬, 제로 패딩)
     */
    NUMBER("N"),

    /**
     * 숫자 문자열 (우측 정렬, 제로 패딩, 문자열로 처리)
     */
    NUMERIC_STRING("NS"),

    /**
     * 금액 (우측 정렬, 제로 패딩, 소수점 처리)
     */
    AMOUNT("A"),

    /**
     * 날짜 (YYYYMMDD 형식)
     */
    DATE("D"),

    /**
     * 시간 (HHmmss 형식)
     */
    TIME("T"),

    /**
     * 날짜시간 (YYYYMMDDHHmmss 형식)
     */
    DATETIME("DT"),

    /**
     * 바이너리
     */
    BINARY("B"),

    /**
     * 가변 길이 문자열
     */
    VARCHAR("V");

    private final String code;

    FieldType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static FieldType fromCode(String code) {
        for (FieldType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        // 기본값은 STRING
        return STRING;
    }

    /**
     * 우측 정렬 타입 여부
     */
    public boolean isRightAligned() {
        return this == NUMBER || this == NUMERIC_STRING || this == AMOUNT;
    }

    /**
     * 숫자 타입 여부
     */
    public boolean isNumeric() {
        return this == NUMBER || this == NUMERIC_STRING || this == AMOUNT;
    }
}
