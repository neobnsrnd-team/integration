package springware.mci.common.response;

/**
 * 정규화된 응답 상태
 */
public enum ResponseStatus {
    /**
     * 성공
     */
    SUCCESS,

    /**
     * 실패
     */
    FAILURE,

    /**
     * 알 수 없음
     */
    UNKNOWN;

    /**
     * 성공 여부 확인
     */
    public boolean isSuccess() {
        return this == SUCCESS;
    }

    /**
     * 실패 여부 확인
     */
    public boolean isFailure() {
        return this == FAILURE;
    }

    /**
     * 알 수 없음 여부 확인
     */
    public boolean isUnknown() {
        return this == UNKNOWN;
    }
}
