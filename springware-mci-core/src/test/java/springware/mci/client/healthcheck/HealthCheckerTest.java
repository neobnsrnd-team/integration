package springware.mci.client.healthcheck;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import springware.mci.client.config.ClientConfig;
import springware.mci.client.core.MciClient;
import springware.mci.common.core.Message;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("HealthChecker Tests")
class HealthCheckerTest {

    private MockMciClient mockClient;
    private HealthChecker healthChecker;

    @BeforeEach
    void setUp() {
        mockClient = new MockMciClient();
    }

    @AfterEach
    void tearDown() {
        if (healthChecker != null) {
            healthChecker.close();
        }
    }

    @Test
    @DisplayName("기본 설정값 확인")
    void defaultConfigValues() {
        HealthCheckConfig config = HealthCheckConfig.defaultConfig();

        assertThat(config.isEnabled()).isTrue();
        assertThat(config.getIntervalMillis()).isEqualTo(30000);
        assertThat(config.getTimeoutMillis()).isEqualTo(5000);
        assertThat(config.getFailureThreshold()).isEqualTo(3);
        assertThat(config.isAutoReconnect()).isTrue();
    }

    @Test
    @DisplayName("비활성화 설정")
    void disabledConfig() {
        HealthCheckConfig config = HealthCheckConfig.disabled();

        assertThat(config.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("설정 검증 - 타임아웃이 인터벌보다 크면 실패")
    void validateConfig_timeoutGreaterThanInterval() {
        HealthCheckConfig config = HealthCheckConfig.builder()
                .intervalMillis(1000)
                .timeoutMillis(2000)
                .build();

        assertThatThrownBy(config::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Timeout must be less than interval");
    }

    @Test
    @DisplayName("비활성화시 시작하지 않음")
    void doesNotStartWhenDisabled() {
        HealthCheckConfig config = HealthCheckConfig.disabled();
        healthChecker = new HealthChecker("test", mockClient, config);

        healthChecker.start();

        assertThat(healthChecker.isRunning()).isFalse();
    }

    @Test
    @DisplayName("시작 및 중지")
    void startAndStop() {
        HealthCheckConfig config = HealthCheckConfig.builder()
                .intervalMillis(100)
                .timeoutMillis(50)
                .initialDelayMillis(1000) // 긴 초기 지연
                .build();
        healthChecker = new HealthChecker("test", mockClient, config);

        healthChecker.start();
        assertThat(healthChecker.isRunning()).isTrue();

        healthChecker.stop();
        assertThat(healthChecker.isRunning()).isFalse();
    }

    @Test
    @DisplayName("중복 시작 방지")
    void preventsDuplicateStart() {
        HealthCheckConfig config = HealthCheckConfig.builder()
                .intervalMillis(100)
                .timeoutMillis(50)
                .initialDelayMillis(1000)
                .build();
        healthChecker = new HealthChecker("test", mockClient, config);

        healthChecker.start();
        healthChecker.start(); // 두 번째 시작 시도

        assertThat(healthChecker.isRunning()).isTrue();
    }

    @Test
    @DisplayName("헬스 체크 성공시 healthy 상태 유지")
    void staysHealthyOnSuccess() throws InterruptedException {
        mockClient.setConnected(true);
        mockClient.setResponseSupplier(() -> Message.builder().messageCode("PONG").build());

        HealthCheckConfig config = HealthCheckConfig.builder()
                .intervalMillis(100)
                .timeoutMillis(50)
                .initialDelayMillis(10)
                .build();
        healthChecker = new HealthChecker("test", mockClient, config);

        healthChecker.start();
        Thread.sleep(200);

        assertThat(healthChecker.isHealthy()).isTrue();
        assertThat(healthChecker.getConsecutiveFailures()).isEqualTo(0);
    }

    @Test
    @DisplayName("연속 실패시 unhealthy 상태로 전환")
    void becomesUnhealthyAfterConsecutiveFailures() throws InterruptedException {
        mockClient.setConnected(true);
        mockClient.setResponseSupplier(() -> {
            throw new RuntimeException("Connection failed");
        });

        HealthCheckConfig config = HealthCheckConfig.builder()
                .intervalMillis(50)
                .timeoutMillis(20)
                .failureThreshold(2)
                .initialDelayMillis(10)
                .autoReconnect(false)
                .build();
        healthChecker = new HealthChecker("test", mockClient, config);

        CountDownLatch unhealthyLatch = new CountDownLatch(1);
        healthChecker.addListener(new HealthCheckListener() {
            @Override
            public void onConnectionUnhealthy() {
                unhealthyLatch.countDown();
            }
        });

        healthChecker.start();

        boolean unhealthy = unhealthyLatch.await(500, TimeUnit.MILLISECONDS);
        assertThat(unhealthy).isTrue();
        assertThat(healthChecker.isHealthy()).isFalse();
    }

    @Test
    @DisplayName("리스너에 성공 이벤트 전달")
    void notifiesListenerOnSuccess() throws InterruptedException {
        mockClient.setConnected(true);
        mockClient.setResponseSupplier(() -> Message.builder().messageCode("PONG").build());

        HealthCheckConfig config = HealthCheckConfig.builder()
                .intervalMillis(50)
                .timeoutMillis(20)
                .initialDelayMillis(10)
                .build();
        healthChecker = new HealthChecker("test", mockClient, config);

        AtomicLong lastLatency = new AtomicLong(-1);
        CountDownLatch successLatch = new CountDownLatch(1);

        healthChecker.addListener(new HealthCheckListener() {
            @Override
            public void onHealthCheckSuccess(long latencyMillis) {
                lastLatency.set(latencyMillis);
                successLatch.countDown();
            }
        });

        healthChecker.start();

        boolean success = successLatch.await(500, TimeUnit.MILLISECONDS);
        assertThat(success).isTrue();
        assertThat(lastLatency.get()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("리스너에 실패 이벤트 전달")
    void notifiesListenerOnFailure() throws InterruptedException {
        mockClient.setConnected(true);
        mockClient.setResponseSupplier(() -> {
            throw new RuntimeException("Failed");
        });

        HealthCheckConfig config = HealthCheckConfig.builder()
                .intervalMillis(50)
                .timeoutMillis(20)
                .initialDelayMillis(10)
                .autoReconnect(false)
                .build();
        healthChecker = new HealthChecker("test", mockClient, config);

        AtomicInteger failureCount = new AtomicInteger(0);
        CountDownLatch failureLatch = new CountDownLatch(1);

        healthChecker.addListener(new HealthCheckListener() {
            @Override
            public void onHealthCheckFailure(int consecutiveFailures, Throwable cause) {
                failureCount.set(consecutiveFailures);
                failureLatch.countDown();
            }
        });

        healthChecker.start();

        boolean failed = failureLatch.await(500, TimeUnit.MILLISECONDS);
        assertThat(failed).isTrue();
        assertThat(failureCount.get()).isGreaterThan(0);
    }

    @Test
    @DisplayName("연결 복구시 리스너에 알림")
    void notifiesListenerOnRecovery() throws InterruptedException {
        mockClient.setConnected(true);
        AtomicInteger callCount = new AtomicInteger(0);

        // 처음 2번은 실패, 그 이후는 성공
        mockClient.setResponseSupplier(() -> {
            if (callCount.incrementAndGet() <= 2) {
                throw new RuntimeException("Failed");
            }
            return Message.builder().messageCode("PONG").build();
        });

        HealthCheckConfig config = HealthCheckConfig.builder()
                .intervalMillis(50)
                .timeoutMillis(20)
                .failureThreshold(2)
                .initialDelayMillis(10)
                .autoReconnect(false)
                .build();
        healthChecker = new HealthChecker("test", mockClient, config);

        CountDownLatch recoveredLatch = new CountDownLatch(1);

        healthChecker.addListener(new HealthCheckListener() {
            @Override
            public void onConnectionRecovered() {
                recoveredLatch.countDown();
            }
        });

        healthChecker.start();

        boolean recovered = recoveredLatch.await(500, TimeUnit.MILLISECONDS);
        assertThat(recovered).isTrue();
        assertThat(healthChecker.isHealthy()).isTrue();
    }

    @Test
    @DisplayName("통계 정보 확인")
    void statsAreTracked() throws InterruptedException {
        mockClient.setConnected(true);
        AtomicInteger callCount = new AtomicInteger(0);

        // 2번 성공, 1번 실패
        mockClient.setResponseSupplier(() -> {
            int count = callCount.incrementAndGet();
            if (count == 2) {
                throw new RuntimeException("Failed");
            }
            return Message.builder().messageCode("PONG").build();
        });

        HealthCheckConfig config = HealthCheckConfig.builder()
                .intervalMillis(30)
                .timeoutMillis(15)
                .failureThreshold(3)
                .initialDelayMillis(5)
                .autoReconnect(false)
                .build();
        healthChecker = new HealthChecker("test", mockClient, config);

        healthChecker.start();
        Thread.sleep(150);

        HealthChecker.Stats stats = healthChecker.getStats();
        assertThat(stats.totalChecks()).isGreaterThanOrEqualTo(3);
        assertThat(stats.totalFailures()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("리스너 제거")
    void removeListener() throws InterruptedException {
        mockClient.setConnected(true);
        mockClient.setResponseSupplier(() -> Message.builder().messageCode("PONG").build());

        HealthCheckConfig config = HealthCheckConfig.builder()
                .intervalMillis(50)
                .timeoutMillis(20)
                .initialDelayMillis(10)
                .build();
        healthChecker = new HealthChecker("test", mockClient, config);

        AtomicInteger callCount = new AtomicInteger(0);
        HealthCheckListener listener = new HealthCheckListener() {
            @Override
            public void onHealthCheckSuccess(long latencyMillis) {
                callCount.incrementAndGet();
            }
        };

        healthChecker.addListener(listener);
        healthChecker.removeListener(listener);
        healthChecker.start();
        Thread.sleep(100);

        assertThat(callCount.get()).isEqualTo(0);
    }

    @Test
    @DisplayName("클라이언트 연결 안됨 상태에서 실패 처리")
    void failsWhenClientNotConnected() throws InterruptedException {
        mockClient.setConnected(false);

        HealthCheckConfig config = HealthCheckConfig.builder()
                .intervalMillis(50)
                .timeoutMillis(20)
                .failureThreshold(2)
                .initialDelayMillis(10)
                .autoReconnect(false)
                .build();
        healthChecker = new HealthChecker("test", mockClient, config);

        CountDownLatch unhealthyLatch = new CountDownLatch(1);
        healthChecker.addListener(new HealthCheckListener() {
            @Override
            public void onConnectionUnhealthy() {
                unhealthyLatch.countDown();
            }
        });

        healthChecker.start();

        boolean unhealthy = unhealthyLatch.await(500, TimeUnit.MILLISECONDS);
        assertThat(unhealthy).isTrue();
    }

    /**
     * Mock MciClient for testing
     */
    private static class MockMciClient implements MciClient {
        private boolean connected = false;
        private java.util.function.Supplier<Message> responseSupplier = () -> Message.builder().build();

        void setConnected(boolean connected) {
            this.connected = connected;
        }

        void setResponseSupplier(java.util.function.Supplier<Message> supplier) {
            this.responseSupplier = supplier;
        }

        @Override
        public void connect() {}

        @Override
        public void disconnect() {}

        @Override
        public boolean isConnected() {
            return connected;
        }

        @Override
        public Message send(Message message) {
            return responseSupplier.get();
        }

        @Override
        public Message send(Message message, long timeoutMillis) {
            return responseSupplier.get();
        }

        @Override
        public CompletableFuture<Message> sendAsync(Message message) {
            try {
                return CompletableFuture.completedFuture(responseSupplier.get());
            } catch (Exception e) {
                CompletableFuture<Message> future = new CompletableFuture<>();
                future.completeExceptionally(e);
                return future;
            }
        }

        @Override
        public void sendOneWay(Message message) {}

        @Override
        public ClientConfig getConfig() {
            return ClientConfig.builder()
                    .host("localhost")
                    .port(9999)
                    .build();
        }

        @Override
        public void close() {}
    }
}
