package demo.mci.external;

import java.util.Map;

/**
 * 외부 제공자 상수 정의
 */
public final class ExternalProviderConstants {

    private ExternalProviderConstants() {
        // Utility class
    }

    // ==================== 제공자 ID ====================

    /**
     * Provider A - "00"/"99" 형식 사용
     */
    public static final String PROVIDER_A = "PROVIDER_A";

    /**
     * Provider B - "SUCCESS"/"FAIL" 형식 사용
     */
    public static final String PROVIDER_B = "PROVIDER_B";

    /**
     * Provider C - 내부 형식과 동일 ("0000"/"9999")
     */
    public static final String PROVIDER_C = "PROVIDER_C";

    // ==================== Provider A 상수 ====================

    /**
     * Provider A 응답 코드 필드명
     */
    public static final String PROVIDER_A_CODE_FIELD = "resultCode";

    /**
     * Provider A 에러 메시지 필드명
     */
    public static final String PROVIDER_A_ERROR_FIELD = "error_message";

    /**
     * Provider A 성공 코드
     */
    public static final String PROVIDER_A_SUCCESS = "00";

    /**
     * Provider A 무효 계좌 코드
     */
    public static final String PROVIDER_A_INVALID_ACCOUNT = "01";

    /**
     * Provider A 잔액 부족 코드
     */
    public static final String PROVIDER_A_INSUFFICIENT_BALANCE = "02";

    /**
     * Provider A 시스템 에러 코드
     */
    public static final String PROVIDER_A_SYSTEM_ERROR = "99";

    /**
     * Provider A 에러 코드 매핑
     */
    public static final Map<String, String> PROVIDER_A_ERROR_MAPPING = Map.of(
            PROVIDER_A_SUCCESS, "0000",
            PROVIDER_A_INVALID_ACCOUNT, "1001",
            PROVIDER_A_INSUFFICIENT_BALANCE, "1002",
            PROVIDER_A_SYSTEM_ERROR, "9999"
    );

    // ==================== Provider B 상수 ====================

    /**
     * Provider B 응답 코드 필드명
     */
    public static final String PROVIDER_B_CODE_FIELD = "status";

    /**
     * Provider B 에러 메시지 필드명
     */
    public static final String PROVIDER_B_ERROR_FIELD = "error_reason";

    /**
     * Provider B 성공 코드
     */
    public static final String PROVIDER_B_SUCCESS = "SUCCESS";

    /**
     * Provider B 실패 코드
     */
    public static final String PROVIDER_B_FAIL = "FAIL";

    /**
     * Provider B 에러 코드
     */
    public static final String PROVIDER_B_ERROR = "ERROR";

    /**
     * Provider B 계좌 에러 코드
     */
    public static final String PROVIDER_B_ACCT_ERR = "ACCT_ERR";

    /**
     * Provider B 에러 코드 매핑
     */
    public static final Map<String, String> PROVIDER_B_ERROR_MAPPING = Map.of(
            PROVIDER_B_SUCCESS, "0000",
            PROVIDER_B_FAIL, "9999",
            PROVIDER_B_ERROR, "9999",
            PROVIDER_B_ACCT_ERR, "1001"
    );

    // ==================== Provider C 상수 ====================

    /**
     * Provider C 응답 코드 필드명
     */
    public static final String PROVIDER_C_CODE_FIELD = "rspCode";

    /**
     * Provider C 에러 메시지 필드명
     */
    public static final String PROVIDER_C_ERROR_FIELD = "errMsg";

    /**
     * Provider C 성공 코드 (내부 형식과 동일)
     */
    public static final String PROVIDER_C_SUCCESS = "0000";

    /**
     * Provider C 무효 계좌 코드
     */
    public static final String PROVIDER_C_INVALID_ACCOUNT = "1001";

    /**
     * Provider C 잔액 부족 코드
     */
    public static final String PROVIDER_C_INSUFFICIENT_BALANCE = "1002";

    /**
     * Provider C 시스템 에러 코드
     */
    public static final String PROVIDER_C_SYSTEM_ERROR = "9999";

    // ==================== 시뮬레이터 메시지 코드 ====================

    /**
     * Provider A 잔액 조회 요청
     */
    public static final String PROVIDER_A_BALANCE_REQ = "PA01";

    /**
     * Provider A 잔액 조회 응답
     */
    public static final String PROVIDER_A_BALANCE_RES = "PA02";

    /**
     * Provider B 잔액 조회 요청
     */
    public static final String PROVIDER_B_BALANCE_REQ = "PB01";

    /**
     * Provider B 잔액 조회 응답
     */
    public static final String PROVIDER_B_BALANCE_RES = "PB02";

    /**
     * Provider C 잔액 조회 요청
     */
    public static final String PROVIDER_C_BALANCE_REQ = "PC01";

    /**
     * Provider C 잔액 조회 응답
     */
    public static final String PROVIDER_C_BALANCE_RES = "PC02";
}
