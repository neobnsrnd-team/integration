package springware.mci.client.tcp;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import springware.mci.client.config.ClientConfig;
import springware.mci.client.core.AbstractMciClient;
import springware.mci.common.core.Message;
import springware.mci.common.exception.ConnectionException;
import springware.mci.common.layout.LayoutManager;
import springware.mci.common.layout.MessageLayout;
import springware.mci.common.logging.MessageLogger;
import springware.mci.common.protocol.LengthFieldType;
import springware.mci.common.protocol.ProtocolConfig;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Netty 기반 TCP 클라이언트
 */
@Slf4j
public class TcpClient extends AbstractMciClient {

    private EventLoopGroup workerGroup;
    private Channel channel;
    private final Map<String, CompletableFuture<Message>> pendingRequests = new ConcurrentHashMap<>();

    public TcpClient(ClientConfig config) {
        super(config);
    }

    public TcpClient(ClientConfig config, LayoutManager layoutManager, MessageLogger messageLogger) {
        super(config, layoutManager, messageLogger);
    }

    @Override
    protected void doConnect() {
        workerGroup = new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getConnectTimeout())
                    .option(ChannelOption.SO_KEEPALIVE, config.isKeepAlive())
                    .option(ChannelOption.TCP_NODELAY, config.isTcpNoDelay())
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();

                            // 유휴 상태 핸들러
                            pipeline.addLast("idle", new IdleStateHandler(
                                    config.getReadTimeout(),
                                    config.getWriteTimeout(),
                                    config.getIdleTimeout(),
                                    TimeUnit.MILLISECONDS));

                            // 프레임 디코더/인코더 추가
                            addFrameCodec(pipeline);

