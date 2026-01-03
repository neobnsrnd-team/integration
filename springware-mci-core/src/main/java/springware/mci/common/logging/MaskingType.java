package springware.mci.common.logging;

/**
 * 마스킹 타입
 */
public enum MaskingType {
    /**
     * 마스킹 없음
     */
    NONE,

    /**
     * 전체 마스킹 (모든 문자를 마스킹 문자로 대체)
     */
    FULL,

    /**
     * 부분 마스킹 (앞/뒤 일부만 표시)
     */
    PARTIAL,

    /**
     * 고정 길이 마스킹 (실제 길이와 관계없이 고정 문자열로 표시)
     */
    FIXED,

    /**
     * 해시 마스킹 (해시값으로 대체)
     */
    HASH
}
