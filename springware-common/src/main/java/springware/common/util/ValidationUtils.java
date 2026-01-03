package springware.common.util;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 검증 유틸리티 클래스
 */
public final class ValidationUtils {

    // 정규식 패턴
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern PHONE_PATTERN =
        Pattern.compile("^\\d{2,3}-\\d{3,4}-\\d{4}$");
    private static final Pattern SSN_PATTERN =
        Pattern.compile("^\\d{6}-\\d{7}$");
    private static final Pattern ACCOUNT_PATTERN =
        Pattern.compile("^\\d{3}-\\d{2,6}-\\d{4,12}$");
    private static final Pattern CARD_PATTERN =
        Pattern.compile("^\\d{4}-\\d{4}-\\d{4}-\\d{4}$");

    private ValidationUtils() {
        // Utility class
    }

    /**
     * null 체크
     */
    public static void requireNonNull(Object obj, String fieldName) {
        if (obj == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
    }

    /**
     * 문자열 비어있음 체크
     */
    public static void requireNotEmpty(String str, String fieldName) {
        if (StringUtils.isEmpty(str)) {
            throw new IllegalArgumentException(fieldName + " must not be empty");
        }
    }

    /**
     * 문자열 공백 체크
     */
    public static void requireNotBlank(String str, String fieldName) {
        if (StringUtils.isBlank(str)) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    /**
     * 컬렉션 비어있음 체크
     */
    public static void requireNotEmpty(Collection<?> collection, String fieldName) {
        if (collection == null || collection.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be empty");
        }
    }

    /**
     * 맵 비어있음 체크
     */
    public static void requireNotEmpty(Map<?, ?> map, String fieldName) {
        if (map == null || map.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be empty");
        }
    }

    /**
     * 양수 체크
     */
    public static void requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }

    /**
     * 음수가 아닌 값 체크
     */
    public static void requireNonNegative(long value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
    }

    /**
     * 범위 체크
     */
    public static void requireInRange(long value, long min, long max, String fieldName) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                String.format("%s must be between %d and %d", fieldName, min, max));
        }
    }

    /**
     * 문자열 길이 체크
     */
    public static void requireMaxLength(String str, int maxLength, String fieldName) {
        if (str != null && str.length() > maxLength) {
            throw new IllegalArgumentException(
                String.format("%s must not exceed %d characters", fieldName, maxLength));
        }
    }

    /**
     * 바이트 길이 체크
     */
    public static void requireMaxByteLength(String str, int maxByteLength, String fieldName) {
        if (str != null && StringUtils.getByteLength(str) > maxByteLength) {
            throw new IllegalArgumentException(
                String.format("%s must not exceed %d bytes", fieldName, maxByteLength));
        }
    }

    /**
     * 이메일 형식 검증
     */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * 전화번호 형식 검증
     */
    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * 주민번호 형식 검증
     */
    public static boolean isValidSsn(String ssn) {
        return ssn != null && SSN_PATTERN.matcher(ssn).matches();
    }

    /**
     * 계좌번호 형식 검증
     */
    public static boolean isValidAccount(String account) {
        return account != null && ACCOUNT_PATTERN.matcher(account).matches();
    }

    /**
     * 카드번호 형식 검증
     */
    public static boolean isValidCard(String card) {
        return card != null && CARD_PATTERN.matcher(card).matches();
    }

    /**
     * 숫자 문자열 검증
     */
    public static boolean isNumeric(String str) {
        if (StringUtils.isEmpty(str)) {
            return false;
        }
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 정수 파싱 가능 여부
     */
    public static boolean isInteger(String str) {
        if (StringUtils.isEmpty(str)) {
            return false;
        }
        try {
            Integer.parseInt(str.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Long 파싱 가능 여부
     */
    public static boolean isLong(String str) {
        if (StringUtils.isEmpty(str)) {
            return false;
        }
        try {
            Long.parseLong(str.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Double 파싱 가능 여부
     */
    public static boolean isDouble(String str) {
        if (StringUtils.isEmpty(str)) {
            return false;
        }
        try {
            Double.parseDouble(str.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
