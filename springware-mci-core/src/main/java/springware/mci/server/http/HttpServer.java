package springware.mci.server.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import springware.mci.common.http.HttpMessageConverter;
import springware.mci.common.layout.LayoutManager;
import springware.mci.common.logging.MessageLogger;
import springware.mci.server.config.ServerConfig;
import springware.mci.server.core.AbstractMciServer;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
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
    private SslContext sslContext;

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
            // SSL/TLS 설정
            if (config.isSslEnabled()) {
                sslContext = createSslContext();
                log.info("SSL/TLS enabled with protocol: {}", config.getSslProtocol());
            }

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

                            // SSL/TLS 핸들러 (첫 번째로 추가)
                            if (sslContext != null) {
                                pipeline.addLast("ssl", sslContext.newHandler(ch.alloc()));
                            }

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

            String scheme = config.isSslEnabled() ? "HTTPS" : "HTTP";
            log.info("{} server started on {}:{}", scheme, config.getHost(), config.getPort());
            log.info("Registered {} endpoints", endpointRegistry.size());

            if (config.isHealthCheckEnabled()) {
                log.info("Health check endpoint: {}", config.getHealthCheckPath());
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("HTTP server startup interrupted", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize HTTP server with SSL", e);
        }
    }

    /**
     * SSL Context 생성
     */
    private SslContext createSslContext() throws Exception {
        SslContextBuilder builder;

        // PEM 파일 사용 (sslCertPath, sslKeyPath)
        if (config.getSslCertPath() != null && !config.getSslCertPath().isEmpty()
                && config.getSslKeyPath() != null && !config.getSslKeyPath().isEmpty()) {

            File certFile = new File(config.getSslCertPath());
            File keyFile = new File(config.getSslKeyPath());
            builder = SslContextBuilder.forServer(certFile, keyFile);
            log.debug("Using PEM certificate: {}", config.getSslCertPath());

        } else if (config.getKeyStorePath() != null && !config.getKeyStorePath().isEmpty()) {
            // KeyStore 사용 (PKCS12/JKS)
            KeyStore keyStore = KeyStore.getInstance(config.getKeyStoreType());
            char[] password = config.getKeyStorePassword() != null
                    ? config.getKeyStorePassword().toCharArray()
                    : new char[0];

            try (FileInputStream fis = new FileInputStream(config.getKeyStorePath())) {
                keyStore.load(fis, password);
            }

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, password);
            builder = SslContextBuilder.forServer(kmf);
            log.debug("Using KeyStore: {}", config.getKeyStorePath());

        } else {
            throw new IllegalStateException("SSL is enabled but no certificate/keystore is configured");
        }

        // TrustStore 설정 (클라이언트 인증용)
        if (config.getTrustStorePath() != null && !config.getTrustStorePath().isEmpty()) {
            KeyStore trustStore = KeyStore.getInstance(config.getTrustStoreType());
            char[] password = config.getTrustStorePassword() != null
                    ? config.getTrustStorePassword().toCharArray()
                    : new char[0];

            try (FileInputStream fis = new FileInputStream(config.getTrustStorePath())) {
                trustStore.load(fis, password);
            }

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
            builder.trustManager(tmf);
            log.debug("Using TrustStore for client auth: {}", config.getTrustStorePath());
        }

        // 클라이언트 인증 설정
        if (config.isClientAuthRequired()) {
            builder.clientAuth(ClientAuth.REQUIRE);
            log.info("Client authentication required");
        } else if (config.getTrustStorePath() != null && !config.getTrustStorePath().isEmpty()) {
            builder.clientAuth(ClientAuth.OPTIONAL);
            log.info("Client authentication optional");
        }

        // SSL 프로토콜 설정
        if (config.getSslProtocol() != null && !config.getSslProtocol().isEmpty()) {
            builder.protocols(config.getSslProtocol());
        }

        return builder.build();
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

        sslContext = null;

        String scheme = config.isSslEnabled() ? "HTTPS" : "HTTP";
        log.info("{} server stopped", scheme);
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
