package springware.mci.client.tcp;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.extern.slf4j.Slf4j;
import springware.mci.client.config.ClientConfig;
import springware.mci.client.core.AbstractMciClient;
import springware.mci.common.core.Message;
import springware.mci.common.exception.ConnectionException;
import springware.mci.common.layout.LayoutManager;
import springware.mci.common.layout.MessageLayout;
import springware.mci.common.logging.MessageLogger;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Netty 기반 UDP 클라이언트
 */
@Slf4j
public class UdpClient extends AbstractMciClient {

    private EventLoopGroup workerGroup;
    private Channel channel;
    private InetSocketAddress remoteAddress;
    private final Map<String, CompletableFuture<Message>> pendingRequests = new ConcurrentHashMap<>();

    public UdpClient(ClientConfig config) {
        super(config);
    }

    public UdpClient(ClientConfig config, LayoutManager layoutManager, MessageLogger messageLogger) {
        super(config, layoutManager, messageLogger);
    }

    @Override
    protected void doConnect() {
        workerGroup = new NioEventLoopGroup();
        remoteAddress = new InetSocketAddress(config.getHost(), config.getPort());

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(workerGroup)
                    .channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, false)
                    .handler(new ChannelInitializer<DatagramChannel>() {
                        @Override
                        protected void initChannel(DatagramChannel ch) {
                            ch.pipeline().addLast("handler", new UdpClientHandler());
                        }
                    });

            channel = bootstrap.bind(0).sync().channel();

            log.debug("UDP client started, target: {}:{}", config.getHost(), config.getPort());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConnectionException("Connection interrupted", e);
        } catch (Exception e) {
            shutdown();
            throw new ConnectionException("Failed to start UDP client", e);
        }
    }

    @Override
    protected void doDisconnect() {
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
            MessageLayout layout = layoutManager.getLayout(message.getMessageCode());
            byte[] data = layoutManager.encode(message, config.getCharset());

            ByteBuf buf = Unpooled.wrappedBuffer(data);
            DatagramPacket packet = new DatagramPacket(buf, remoteAddress);

            channel.writeAndFlush(packet).addListener((ChannelFutureListener) f -> {
                if (f.isSuccess()) {
                    messageLogger.logSend(message, layout, data);
                    log.debug("UDP message sent: {}", message.getMessageId());
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

            ByteBuf buf = Unpooled.wrappedBuffer(data);
            DatagramPacket packet = new DatagramPacket(buf, remoteAddress);

            channel.writeAndFlush(packet).addListener((ChannelFutureListener) f -> {
                if (f.isSuccess()) {
                    messageLogger.logSend(message, layout, data);
                }
            });

        } catch (Exception e) {
            throw new ConnectionException("Failed to send message", e);
        }
    }

    /**
     * UDP 클라이언트 핸들러
     */
    private class UdpClientHandler extends SimpleChannelInboundHandler<DatagramPacket> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
            ByteBuf content = packet.content();
            byte[] data = new byte[content.readableBytes()];
            content.readBytes(data);

            try {
                // 메시지 코드 추출
                String messageCode = new String(data, 0, Math.min(4, data.length), config.getCharset()).trim();

                MessageLayout layout = layoutManager.getLayout(messageCode);
                Message response = layout != null
                        ? layout.decode(data, config.getCharset())
                        : Message.builder().messageCode(messageCode).build();

                response.setRawData(data);

                messageLogger.logReceive(response, layout, data);

                // 응답 전달
                if (!pendingRequests.isEmpty()) {
                    String firstKey = pendingRequests.keySet().iterator().next();
                    CompletableFuture<Message> future = pendingRequests.remove(firstKey);
                    if (future != null) {
                        future.complete(response);
                    }
                }

            } catch (Exception e) {
                log.error("Failed to process received UDP message", e);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error("UDP channel exception", cause);
        }
    }
}
