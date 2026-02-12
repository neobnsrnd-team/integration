package demo.mci.external;

import demo.mci.banking.biz.BizTestHelper;
import demo.mci.common.DemoConstants;
import demo.mci.external.simulator.ProviderASimulator;
import demo.mci.external.simulator.ProviderBSimulator;
import demo.mci.external.simulator.ProviderCSimulator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import springware.mci.common.core.Message;
import springware.mci.common.exception.ErrorResponseException;
import springware.mci.common.protocol.normalize.ProtocolNormalizer;
import springware.mci.common.protocol.normalize.ProtocolNormalizerRegistry;
import springware.mci.common.response.NormalizedResponse;
import springware.mci.common.response.ResponseStatus;
import springware.mci.server.biz.Biz;
import springware.mci.server.core.MessageContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("외부 제공자 통합 테스트")
class ExternalProviderIntegrationTest {

    private ExternalProviderRegistry providerRegistry;
    private ProtocolNormalizerRegistry normalizerRegistry;
    private MessageContext context;

    @BeforeEach
    void setUp() {
        providerRegistry = new ExternalProviderRegistry();
        normalizerRegistry = providerRegistry.getNormalizerRegistry();
        context = BizTestHelper.createContext();
    }

    @Nested
    @DisplayName("Provider A 통합 테스트")
    class ProviderAIntegrationTest {

        private Biz simulator;
        private ProtocolNormalizer normalizer;

        @BeforeEach
        void setUp() {
            simulator = new ProviderASimulator();
            normalizer = normalizerRegistry.getNormalizer(ExternalProviderConstants.PROVIDER_A);
        }

        @Test
        @DisplayName("정상 계좌 조회 - '00' -> '0000'")
        void successfulBalanceInquiry() {
            // given
            Message request = BizTestHelper.createRequest(ExternalProviderConstants.PROVIDER_A_BALANCE_REQ);
            request.setField("accountNo", "1234567890");

            // when
            Message externalResponse = simulator.execute(request, context);
            NormalizedResponse response = normalizer.normalize(externalResponse);

            // then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getErrorCode()).isEqualTo(DemoConstants.RSP_SUCCESS);
            assertThat(response.getOriginalErrorCode()).isEqualTo("00");
            assertThat(response.<Long>getField("balance")).isEqualTo(1000000L);
        }

        @Test
        @DisplayName("무효 계좌 - '01' -> '1001'")
        void invalidAccount() {
            // given
            Message request = BizTestHelper.createRequest(ExternalProviderConstants.PROVIDER_A_BALANCE_REQ);
            request.setField("accountNo", "");

            // when
            Message externalResponse = simulator.execute(request, context);
            NormalizedResponse response = normalizer.normalize(externalResponse);

            // then
            assertThat(response.isFailure()).isTrue();
            assertThat(response.getErrorCode()).isEqualTo(DemoConstants.RSP_INVALID_ACCOUNT);
            assertThat(response.getOriginalErrorCode()).isEqualTo("01");
            assertThat(response.getErrorMessage()).isEqualTo("Account number is required");
        }

        @Test
        @DisplayName("시스템 에러 - '99' -> '9999'")
        void systemError() {
            // given
            Message request = BizTestHelper.createRequest(ExternalProviderConstants.PROVIDER_A_BALANCE_REQ);
            request.setField("accountNo", "9991234567");

            // when
            Message externalResponse = simulator.execute(request, context);
            NormalizedResponse response = normalizer.normalize(externalResponse);

            // then
            assertThat(response.isFailure()).isTrue();
            assertThat(response.getErrorCode()).isEqualTo(DemoConstants.RSP_SYSTEM_ERROR);
            assertThat(response.getOriginalErrorCode()).isEqualTo("99");
        }
    }

    @Nested
    @DisplayName("Provider B 통합 테스트")
    class ProviderBIntegrationTest {

        private Biz simulator;
        private ProtocolNormalizer normalizer;

        @BeforeEach
        void setUp() {
            simulator = new ProviderBSimulator();
            normalizer = normalizerRegistry.getNormalizer(ExternalProviderConstants.PROVIDER_B);
        }

