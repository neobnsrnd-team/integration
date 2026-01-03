package springware.mci.server.core;

import io.netty.channel.Channel;
import lombok.Builder;
import lombok.Getter;
import springware.mci.common.core.TransportType;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 메시지 처리 컨텍스트
 */
@Getter
@Builder
public class MessageContext {

    /**
     * 채널 (Netty)
     */
    private final Channel channel;

    /**
     * 전송 타입
     */
    private final TransportType transportType;

    /**
     * 원격 주소
     */
    private final InetSocketAddress remoteAddress;

    /**
     * 메시지 수신 시각
     */
    @Builder.Default
    private final LocalDateTime receivedAt = LocalDateTime.now();

    /**
     * 컨텍스트 속성
     */
    @Builder.Default
    private final Map<String, Object> attributes = new HashMap<>();

    /**
     * 원격 IP 주소
     */
    public String getRemoteIp() {
        return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : null;
    }

    /**
     * 원격 포트
     */
    public int getRemotePort() {
        return remoteAddress != null ? remoteAddress.getPort() : -1;
    }

    /**
     * 속성 설정
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    /**
     * 속성 조회
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }

    /**
     * 속성 조회 (기본값 포함)
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key, T defaultValue) {
        Object value = attributes.get(key);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * 채널에 데이터 전송
     */
    public void write(Object data) {
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(data);
        }
    }

    /**
     * 채널 활성 여부
     */
    public boolean isChannelActive() {
        return channel != null && channel.isActive();
    }
}
