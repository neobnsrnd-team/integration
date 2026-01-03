package springware.common.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 문자열 유틸리티 클래스
 */
public final class StringUtils {

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private StringUtils() {
        // Utility class
    }

    /**
     * 문자열이 null이거나 비어있는지 확인
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * 문자열이 null이거나 공백만 있는지 확인
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 문자열이 비어있지 않은지 확인
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * 문자열이 공백이 아닌지 확인
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    /**
     * 좌측 패딩 (문자열 기본값: 공백)
     */
    public static String leftPad(String str, int length) {
        return leftPad(str, length, ' ');
    }

    /**
     * 좌측 패딩
     */
    public static String leftPad(String str, int length, char padChar) {
        if (str == null) {
            str = "";
        }
        int padLength = length - str.length();
        if (padLength <= 0) {
            return str.substring(0, length);
        }
        return String.valueOf(padChar).repeat(padLength) + str;
    }

    /**
     * 우측 패딩 (문자열 기본값: 공백)
     */
    public static String rightPad(String str, int length) {
        return rightPad(str, length, ' ');
    }

    /**
     * 우측 패딩
     */
    public static String rightPad(String str, int length, char padChar) {
        if (str == null) {
            str = "";
        }
        int strByteLength = getByteLength(str);
        if (strByteLength >= length) {
            return truncateByBytes(str, length);
        }
        int padLength = length - strByteLength;
        return str + String.valueOf(padChar).repeat(padLength);
    }

    /**
     * 문자열의 바이트 길이 계산 (UTF-8 기준)
     */
    public static int getByteLength(String str) {
        if (str == null) {
            return 0;
        }
        return str.getBytes(DEFAULT_CHARSET).length;
    }

    /**
     * 바이트 길이로 문자열 자르기
     */
    public static String truncateByBytes(String str, int maxBytes) {
        return truncateByBytes(str, maxBytes, DEFAULT_CHARSET);
    }

    /**
     * 바이트 길이로 문자열 자르기 (문자셋 지정)
     */
    public static String truncateByBytes(String str, int maxBytes, Charset charset) {
        if (str == null || maxBytes <= 0) {
            return "";
        }
        if (charset == null) {
            charset = DEFAULT_CHARSET;
        }
        byte[] bytes = str.getBytes(charset);
        if (bytes.length <= maxBytes) {
            return str;
        }
        // 바이트 단위로 자르되, 멀티바이트 문자가 깨지지 않도록 처리
        String truncated = new String(bytes, 0, maxBytes, charset);
        // 마지막 문자가 깨진 경우 제거
        if (!truncated.isEmpty() && truncated.charAt(truncated.length() - 1) == '\uFFFD') {
            return truncated.substring(0, truncated.length() - 1);
        }
        return truncated;
    }

    /**
     * 숫자 문자열 좌측 제로 패딩
     */
    public static String zeroPad(long number, int length) {
        return leftPad(String.valueOf(number), length, '0');
    }

    /**
     * 숫자 문자열 좌측 제로 패딩 (소수점 포함)
     */
    public static String zeroPad(double number, int totalLength, int scale) {
        String format = "%0" + totalLength + "." + scale + "f";
        String formatted = String.format(format, number);
        // 소수점 포함 총 길이 맞추기
        if (formatted.length() > totalLength) {
            return formatted.substring(formatted.length() - totalLength);
        }
        return leftPad(formatted, totalLength, '0');
    }

    /**
     * 문자열 트림 (null-safe)
     */
    public static String trim(String str) {
        return str == null ? null : str.trim();
    }

    /**
     * 문자열 트림 후 빈 문자열이면 null 반환
     */
    public static String trimToNull(String str) {
        String trimmed = trim(str);
        return isEmpty(trimmed) ? null : trimmed;
    }

    /**
     * 문자열 트림 후 null이면 빈 문자열 반환
     */
    public static String trimToEmpty(String str) {
        String trimmed = trim(str);
        return trimmed == null ? "" : trimmed;
    }

    /**
     * 기본값 반환 (null-safe)
     */
    public static String defaultIfEmpty(String str, String defaultValue) {
        return isEmpty(str) ? defaultValue : str;
    }

    /**
     * 기본값 반환 (blank-safe)
     */
    public static String defaultIfBlank(String str, String defaultValue) {
        return isBlank(str) ? defaultValue : str;
    }
}
