package springware.common.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 날짜/시간 유틸리티 클래스
 */
public final class DateUtils {

    private static final Map<String, DateTimeFormatter> FORMATTER_CACHE = new ConcurrentHashMap<>();

    // 기본 포맷 패턴
    public static final String PATTERN_DATE = "yyyyMMdd";
    public static final String PATTERN_TIME = "HHmmss";
    public static final String PATTERN_DATETIME = "yyyyMMddHHmmss";
    public static final String PATTERN_DATETIME_MILLIS = "yyyyMMddHHmmssSSS";

    // 날짜 연산 패턴: ${DATE:yyyyMMdd:-1d}
    private static final Pattern DATE_EXPRESSION_PATTERN =
        Pattern.compile("\\$\\{(DATE|TIME|DATETIME):([^:}]+)(?::([^}]+))?\\}");

    private DateUtils() {
        // Utility class
    }

    /**
     * 포맷터 캐싱하여 반환
     */
    public static DateTimeFormatter getFormatter(String pattern) {
        return FORMATTER_CACHE.computeIfAbsent(pattern, DateTimeFormatter::ofPattern);
    }

    /**
     * 오늘 날짜 반환 (yyyyMMdd)
     */
    public static String today() {
        return today(PATTERN_DATE);
    }

    /**
     * 오늘 날짜 반환 (지정 포맷)
     */
    public static String today(String pattern) {
        return LocalDate.now().format(getFormatter(pattern));
    }

    /**
     * 현재 시각 반환 (HHmmss)
     */
    public static String now() {
        return now(PATTERN_TIME);
    }

    /**
     * 현재 시각 반환 (지정 포맷)
     */
    public static String now(String pattern) {
        return LocalTime.now().format(getFormatter(pattern));
    }

    /**
     * 현재 일시 반환 (yyyyMMddHHmmss)
     */
    public static String timestamp() {
        return timestamp(PATTERN_DATETIME);
    }

    /**
     * 현재 일시 반환 (지정 포맷)
     */
    public static String timestamp(String pattern) {
        return LocalDateTime.now().format(getFormatter(pattern));
    }

    /**
     * 현재 일시 반환 (밀리초 포함)
     */
    public static String timestampMillis() {
        return LocalDateTime.now().format(getFormatter(PATTERN_DATETIME_MILLIS));
    }

    /**
     * 날짜 연산 (일 단위)
     */
    public static String addDays(String pattern, int days) {
        return LocalDate.now().plusDays(days).format(getFormatter(pattern));
    }

    /**
     * 날짜 연산 (월 단위)
     */
    public static String addMonths(String pattern, int months) {
        return LocalDate.now().plusMonths(months).format(getFormatter(pattern));
    }

    /**
     * 날짜 연산 (년 단위)
     */
    public static String addYears(String pattern, int years) {
        return LocalDate.now().plusYears(years).format(getFormatter(pattern));
    }

    /**
     * 이번 달 첫째 날
     */
    public static String firstDayOfMonth(String pattern) {
        return LocalDate.now()
                .with(TemporalAdjusters.firstDayOfMonth())
                .format(getFormatter(pattern));
    }

    /**
     * 이번 달 마지막 날
     */
    public static String lastDayOfMonth(String pattern) {
        return LocalDate.now()
                .with(TemporalAdjusters.lastDayOfMonth())
                .format(getFormatter(pattern));
    }

    /**
     * 날짜 표현식 파싱 및 평가
     *
     * 지원 형식:
     * - ${DATE:yyyyMMdd} : 오늘 날짜
     * - ${DATE:yyyyMMdd:-1d} : 어제
     * - ${DATE:yyyyMMdd:+1M} : 한달 후
     * - ${DATE:yyyyMMdd:firstDayOfMonth} : 이번달 1일
     * - ${TIME:HHmmss} : 현재 시각
     * - ${DATETIME:yyyyMMddHHmmss} : 현재 일시
     */
    public static String evaluateDateExpression(String expression) {
        if (expression == null || !expression.startsWith("${")) {
            return expression;
        }

        Matcher matcher = DATE_EXPRESSION_PATTERN.matcher(expression);
        if (!matcher.matches()) {
            return expression;
        }

        String type = matcher.group(1);      // DATE, TIME, DATETIME
        String pattern = matcher.group(2);   // 포맷 패턴
        String modifier = matcher.group(3);  // 연산자 (optional)

        return switch (type) {
            case "DATE" -> evaluateDatePart(pattern, modifier);
            case "TIME" -> now(pattern);
            case "DATETIME" -> evaluateDateTimePart(pattern, modifier);
            default -> expression;
        };
    }

    private static String evaluateDatePart(String pattern, String modifier) {
        if (modifier == null || modifier.isEmpty()) {
            return today(pattern);
        }

        return switch (modifier.toLowerCase()) {
            case "firstdayofmonth" -> firstDayOfMonth(pattern);
            case "lastdayofmonth" -> lastDayOfMonth(pattern);
            default -> evaluateDateArithmetic(pattern, modifier);
        };
    }

    private static String evaluateDateTimePart(String pattern, String modifier) {
        if (modifier == null || modifier.isEmpty()) {
            return timestamp(pattern);
        }
        // DateTime의 경우 날짜 연산만 지원
        return evaluateDateArithmetic(pattern, modifier);
    }

    private static String evaluateDateArithmetic(String pattern, String modifier) {
        // +1d, -1d, +1M, -1M, +1y, -1y 형식 파싱
        Pattern arithmeticPattern = Pattern.compile("([+-])(\\d+)([dMy])");
        Matcher matcher = arithmeticPattern.matcher(modifier);

        if (!matcher.matches()) {
            return today(pattern);
        }

        String sign = matcher.group(1);
        int amount = Integer.parseInt(matcher.group(2));
        String unit = matcher.group(3);

        if ("-".equals(sign)) {
            amount = -amount;
        }

        LocalDate date = LocalDate.now();
        date = switch (unit) {
            case "d" -> date.plusDays(amount);
            case "M" -> date.plusMonths(amount);
            case "y" -> date.plusYears(amount);
            default -> date;
        };

        return date.format(getFormatter(pattern));
    }

    /**
     * 문자열이 날짜 표현식인지 확인
     */
    public static boolean isDateExpression(String value) {
        return value != null && DATE_EXPRESSION_PATTERN.matcher(value).matches();
    }

    /**
     * 특수 키워드 평가
     */
    public static String evaluateKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }

        return switch (keyword) {
            case "${TODAY}" -> today();
            case "${NOW}" -> now();
            case "${TIMESTAMP}" -> timestamp();
            case "${UUID}" -> java.util.UUID.randomUUID().toString();
            case "${EMPTY}" -> "";
            case "${ZERO}" -> "0";
            default -> {
                if (keyword.startsWith("${")) {
                    yield evaluateDateExpression(keyword);
                }
                yield keyword;
            }
        };
    }
}
