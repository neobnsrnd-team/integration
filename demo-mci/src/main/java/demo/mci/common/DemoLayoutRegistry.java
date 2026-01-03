package demo.mci.common;

import springware.mci.common.layout.DefaultLayoutManager;
import springware.mci.common.layout.FieldDefinition;
import springware.mci.common.layout.FieldType;
import springware.mci.common.layout.LayoutManager;
import springware.mci.common.layout.MessageLayout;

/**
 * 데모용 레이아웃 등록
 */
public class DemoLayoutRegistry {

    private final LayoutManager layoutManager;

    public DemoLayoutRegistry() {
        this.layoutManager = new DefaultLayoutManager();
        registerLayouts();
    }

    public DemoLayoutRegistry(LayoutManager layoutManager) {
        this.layoutManager = layoutManager;
        registerLayouts();
    }

    /**
     * 모든 데모 레이아웃 등록
     */
    public void registerLayouts() {
        registerCommonHeader();
        registerBalanceInquiry();
        registerTransfer();
        registerEcho();
        registerHeartbeat();
    }

    /**
     * 공통 헤더 레이아웃
     * - 전문코드(4) + 기관코드(3) + 거래일자(8) + 거래시간(6) + 전문일련번호(10) + 응답코드(4) + 필러(15) = 50바이트
     */
    private void registerCommonHeader() {
        MessageLayout header = MessageLayout.builder("HEADER")
                .description("공통 헤더")
                .field(FieldDefinition.builder()
                        .name("msgCode")
                        .length(4)
                        .type(FieldType.STRING)
                        .description("전문코드")
                        .build())
                .field(FieldDefinition.builder()
                        .name("orgCode")
                        .length(3)
                        .type(FieldType.STRING)
                        .description("기관코드")
                        .build())
                .field(FieldDefinition.builder()
                        .name("txDate")
                        .length(8)
                        .type(FieldType.DATE)
                        .expression("${DATE:yyyyMMdd}")
                        .description("거래일자")
                        .build())
                .field(FieldDefinition.builder()
                        .name("txTime")
                        .length(6)
                        .type(FieldType.TIME)
                        .expression("${TIME:HHmmss}")
                        .description("거래시간")
                        .build())
                .field(FieldDefinition.builder()
                        .name("seqNo")
                        .length(10)
                        .type(FieldType.NUMERIC_STRING)
                        .description("전문일련번호")
                        .build())
                .field(FieldDefinition.builder()
                        .name("rspCode")
                        .length(4)
                        .type(FieldType.STRING)
                        .defaultValue("0000")
                        .description("응답코드")
                        .build())
                .field(FieldDefinition.builder()
                        .name("filler")
                        .length(15)
                        .type(FieldType.STRING)
                        .description("필러")
                        .build())
                .build();

        layoutManager.registerLayout(header);
    }

    /**
     * 잔액조회 요청/응답 레이아웃
     * 헤더(50) + 계좌번호(20) + [잔액(15)] = 70/85바이트
     */
    private void registerBalanceInquiry() {
        // 요청
        MessageLayout request = MessageLayout.builder(DemoMessageCodes.BALANCE_INQUIRY_REQ)
                .description("잔액조회 요청")
                .field(FieldDefinition.builder().name("msgCode").length(4).type(FieldType.STRING).build())
                .field(FieldDefinition.builder().name("orgCode").length(3).type(FieldType.STRING).build())
                .field(FieldDefinition.builder().name("txDate").length(8).type(FieldType.DATE).expression("${DATE:yyyyMMdd}").build())
                .field(FieldDefinition.builder().name("txTime").length(6).type(FieldType.TIME).expression("${TIME:HHmmss}").build())
                .field(FieldDefinition.builder().name("seqNo").length(10).type(FieldType.NUMERIC_STRING).build())
                .field(FieldDefinition.builder().name("rspCode").length(4).type(FieldType.STRING).defaultValue("0000").build())
                .field(FieldDefinition.builder().name("filler").length(15).type(FieldType.STRING).build())
                .field(FieldDefinition.builder()
                        .name("accountNo")
                        .length(20)
                        .type(FieldType.STRING)
                        .masked(true)
                        .description("계좌번호")
                        .build())
                .build();

        // 응답
        MessageLayout response = MessageLayout.builder(DemoMessageCodes.BALANCE_INQUIRY_RES)
                .description("잔액조회 응답")
                .field(FieldDefinition.builder().name("msgCode").length(4).type(FieldType.STRING).build())
                .field(FieldDefinition.builder().name("orgCode").length(3).type(FieldType.STRING).build())
                .field(FieldDefinition.builder().name("txDate").length(8).type(FieldType.DATE).build())
                .field(FieldDefinition.builder().name("txTime").length(6).type(FieldType.TIME).build())
                .field(FieldDefinition.builder().name("seqNo").length(10).type(FieldType.NUMERIC_STRING).build())
                .field(FieldDefinition.builder().name("rspCode").length(4).type(FieldType.STRING).build())
                .field(FieldDefinition.builder().name("filler").length(15).type(FieldType.STRING).build())
                .field(FieldDefinition.builder()
                        .name("accountNo")
                        .length(20)
                        .type(FieldType.STRING)
                        .masked(true)
                        .description("계좌번호")
                        .build())
                .field(FieldDefinition.builder()
                        .name("balance")
                        .length(15)
                        .type(FieldType.NUMBER)
                        .description("잔액")
                        .build())
                .build();

        layoutManager.registerLayout(request);
        layoutManager.registerLayout(response);
    }

