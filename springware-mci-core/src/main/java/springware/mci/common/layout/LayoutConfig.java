package springware.mci.common.layout;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * YAML 레이아웃 설정 클래스
 */
@Data
public class LayoutConfig {

    /**
     * 레이아웃 ID
     */
    private String id;

    /**
     * 레이아웃 설명
     */
    private String description;

    /**
     * 필드 목록
     */
    private List<FieldConfig> fields = new ArrayList<>();

    /**
     * 필드 설정
     */
    @Data
    public static class FieldConfig {
        /**
         * 필드명
         */
        private String name;

        /**
         * 필드 길이
         */
        private int length;

        /**
         * 필드 타입 (S, N, NS, A, D, T, DT, B, V)
         */
        private String type = "S";

        /**
         * 기본값
         */
        private String defaultValue;

        /**
         * 마스킹 여부
         */
        private boolean masked = false;

        /**
         * 마스킹 패턴
         */
        private String maskPattern;

        /**
         * 필수 여부
         */
        private boolean required = false;

        /**
         * 설명
         */
        private String description;

        /**
         * 소수점 자릿수
         */
        private int decimalPlaces = 0;

        /**
         * 날짜/시간 표현식
         */
        private String expression;

        /**
         * 반복 횟수 참조 필드명 (반복부인 경우)
         */
        private String repeatCountField;

        /**
         * 반복부 자식 필드들
         */
        private List<FieldConfig> children;

        /**
         * FieldDefinition으로 변환
         */
        public FieldDefinition toFieldDefinition() {
            List<FieldDefinition> childDefinitions = null;
            if (children != null && !children.isEmpty()) {
                childDefinitions = new ArrayList<>();
                for (FieldConfig child : children) {
                    childDefinitions.add(child.toFieldDefinition());
                }
            }

            return FieldDefinition.builder()
                    .name(name)
                    .length(length)
                    .type(FieldType.fromCode(type))
                    .defaultValue(defaultValue)
                    .masked(masked)
                    .maskPattern(maskPattern)
                    .required(required)
                    .description(description)
                    .decimalPlaces(decimalPlaces)
                    .expression(expression)
                    .repeatCountField(repeatCountField)
                    .children(childDefinitions)
                    .build();
        }
    }

    /**
     * MessageLayout으로 변환
     */
    public MessageLayout toMessageLayout() {
        MessageLayout.Builder builder = MessageLayout.builder(id)
                .description(description);

        for (FieldConfig fieldConfig : fields) {
            builder.field(fieldConfig.toFieldDefinition());
        }

        return builder.build();
    }
}
