package demo.mci.common;

/**
 * 데모 메시지 코드 정의
 */
public final class DemoMessageCodes {

    private DemoMessageCodes() {
        // Utility class
    }

    // 잔액조회 (Balance Inquiry)
    public static final String BALANCE_INQUIRY_REQ = "BAL1";
    public static final String BALANCE_INQUIRY_RES = "BAL2";

    // 이체 (Transfer)
    public static final String TRANSFER_REQ = "TRF1";
    public static final String TRANSFER_RES = "TRF2";

    // 거래내역조회 (Transaction History)
    public static final String TX_HISTORY_REQ = "TXH1";
    public static final String TX_HISTORY_RES = "TXH2";

    // 계좌정보조회 (Account Info)
    public static final String ACCOUNT_INFO_REQ = "ACT1";
    public static final String ACCOUNT_INFO_RES = "ACT2";

    // 에코 (테스트용)
    public static final String ECHO_REQ = "ECH1";
    public static final String ECHO_RES = "ECH2";

    // 하트비트
    public static final String HEARTBEAT_REQ = "HBT1";
    public static final String HEARTBEAT_RES = "HBT2";
}