                            // 비즈니스 로직 핸들러
                            pipeline.addLast("handler", new TcpClientHandler());
                        }
                    });

            ChannelFuture future = bootstrap.connect(config.getHost(), config.getPort()).sync();
            channel = future.channel();

            log.debug("TCP connection established to {}:{}", config.getHost(), config.getPort());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConnectionException("Connection interrupted", e);
        } catch (Exception e) {
            shutdown();
            throw new ConnectionException("Failed to connect", e);
        }
    }

    /**
     * 프레임 코덱 추가
     *
     * ● 코드 설명
     *
     *   이 코드는 Netty 기반 TCP 클라이언트의 프레임 코덱 설정 메서드입니다.
     *
     *   목적
     *
     *   TCP는 스트림 프로토콜이므로 메시지 경계를 구분하기 위해 프레임 처리가 필요합니다.
     *
     *   동작 흐름
     *
     *   ┌─────────────────────────────────────────────────────────┐
     *   │  LengthFieldType 확인                                    │
     *   ├─────────────────────────────────────────────────────────┤
     *   │  NONE           → 리턴 (고정 길이 메시지, 코덱 불필요)      │
     *   │  BINARY_BIG_ENDIAN → 디코더 + 인코더 추가                 │
     *   │  기타 (문자열 등)  → 디코더만 추가 (인코더는 커스텀 필요)    │
     *   └─────────────────────────────────────────────────────────┘
     *
     *   주요 컴포넌트
     *
     *   | 컴포넌트                     | 역할                                                |
     *   |------------------------------|-----------------------------------------------------|
     *   | LengthFieldBasedFrameDecoder | 수신 데이터에서 길이 필드를 읽어 메시지 프레임 분리 |
     *   | LengthFieldPrepender         | 송신 메시지 앞에 길이 필드 자동 추가                |
     *
     *   프레임 구조 예시
     *
     *   +--------+----------------+
     *   | Length |    Payload     |
     *   | (2~4B) |   (가변 길이)   |
     *   +--------+----------------+
     *
     *   디코더 파라미터 의미
     *
     *   new LengthFieldBasedFrameDecoder(
     *       maxMessageSize,      // 최대 메시지 크기
     *       lengthFieldOffset,   // 길이 필드 시작 위치
     *       lengthFieldLength,   // 길이 필드 바이트 수 (2 or 4)
     *       lengthAdjustment,    // 길이 값 보정 (헤더 포함 여부 등)
     *       initialBytesToStrip  // 디코딩 후 제거할 바이트 수
     *   )
     *
     *   코드 개선 포인트
     *
     *   주석에 언급된 대로, LengthFieldType이 숫자 문자열 타입인 경우(예: "0042") 커스텀 인코더가 필요하지만 현재 구현되어 있지 않습니다.
     */
    private void addFrameCodec(ChannelPipeline pipeline) {
        ProtocolConfig protocolConfig = config.getProtocolConfig();

        if (protocolConfig.getLengthFieldType() == LengthFieldType.NONE) {
            // 길이 필드 없는 경우 (고정 길이 메시지), codec설치가 필요 없으니 빠져 나간다.
            return;
        }

        // 길이 필드 기반 프레임 디코더
        pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(
                protocolConfig.getMaxMessageSize(),
                protocolConfig.getLengthFieldOffset(),
                protocolConfig.getLengthFieldLength(),
                protocolConfig.getLengthAdjustment(),
                protocolConfig.getInitialBytesToStrip()
        ));

        // 길이 필드 프리펜더 (숫자 문자열 타입인 경우 커스텀 처리 필요)
        if (protocolConfig.getLengthFieldType() == LengthFieldType.BINARY_BIG_ENDIAN) {
            pipeline.addLast("frameEncoder", new LengthFieldPrepender(
                    protocolConfig.getLengthFieldLength()
            ));
        }
    }

    @Override
    protected void doDisconnect() {
        // 대기 중인 요청 취소
        pendingRequests.values().forEach(future ->
                future.completeExceptionally(new ConnectionException("Client disconnected")));
        pendingRequests.clear();

        if (channel != null) {
            try {
                channel.close().sync();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            channel = null;
        }

        shutdown();
    }

    private void shutdown() {
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            workerGroup = null;
        }
    }

    @Override
    public CompletableFuture<Message> sendAsync(Message message) {
        ensureConnected();

        CompletableFuture<Message> future = new CompletableFuture<>();
        pendingRequests.put(message.getMessageId(), future);

        try {
            // 메시지 인코딩
            MessageLayout layout = layoutManager.getLayout(message.getMessageCode());
            byte[] data = layoutManager.encode(message, config.getCharset());

            // 프로토콜 설정에 따른 길이 필드 추가
            byte[] frameData = prependLengthField(data);

            // 전송
            ByteBuf buf = Unpooled.wrappedBuffer(frameData);
            channel.writeAndFlush(buf).addListener((ChannelFutureListener) f -> {
                if (f.isSuccess()) {
                    // 클라이언트: writeAndFlush 완료 후 로깅
                    messageLogger.logSend(message, layout, frameData);
                    log.debug("Message sent: {}", message.getMessageId());
                } else {
                    pendingRequests.remove(message.getMessageId());
                    future.completeExceptionally(new ConnectionException("Failed to send message", f.cause()));
                }
            });

        } catch (Exception e) {
            pendingRequests.remove(message.getMessageId());
            future.completeExceptionally(e);
        }

        return future;
    }

    @Override
    protected void doSendOneWay(Message message) {
        try {
            MessageLayout layout = layoutManager.getLayout(message.getMessageCode());
            byte[] data = layoutManager.encode(message, config.getCharset());
            byte[] frameData = prependLengthField(data);

            ByteBuf buf = Unpooled.wrappedBuffer(frameData);
            channel.writeAndFlush(buf).addListener((ChannelFutureListener) f -> {
                if (f.isSuccess()) {
                    messageLogger.logSend(message, layout, frameData);
                }
            });

        } catch (Exception e) {
            throw new ConnectionException("Failed to send message", e);
        }
    }

    /**
     * 길이 필드 추가
     * BINARY_BIG_ENDIAN인 경우 LengthFieldPrepender가 파이프라인에서 자동으로 처리하므로 수동 추가하지 않음
     */
    private byte[] prependLengthField(byte[] data) {
        ProtocolConfig protocolConfig = config.getProtocolConfig();

        // NONE이거나 BINARY_BIG_ENDIAN인 경우 (LengthFieldPrepender가 처리)
        if (protocolConfig.getLengthFieldType() == LengthFieldType.NONE ||
            protocolConfig.getLengthFieldType() == LengthFieldType.BINARY_BIG_ENDIAN) {
            return data;
        }

        int lengthFieldLength = protocolConfig.getLengthFieldLength();
        int bodyLength = data.length;

        if (protocolConfig.isLengthIncludesHeader()) {
            bodyLength += lengthFieldLength;
        }
        bodyLength += protocolConfig.getLengthAdjustment();

        byte[] result = new byte[lengthFieldLength + data.length];

        // 길이 필드 인코딩
        switch (protocolConfig.getLengthFieldType()) {
            case NUMERIC_STRING:
                String lengthStr = String.format("%0" + lengthFieldLength + "d", bodyLength);
                System.arraycopy(lengthStr.getBytes(config.getCharset()), 0, result, 0, lengthFieldLength);
                break;
            case BINARY_LITTLE_ENDIAN:
                for (int i = 0; i < lengthFieldLength; i++) {
                    result[i] = (byte) (bodyLength & 0xFF);
                    bodyLength >>= 8;
                }
                break;
            default:
                break;
        }

        System.arraycopy(data, 0, result, lengthFieldLength, data.length);
        return result;
    }

    /**
     * TCP 클라이언트 핸들러
     */
    private class TcpClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
            byte[] data = new byte[msg.readableBytes()];
            msg.readBytes(data);

            try {
                // 메시지 디코딩
                ProtocolConfig protocolConfig = config.getProtocolConfig();
                // initialBytesToStrip으로 이미 제거된 바이트를 고려하여 오프셋 계산
                int offset = protocolConfig.getLengthFieldOffset() + protocolConfig.getLengthFieldLength()
                        - protocolConfig.getInitialBytesToStrip();
                offset = Math.max(0, offset);

                byte[] bodyData;
                if (offset > 0 && offset < data.length) {
                    bodyData = new byte[data.length - offset];
                    System.arraycopy(data, offset, bodyData, 0, bodyData.length);
                } else {
                    bodyData = data;
                }

                // 메시지 코드 추출 (처음 4바이트)
                String messageCode = new String(bodyData, 0, Math.min(4, bodyData.length), config.getCharset()).trim();

                MessageLayout layout = layoutManager.getLayout(messageCode);
                Message response = layout != null
                        ? layout.decode(bodyData, config.getCharset())
                        : Message.builder().messageCode(messageCode).build();

                response.setRawData(data);

                // 로깅
                messageLogger.logReceive(response, layout, data);

                // 대기 중인 요청에 응답 전달
                // 실제로는 요청-응답 매핑 로직 필요 (메시지 ID 또는 시퀀스 번호 기반)
                // 여기서는 첫 번째 대기 중인 요청에 응답
                if (!pendingRequests.isEmpty()) {
                    String firstKey = pendingRequests.keySet().iterator().next();
                    CompletableFuture<Message> future = pendingRequests.remove(firstKey);
                    if (future != null) {
                        future.complete(response);
                    }
                }

            } catch (Exception e) {
                log.error("Failed to process received message", e);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error("Channel exception", cause);

            // 모든 대기 중인 요청 실패 처리
            pendingRequests.values().forEach(future ->
                    future.completeExceptionally(new ConnectionException("Channel error", cause)));
            pendingRequests.clear();

            ctx.close();
            tryReconnect();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            log.warn("Channel disconnected");
            tryReconnect();
        }
    }
}
