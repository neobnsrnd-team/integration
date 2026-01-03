package springware.mci.server.tcp;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import springware.mci.common.core.Message;
import springware.mci.common.core.TransportType;
import springware.mci.common.layout.LayoutManager;
import springware.mci.common.layout.MessageLayout;
import springware.mci.common.logging.MessageLogger;
import springware.mci.common.protocol.LengthFieldType;
import springware.mci.common.protocol.ProtocolConfig;
import springware.mci.server.config.ServerConfig;
import springware.mci.server.core.AbstractMciServer;
import springware.mci.server.core.MessageContext;
import springware.mci.server.core.MessageHandler;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * Netty 기반 TCP 서버
 */
@Slf4j
public class TcpServer extends AbstractMciServer {

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public TcpServer(ServerConfig config) {
        super(config);
    }

    public TcpServer(ServerConfig config, LayoutManager layoutManager, MessageLogger messageLogger) {
        super(config, layoutManager, messageLogger);
    }

    @Override
    protected void doStart() {
        int bossThreads = config.getBossThreads();
        int workerThreads = config.getWorkerThreads();

        bossGroup = new NioEventLoopGroup(bossThreads);
        workerGroup = workerThreads > 0
                ? new NioEventLoopGroup(workerThreads)
                : new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, config.getBacklog())
                    .option(ChannelOption.SO_REUSEADDR, config.isReuseAddress())
                    .childOption(ChannelOption.SO_KEEPALIVE, config.isKeepAlive())
                    .childOption(ChannelOption.TCP_NODELAY, config.isTcpNoDelay())
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();

                            // 유휴 상태 핸들러
                            pipeline.addLast("idle", new IdleStateHandler(
                                    config.getReadTimeout(),
                                    config.getWriteTimeout(),
                                    config.getIdleTimeout(),
                                    TimeUnit.MILLISECONDS));

                            // 프레임 디코더 추가
                            addFrameDecoder(pipeline);

