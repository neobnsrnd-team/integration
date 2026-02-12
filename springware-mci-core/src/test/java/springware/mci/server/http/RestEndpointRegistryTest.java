package springware.mci.server.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RestEndpointRegistry 테스트")
class RestEndpointRegistryTest {

    private RestEndpointRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new RestEndpointRegistry();
    }

    // ==================== Default Endpoints Tests ====================

    @Test
    @DisplayName("기본 엔드포인트 등록 확인")
    void defaultEndpoints() {
        // then - 기본 엔드포인트가 등록되어 있어야 함
        assertThat(registry.getMessageCode("/api/balance")).isEqualTo("BAL1");
        assertThat(registry.getMessageCode("/api/transfer")).isEqualTo("TRF1");
        assertThat(registry.getMessageCode("/api/transactions")).isEqualTo("TXH1");
        assertThat(registry.getMessageCode("/api/account")).isEqualTo("ACT1");
        assertThat(registry.getMessageCode("/api/echo")).isEqualTo("ECH1");
        assertThat(registry.getMessageCode("/api/heartbeat")).isEqualTo("HBT1");
    }

    @Test
    @DisplayName("기본 엔드포인트 역방향 조회")
    void defaultEndpoints_reverse() {
        // then
        assertThat(registry.getPath("BAL1")).isEqualTo("/api/balance");
        assertThat(registry.getPath("TRF1")).isEqualTo("/api/transfer");
        assertThat(registry.getPath("ECH1")).isEqualTo("/api/echo");
    }

    // ==================== Register Tests ====================

    @Test
    @DisplayName("새 엔드포인트 등록")
    void register() {
        // when
        registry.register("/api/custom", "CST1");

        // then
        assertThat(registry.getMessageCode("/api/custom")).isEqualTo("CST1");
        assertThat(registry.getPath("CST1")).isEqualTo("/api/custom");
    }

    @Test
    @DisplayName("엔드포인트 일괄 등록")
    void registerAll() {
        // given
        Map<String, String> mappings = Map.of(
                "/api/new1", "NEW1",
                "/api/new2", "NEW2"
        );

        // when
        registry.registerAll(mappings);

        // then
        assertThat(registry.getMessageCode("/api/new1")).isEqualTo("NEW1");
        assertThat(registry.getMessageCode("/api/new2")).isEqualTo("NEW2");
    }

    @Test
    @DisplayName("기존 엔드포인트 덮어쓰기")
    void register_overwrite() {
        // when
        registry.register("/api/balance", "NEWBAL");

        // then
        assertThat(registry.getMessageCode("/api/balance")).isEqualTo("NEWBAL");
    }

    // ==================== Get Tests ====================

    @Test
    @DisplayName("등록되지 않은 경로 조회시 null 반환")
    void getMessageCode_notFound() {
        // when
        String result = registry.getMessageCode("/api/unknown");

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("등록되지 않은 메시지 코드 조회시 null 반환")
    void getPath_notFound() {
        // when
        String result = registry.getPath("UNKNOWN");

        // then
        assertThat(result).isNull();
    }

    // ==================== Has Tests ====================

    @Test
    @DisplayName("경로 존재 여부 확인")
    void hasPath() {
        // then
        assertThat(registry.hasPath("/api/balance")).isTrue();
        assertThat(registry.hasPath("/api/unknown")).isFalse();
    }

    @Test
    @DisplayName("메시지 코드 존재 여부 확인")
    void hasMessageCode() {
        // then
        assertThat(registry.hasMessageCode("BAL1")).isTrue();
        assertThat(registry.hasMessageCode("UNKNOWN")).isFalse();
    }

    // ==================== Unregister Tests ====================

    @Test
    @DisplayName("경로로 엔드포인트 제거")
    void unregister() {
        // when
        registry.unregister("/api/balance");

        // then
        assertThat(registry.getMessageCode("/api/balance")).isNull();
        assertThat(registry.getPath("BAL1")).isNull();
    }

    @Test
    @DisplayName("메시지 코드로 엔드포인트 제거")
    void unregisterByMessageCode() {
        // when
        registry.unregisterByMessageCode("TRF1");

        // then
        assertThat(registry.getMessageCode("/api/transfer")).isNull();
        assertThat(registry.getPath("TRF1")).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 경로 제거 - 에러 없음")
    void unregister_notExists() {
        // when/then - 예외 발생하지 않음
        registry.unregister("/api/unknown");
    }

    // ==================== Clear Tests ====================

    @Test
    @DisplayName("모든 엔드포인트 제거")
    void clear() {
        // when
        registry.clear();

        // then
        assertThat(registry.size()).isEqualTo(0);
        assertThat(registry.getMessageCode("/api/balance")).isNull();
    }

    // ==================== Size Tests ====================

    @Test
    @DisplayName("등록된 엔드포인트 수 확인")
    void size() {
        // then - 기본 6개 등록됨
        assertThat(registry.size()).isEqualTo(6);

        // when
        registry.register("/api/new", "NEW1");

        // then
        assertThat(registry.size()).isEqualTo(7);
    }

    // ==================== GetAllMappings Tests ====================

    @Test
    @DisplayName("모든 매핑 조회")
    void getAllMappings() {
        // when
        Map<String, String> mappings = registry.getAllMappings();

        // then
        assertThat(mappings).hasSize(6);
        assertThat(mappings).containsEntry("/api/balance", "BAL1");
        assertThat(mappings).containsEntry("/api/transfer", "TRF1");
    }

    @Test
    @DisplayName("반환된 매핑은 불변")
    void getAllMappings_immutable() {
        // when
        Map<String, String> mappings = registry.getAllMappings();

        // then - UnsupportedOperationException 발생해야 함
        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> mappings.put("/api/new", "NEW1")
        );
    }

    // ==================== Path Normalization Tests ====================

    @Test
    @DisplayName("경로 정규화 - 후행 슬래시 제거")
    void normalizePath_trailingSlash() {
        // given
        registry.register("/api/test/", "TEST");

        // then
        assertThat(registry.getMessageCode("/api/test")).isEqualTo("TEST");
        assertThat(registry.getMessageCode("/api/test/")).isEqualTo("TEST");
    }

    @Test
    @DisplayName("경로 정규화 - 공백 제거")
    void normalizePath_whitespace() {
        // given
        registry.register("  /api/test  ", "TEST");

        // then
        assertThat(registry.getMessageCode("/api/test")).isEqualTo("TEST");
    }

    // ==================== ExtractPath Tests ====================

    @Test
    @DisplayName("URI에서 경로 추출 - 쿼리 스트링 포함")
    void extractPath_withQueryString() {
        // when
        String path = RestEndpointRegistry.extractPath("/api/balance?accountNo=123");

        // then
        assertThat(path).isEqualTo("/api/balance");
    }

    @Test
    @DisplayName("URI에서 경로 추출 - 쿼리 스트링 없음")
    void extractPath_noQueryString() {
        // when
        String path = RestEndpointRegistry.extractPath("/api/balance");

        // then
        assertThat(path).isEqualTo("/api/balance");
    }

    @Test
    @DisplayName("URI에서 경로 추출 - null 입력")
    void extractPath_null() {
        // when
        String path = RestEndpointRegistry.extractPath(null);

        // then
        assertThat(path).isEqualTo("/");
    }

    @Test
    @DisplayName("URI에서 경로 추출 - 복잡한 쿼리 스트링")
    void extractPath_complexQueryString() {
        // when
        String path = RestEndpointRegistry.extractPath("/api/search?q=test&page=1&size=10");

        // then
        assertThat(path).isEqualTo("/api/search");
    }
}
