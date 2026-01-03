package springware.mci.server.biz;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import springware.mci.common.core.Message;
import springware.mci.server.core.MessageContext;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BizRegistry 테스트")
class BizRegistryTest {

    private BizRegistry bizRegistry;

    @BeforeEach
    void setUp() {
        bizRegistry = new BizRegistry();
    }

    @Test
    @DisplayName("Biz 등록 및 조회")
    void registerAndGetBiz() {
        // given
        Biz testBiz = new TestBiz("TEST");

        // when
        bizRegistry.register(testBiz);
        Biz result = bizRegistry.getBiz("TEST");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getMessageCode()).isEqualTo("TEST");
    }

    @Test
    @DisplayName("등록되지 않은 메시지 코드 조회시 null 반환")
    void getBizNotFound() {
        // when
        Biz result = bizRegistry.getBiz("UNKNOWN");

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("기본 Biz 설정 및 조회")
    void defaultBiz() {
        // given
        Biz defaultBiz = new TestBiz("DEFAULT");
        bizRegistry.setDefaultBiz(defaultBiz);

        // when
        Biz result = bizRegistry.getBiz("UNKNOWN");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getMessageCode()).isEqualTo("DEFAULT");
    }

    @Test
    @DisplayName("등록된 Biz가 있으면 기본 Biz 대신 반환")
    void registeredBizOverDefault() {
        // given
        Biz testBiz = new TestBiz("TEST");
        Biz defaultBiz = new TestBiz("DEFAULT");
        bizRegistry.register(testBiz);
        bizRegistry.setDefaultBiz(defaultBiz);

        // when
        Biz result = bizRegistry.getBiz("TEST");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getMessageCode()).isEqualTo("TEST");
    }

    @Test
    @DisplayName("Biz 존재 여부 확인")
    void hasBiz() {
        // given
        bizRegistry.register(new TestBiz("TEST"));

        // then
        assertThat(bizRegistry.hasBiz("TEST")).isTrue();
        assertThat(bizRegistry.hasBiz("UNKNOWN")).isFalse();
    }

    @Test
    @DisplayName("등록된 Biz 수 확인")
    void size() {
        // given
        bizRegistry.register(new TestBiz("TEST1"));
        bizRegistry.register(new TestBiz("TEST2"));
        bizRegistry.register(new TestBiz("TEST3"));

        // then
        assertThat(bizRegistry.size()).isEqualTo(3);
    }

    @Test
    @DisplayName("모든 Biz 제거")
    void clear() {
        // given
        bizRegistry.register(new TestBiz("TEST1"));
        bizRegistry.register(new TestBiz("TEST2"));
        bizRegistry.setDefaultBiz(new TestBiz("DEFAULT"));

        // when
        bizRegistry.clear();

        // then
        assertThat(bizRegistry.size()).isEqualTo(0);
        assertThat(bizRegistry.getBiz("TEST1")).isNull();
        assertThat(bizRegistry.getBiz("UNKNOWN")).isNull(); // default도 제거됨
    }

    /**
     * 테스트용 Biz 구현
     */
    private static class TestBiz implements Biz {
        private final String messageCode;

        TestBiz(String messageCode) {
            this.messageCode = messageCode;
        }

        @Override
        public Message execute(Message request, MessageContext context) {
            return Message.builder().messageCode(messageCode + "_RES").build();
        }

        @Override
        public String getMessageCode() {
            return messageCode;
        }
    }
}