                            // 비즈니스 로직 핸들러
                            pipeline.addLast("handler", new TcpServerHandler());
                        }
                    });

            ChannelFuture future = bootstrap.bind(config.getHost(), config.getPort()).sync();
            serverChannel = future.channel();

            log.info("TCP server started on {}:{}", config.getHost(), config.getPort());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Server startup interrupted", e);
        }
    }

    /**
     * 프레임 디코더 추가
     */
    private void addFrameDecoder(ChannelPipeline pipeline) {
        ProtocolConfig protocolConfig = config.getProtocolConfig();

        if (protocolConfig.getLengthFieldType() == LengthFieldType.NONE) {
            return;
        }

        pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(
                protocolConfig.getMaxMessageSize(),
                protocolConfig.getLengthFieldOffset(),
                protocolConfig.getLengthFieldLength(),
                protocolConfig.getLengthAdjustment(),
                protocolConfig.getInitialBytesToStrip()
        ));
    }

    @Override
    protected void doStop() {
        if (serverChannel != null) {
            try {
                serverChannel.close().sync();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            serverChannel = null;
        }

        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
            bossGroup = null;
        }

        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            workerGroup = null;
        }
    }

    /**
     * TCP 서버 핸들러
     */
    private class TcpServerHandler extends SimpleChannelInboundHandler<ByteBuf> {

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            log.debug("Client connected: {}", remoteAddress);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
            byte[] data = new byte[msg.readableBytes()];
            msg.readBytes(data);

            try {
                // 1단계: 헤더 로깅 (즉시)
                ProtocolConfig protocolConfig = config.getProtocolConfig();
                // initialBytesToStrip으로 이미 제거된 바이트를 고려하여 오프셋 계산
                int headerOffset = protocolConfig.getLengthFieldOffset() + protocolConfig.getLengthFieldLength()
                        - protocolConfig.getInitialBytesToStrip();
                headerOffset = Math.max(0, headerOffset);

                byte[] bodyData;
                if (headerOffset > 0 && headerOffset < data.length) {
                    bodyData = new byte[data.length - headerOffset];
                    System.arraycopy(data, headerOffset, bodyData, 0, bodyData.length);
                } else {
                    bodyData = data;
                }

                // 메시지 코드 추출
                String messageCode = new String(bodyData, 0, Math.min(4, bodyData.length), config.getCharset()).trim();

                // 레이아웃 조회 및 디코딩
                MessageLayout layout = layoutManager.getLayout(messageCode);
                Message request;

                if (layout != null) {
                    request = layout.decode(bodyData, config.getCharset());
                } else {
                    request = Message.builder().messageCode(messageCode).build();
                    log.warn("Layout not found for message code: {}", messageCode);
                }

                request.setRawData(data);

                // 로깅 (2단계 로깅은 비동기로 처리됨)
                messageLogger.logReceive(request, layout, data);

                // 컨텍스트 생성
                InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
                MessageContext context = MessageContext.builder()
                        .channel(ctx.channel())
                        .transportType(TransportType.TCP)
                        .remoteAddress(remoteAddress)
                        .build();

                // 핸들러 조회 및 처리
                MessageHandler handler = getHandler(messageCode);
                if (handler != null) {
                    Message response = handler.handle(request, context);

                    // 응답 전송
                    if (response != null) {
                        sendResponse(ctx, response, layout);
                    }
                } else {
                    log.warn("No handler found for message code: {}", messageCode);
                }

            } catch (Exception e) {
                log.error("Failed to process message", e);
            }
        }

        /**
         * 응답 메시지 전송
         */
        private void sendResponse(ChannelHandlerContext ctx, Message response, MessageLayout requestLayout) {
            try {
                // 응답 레이아웃 조회 (기본적으로 요청과 동일한 레이아웃 사용)
                MessageLayout foundLayout = layoutManager.getLayout(response.getMessageCode());
                final MessageLayout responseLayout = (foundLayout != null) ? foundLayout : requestLayout;

                if (responseLayout != null) {
                    byte[] responseData = layoutManager.encode(response, config.getCharset());
                    byte[] frameData = prependLengthField(responseData);

                    ByteBuf buf = Unpooled.wrappedBuffer(frameData);
                    ctx.writeAndFlush(buf).addListener((ChannelFutureListener) f -> {
                        if (f.isSuccess()) {
                            // 전송 완료 후 로깅
                            messageLogger.logSend(response, responseLayout, frameData);
                        } else {
                            log.error("Failed to send response", f.cause());
                        }
                    });
                }
            } catch (Exception e) {
                log.error("Failed to send response", e);
            }
        }

        /**
         * 길이 필드 추가
         */
        private byte[] prependLengthField(byte[] data) {
            ProtocolConfig protocolConfig = config.getProtocolConfig();

            if (protocolConfig.getLengthFieldType() == LengthFieldType.NONE) {
                return data;
            }

            int lengthFieldLength = protocolConfig.getLengthFieldLength();
            int bodyLength = data.length;

            if (protocolConfig.isLengthIncludesHeader()) {
                bodyLength += lengthFieldLength;
            }
            bodyLength += protocolConfig.getLengthAdjustment();

            byte[] result = new byte[lengthFieldLength + data.length];

            switch (protocolConfig.getLengthFieldType()) {
                case NUMERIC_STRING:
                    String lengthStr = String.format("%0" + lengthFieldLength + "d", bodyLength);
                    System.arraycopy(lengthStr.getBytes(config.getCharset()), 0, result, 0, lengthFieldLength);
                    break;
                case BINARY_BIG_ENDIAN:
                    for (int i = lengthFieldLength - 1; i >= 0; i--) {
                        result[i] = (byte) (bodyLength & 0xFF);
                        bodyLength >>= 8;
                    }
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

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            log.debug("Client disconnected: {}", remoteAddress);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error("Channel exception", cause);
            ctx.close();
        }
    }
}
