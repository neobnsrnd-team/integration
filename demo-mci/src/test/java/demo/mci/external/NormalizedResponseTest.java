package demo.mci.external;

import demo.mci.common.DemoConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import springware.mci.common.core.Message;
import springware.mci.common.core.MessageType;
import springware.mci.common.exception.ErrorResponseException;
import springware.mci.common.response.NormalizedResponse;
import springware.mci.common.response.ResponseStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("NormalizedResponse 테스트")
class NormalizedResponseTest {

    @Nested
    @DisplayName("팩토리 메서드 테스트")
    class FactoryMethodTest {

        @Test
        @DisplayName("success() 팩토리 메서드")
        void successFactory() {
            // given
            Message original = createResponse();

            // when
            NormalizedResponse response = NormalizedResponse.success(
                    ExternalProviderConstants.PROVIDER_A,
                    original,
                    "00"
            );

            // then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.isFailure()).isFalse();
            assertThat(response.getStatus()).isEqualTo(ResponseStatus.SUCCESS);
            assertThat(response.getErrorCode()).isEqualTo(DemoConstants.RSP_SUCCESS);
            assertThat(response.getErrorMessage()).isNull();
            assertThat(response.getOriginalErrorCode()).isEqualTo("00");
            assertThat(response.getProviderId()).isEqualTo(ExternalProviderConstants.PROVIDER_A);
        }

        @Test
        @DisplayName("failure() 팩토리 메서드")
        void failureFactory() {
            // given
            Message original = createResponse();

            // when
            NormalizedResponse response = NormalizedResponse.failure(
                    ExternalProviderConstants.PROVIDER_A,
                    DemoConstants.RSP_INVALID_ACCOUNT,
                    "Account not found",
                    original,
                    "01"
            );

            // then
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.isFailure()).isTrue();
            assertThat(response.getStatus()).isEqualTo(ResponseStatus.FAILURE);
            assertThat(response.getErrorCode()).isEqualTo(DemoConstants.RSP_INVALID_ACCOUNT);
            assertThat(response.getErrorMessage()).isEqualTo("Account not found");
            assertThat(response.getOriginalErrorCode()).isEqualTo("01");
        }

