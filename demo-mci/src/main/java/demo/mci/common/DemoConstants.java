package demo.mci.common;

/**
 * 데모 상수 정의
 */
public final class DemoConstants {

    private DemoConstants() {
        // Utility class
    }

    // 기관 코드
    public static final String ORG_CODE_BANK = "001";
    public static final String ORG_CODE_CARD = "002";
    public static final String ORG_CODE_INSURANCE = "003";

    // 거래 코드
    public static final String TX_BALANCE_INQUIRY = "0100";     // 잔액조회
    public static final String TX_TRANSFER = "0200";            // 이체
    public static final String TX_TRANSACTION_HISTORY = "0300"; // 거래내역조회
    public static final String TX_ACCOUNT_INFO = "0400";        // 계좌정보조회

    // 응답 코드
    public static final String RSP_SUCCESS = "0000";
    public static final String RSP_INVALID_ACCOUNT = "1001";
    public static final String RSP_INSUFFICIENT_BALANCE = "1002";
    public static final String RSP_SYSTEM_ERROR = "9999";

    // 기본 설정
    public static final int DEFAULT_TCP_PORT = 9001;
    public static final int DEFAULT_UDP_PORT = 9002;
    public static final int DEFAULT_HTTP_PORT = 9003;
    public static final int DEFAULT_HTTPS_PORT = 9443;

    public static final String DEFAULT_HOST = "localhost";

    // 메시지 길이
    public static final int LENGTH_FIELD_SIZE = 4;
    public static final int HEADER_SIZE = 50;
    public static final int MAX_MESSAGE_SIZE = 65536;

    // 타임아웃 (밀리초)
    public static final int CONNECT_TIMEOUT = 10000;
    public static final int READ_TIMEOUT = 30000;
    public static final int WRITE_TIMEOUT = 10000;
}
