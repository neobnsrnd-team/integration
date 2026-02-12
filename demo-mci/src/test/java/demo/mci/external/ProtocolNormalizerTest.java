package demo.mci.external;

import demo.mci.common.DemoConstants;
import demo.mci.external.normalizer.ProviderANormalizer;
import demo.mci.external.normalizer.ProviderBNormalizer;
import demo.mci.external.normalizer.ProviderCNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import springware.mci.common.core.Message;
import springware.mci.common.core.MessageType;
import springware.mci.common.protocol.normalize.DefaultProtocolNormalizer;
import springware.mci.common.protocol.normalize.ProtocolNormalizer;
import springware.mci.common.protocol.normalize.ProtocolNormalizerRegistry;
import springware.mci.common.response.NormalizedResponse;
import springware.mci.common.response.ResponseStatus;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProtocolNormalizer 테스트")
class ProtocolNormalizerTest {

    @Nested
    @DisplayName("ProviderANormalizer 테스트")
    class ProviderANormalizerTest {

        private ProtocolNormalizer normalizer;

        @BeforeEach
        void setUp() {
            normalizer = new ProviderANormalizer();
        }

        @Test
        @DisplayName("제공자 ID 확인")
        void getProviderId() {
            assertThat(normalizer.getProviderId()).isEqualTo(ExternalProviderConstants.PROVIDER_A);
        }

        @Test
        @DisplayName("성공 코드 '00' -> 내부 '0000'으로 정규화")
        void normalizeSuccess() {
            // given
            Message response = createProviderAResponse(
                    ExternalProviderConstants.PROVIDER_A_SUCCESS,
                    null
            );

            // when
            NormalizedResponse normalized = normalizer.normalize(response);

            // then
            assertThat(normalized.isSuccess()).isTrue();
            assertThat(normalized.getStatus()).isEqualTo(ResponseStatus.SUCCESS);
            assertThat(normalized.getErrorCode()).isEqualTo(DemoConstants.RSP_SUCCESS);
            assertThat(normalized.getOriginalErrorCode()).isEqualTo(ExternalProviderConstants.PROVIDER_A_SUCCESS);
        }

        @Test
        @DisplayName("에러 코드 '01' (무효계좌) -> 내부 '1001'으로 정규화")
        void normalizeInvalidAccount() {
            // given
            Message response = createProviderAResponse(
                    ExternalProviderConstants.PROVIDER_A_INVALID_ACCOUNT,
                    "Account number is invalid"
            );

            // when
            NormalizedResponse normalized = normalizer.normalize(response);

            // then
            assertThat(normalized.isFailure()).isTrue();
            assertThat(normalized.getStatus()).isEqualTo(ResponseStatus.FAILURE);
            assertThat(normalized.getErrorCode()).isEqualTo(DemoConstants.RSP_INVALID_ACCOUNT);
            assertThat(normalized.getErrorMessage()).isEqualTo("Account number is invalid");
            assertThat(normalized.getOriginalErrorCode()).isEqualTo(ExternalProviderConstants.PROVIDER_A_INVALID_ACCOUNT);
        }

        @Test
        @DisplayName("에러 코드 '02' (잔액부족) -> 내부 '1002'으로 정규화")
        void normalizeInsufficientBalance() {
            // given
            Message response = createProviderAResponse(
                    ExternalProviderConstants.PROVIDER_A_INSUFFICIENT_BALANCE,
                    "Insufficient balance"
            );

            // when
            NormalizedResponse normalized = normalizer.normalize(response);

            // then
            assertThat(normalized.isFailure()).isTrue();
            assertThat(normalized.getErrorCode()).isEqualTo(DemoConstants.RSP_INSUFFICIENT_BALANCE);
        }

        @Test
        @DisplayName("에러 코드 '99' (시스템에러) -> 내부 '9999'으로 정규화")
        void normalizeSystemError() {
            // given
            Message response = createProviderAResponse(
                    ExternalProviderConstants.PROVIDER_A_SYSTEM_ERROR,
                    "System error occurred"
            );

            // when
            NormalizedResponse normalized = normalizer.normalize(response);

            // then
            assertThat(normalized.isFailure()).isTrue();
            assertThat(normalized.getErrorCode()).isEqualTo(DemoConstants.RSP_SYSTEM_ERROR);
            assertThat(normalized.getErrorMessage()).isEqualTo("System error occurred");
        }

