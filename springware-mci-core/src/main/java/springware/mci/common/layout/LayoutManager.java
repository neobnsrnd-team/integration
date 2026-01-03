package springware.mci.common.layout;

import springware.mci.common.core.Message;

import java.nio.charset.Charset;

/**
 * 레이아웃 관리자 인터페이스
 */
public interface LayoutManager {

    /**
     * 레이아웃 등록
     *
     * @param layout 등록할 레이아웃
     */
    void registerLayout(MessageLayout layout);

    /**
     * 레이아웃 조회
     *
     * @param layoutId 레이아웃 ID
     * @return 레이아웃 (없으면 null)
     */
    MessageLayout getLayout(String layoutId);

    /**
     * 레이아웃 존재 여부
     *
     * @param layoutId 레이아웃 ID
     * @return 존재 여부
     */
    boolean hasLayout(String layoutId);

    /**
     * 레이아웃 제거
     *
     * @param layoutId 제거할 레이아웃 ID
     */
    void removeLayout(String layoutId);

    /**
     * 모든 레이아웃 제거
     */
    void clear();

    /**
     * 등록된 레이아웃 수
     *
     * @return 레이아웃 수
     */
    int size();

    /**
     * 메시지 인코딩 (레이아웃 자동 조회)
     *
     * @param message 인코딩할 메시지
     * @param charset 문자셋
     * @return 인코딩된 바이트 배열
     */
    byte[] encode(Message message, Charset charset);

    /**
     * 메시지 디코딩 (레이아웃 자동 조회)
     *
     * @param layoutId 레이아웃 ID
     * @param data     디코딩할 데이터
     * @param charset  문자셋
     * @return 디코딩된 메시지
     */
    Message decode(String layoutId, byte[] data, Charset charset);
}
