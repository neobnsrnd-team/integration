package springware.mci.common.layout;

import lombok.extern.slf4j.Slf4j;
import springware.mci.common.core.Message;
import springware.mci.common.exception.LayoutException;

import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 기본 레이아웃 관리자 구현
 */
@Slf4j
public class DefaultLayoutManager implements LayoutManager {

    private final ConcurrentHashMap<String, MessageLayout> layouts = new ConcurrentHashMap<>();

    @Override
    public void registerLayout(MessageLayout layout) {
        if (layout == null) {
            throw new IllegalArgumentException("Layout must not be null");
        }
        layouts.put(layout.getLayoutId(), layout);
        log.debug("Registered layout: {} (fields: {}, length: {})",
                layout.getLayoutId(), layout.getFields().size(), layout.getTotalLength());
    }

    @Override
    public MessageLayout getLayout(String layoutId) {
        return layouts.get(layoutId);
    }

    @Override
    public boolean hasLayout(String layoutId) {
        return layouts.containsKey(layoutId);
    }

    @Override
    public void removeLayout(String layoutId) {
        layouts.remove(layoutId);
        log.debug("Removed layout: {}", layoutId);
    }

    @Override
    public void clear() {
        layouts.clear();
        log.debug("Cleared all layouts");
    }

    @Override
    public int size() {
        return layouts.size();
    }

    @Override
    public byte[] encode(Message message, Charset charset) {
        String layoutId = message.getMessageCode();
        MessageLayout layout = getLayout(layoutId);
        if (layout == null) {
            throw new LayoutException("Layout not found: " + layoutId);
        }
        return layout.encode(message, charset);
    }

    @Override
    public Message decode(String layoutId, byte[] data, Charset charset) {
        MessageLayout layout = getLayout(layoutId);
        if (layout == null) {
            throw new LayoutException("Layout not found: " + layoutId);
        }
        return layout.decode(data, charset);
    }
}
