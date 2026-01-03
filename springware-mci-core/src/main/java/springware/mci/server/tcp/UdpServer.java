package springware.mci.server.tcp;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.extern.slf4j.Slf4j;
import springware.mci.common.core.Message;
import springware.mci.common.core.TransportType;
import springware.mci.common.layout.LayoutManager;
import springware.mci.common.layout.MessageLayout;
import springware.mci.common.logging.MessageLogger;
import springware.mci.server.config.ServerConfig;
import springware.mci.server.core.AbstractMciServer;
import springware.mci.server.core.MessageContext;
import springware.mci.server.core.MessageHandler;

import java.net.InetSocketAddress;

/**
 * Netty 기반 UDP 서버
 */
@Slf4j
public class UdpServer extends AbstractMciServer {

    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public UdpServer(ServerConfig config) {
        super(config);
    }

    public UdpServer(ServerConfig config, LayoutManager layoutManager, MessageLogger messageLogger) {
        super(config, layoutManager, messageLogger);
    }

    @Override
    protected void doStart() {
        int workerThreads = config.getWorkerThreads();
        workerGroup = workerThreads > 0
                ? new NioEventLoopGroup(workerThreads)
                : new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(workerGroup)
                    .channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, false)
                    .option(ChannelOption.SO_REUSEADDR, config.isReuseAddress())
                    .handler(new ChannelInitializer<DatagramChannel>() {
                        @Override
                        protected void initChannel(DatagramChannel ch) {
                            ch.pipeline().addLast("handler", new UdpServerHandler());
                        }
                    });

            ChannelFuture future = bootstrap.bind(config.getHost(), config.getPort()).sync();
            serverChannel = future.channel();

            log.info("UDP server started on {}:{}", config.getHost(), config.getPort());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Server startup interrupted", e);
        }
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

        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            workerGroup = null;
        }
    }

    /**
     * UDP 서버 핸들러
     */
    private class UdpServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
            InetSocketAddress sender = packet.sender();
            ByteBuf content = packet.content();

            byte[] data = new byte[content.readableBytes()];
            content.readBytes(data);

            try {
                // 메시지 코드 추출
                String messageCode = new String(data, 0, Math.min(4, data.length), config.getCharset()).trim();

                // 레이아웃 조회 및 디코딩
                MessageLayout layout = layoutManager.getLayout(messageCode);
                Message request;

                if (layout != null) {
                    request = layout.decode(data, config.getCharset());
                } else {
                    request = Message.builder().messageCode(messageCode).build();
                    log.warn("Layout not found for message code: {}", messageCode);
                }

                request.setRawData(data);

                // 로깅
                messageLogger.logReceive(request, layout, data);

                // 컨텍스트 생성
                MessageContext context = MessageContext.builder()
                        .channel(ctx.channel())
                        .transportType(TransportType.UDP)
                        .remoteAddress(sender)
                        .build();

                // 핸들러 조회 및 처리
                MessageHandler handler = getHandler(messageCode);
                if (handler != null) {
                    Message response = handler.handle(request, context);

                    // 응답 전송
                    if (response != null) {
                        sendResponse(ctx, sender, response, layout);
                    }
                } else {
                    log.warn("No handler found for message code: {}", messageCode);
                }

            } catch (Exception e) {
                log.error("Failed to process UDP message", e);
            }
        }

        /**
         * 응답 메시지 전송
         */
        private void sendResponse(ChannelHandlerContext ctx, InetSocketAddress recipient,
                                  Message response, MessageLayout requestLayout) {
            try {
                MessageLayout foundLayout = layoutManager.getLayout(response.getMessageCode());
                final MessageLayout responseLayout = (foundLayout != null) ? foundLayout : requestLayout;

                if (responseLayout != null) {
                    byte[] responseData = layoutManager.encode(response, config.getCharset());

                    ByteBuf buf = Unpooled.wrappedBuffer(responseData);
                    DatagramPacket packet = new DatagramPacket(buf, recipient);

                    ctx.writeAndFlush(packet).addListener((ChannelFutureListener) f -> {
                        if (f.isSuccess()) {
                            messageLogger.logSend(response, responseLayout, responseData);
                        } else {
                            log.error("Failed to send UDP response", f.cause());
                        }
                    });
                }
            } catch (Exception e) {
                log.error("Failed to send UDP response", e);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error("UDP channel exception", cause);
        }
    }
}
