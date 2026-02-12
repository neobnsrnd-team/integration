package demo.mci.common;

import lombok.extern.slf4j.Slf4j;
import springware.mci.common.layout.DefaultLayoutManager;
import springware.mci.common.layout.LayoutManager;
import springware.mci.common.layout.MessageLayout;
import springware.mci.common.layout.YamlLayoutLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * 데모용 레이아웃 등록
 * YAML 파일에서 레이아웃을 로드하여 등록
 */
@Slf4j
public class DemoLayoutRegistry {

    private static final String LAYOUTS_PATH = "/layouts";
    private static final List<String> LAYOUT_FILES = Arrays.asList(
            "HEADER.yaml",
            // Banking layouts
            "BAL1.yaml", "BAL2.yaml",
            "TRF1.yaml", "TRF2.yaml",
            "TXH1.yaml", "TXH2.yaml",
            "ACT1.yaml", "ACT2.yaml",
            "ECH1.yaml", "ECH2.yaml",
            "HBT1.yaml", "HBT2.yaml",
            // Card layouts
            "CRD1.yaml", "CRD2.yaml",
            "CUH1.yaml", "CUH2.yaml"
    );

    private final LayoutManager layoutManager;
    private final YamlLayoutLoader yamlLoader;

    public DemoLayoutRegistry() {
        this.layoutManager = new DefaultLayoutManager();
        this.yamlLoader = new YamlLayoutLoader();
        registerLayouts();
    }

    public DemoLayoutRegistry(LayoutManager layoutManager) {
        this.layoutManager = layoutManager;
        this.yamlLoader = new YamlLayoutLoader();
        registerLayouts();
    }

    /**
     * 모든 데모 레이아웃 등록
     */
    public void registerLayouts() {
        int count = 0;
        for (String fileName : LAYOUT_FILES) {
            String resourcePath = LAYOUTS_PATH + "/" + fileName;
            try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
                if (is == null) {
                    log.warn("Layout file not found: {}", resourcePath);
                    continue;
                }
                MessageLayout layout = yamlLoader.load(is);
                layoutManager.registerLayout(layout);
                count++;
            } catch (IOException e) {
                log.error("Failed to load layout: {}", resourcePath, e);
            }
        }
        log.info("Registered {} layouts from YAML files", count);
    }

    /**
     * 레이아웃 매니저 반환
     */
    public LayoutManager getLayoutManager() {
        return layoutManager;
    }
}
