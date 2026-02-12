package demo.mci.external;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import springware.mci.common.core.Message;
import springware.mci.common.core.MessageType;
import springware.mci.common.exception.ErrorResponseException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ErrorResponseException 테스트")
class ErrorResponseExceptionTest {

    @Nested
    @DisplayName("생성자 테스트")
    class ConstructorTest {

        @Test
        @DisplayName("기본 생성자 - 제공자ID, 에러코드, 에러메시지")
        void basicConstructor() {
            // given & when
            ErrorResponseException ex = new ErrorResponseException(
                    "PROVIDER_X",
                    "ERR_001",
                    "Some external error"
            );

            // then
            assertThat(ex.getProviderId()).isEqualTo("PROVIDER_X");
            assertThat(ex.getExternalErrorCode()).isEqualTo("ERR_001");
            assertThat(ex.getExternalErrorMessage()).isEqualTo("Some external error");
            assertThat(ex.getOriginalResponse()).isNull();
            assertThat(ex.getErrorCode()).isEqualTo("MCI_EXT_ERROR");
        }

        @Test
        @DisplayName("원본 응답 포함 생성자")
        void constructorWithOriginalResponse() {
            // given
            Message original = Message.builder()
                    .messageCode("TEST")
                    .messageType(MessageType.RESPONSE)
                    .build();
            original.setField("detail", "Additional info");

            // when
            ErrorResponseException ex = new ErrorResponseException(
                    "PROVIDER_Y",
                    "ERR_999",
                    "External system error",
                    original
            );

            // then
            assertThat(ex.getProviderId()).isEqualTo("PROVIDER_Y");
            assertThat(ex.getExternalErrorCode()).isEqualTo("ERR_999");
            assertThat(ex.getExternalErrorMessage()).isEqualTo("External system error");
            assertThat(ex.getOriginalResponse()).isSameAs(original);
        }

        @Test
        @DisplayName("예외 체인 포함 생성자")
        void constructorWithCause() {
            // given
            RuntimeException cause = new RuntimeException("Root cause");

            // when
            ErrorResponseException ex = new ErrorResponseException(
                    "PROVIDER_Z",
                    "ERR_500",
                    "Connection failed",
                    cause
            );

            // then
            assertThat(ex.getCause()).isSameAs(cause);
        }
    }

    @Nested
    @DisplayName("getter 메서드 테스트")
    class GetterTest {

        @Test
        @DisplayName("getExternalErrorCode() - 외부 시스템 에러 코드 반환")
        void getExternalErrorCode() {
            // given
            ErrorResponseException ex = new ErrorResponseException(
                    "PROVIDER_A",
                    "99",
                    "System error"
            );

            // when & then
            assertThat(ex.getExternalErrorCode()).isEqualTo("99");
        }

        @Test
        @DisplayName("getExternalErrorMessage() - 외부 시스템 에러 메시지 반환")
        void getExternalErrorMessage() {
            // given
            ErrorResponseException ex = new ErrorResponseException(
                    "PROVIDER_B",
                    "FAIL",
                    "Transaction rejected by external system"
            );

            // when & then
            assertThat(ex.getExternalErrorMessage()).isEqualTo("Transaction rejected by external system");
        }

        @Test
        @DisplayName("getOriginalField() - 원본 응답에서 필드 조회")
        void getOriginalField() {
            // given
            Message original = Message.builder()
                    .messageCode("TEST")
                    .messageType(MessageType.RESPONSE)
                    .build();
            original.setField("errorDetail", "Detailed error description");
            original.setField("retryCount", 3);

            ErrorResponseException ex = new ErrorResponseException(
                    "PROVIDER_C",
                    "1234",
                    "Error occurred",
                    original
            );

            // when & then
            assertThat(ex.getOriginalString("errorDetail")).isEqualTo("Detailed error description");
            assertThat(ex.<Integer>getOriginalField("retryCount")).isEqualTo(3);
        }

