package springware.mci.common.protocol.normalize;

import springware.mci.common.core.Message;
import springware.mci.common.response.NormalizedResponse;

/**
 * 외부 제공자 프로토콜 정규화 인터페이스
 * <p>
 * 각 외부 제공자는 서로 다른 성공/실패 코드와 에러 필드명을 사용합니다.
 * 이 인터페이스는 다양한 프로토콜을 내부 표준 형식으로 정규화하여
 * 일관된 예외 처리를 가능하게 합니다.
 */
public interface ProtocolNormalizer {

    /**
     * 제공자 ID 조회
     *
     * @return 제공자 ID
     */
    String getProviderId();

    /**
     * 외부 응답을 정규화된 응답으로 변환
     *
     * @param externalResponse 외부 제공자 응답
     * @return 정규화된 응답
     */
    NormalizedResponse normalize(Message externalResponse);

    /**
     * 응답 성공 여부 확인
     *
     * @param externalResponse 외부 제공자 응답
     * @return 성공 여부
     */
    boolean isSuccess(Message externalResponse);

    /**
     * 에러 코드 추출 (원본)
     *
     * @param externalResponse 외부 제공자 응답
     * @return 원본 에러 코드
     */
    String extractErrorCode(Message externalResponse);

    /**
     * 에러 메시지 추출
     *
     * @param externalResponse 외부 제공자 응답
     * @return 에러 메시지
     */
    String extractErrorMessage(Message externalResponse);

    /**
     * 에러 코드를 내부 형식으로 매핑
     *
     * @param externalErrorCode 외부 제공자 에러 코드
     * @return 내부 에러 코드 (예: "0000", "1001", "9999")
     */
    String mapToInternalErrorCode(String externalErrorCode);
}
