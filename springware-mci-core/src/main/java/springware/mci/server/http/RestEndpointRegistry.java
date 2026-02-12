package springware.mci.server.http;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REST 엔드포인트 레지스트리
 * 경로(Path) <-> 메시지 코드(Message Code) 양방향 매핑 관리
 */
@Slf4j
public class RestEndpointRegistry {

    /**
     * 경로 -> 메시지 코드 매핑
     */
    private final Map<String, String> pathToMessageCode = new ConcurrentHashMap<>();

    /**
     * 메시지 코드 -> 경로 매핑
     */
    private final Map<String, String> messageCodeToPath = new ConcurrentHashMap<>();

    /**
     * 빈 레지스트리 생성
     */
    public RestEndpointRegistry() {
        // 엔드포인트는 애플리케이션에서 등록
    }

    /**
     * 엔드포인트 등록
     *
     * @param path 경로 (예: /api/balance)
     * @param messageCode 메시지 코드 (예: BAL1)
     */
    public void register(String path, String messageCode) {
        String normalizedPath = normalizePath(path);
        pathToMessageCode.put(normalizedPath, messageCode);
        messageCodeToPath.put(messageCode, normalizedPath);
        log.debug("Registered endpoint: {} <-> {}", normalizedPath, messageCode);
    }

    /**
     * 엔드포인트 일괄 등록
     *
     * @param mappings 경로 -> 메시지 코드 매핑
     */
    public void registerAll(Map<String, String> mappings) {
        mappings.forEach(this::register);
    }

    /**
     * 경로로 메시지 코드 조회
     *
     * @param path 경로
     * @return 메시지 코드 (없으면 null)
     */
    public String getMessageCode(String path) {
        return pathToMessageCode.get(normalizePath(path));
    }

    /**
     * 메시지 코드로 경로 조회
     *
     * @param messageCode 메시지 코드
     * @return 경로 (없으면 null)
     */
    public String getPath(String messageCode) {
        return messageCodeToPath.get(messageCode);
    }

    /**
     * 경로 존재 여부 확인
     *
     * @param path 경로
     * @return 존재 여부
     */
    public boolean hasPath(String path) {
        return pathToMessageCode.containsKey(normalizePath(path));
    }

    /**
     * 메시지 코드 존재 여부 확인
     *
     * @param messageCode 메시지 코드
     * @return 존재 여부
     */
    public boolean hasMessageCode(String messageCode) {
        return messageCodeToPath.containsKey(messageCode);
    }

    /**
     * 엔드포인트 제거 (경로 기준)
     *
     * @param path 경로
     */
    public void unregister(String path) {
        String normalizedPath = normalizePath(path);
        String messageCode = pathToMessageCode.remove(normalizedPath);
        if (messageCode != null) {
            messageCodeToPath.remove(messageCode);
            log.debug("Unregistered endpoint: {} <-> {}", normalizedPath, messageCode);
        }
    }

    /**
     * 엔드포인트 제거 (메시지 코드 기준)
     *
     * @param messageCode 메시지 코드
     */
    public void unregisterByMessageCode(String messageCode) {
        String path = messageCodeToPath.remove(messageCode);
        if (path != null) {
            pathToMessageCode.remove(path);
            log.debug("Unregistered endpoint: {} <-> {}", path, messageCode);
        }
    }

    /**
     * 모든 엔드포인트 제거
     */
    public void clear() {
        pathToMessageCode.clear();
        messageCodeToPath.clear();
    }

    /**
     * 등록된 엔드포인트 수 반환
     *
     * @return 엔드포인트 수
     */
    public int size() {
        return pathToMessageCode.size();
    }

    /**
     * 모든 경로 -> 메시지 코드 매핑 조회
     *
     * @return 매핑 맵 (읽기 전용 복사본)
     */
    public Map<String, String> getAllMappings() {
        return Map.copyOf(pathToMessageCode);
    }

    /**
     * 경로 정규화
     * - 앞뒤 공백 제거
     * - 후행 슬래시 제거
     * - 소문자 변환 (선택적)
     */
    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }

        String normalized = path.trim();

        // 후행 슬래시 제거 (루트 경로 제외)
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }

    /**
     * 쿼리 스트링 제외한 경로 추출
     *
     * @param uri URI (경로 + 쿼리 스트링)
     * @return 경로만
     */
    public static String extractPath(String uri) {
        if (uri == null) {
            return "/";
        }

        int queryStart = uri.indexOf('?');
        if (queryStart > 0) {
            return uri.substring(0, queryStart);
        }

        return uri;
    }
}