        @Test
        @DisplayName("getOriginalField() - 원본 응답이 없으면 null 반환")
        void getOriginalFieldWhenNoOriginal() {
            // given
            ErrorResponseException ex = new ErrorResponseException(
                    "PROVIDER_D",
                    "ERR",
                    "Error"
            );

            // when & then
            assertThat(ex.getOriginalString("anyField")).isNull();
            assertThat(ex.<Object>getOriginalField("anyField")).isNull();
        }
    }

    @Nested
    @DisplayName("hasExternalErrorCode 테스트")
    class HasExternalErrorCodeTest {

        @Test
        @DisplayName("에러 코드 일치 확인 - 일치")
        void hasExternalErrorCodeMatch() {
            // given
            ErrorResponseException ex = new ErrorResponseException(
                    "PROVIDER_A",
                    "ERR_TIMEOUT",
                    "Timeout occurred"
            );

            // when & then
            assertThat(ex.hasExternalErrorCode("ERR_TIMEOUT")).isTrue();
        }

        @Test
        @DisplayName("에러 코드 일치 확인 - 불일치")
        void hasExternalErrorCodeNoMatch() {
            // given
            ErrorResponseException ex = new ErrorResponseException(
                    "PROVIDER_A",
                    "ERR_TIMEOUT",
                    "Timeout occurred"
            );

            // when & then
            assertThat(ex.hasExternalErrorCode("ERR_CONNECTION")).isFalse();
        }
    }

    @Nested
    @DisplayName("메시지 포맷 테스트")
    class MessageFormatTest {

        @Test
        @DisplayName("예외 메시지 포맷 확인")
        void messageFormat() {
            // given
            ErrorResponseException ex = new ErrorResponseException(
                    "EXTERNAL_BANK",
                    "BNK_001",
                    "Invalid account format"
            );

            // when & then
            assertThat(ex.getMessage()).contains("[EXTERNAL_BANK]");
            assertThat(ex.getMessage()).contains("BNK_001");
            assertThat(ex.getMessage()).contains("Invalid account format");
        }

        @Test
        @DisplayName("에러 메시지가 null인 경우")
        void messageFormatWithNullErrorMessage() {
            // given
            ErrorResponseException ex = new ErrorResponseException(
                    "EXTERNAL_CARD",
                    "CRD_ERR",
                    null
            );

            // when & then
            assertThat(ex.getMessage()).contains("[EXTERNAL_CARD]");
            assertThat(ex.getMessage()).contains("CRD_ERR");
        }
    }

    @Nested
    @DisplayName("실제 사용 시나리오 테스트")
    class UsageScenarioTest {

        @Test
        @DisplayName("매핑되지 않은 외부 에러 코드 처리")
        void unmappedExternalErrorCode() {
            // 외부 시스템이 알 수 없는 에러 코드를 반환한 경우
            // given
            String unknownExternalCode = "EXT_ERR_12345";
            String externalMessage = "Unknown error from external system";

            // when
            ErrorResponseException ex = new ErrorResponseException(
                    "LEGACY_SYSTEM",
                    unknownExternalCode,
                    externalMessage
            );

            // then - 원본 에러 정보를 그대로 보존
            assertThat(ex.getExternalErrorCode()).isEqualTo(unknownExternalCode);
            assertThat(ex.getExternalErrorMessage()).isEqualTo(externalMessage);
        }

        @Test
        @DisplayName("catch 블록에서 에러 정보 추출")
        void extractErrorInfoInCatchBlock() {
            // given
            ErrorResponseException thrownException = new ErrorResponseException(
                    "PAYMENT_GATEWAY",
                    "PG_DECLINED",
                    "Card declined by issuer"
            );

            // when - simulate catch block
            String providerId = thrownException.getProviderId();
            String errorCode = thrownException.getExternalErrorCode();
            String errorMessage = thrownException.getExternalErrorMessage();

            // then
            assertThat(providerId).isEqualTo("PAYMENT_GATEWAY");
            assertThat(errorCode).isEqualTo("PG_DECLINED");
            assertThat(errorMessage).isEqualTo("Card declined by issuer");

            // 조건부 처리
            if (thrownException.hasExternalErrorCode("PG_DECLINED")) {
                // 카드 거절 처리 로직
                assertThat(true).isTrue();
            }
        }
    }
}
