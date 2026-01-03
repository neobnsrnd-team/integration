package springware.mci.common.logging;

import lombok.Builder;
import lombok.Getter;

/**
 * 마스킹 규칙 정의
 */
@Getter
@Builder
public class MaskingRule {

    /**
     * 필드명
     */
    private final String fieldName;

    /**
     * 마스킹 타입
     */
    @Builder.Default
    private final MaskingType type = MaskingType.FULL;

    /**
     * 앞에서 보여줄 문자 수
     */
    @Builder.Default
    private final int showFirst = 0;

    /**
     * 뒤에서 보여줄 문자 수
     */
    @Builder.Default
    private final int showLast = 0;

    /**
     * 마스킹 문자
     */
    @Builder.Default
    private final char maskChar = '*';

    /**
     * 값에 마스킹 적용
     *
     * @param value 원본 값
     * @return 마스킹된 값
     */
    public String apply(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        int length = value.length();

        switch (type) {
            case FULL:
                return repeat(maskChar, length);

            case PARTIAL:
                return applyPartialMask(value, length);

            case FIXED:
                return repeat(maskChar, 8);

            case HASH:
                return "[HASH:" + Integer.toHexString(value.hashCode()) + "]";

            case NONE:
            default:
                return value;
        }
    }

    /**
     * 부분 마스킹 적용
     */
    private String applyPartialMask(String value, int length) {
        StringBuilder sb = new StringBuilder();

        // 앞부분 표시
        int firstEnd = Math.min(showFirst, length);
        sb.append(value, 0, firstEnd);

        // 중간 마스킹
        int maskLength = Math.max(0, length - showFirst - showLast);
        sb.append(repeat(maskChar, maskLength));

        // 뒷부분 표시
        if (showLast > 0 && length > showFirst) {
            int lastStart = Math.max(firstEnd, length - showLast);
            sb.append(value.substring(lastStart));
        }

        return sb.toString();
    }

    /**
     * 문자 반복
     */
    private String repeat(char c, int count) {
        if (count <= 0) {
            return "";
        }
        char[] chars = new char[count];
        java.util.Arrays.fill(chars, c);
        return new String(chars);
    }

    /**
     * 전체 마스킹 규칙 생성
     */
    public static MaskingRule full(String fieldName) {
        return MaskingRule.builder()
                .fieldName(fieldName)
                .type(MaskingType.FULL)
                .build();
    }

    /**
     * 부분 마스킹 규칙 생성 (카드번호용: 앞 6자리, 뒤 4자리 표시)
     */
    public static MaskingRule cardNumber(String fieldName) {
        return MaskingRule.builder()
                .fieldName(fieldName)
                .type(MaskingType.PARTIAL)
                .showFirst(6)
                .showLast(4)
                .build();
    }

    /**
     * 부분 마스킹 규칙 생성 (주민번호용: 앞 6자리만 표시)
     */
    public static MaskingRule ssn(String fieldName) {
        return MaskingRule.builder()
                .fieldName(fieldName)
                .type(MaskingType.PARTIAL)
                .showFirst(6)
                .showLast(0)
                .build();
    }

    /**
     * 부분 마스킹 규칙 생성 (계좌번호용: 뒤 4자리만 표시)
     */
    public static MaskingRule accountNumber(String fieldName) {
        return MaskingRule.builder()
                .fieldName(fieldName)
                .type(MaskingType.PARTIAL)
                .showFirst(0)
                .showLast(4)
                .build();
    }

    /**
     * 부분 마스킹 규칙 생성 (전화번호용: 뒤 4자리만 표시)
     */
    public static MaskingRule phoneNumber(String fieldName) {
        return MaskingRule.builder()
                .fieldName(fieldName)
                .type(MaskingType.PARTIAL)
                .showFirst(3)
                .showLast(4)
                .build();
    }
}
