package springware.mci.server.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import springware.mci.common.layout.LayoutManager;
import springware.mci.common.logging.MessageLogger;
import springware.mci.server.config.ServerConfig;
import springware.mci.server.core.AbstractMciServer;

import java.util.concurrent.TimeUnit;

/**
 * Netty 기반 HTTP 서버
 * REST API 엔드포인트 제공
 */
@Slf4j
public class HttpServer extends AbstractMciServer {

    private static final int MAX_CONTENT_LENGTH = 65536;  // 64KB

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    @Getter
    private final RestEndpointRegistry endpointRegistry;
    private final HttpMessageConverter messageConverter;

    public HttpServer(ServerConfig config) {
        super(config);
        this.endpointRegistry = new RestEndpointRegistry();
        this.messageConverter = new HttpMessageConverter(config.getCharset());
    }

    public HttpServer(ServerConfig config, LayoutManager layoutManager, MessageLogger messageLogger) {
        super(config, layoutManager, messageLogger);
        this.endpointRegistry = new RestEndpointRegistry();
        this.messageConverter = new HttpMessageConverter(config.getCharset());
    }

    public HttpServer(ServerConfig config, LayoutManager layoutManager, MessageLogger messageLogger,
                      RestEndpointRegistry endpointRegistry) {
        super(config, layoutManager, messageLogger);
        this.endpointRegistry = endpointRegistry != null ? endpointRegistry : new RestEndpointRegistry();
        this.messageConverter = new HttpMessageConverter(config.getCharset());
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

                            // HTTP 코덱
                            pipeline.addLast("httpCodec", new HttpServerCodec());

                            // HTTP 요청 집계 (FullHttpRequest로 변환)
                            pipeline.addLast("httpAggregator", new HttpObjectAggregator(MAX_CONTENT_LENGTH));

                            // 비즈니스 로직 핸들러
                            pipeline.addLast("handler", new HttpServerHandler(
                                    config,
                                    handlers,
                                    defaultHandler,
                                    endpointRegistry,
                                    messageConverter,
                                    layoutManager,
                                    messageLogger
                            ));
                        }
                    });

            ChannelFuture future = bootstrap.bind(config.getHost(), config.getPort()).sync();
            serverChannel = future.channel();

            log.info("HTTP server started on {}:{}", config.getHost(), config.getPort());
            log.info("Registered {} endpoints", endpointRegistry.size());

            if (config.isHealthCheckEnabled()) {
                log.info("Health check endpoint: {}", config.getHealthCheckPath());
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("HTTP server startup interrupted", e);
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

        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
            bossGroup = null;
        }

        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            workerGroup = null;
        }

        log.info("HTTP server stopped");
    }

    /**
     * 엔드포인트 등록
     *
     * @param path 경로
     * @param messageCode 메시지 코드
     */
    public void registerEndpoint(String path, String messageCode) {
        endpointRegistry.register(path, messageCode);
    }

    /**
     * 메시지 변환기 조회
     */
    public HttpMessageConverter getMessageConverter() {
        return messageConverter;
    }
}
