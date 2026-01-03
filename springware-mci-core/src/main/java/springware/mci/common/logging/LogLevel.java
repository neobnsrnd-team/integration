package springware.mci.common.logging;

/**
 * 메시지 로깅 레벨
 */
public enum LogLevel {
    /**
     * 로깅 안함
     */
    NONE(0),

    /**
     * 헤더만 로깅
     */
    HEADER(1),

    /**
     * 헤더 + 마스킹된 상세 로깅
     */
    DETAIL_MASKED(2),

    /**
     * 전체 로깅 (마스킹 없음, 개발용)
     */
    FULL(3);

    private final int level;

    LogLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public boolean isEnabled(LogLevel required) {
        return this.level >= required.level;
    }
}