        private Message createProviderAResponse(String resultCode, String errorMessage) {
            Message message = Message.builder()
                    .messageCode(ExternalProviderConstants.PROVIDER_A_BALANCE_RES)
                    .messageType(MessageType.RESPONSE)
                    .build();
            message.setField(ExternalProviderConstants.PROVIDER_A_CODE_FIELD, resultCode);
            if (errorMessage != null) {
                message.setField(ExternalProviderConstants.PROVIDER_A_ERROR_FIELD, errorMessage);
            }
            return message;
        }
    }

    @Nested
    @DisplayName("ProviderBNormalizer 테스트")
    class ProviderBNormalizerTest {

        private ProtocolNormalizer normalizer;

        @BeforeEach
        void setUp() {
            normalizer = new ProviderBNormalizer();
        }

        @Test
        @DisplayName("제공자 ID 확인")
        void getProviderId() {
            assertThat(normalizer.getProviderId()).isEqualTo(ExternalProviderConstants.PROVIDER_B);
        }

        @Test
        @DisplayName("성공 코드 'SUCCESS' -> 내부 '0000'으로 정규화")
        void normalizeSuccess() {
            // given
            Message response = createProviderBResponse(
                    ExternalProviderConstants.PROVIDER_B_SUCCESS,
                    null
            );

            // when
            NormalizedResponse normalized = normalizer.normalize(response);

            // then
            assertThat(normalized.isSuccess()).isTrue();
            assertThat(normalized.getErrorCode()).isEqualTo(DemoConstants.RSP_SUCCESS);
            assertThat(normalized.getOriginalErrorCode()).isEqualTo(ExternalProviderConstants.PROVIDER_B_SUCCESS);
        }

        @Test
        @DisplayName("에러 코드 'FAIL' -> 내부 '9999'으로 정규화")
        void normalizeFail() {
            // given
            Message response = createProviderBResponse(
                    ExternalProviderConstants.PROVIDER_B_FAIL,
                    "Transaction failed"
            );

            // when
            NormalizedResponse normalized = normalizer.normalize(response);

            // then
            assertThat(normalized.isFailure()).isTrue();
            assertThat(normalized.getErrorCode()).isEqualTo(DemoConstants.RSP_SYSTEM_ERROR);
            assertThat(normalized.getErrorMessage()).isEqualTo("Transaction failed");
        }

        @Test
        @DisplayName("에러 코드 'ACCT_ERR' -> 내부 '1001'으로 정규화")
        void normalizeAccountError() {
            // given
            Message response = createProviderBResponse(
                    ExternalProviderConstants.PROVIDER_B_ACCT_ERR,
                    "Account validation failed"
            );

            // when
            NormalizedResponse normalized = normalizer.normalize(response);

            // then
            assertThat(normalized.isFailure()).isTrue();
            assertThat(normalized.getErrorCode()).isEqualTo(DemoConstants.RSP_INVALID_ACCOUNT);
        }

        private Message createProviderBResponse(String status, String errorReason) {
            Message message = Message.builder()
                    .messageCode(ExternalProviderConstants.PROVIDER_B_BALANCE_RES)
                    .messageType(MessageType.RESPONSE)
                    .build();
            message.setField(ExternalProviderConstants.PROVIDER_B_CODE_FIELD, status);
            if (errorReason != null) {
                message.setField(ExternalProviderConstants.PROVIDER_B_ERROR_FIELD, errorReason);
            }
            return message;
        }
    }

    @Nested
    @DisplayName("ProviderCNormalizer 테스트")
    class ProviderCNormalizerTest {

        private ProtocolNormalizer normalizer;

        @BeforeEach
        void setUp() {
            normalizer = new ProviderCNormalizer();
        }

        @Test
        @DisplayName("제공자 ID 확인")
        void getProviderId() {
            assertThat(normalizer.getProviderId()).isEqualTo(ExternalProviderConstants.PROVIDER_C);
        }

        @Test
        @DisplayName("내부 형식과 동일 - 성공 '0000'")
        void normalizeSuccess() {
            // given
            Message response = createProviderCResponse(
                    ExternalProviderConstants.PROVIDER_C_SUCCESS,
                    null
            );

            // when
            NormalizedResponse normalized = normalizer.normalize(response);

            // then
            assertThat(normalized.isSuccess()).isTrue();
            assertThat(normalized.getErrorCode()).isEqualTo(DemoConstants.RSP_SUCCESS);
        }

        @Test
        @DisplayName("내부 형식과 동일 - 에러 '1001'")
        void normalizeInvalidAccount() {
            // given
            Message response = createProviderCResponse(
                    ExternalProviderConstants.PROVIDER_C_INVALID_ACCOUNT,
                    "Invalid account"
            );

            // when
            NormalizedResponse normalized = normalizer.normalize(response);

            // then
            assertThat(normalized.isFailure()).isTrue();
            assertThat(normalized.getErrorCode()).isEqualTo(DemoConstants.RSP_INVALID_ACCOUNT);
        }