        @Test
        @DisplayName("unknown() 팩토리 메서드")
        void unknownFactory() {
            // given
            Message original = createResponse();

            // when
            NormalizedResponse response = NormalizedResponse.unknown(
                    ExternalProviderConstants.PROVIDER_B,
                    original,
                    "UNKNOWN_CODE"
            );

            // then
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.isFailure()).isFalse();
            assertThat(response.getStatus()).isEqualTo(ResponseStatus.UNKNOWN);
            assertThat(response.getErrorCode()).isEqualTo("UNKNOWN_CODE");
            assertThat(response.getErrorMessage()).isEqualTo("Unknown response status");
        }

        private Message createResponse() {
            Message message = Message.builder()
                    .messageCode("TEST")
                    .messageType(MessageType.RESPONSE)
                    .build();
            message.setField("balance", 1000000L);
            message.setField("accountName", "Test Account");
            return message;
        }
    }

    @Nested
    @DisplayName("필드 접근 테스트")
    class FieldAccessTest {

        @Test
        @DisplayName("원본 응답에서 필드 조회")
        void getField() {
            // given
            Message original = Message.builder()
                    .messageCode("TEST")
                    .messageType(MessageType.RESPONSE)
                    .build();
            original.setField("balance", 1000000L);
            original.setField("accountName", "Test Account");
            original.setField("count", 5);

            NormalizedResponse response = NormalizedResponse.success(
                    ExternalProviderConstants.PROVIDER_A,
                    original,
                    "00"
            );

            // when & then
            assertThat(response.<Long>getField("balance")).isEqualTo(1000000L);
            assertThat(response.getString("accountName")).isEqualTo("Test Account");
            assertThat(response.getInt("count")).isEqualTo(5);
            assertThat(response.getLong("balance")).isEqualTo(1000000L);
        }

        @Test
        @DisplayName("원본 응답이 null인 경우 필드 조회시 null 반환")
        void getFieldFromNullOriginal() {
            // given
            NormalizedResponse response = NormalizedResponse.builder()
                    .providerId(ExternalProviderConstants.PROVIDER_A)
                    .status(ResponseStatus.SUCCESS)
                    .errorCode(DemoConstants.RSP_SUCCESS)
                    .originalResponse(null)
                    .build();

            // when & then
            assertThat(response.<Long>getField("balance")).isNull();
            assertThat(response.getString("accountName")).isNull();
            assertThat(response.getInt("count")).isNull();
            assertThat(response.getLong("balance")).isNull();
        }
    }

    @Nested
    @DisplayName("orThrow 테스트")
    class OrThrowTest {

        @Test
        @DisplayName("성공 응답에서 orThrow() 호출시 자기 자신 반환")
        void orThrowOnSuccess() {
            // given
            NormalizedResponse response = NormalizedResponse.success(
                    ExternalProviderConstants.PROVIDER_A,
                    createResponse(),
                    "00"
            );

            // when
            NormalizedResponse result = response.orThrow();

            // then
            assertThat(result).isSameAs(response);
        }

        @Test
        @DisplayName("실패 응답에서 orThrow() 호출시 ErrorResponseException 발생")
        void orThrowOnFailure() {
            // given
            Message original = createResponse();
            NormalizedResponse response = NormalizedResponse.failure(
                    ExternalProviderConstants.PROVIDER_A,
                    DemoConstants.RSP_INVALID_ACCOUNT,
                    "Account not found",
                    original,
                    "01"
            );

            // when & then
            assertThatThrownBy(response::orThrow)
                    .isInstanceOf(ErrorResponseException.class)
                    .satisfies(e -> {
                        ErrorResponseException ex = (ErrorResponseException) e;
                        assertThat(ex.getProviderId()).isEqualTo(ExternalProviderConstants.PROVIDER_A);
                        assertThat(ex.getExternalErrorCode()).isEqualTo("01");
                        assertThat(ex.getExternalErrorMessage()).isEqualTo("Account not found");
                        assertThat(ex.getOriginalResponse()).isSameAs(original);
                    });
        }

        @Test
        @DisplayName("커스텀 메시지로 orThrow() 호출")
        void orThrowWithCustomMessage() {
            // given
            NormalizedResponse response = NormalizedResponse.failure(
                    ExternalProviderConstants.PROVIDER_B,
                    DemoConstants.RSP_SYSTEM_ERROR,
                    "Original error",
                    createResponse(),
                    "FAIL"
            );

            // when & then
            assertThatThrownBy(() -> response.orThrow("Custom error message"))
                    .isInstanceOf(ErrorResponseException.class)
                    .satisfies(e -> {
                        ErrorResponseException ex = (ErrorResponseException) e;
                        assertThat(ex.getExternalErrorCode()).isEqualTo("FAIL");
                        assertThat(ex.getExternalErrorMessage()).isEqualTo("Custom error message");
                    });
        }

        private Message createResponse() {
            return Message.builder()
                    .messageCode("TEST")
                    .messageType(MessageType.RESPONSE)
                    .build();
        }
    }

    @Nested
    @DisplayName("빌더 테스트")
    class BuilderTest {

        @Test
        @DisplayName("빌더로 응답 생성")
        void buildResponse() {
            // given
            Message original = Message.builder()
                    .messageCode("TEST")
                    .messageType(MessageType.RESPONSE)
                    .build();

            // when
            NormalizedResponse response = NormalizedResponse.builder()
                    .providerId(ExternalProviderConstants.PROVIDER_C)
                    .status(ResponseStatus.FAILURE)
                    .errorCode(DemoConstants.RSP_INSUFFICIENT_BALANCE)
                    .errorMessage("Insufficient balance")
                    .originalResponse(original)
                    .originalErrorCode("1002")
                    .build();

            // then
            assertThat(response.getProviderId()).isEqualTo(ExternalProviderConstants.PROVIDER_C);
            assertThat(response.getStatus()).isEqualTo(ResponseStatus.FAILURE);
            assertThat(response.getErrorCode()).isEqualTo(DemoConstants.RSP_INSUFFICIENT_BALANCE);
            assertThat(response.getErrorMessage()).isEqualTo("Insufficient balance");
            assertThat(response.getOriginalResponse()).isSameAs(original);
            assertThat(response.getOriginalErrorCode()).isEqualTo("1002");
        }
    }
}
