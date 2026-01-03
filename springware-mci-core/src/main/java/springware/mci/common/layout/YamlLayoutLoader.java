package springware.mci.common.layout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import springware.mci.common.exception.LayoutException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * YAML 파일에서 레이아웃을 로드하는 클래스
 */
@Slf4j
public class YamlLayoutLoader {

    private final ObjectMapper yamlMapper;

    public YamlLayoutLoader() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    /**
     * 파일에서 레이아웃 로드
     *
     * @param filePath YAML 파일 경로
     * @return 로드된 레이아웃
     */
    public MessageLayout load(Path filePath) {
        try (InputStream is = Files.newInputStream(filePath)) {
            return load(is);
        } catch (IOException e) {
            throw new LayoutException("Failed to load layout from file: " + filePath, e);
        }
    }

    /**
     * 입력 스트림에서 레이아웃 로드
     *
     * @param inputStream YAML 입력 스트림
     * @return 로드된 레이아웃
     */
    public MessageLayout load(InputStream inputStream) {
        try {
            LayoutConfig config = yamlMapper.readValue(inputStream, LayoutConfig.class);
            MessageLayout layout = config.toMessageLayout();
            log.info("Loaded layout: {} (fields: {}, length: {})",
                    layout.getLayoutId(), layout.getFields().size(), layout.getTotalLength());
            return layout;
        } catch (IOException e) {
            throw new LayoutException("Failed to parse layout YAML", e);
        }
    }

    /**
     * 클래스패스 리소스에서 레이아웃 로드
     *
     * @param resourcePath 리소스 경로 (예: "/layouts/MSG001.yaml")
     * @return 로드된 레이아웃
     */
    public MessageLayout loadFromClasspath(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new LayoutException("Resource not found: " + resourcePath);
            }
            return load(is);
        } catch (IOException e) {
            throw new LayoutException("Failed to load layout from classpath: " + resourcePath, e);
        }
    }

    /**
     * 디렉토리에서 모든 레이아웃 로드
     *
     * @param directory 레이아웃 파일 디렉토리
     * @return 로드된 레이아웃 목록
     */
    public List<MessageLayout> loadAll(Path directory) {
        List<MessageLayout> layouts = new ArrayList<>();

        try {
            Files.walk(directory, 1)
                    .filter(path -> path.toString().endsWith(".yaml") || path.toString().endsWith(".yml"))
                    .forEach(path -> {
                        try {
                            layouts.add(load(path));
                        } catch (Exception e) {
                            log.warn("Failed to load layout: {}", path, e);
                        }
                    });
        } catch (IOException e) {
            throw new LayoutException("Failed to read layout directory: " + directory, e);
        }

        log.info("Loaded {} layouts from directory: {}", layouts.size(), directory);
        return layouts;
    }

    /**
     * 레이아웃 매니저에 로드 및 등록
     *
     * @param directory     레이아웃 파일 디렉토리
     * @param layoutManager 레이아웃 매니저
     * @return 등록된 레이아웃 수
     */
    public int loadAndRegister(Path directory, LayoutManager layoutManager) {
        List<MessageLayout> layouts = loadAll(directory);
        for (MessageLayout layout : layouts) {
            layoutManager.registerLayout(layout);
        }
        return layouts.size();
    }
}