        private Message createProviderCResponse(String rspCode, String errMsg) {
            Message message = Message.builder()
                    .messageCode(ExternalProviderConstants.PROVIDER_C_BALANCE_RES)
                    .messageType(MessageType.RESPONSE)
                    .build();
            message.setField(ExternalProviderConstants.PROVIDER_C_CODE_FIELD, rspCode);
            if (errMsg != null) {
                message.setField(ExternalProviderConstants.PROVIDER_C_ERROR_FIELD, errMsg);
            }
            return message;
        }
    }

    @Nested
    @DisplayName("ProtocolNormalizerRegistry 테스트")
    class ProtocolNormalizerRegistryTest {

        private ProtocolNormalizerRegistry registry;

        @BeforeEach
        void setUp() {
            registry = new ProtocolNormalizerRegistry();
        }

        @Test
        @DisplayName("정규화기 등록 및 조회")
        void registerAndGet() {
            // given
            ProtocolNormalizer normalizer = new ProviderANormalizer();

            // when
            registry.register(normalizer);

            // then
            assertThat(registry.hasNormalizer(ExternalProviderConstants.PROVIDER_A)).isTrue();
            assertThat(registry.getNormalizer(ExternalProviderConstants.PROVIDER_A)).isNotNull();
            assertThat(registry.size()).isEqualTo(1);
        }

        @Test
        @DisplayName("기본 정규화기 설정 및 사용")
        void defaultNormalizer() {
            // given
            ProtocolNormalizer defaultNormalizer = new DefaultProtocolNormalizer();
            registry.setDefaultNormalizer(defaultNormalizer);

            // when
            ProtocolNormalizer result = registry.getNormalizer("UNKNOWN_PROVIDER");

            // then
            assertThat(result).isNotNull();
            assertThat(result.getProviderId()).isEqualTo(DefaultProtocolNormalizer.PROVIDER_ID);
        }

        @Test
        @DisplayName("정규화기 제거")
        void unregister() {
            // given
            registry.register(new ProviderANormalizer());

            // when
            registry.unregister(ExternalProviderConstants.PROVIDER_A);

            // then
            assertThat(registry.hasNormalizer(ExternalProviderConstants.PROVIDER_A)).isFalse();
        }

        @Test
        @DisplayName("ExternalProviderRegistry 초기화")
        void externalProviderRegistry() {
            // given & when
            ExternalProviderRegistry externalRegistry = new ExternalProviderRegistry();

            // then
            ProtocolNormalizerRegistry normalizerRegistry = externalRegistry.getNormalizerRegistry();
            assertThat(normalizerRegistry.size()).isEqualTo(3);
            assertThat(normalizerRegistry.hasNormalizer(ExternalProviderConstants.PROVIDER_A)).isTrue();
            assertThat(normalizerRegistry.hasNormalizer(ExternalProviderConstants.PROVIDER_B)).isTrue();
            assertThat(normalizerRegistry.hasNormalizer(ExternalProviderConstants.PROVIDER_C)).isTrue();
            assertThat(normalizerRegistry.getDefaultNormalizer()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Null 응답 처리 테스트")
    class NullHandlingTest {

        @Test
        @DisplayName("null 응답 정규화시 UNKNOWN 반환")
        void normalizeNullResponse() {
            // given
            ProtocolNormalizer normalizer = new ProviderANormalizer();

            // when
            NormalizedResponse normalized = normalizer.normalize(null);

            // then
            assertThat(normalized.getStatus()).isEqualTo(ResponseStatus.UNKNOWN);
            assertThat(normalized.getErrorCode()).isEqualTo(DemoConstants.RSP_SYSTEM_ERROR);
            assertThat(normalized.getErrorMessage()).isEqualTo("External response is null");
        }

        @Test
        @DisplayName("응답 코드가 없는 경우 UNKNOWN 반환")
        void normalizeResponseWithoutCode() {
            // given
            ProtocolNormalizer normalizer = new ProviderANormalizer();
            Message response = Message.builder()
                    .messageCode(ExternalProviderConstants.PROVIDER_A_BALANCE_RES)
                    .messageType(MessageType.RESPONSE)
                    .build();
            // resultCode 필드를 설정하지 않음

            // when
            NormalizedResponse normalized = normalizer.normalize(response);

            // then
            assertThat(normalized.getStatus()).isEqualTo(ResponseStatus.UNKNOWN);
        }
    }
}