        @Test
        @DisplayName("정상 계좌 조회 - 'SUCCESS' -> '0000'")
        void successfulBalanceInquiry() {
            // given
            Message request = BizTestHelper.createRequest(ExternalProviderConstants.PROVIDER_B_BALANCE_REQ);
            request.setField("accountNo", "1234567890");

            // when
            Message externalResponse = simulator.execute(request, context);
            NormalizedResponse response = normalizer.normalize(externalResponse);

            // then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getErrorCode()).isEqualTo(DemoConstants.RSP_SUCCESS);
            assertThat(response.getOriginalErrorCode()).isEqualTo("SUCCESS");
            assertThat(response.<Long>getField("balance")).isEqualTo(2000000L);
        }

        @Test
        @DisplayName("계좌 에러 - 'ACCT_ERR' -> '1001'")
        void accountError() {
            // given
            Message request = BizTestHelper.createRequest(ExternalProviderConstants.PROVIDER_B_BALANCE_REQ);
            request.setField("accountNo", "");

            // when
            Message externalResponse = simulator.execute(request, context);
            NormalizedResponse response = normalizer.normalize(externalResponse);

            // then
            assertThat(response.isFailure()).isTrue();
            assertThat(response.getErrorCode()).isEqualTo(DemoConstants.RSP_INVALID_ACCOUNT);
            assertThat(response.getOriginalErrorCode()).isEqualTo("ACCT_ERR");
        }

        @Test
        @DisplayName("일반 실패 - 'FAIL' -> '9999'")
        void generalFailure() {
            // given
            Message request = BizTestHelper.createRequest(ExternalProviderConstants.PROVIDER_B_BALANCE_REQ);
            request.setField("accountNo", "0001234567");

            // when
            Message externalResponse = simulator.execute(request, context);
            NormalizedResponse response = normalizer.normalize(externalResponse);

            // then
            assertThat(response.isFailure()).isTrue();
            assertThat(response.getErrorCode()).isEqualTo(DemoConstants.RSP_SYSTEM_ERROR);
            assertThat(response.getOriginalErrorCode()).isEqualTo("FAIL");
        }
    }

    @Nested
    @DisplayName("Provider C 통합 테스트")
    class ProviderCIntegrationTest {

        private Biz simulator;
        private ProtocolNormalizer normalizer;

        @BeforeEach
        void setUp() {
            simulator = new ProviderCSimulator();
            normalizer = normalizerRegistry.getNormalizer(ExternalProviderConstants.PROVIDER_C);
        }

        @Test
        @DisplayName("정상 계좌 조회 - '0000' 동일")
        void successfulBalanceInquiry() {
            // given
            Message request = BizTestHelper.createRequest(ExternalProviderConstants.PROVIDER_C_BALANCE_REQ);
            request.setField("accountNo", "1234567890");

            // when
            Message externalResponse = simulator.execute(request, context);
            NormalizedResponse response = normalizer.normalize(externalResponse);

            // then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getErrorCode()).isEqualTo(DemoConstants.RSP_SUCCESS);
            assertThat(response.getOriginalErrorCode()).isEqualTo("0000");
            assertThat(response.<Long>getField("balance")).isEqualTo(3000000L);
        }

        @Test
        @DisplayName("무효 계좌 - '1001' 동일")
        void invalidAccount() {
            // given
            Message request = BizTestHelper.createRequest(ExternalProviderConstants.PROVIDER_C_BALANCE_REQ);
            request.setField("accountNo", "");

            // when
            Message externalResponse = simulator.execute(request, context);
            NormalizedResponse response = normalizer.normalize(externalResponse);

            // then
            assertThat(response.isFailure()).isTrue();
            assertThat(response.getErrorCode()).isEqualTo(DemoConstants.RSP_INVALID_ACCOUNT);
            assertThat(response.getOriginalErrorCode()).isEqualTo("1001");
        }
    }

    @Nested
    @DisplayName("일관된 에러 코드 확인 테스트")
    class ConsistentErrorCodeTest {

