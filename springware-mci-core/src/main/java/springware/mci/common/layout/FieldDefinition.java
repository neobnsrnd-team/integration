package springware.mci.common.layout;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

/**
 * 필드 정의
 */
@Getter
@Builder
@ToString
public class FieldDefinition {

    /**
     * 필드명
     */
    private final String name;

    /**
     * 필드 길이 (바이트)
     * 반복부인 경우 0 (동적 계산)
     */
    private final int length;

    /**
     * 필드 타입
     */
    @Builder.Default
    private final FieldType type = FieldType.STRING;

    /**
     * 기본값 (null이면 타입에 따른 기본 패딩)
     */
    private final String defaultValue;

    /**
     * 마스킹 여부 (로깅 시)
     */
    @Builder.Default
    private final boolean masked = false;

    /**
     * 마스킹 패턴 (예: "****1234" 형태의 마스킹)
     */
    private final String maskPattern;

    /**
     * 필수 필드 여부
     */
    @Builder.Default
    private final boolean required = false;

    /**
     * 필드 설명
     */
    private final String description;

    /**
     * 소수점 자릿수 (AMOUNT 타입용)
     */
    @Builder.Default
    private final int decimalPlaces = 0;

    /**
     * 날짜/시간 표현식 (예: ${DATE:yyyyMMdd:-1d})
     */
    private final String expression;

    /**
     * 반복 횟수 참조 필드명 (반복부인 경우)
     * 예: "recordCount" - recordCount 필드의 값만큼 반복
     */
    private final String repeatCountField;

    /**
     * 반복부 자식 필드들
     */
    private final List<FieldDefinition> children;

    /**
     * 필드 시작 위치 (바이트 오프셋, 0부터 시작)
     * 레이아웃 로딩 시 계산됨
     */
    private int offset;

    /**
     * 오프셋 설정 (레이아웃 매니저에서 호출)
     */
    public void setOffset(int offset) {
        this.offset = offset;
    }

    /**
     * 패딩 문자 조회
     */
    public char getPaddingChar() {
        if (type.isRightAligned()) {
            return '0';
        }
        return ' ';
    }

    /**
     * 문자열 간편 생성
     */
    public static FieldDefinition string(String name, int length) {
        return FieldDefinition.builder()
                .name(name)
                .length(length)
                .type(FieldType.STRING)
                .build();
    }

    /**
     * 숫자 간편 생성
     */
    public static FieldDefinition number(String name, int length) {
        return FieldDefinition.builder()
                .name(name)
                .length(length)
                .type(FieldType.NUMBER)
                .build();
    }

    /**
     * 마스킹 필드 간편 생성
     */
    public static FieldDefinition masked(String name, int length, FieldType type) {
        return FieldDefinition.builder()
                .name(name)
                .length(length)
                .type(type)
                .masked(true)
                .build();
    }

    /**
     * 반복부 여부 확인
     */
    public boolean isRepeating() {
        return repeatCountField != null && children != null && !children.isEmpty();
    }

    /**
     * 반복부 1건의 길이 (자식 필드 합계)
     */
    public int getRepeatingRecordLength() {
        if (!isRepeating()) {
            return 0;
        }
        return children.stream().mapToInt(FieldDefinition::getLength).sum();
    }

    /**
     * 반복부 간편 생성
     */
    public static FieldDefinition repeating(String name, String repeatCountField, List<FieldDefinition> children) {
        return FieldDefinition.builder()
                .name(name)
                .length(0) // 동적 계산
                .type(FieldType.STRING)
                .repeatCountField(repeatCountField)
                .children(children)
                .build();
    }
}
