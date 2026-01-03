package springware.mci.client.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import springware.mci.client.config.ClientConfig;
import springware.mci.common.core.Message;
import springware.mci.common.exception.ConnectionException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Connection Retry Tests")
class ConnectionRetryTest {

    @Test
    @DisplayName("첫 번째 시도에서 연결 성공")
    void connectSuccessOnFirstAttempt() {
        // given
        AtomicInteger attempts = new AtomicInteger(0);
        ClientConfig config = ClientConfig.builder()
                .clientId("test-client")
                .host("localhost")
                .port(9999)
                .retryEnabled(true)
                .retryAttempts(3)
                .retryDelay(100)
                .build();

        TestMciClient client = new TestMciClient(config, () -> {
            attempts.incrementAndGet();
            // 성공
        });

        // when
        client.connect();

        // then
        assertThat(attempts.get()).isEqualTo(1);
        assertThat(client.isConnected()).isTrue();
    }

    @Test
    @DisplayName("세 번째 시도에서 연결 성공")
    void connectSuccessOnThirdAttempt() {
        // given
        AtomicInteger attempts = new AtomicInteger(0);
        ClientConfig config = ClientConfig.builder()
                .clientId("test-client")
                .host("localhost")
                .port(9999)
                .retryEnabled(true)
                .retryAttempts(3)
                .retryDelay(50)
                .retryBackoffMultiplier(1.0)
                .build();

        TestMciClient client = new TestMciClient(config, () -> {
            int attempt = attempts.incrementAndGet();
            if (attempt < 3) {
                throw new RuntimeException("Connection refused");
            }
            // 세 번째 시도에서 성공
        });

        // when
        long startTime = System.currentTimeMillis();
        client.connect();
        long elapsed = System.currentTimeMillis() - startTime;

        // then
        assertThat(attempts.get()).isEqualTo(3);
        assertThat(client.isConnected()).isTrue();
        // 최소 2번의 대기시간 (50ms * 2 = 100ms)
        assertThat(elapsed).isGreaterThanOrEqualTo(100);
    }

    @Test
    @DisplayName("모든 재시도 실패 후 예외 발생")
    void connectFailsAfterAllRetries() {
        // given
        AtomicInteger attempts = new AtomicInteger(0);
        ClientConfig config = ClientConfig.builder()
                .clientId("test-client")
                .host("localhost")
                .port(9999)
                .retryEnabled(true)
                .retryAttempts(3)
                .retryDelay(50)
                .retryBackoffMultiplier(1.0)
                .build();

        TestMciClient client = new TestMciClient(config, () -> {
            attempts.incrementAndGet();
            throw new RuntimeException("Connection refused");
        });

        // when & then
        assertThatThrownBy(client::connect)
                .isInstanceOf(ConnectionException.class)
                .hasMessageContaining("after 3 attempts");

        assertThat(attempts.get()).isEqualTo(3);
        assertThat(client.isConnected()).isFalse();
    }

    @Test
    @DisplayName("재시도 비활성화시 한 번만 시도")
    void connectWithoutRetry() {
        // given
        AtomicInteger attempts = new AtomicInteger(0);
        ClientConfig config = ClientConfig.builder()
                .clientId("test-client")
                .host("localhost")
                .port(9999)
                .retryEnabled(false)
                .build();

        TestMciClient client = new TestMciClient(config, () -> {
            attempts.incrementAndGet();
            throw new RuntimeException("Connection refused");
        });

        // when & then
        assertThatThrownBy(client::connect)
                .isInstanceOf(ConnectionException.class);

        assertThat(attempts.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Exponential backoff 동작 확인")
    void connectWithExponentialBackoff() {
        // given
        AtomicInteger attempts = new AtomicInteger(0);
        ClientConfig config = ClientConfig.builder()
                .clientId("test-client")
                .host("localhost")
                .port(9999)
                .retryEnabled(true)
                .retryAttempts(4)
                .retryDelay(100)
                .retryBackoffMultiplier(2.0)
                .build();

        TestMciClient client = new TestMciClient(config, () -> {
            int attempt = attempts.incrementAndGet();
            if (attempt < 4) {
                throw new RuntimeException("Connection refused");
            }
        });

        // when
        long startTime = System.currentTimeMillis();
        client.connect();
        long elapsed = System.currentTimeMillis() - startTime;

        // then
        assertThat(attempts.get()).isEqualTo(4);
        // 대기 시간: 100 + 200 + 400 = 700ms 이상
        assertThat(elapsed).isGreaterThanOrEqualTo(700);
    }

    @Test
    @DisplayName("기본 설정값 확인")
    void defaultConfigValues() {
        // given
        ClientConfig config = ClientConfig.builder()
                .clientId("test-client")
                .host("localhost")
                .port(9999)
                .build();

        // then
        assertThat(config.isRetryEnabled()).isTrue();
        assertThat(config.getRetryAttempts()).isEqualTo(3);
        assertThat(config.getRetryDelay()).isEqualTo(1000);
        assertThat(config.getRetryBackoffMultiplier()).isEqualTo(1.5);
    }

    /**
     * 테스트용 MCI 클라이언트 구현
     */
    private static class TestMciClient extends AbstractMciClient {

        private final Runnable connectAction;

        TestMciClient(ClientConfig config, Runnable connectAction) {
            super(config);
            this.connectAction = connectAction;
        }

        @Override
        protected void doConnect() {
            connectAction.run();
        }

        @Override
        protected void doDisconnect() {
            // no-op
        }

        @Override
        protected void doSendOneWay(Message message) {
            // no-op
        }

        @Override
        public CompletableFuture<Message> sendAsync(Message message) {
            return CompletableFuture.completedFuture(null);
        }
    }
}
