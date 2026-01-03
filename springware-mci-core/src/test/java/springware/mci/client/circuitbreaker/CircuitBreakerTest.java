package springware.mci.client.circuitbreaker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CircuitBreaker Tests")
class CircuitBreakerTest {

    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        CircuitBreakerConfig config = CircuitBreakerConfig.builder()
                .failureThreshold(3)
                .successThreshold(2)
                .openTimeout(500)
                .halfOpenPermittedCalls(2)
                .build();
        circuitBreaker = new CircuitBreaker("test-cb", config);
    }

    @Test
    @DisplayName("초기 상태는 CLOSED")
    void initialStateIsClosed() {
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreakerState.CLOSED);
    }

    @Test
    @DisplayName("성공시 CLOSED 유지")
    void staysClosedOnSuccess() {
        // when
        circuitBreaker.execute(() -> "success");

        // then
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreakerState.CLOSED);
        assertThat(circuitBreaker.getFailureCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("실패 임계값 도달시 OPEN으로 전환")
    void transitionsToOpenAfterFailureThreshold() {
        // when - 3번 실패
        for (int i = 0; i < 3; i++) {
            try {
                circuitBreaker.execute(() -> {
                    throw new RuntimeException("error");
                });
            } catch (RuntimeException e) {
                // expected
            }
        }

        // then
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreakerState.OPEN);
    }

    @Test
    @DisplayName("OPEN 상태에서 요청 거부")
    void rejectsRequestsWhenOpen() {
        // given - OPEN 상태로 전환
        circuitBreaker.forceOpen();

        // when & then
        assertThatThrownBy(() -> circuitBreaker.execute(() -> "test"))
                .isInstanceOf(CircuitBreakerOpenException.class)
                .hasMessageContaining("OPEN");
    }

    @Test
    @DisplayName("OPEN 타임아웃 후 HALF_OPEN으로 전환")
    void transitionsToHalfOpenAfterTimeout() throws InterruptedException {
        // given - OPEN 상태로 전환
        circuitBreaker.forceOpen();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreakerState.OPEN);

        // when - 타임아웃 대기
        Thread.sleep(600);

        // then - allowRequest 호출시 HALF_OPEN으로 전환
        assertThat(circuitBreaker.allowRequest()).isTrue();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreakerState.HALF_OPEN);
    }

    @Test
    @DisplayName("HALF_OPEN에서 성공 임계값 도달시 CLOSED로 전환")
    void transitionsToClosedAfterSuccessThreshold() throws InterruptedException {
        // given - 더 많은 허용 호출 수로 설정
        CircuitBreakerConfig config = CircuitBreakerConfig.builder()
                .failureThreshold(3)
                .successThreshold(2)
                .openTimeout(500)
                .halfOpenPermittedCalls(5) // 충분히 많은 호출 허용
                .build();
        CircuitBreaker cb = new CircuitBreaker("test-half-open", config);

        // HALF_OPEN 상태로 전환
        cb.forceOpen();
        Thread.sleep(600);
        cb.allowRequest(); // HALF_OPEN으로 전환

        // when - 2번 성공
        cb.execute(() -> "success1");
        cb.execute(() -> "success2");

        // then
        assertThat(cb.getState()).isEqualTo(CircuitBreakerState.CLOSED);
    }

    @Test
    @DisplayName("HALF_OPEN에서 실패시 OPEN으로 전환")
    void transitionsToOpenOnFailureInHalfOpen() throws InterruptedException {
        // given - HALF_OPEN 상태로 전환
        circuitBreaker.forceOpen();
        Thread.sleep(600);
        circuitBreaker.allowRequest();

        // when - 실패 발생
        try {
            circuitBreaker.execute(() -> {
                throw new RuntimeException("error");
            });
        } catch (RuntimeException e) {
            // expected
        }

        // then
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreakerState.OPEN);
    }

    @Test
    @DisplayName("reset() 호출시 CLOSED로 초기화")
    void resetResetsToClose() {
        // given - OPEN 상태
        circuitBreaker.forceOpen();

        // when
        circuitBreaker.reset();

        // then
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreakerState.CLOSED);
        assertThat(circuitBreaker.getFailureCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("비활성화시 항상 요청 허용")
    void allowsAllRequestsWhenDisabled() {
        // given
        CircuitBreaker disabledCb = new CircuitBreaker("disabled",
                CircuitBreakerConfig.disabled());

        // when - 많은 실패
        for (int i = 0; i < 10; i++) {
            try {
                disabledCb.execute(() -> {
                    throw new RuntimeException("error");
                });
            } catch (RuntimeException e) {
                // expected
            }
        }

        // then - 여전히 요청 허용
        assertThat(disabledCb.allowRequest()).isTrue();
    }

    @Test
    @DisplayName("통계 정보 확인")
    void statsAreTracked() {
        // when
        circuitBreaker.execute(() -> "success1");
        circuitBreaker.execute(() -> "success2");
        try {
            circuitBreaker.execute(() -> {
                throw new RuntimeException("error");
            });
        } catch (RuntimeException e) {
            // expected
        }

        // then
        CircuitBreaker.Stats stats = circuitBreaker.getStats();
        assertThat(stats.totalCalls()).isEqualTo(3);
        assertThat(stats.totalSuccess()).isEqualTo(2);
        assertThat(stats.totalFailures()).isEqualTo(1);
        assertThat(stats.getFailureRate()).isCloseTo(0.333, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    @DisplayName("Runnable 실행 지원")
    void supportsRunnableExecution() {
        // given
        AtomicInteger counter = new AtomicInteger(0);

        // when
        circuitBreaker.execute(counter::incrementAndGet);

        // then
        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("HALF_OPEN에서 허용된 호출 수 제한")
    void limitsCallsInHalfOpen() throws InterruptedException {
        // given - HALF_OPEN으로 전환 (허용 호출 수: 2)
        circuitBreaker.forceOpen();
        Thread.sleep(600);

        // when - 3번 호출 시도
        boolean call1 = circuitBreaker.allowRequest();
        boolean call2 = circuitBreaker.allowRequest();
        boolean call3 = circuitBreaker.allowRequest();

        // then
        assertThat(call1).isTrue();
        assertThat(call2).isTrue();
        assertThat(call3).isFalse(); // 3번째는 거부
    }

    @Test
    @DisplayName("성공 후 실패 카운트 리셋")
    void resetsFailureCountOnSuccess() {
        // given - 2번 실패
        for (int i = 0; i < 2; i++) {
            try {
                circuitBreaker.execute(() -> {
                    throw new RuntimeException("error");
                });
            } catch (RuntimeException e) {
                // expected
            }
        }
        assertThat(circuitBreaker.getFailureCount()).isEqualTo(2);

        // when - 성공
        circuitBreaker.execute(() -> "success");

        // then - 실패 카운트 리셋
        assertThat(circuitBreaker.getFailureCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("기본 설정값 확인")
    void defaultConfigValues() {
        CircuitBreakerConfig config = CircuitBreakerConfig.defaultConfig();

        assertThat(config.isEnabled()).isTrue();
        assertThat(config.getFailureThreshold()).isEqualTo(5);
        assertThat(config.getSuccessThreshold()).isEqualTo(3);
        assertThat(config.getOpenTimeout()).isEqualTo(30000);
        assertThat(config.getHalfOpenPermittedCalls()).isEqualTo(3);
    }
}