        @Test
        @DisplayName("모든 제공자에서 무효 계좌 에러가 동일한 내부 코드('1001')로 정규화됨")
        void consistentInvalidAccountCode() {
            // given
            Message requestA = BizTestHelper.createRequest(ExternalProviderConstants.PROVIDER_A_BALANCE_REQ);
            requestA.setField("accountNo", "");

            Message requestB = BizTestHelper.createRequest(ExternalProviderConstants.PROVIDER_B_BALANCE_REQ);
            requestB.setField("accountNo", "");

            Message requestC = BizTestHelper.createRequest(ExternalProviderConstants.PROVIDER_C_BALANCE_REQ);
            requestC.setField("accountNo", "");

            Biz simulatorA = new ProviderASimulator();
            Biz simulatorB = new ProviderBSimulator();
            Biz simulatorC = new ProviderCSimulator();

            // when
            NormalizedResponse responseA = normalizerRegistry.getNormalizer(ExternalProviderConstants.PROVIDER_A)
                    .normalize(simulatorA.execute(requestA, context));
            NormalizedResponse responseB = normalizerRegistry.getNormalizer(ExternalProviderConstants.PROVIDER_B)
                    .normalize(simulatorB.execute(requestB, context));
            NormalizedResponse responseC = normalizerRegistry.getNormalizer(ExternalProviderConstants.PROVIDER_C)
                    .normalize(simulatorC.execute(requestC, context));

            // then - 모든 제공자가 동일한 내부 에러 코드 반환
            assertThat(responseA.getErrorCode()).isEqualTo(DemoConstants.RSP_INVALID_ACCOUNT);
            assertThat(responseB.getErrorCode()).isEqualTo(DemoConstants.RSP_INVALID_ACCOUNT);
            assertThat(responseC.getErrorCode()).isEqualTo(DemoConstants.RSP_INVALID_ACCOUNT);

            // 원본 코드는 각각 다름
            assertThat(responseA.getOriginalErrorCode()).isEqualTo("01");  // Provider A
            assertThat(responseB.getOriginalErrorCode()).isEqualTo("ACCT_ERR");  // Provider B
            assertThat(responseC.getOriginalErrorCode()).isEqualTo("1001");  // Provider C
        }
    }

    @Nested
    @DisplayName("orThrow 패턴 테스트")
    class OrThrowPatternTest {

        @Test
        @DisplayName("성공 응답에서 orThrow() - 정상 진행")
        void orThrowOnSuccess() {
            // given
            Biz simulator = new ProviderASimulator();
            ProtocolNormalizer normalizer = normalizerRegistry.getNormalizer(ExternalProviderConstants.PROVIDER_A);

            Message request = BizTestHelper.createRequest(ExternalProviderConstants.PROVIDER_A_BALANCE_REQ);
            request.setField("accountNo", "1234567890");

            // when
            NormalizedResponse response = normalizer.normalize(simulator.execute(request, context)).orThrow();

            // then
            assertThat(response.isSuccess()).isTrue();
            Long balance = response.getField("balance");
            assertThat(balance).isEqualTo(1000000L);
        }

        @Test
        @DisplayName("실패 응답에서 orThrow() - ErrorResponseException 발생")
        void orThrowOnFailure() {
            // given
            Biz simulator = new ProviderBSimulator();
            ProtocolNormalizer normalizer = normalizerRegistry.getNormalizer(ExternalProviderConstants.PROVIDER_B);

            Message request = BizTestHelper.createRequest(ExternalProviderConstants.PROVIDER_B_BALANCE_REQ);
            request.setField("accountNo", "9991234567");  // 시스템 에러 트리거

            Message externalResponse = simulator.execute(request, context);
            NormalizedResponse normalized = normalizer.normalize(externalResponse);

            // when & then
            assertThatThrownBy(normalized::orThrow)
                    .isInstanceOf(ErrorResponseException.class)
                    .satisfies(e -> {
                        ErrorResponseException ex = (ErrorResponseException) e;
                        assertThat(ex.getProviderId()).isEqualTo(ExternalProviderConstants.PROVIDER_B);
                        assertThat(ex.getExternalErrorCode()).isEqualTo("ERROR");
                        assertThat(ex.getExternalErrorMessage()).isEqualTo("System processing error occurred");
                    });
        }
    }

    @Nested
    @DisplayName("기본 정규화기 테스트")
    class DefaultNormalizerTest {

        @Test
        @DisplayName("알 수 없는 제공자에 기본 정규화기 사용")
        void unknownProviderUsesDefault() {
            // given
            ProtocolNormalizer defaultNormalizer = normalizerRegistry.getNormalizer("UNKNOWN_PROVIDER");

            // when & then
            assertThat(defaultNormalizer).isNotNull();
            assertThat(defaultNormalizer.getProviderId()).isEqualTo("DEFAULT");
        }
    }
}