    /**
     * 이체 요청/응답 레이아웃
     */
    private void registerTransfer() {
        // 요청
        MessageLayout request = MessageLayout.builder(DemoMessageCodes.TRANSFER_REQ)
                .description("이체 요청")
                .field(FieldDefinition.builder().name("msgCode").length(4).type(FieldType.STRING).build())
                .field(FieldDefinition.builder().name("orgCode").length(3).type(FieldType.STRING).build())
                .field(FieldDefinition.builder().name("txDate").length(8).type(FieldType.DATE).expression("${DATE:yyyyMMdd}").build())
                .field(FieldDefinition.builder().name("txTime").length(6).type(FieldType.TIME).expression("${TIME:HHmmss}").build())
                .field(FieldDefinition.builder().name("seqNo").length(10).type(FieldType.NUMERIC_STRING).build())
                .field(FieldDefinition.builder().name("rspCode").length(4).type(FieldType.STRING).defaultValue("0000").build())
                .field(FieldDefinition.builder().name("filler").length(15).type(FieldType.STRING).build())
                .field(FieldDefinition.builder().name("fromAccount").length(20).type(FieldType.STRING).masked(true).description("출금계좌").build())
                .field(FieldDefinition.builder().name("toAccount").length(20).type(FieldType.STRING).masked(true).description("입금계좌").build())
                .field(FieldDefinition.builder().name("amount").length(15).type(FieldType.NUMBER).description("이체금액").build())
                .field(FieldDefinition.builder().name("memo").length(30).type(FieldType.STRING).description("적요").build())
                .build();

        // 응답
        MessageLayout response = MessageLayout.builder(DemoMessageCodes.TRANSFER_RES)
                .description("이체 응답")
                .field(FieldDefinition.builder().name("msgCode").length(4).type(FieldType.STRING).build())
                .field(FieldDefinition.builder().name("orgCode").length(3).type(FieldType.STRING).build())
                .field(FieldDefinition.builder().name("txDate").length(8).type(FieldType.DATE).build())
                .field(FieldDefinition.builder().name("txTime").length(6).type(FieldType.TIME).build())
                .field(FieldDefinition.builder().name("seqNo").length(10).type(FieldType.NUMERIC_STRING).build())
                .field(FieldDefinition.builder().name("rspCode").length(4).type(FieldType.STRING).build())
                .field(FieldDefinition.builder().name("filler").length(15).type(FieldType.STRING).build())
                .field(FieldDefinition.builder().name("fromAccount").length(20).type(FieldType.STRING).masked(true).description("출금계좌").build())
                .field(FieldDefinition.builder().name("toAccount").length(20).type(FieldType.STRING).masked(true).description("입금계좌").build())
                .field(FieldDefinition.builder().name("amount").length(15).type(FieldType.NUMBER).description("이체금액").build())
                .field(FieldDefinition.builder().name("fromBalance").length(15).type(FieldType.NUMBER).description("출금후잔액").build())
                .build();

        layoutManager.registerLayout(request);
        layoutManager.registerLayout(response);
    }

