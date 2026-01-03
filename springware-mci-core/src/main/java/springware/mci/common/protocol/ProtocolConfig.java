package springware.mci.common.protocol;

import lombok.Builder;
import lombok.Getter;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 프로토콜 설정
 */
@Getter
@Builder
public class ProtocolConfig {

    /**
     * 길이 필드 위치 (바이트 오프셋)
     */
    @Builder.Default
    private int lengthFieldOffset = 0;

    /**
     * 길이 필드 크기 (바이트)
     */
    @Builder.Default
    private int lengthFieldLength = 4;

    /**
     * 길이 필드 타입
     */
    @Builder.Default
    private LengthFieldType lengthFieldType = LengthFieldType.NUMERIC_STRING;

    /**
     * 길이 필드가 본문 길이만 포함하는지 (false면 전체 길이)
     */
    @Builder.Default
    private boolean lengthIncludesHeader = false;

    /**
     * 길이 필드 값에 더할 보정값
     */
    @Builder.Default
    private int lengthAdjustment = 0;

    /**
     * 데이터 시작 위치 (헤더 이후)
     */
    @Builder.Default
    private int initialBytesToStrip = 0;

    /**
     * 최대 메시지 크기 (바이트)
     */
    @Builder.Default
    private int maxMessageSize = 65536;

    /**
     * 문자 인코딩
     */
    @Builder.Default
    private Charset charset = StandardCharsets.UTF_8;

    /**
     * 헤더 레이아웃 ID (선택적)
     */
    private String headerLayoutId;

    /**
     * 바디 레이아웃 ID 필드명 (헤더에서 바디 레이아웃을 결정하는 필드)
     */
    private String bodyLayoutField;

    /**
     * 기본 설정 생성
     */
    public static ProtocolConfig defaultConfig() {
        return ProtocolConfig.builder().build();
    }

    /**
     * 4자리 숫자 문자열 길이 필드 설정
     */
    public static ProtocolConfig numericLength4() {
        return ProtocolConfig.builder()
                .lengthFieldOffset(0)
                .lengthFieldLength(4)
                .lengthFieldType(LengthFieldType.NUMERIC_STRING)
                .build();
    }

    /**
     * 바이너리 2바이트 길이 필드 설정
     */
    public static ProtocolConfig binaryLength2() {
        return ProtocolConfig.builder()
                .lengthFieldOffset(0)
                .lengthFieldLength(2)
                .lengthFieldType(LengthFieldType.BINARY_BIG_ENDIAN)
                .build();
    }

    /**
     * EUC-KR 인코딩 설정
     */
    public ProtocolConfig withEucKr() {
        return ProtocolConfig.builder()
                .lengthFieldOffset(this.lengthFieldOffset)
                .lengthFieldLength(this.lengthFieldLength)
                .lengthFieldType(this.lengthFieldType)
                .lengthIncludesHeader(this.lengthIncludesHeader)
                .lengthAdjustment(this.lengthAdjustment)
                .initialBytesToStrip(this.initialBytesToStrip)
                .maxMessageSize(this.maxMessageSize)
                .charset(Charset.forName("EUC-KR"))
                .headerLayoutId(this.headerLayoutId)
                .bodyLayoutField(this.bodyLayoutField)
                .build();
    }
}