    /**
     * 에코 테스트 레이아웃
     */
    private void registerEcho() {
        MessageLayout request = MessageLayout.builder(DemoMessageCodes.ECHO_REQ)
                .description("에코 요청")
                .field(FieldDefinition.builder().name("msgCode").length(4).type(FieldType.STRING).build())
                .field(FieldDefinition.builder().name("orgCode").length(3).type(FieldType.STRING).build())
                .field(FieldDefinition.builder().name("txDate").length(8).type(FieldType.DATE).expression("${DATE:yyyyMMdd}").build())
                .field(FieldDefinition.builder().name("txTime").length(6).type(FieldType.TIME).expression("${TIME:HHmmss}").build())
                .field(FieldDefinition.builder().name("seqNo").length(10).type(FieldType.NUMERIC_STRING).build())
                .field(FieldDefinition.builder().name("rspCode").length(4).type(FieldType.STRING).defaultValue("0000").build())
                .field(FieldDefinition.builder().name("filler").length(15).type(FieldType.STRING).build())
                .field(FieldDefinition.builder().name("echoData").length(100).type(FieldType.STRING).description("에코데이터").build())
                .build();

        MessageLayout response = MessageLayout.builder(DemoMessageCodes.ECHO_RES)
                .description("에코 응답")
                .field(FieldDefinition.builder().name("msgCode").length(4).type(FieldType.STRING).build())
                .field(FieldDefinition.builder().name("orgCode").length(3).type(FieldType.STRING).build())
                .field(FieldDefinition.builder().name("txDate").length(8).type(FieldType.DATE).build())
                .field(FieldDefinition.builder().name("txTime").length(6).type(FieldType.TIME).build())
                .field(FieldDefinition.builder().name("seqNo").length(10).type(FieldType.NUMERIC_STRING).build())
                .field(FieldDefinition.builder().name("rspCode").length(4).type(FieldType.STRING).build())
                .field(FieldDefinition.builder().name("filler").length(15).type(FieldType.STRING).build())
                .field(FieldDefinition.builder().name("echoData").length(100).type(FieldType.STRING).description("에코데이터").build())
                .build();

        layoutManager.registerLayout(request);
        layoutManager.registerLayout(response);
    }

    /**
     * 하트비트 레이아웃
     */
    private void registerHeartbeat() {
        MessageLayout request = MessageLayout.builder(DemoMessageCodes.HEARTBEAT_REQ)
                .description("하트비트 요청")
                .field(FieldDefinition.builder().name("msgCode").length(4).type(FieldType.STRING).build())
                .field(FieldDefinition.builder().name("orgCode").length(3).type(FieldType.STRING).build())
                .field(FieldDefinition.builder().name("txDate").length(8).type(FieldType.DATE).expression("${DATE:yyyyMMdd}").build())
                .field(FieldDefinition.builder().name("txTime").length(6).type(FieldType.TIME).expression("${TIME:HHmmss}").build())
                .field(FieldDefinition.builder().name("seqNo").length(10).type(FieldType.NUMERIC_STRING).build())
                .field(FieldDefinition.builder().name("rspCode").length(4).type(FieldType.STRING).defaultValue("0000").build())
                .field(FieldDefinition.builder().name("filler").length(15).type(FieldType.STRING).build())
                .build();

        MessageLayout response = MessageLayout.builder(DemoMessageCodes.HEARTBEAT_RES)
                .description("하트비트 응답")
                .field(FieldDefinition.builder().name("msgCode").length(4).type(FieldType.STRING).build())
                .field(FieldDefinition.builder().name("orgCode").length(3).type(FieldType.STRING).build())
                .field(FieldDefinition.builder().name("txDate").length(8).type(FieldType.DATE).build())
                .field(FieldDefinition.builder().name("txTime").length(6).type(FieldType.TIME).build())
                .field(FieldDefinition.builder().name("seqNo").length(10).type(FieldType.NUMERIC_STRING).build())
                .field(FieldDefinition.builder().name("rspCode").length(4).type(FieldType.STRING).build())
                .field(FieldDefinition.builder().name("filler").length(15).type(FieldType.STRING).build())
                .build();

        layoutManager.registerLayout(request);
        layoutManager.registerLayout(response);
    }

    /**
     * 레이아웃 매니저 반환
     */
    public LayoutManager getLayoutManager() {
        return layoutManager;
    }
}
