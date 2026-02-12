package springware.mci.server.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;
import springware.mci.common.core.Message;
import springware.mci.common.core.TransportType;
import springware.mci.common.http.HttpMessageConverter;
import springware.mci.common.layout.LayoutManager;
import springware.mci.common.logging.MessageLogger;
import springware.mci.server.config.ServerConfig;
import springware.mci.server.core.MessageContext;
import springware.mci.server.core.MessageHandler;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * Netty HTTP 요청 핸들러
 * FullHttpRequest를 받아 처리하고 FullHttpResponse를 반환
 */
@Slf4j
public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final ServerConfig config;
    private final Map<String, MessageHandler> handlers;
    private final MessageHandler defaultHandler;
    private final RestEndpointRegistry endpointRegistry;
    private final HttpMessageConverter messageConverter;
    private final LayoutManager layoutManager;
    private final MessageLogger messageLogger;

    public HttpServerHandler(
            ServerConfig config,
            Map<String, MessageHandler> handlers,
            MessageHandler defaultHandler,
            RestEndpointRegistry endpointRegistry,
            HttpMessageConverter messageConverter,
            LayoutManager layoutManager,
            MessageLogger messageLogger) {
        this.config = config;
        this.handlers = handlers;
        this.defaultHandler = defaultHandler;
        this.endpointRegistry = endpointRegistry;
        this.messageConverter = messageConverter;
        this.layoutManager = layoutManager;
        this.messageLogger = messageLogger;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        // URI에서 경로 추출
        String uri = request.uri();
        String path = RestEndpointRegistry.extractPath(uri);
        HttpMethod method = request.method();

        log.debug("HTTP request: {} {} from {}", method, uri, ctx.channel().remoteAddress());

        // CORS Preflight 요청 처리
        if (HttpMethod.OPTIONS.equals(method) && config.isCorsEnabled()) {
            sendCorsPreflightResponse(ctx, request);
            return;
        }

        // 헬스 체크 엔드포인트
        if (config.isHealthCheckEnabled() && path.equals(config.getHealthCheckPath())) {
            sendHealthCheckResponse(ctx, request);
            return;
        }

        // GET 요청 처리 (하트비트 등)
        if (HttpMethod.GET.equals(method)) {
            handleGetRequest(ctx, request, path);
            return;
        }

        // POST 요청 처리 (비즈니스 로직)
        if (HttpMethod.POST.equals(method)) {
            handlePostRequest(ctx, request, path);
            return;
        }

        // 지원하지 않는 메서드
        sendErrorResponse(ctx, request, HttpResponseStatus.METHOD_NOT_ALLOWED, "Method not allowed");
    }

    /**
     * GET 요청 처리
     */
    private void handleGetRequest(ChannelHandlerContext ctx, FullHttpRequest request, String path) {
        String messageCode = endpointRegistry.getMessageCode(path);

        if (messageCode == null) {
            sendErrorResponse(ctx, request, HttpResponseStatus.NOT_FOUND, "Endpoint not found: " + path);
            return;
        }

        // GET 요청에 대한 빈 요청 메시지 생성
        Message requestMessage = Message.builder()
                .messageCode(messageCode)
                .transportType(TransportType.HTTP)
                .build();

        processMessage(ctx, request, requestMessage);
    }

    /**
     * POST 요청 처리
     */
    private void handlePostRequest(ChannelHandlerContext ctx, FullHttpRequest request, String path) {
        try {
            // 요청 바디 읽기
            ByteBuf content = request.content();
            String body = content.toString(config.getCharset());

            if (body.isEmpty()) {
                sendErrorResponse(ctx, request, HttpResponseStatus.BAD_REQUEST, "Request body is empty");
                return;
            }

            // JSON -> Message 변환
            String messageCode = endpointRegistry.getMessageCode(path);
            Message requestMessage = messageConverter.fromJson(body, messageCode);

            // 경로에서 메시지 코드를 못 찾은 경우 JSON에서 가져오기
            if (requestMessage.getMessageCode() == null) {
                sendErrorResponse(ctx, request, HttpResponseStatus.BAD_REQUEST, "Message code not specified");
                return;
            }

            // 수신 로깅
            byte[] requestBytes = body.getBytes(config.getCharset());
            messageLogger.logReceive(requestMessage, layoutManager.getLayout(requestMessage.getMessageCode()), requestBytes);

            processMessage(ctx, request, requestMessage);

        } catch (Exception e) {
            log.error("Failed to process POST request", e);
            sendErrorResponse(ctx, request, HttpResponseStatus.BAD_REQUEST, "Invalid request: " + e.getMessage());
        }
    }

    /**
     * 메시지 처리 및 응답
     */
    private void processMessage(ChannelHandlerContext ctx, FullHttpRequest request, Message requestMessage) {
        try {
            // 컨텍스트 생성
            InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            MessageContext context = MessageContext.builder()
                    .channel(ctx.channel())
                    .transportType(TransportType.HTTP)
                    .remoteAddress(remoteAddress)
                    .build();

            // HTTP 헤더 정보 저장
            context.setAttribute("httpMethod", request.method().name());
            context.setAttribute("httpUri", request.uri());

            // 핸들러 조회
            MessageHandler handler = handlers.get(requestMessage.getMessageCode());
            if (handler == null) {
                handler = defaultHandler;
            }

            if (handler == null) {
                sendErrorResponse(ctx, request, HttpResponseStatus.NOT_FOUND,
                        "No handler for message code: " + requestMessage.getMessageCode());
                return;
            }

            // 핸들러 실행
            Message responseMessage = handler.handle(requestMessage, context);

            if (responseMessage != null) {
                sendJsonResponse(ctx, request, HttpResponseStatus.OK, responseMessage);
            } else {
                // 응답 없음
                sendJsonResponse(ctx, request, HttpResponseStatus.NO_CONTENT, null);
            }

        } catch (Exception e) {
            log.error("Failed to process message", e);
            sendErrorResponse(ctx, request, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                    "Internal server error: " + e.getMessage());
        }
    }

    /**
     * JSON 응답 전송
     */
    private void sendJsonResponse(ChannelHandlerContext ctx, FullHttpRequest request,
                                   HttpResponseStatus status, Message responseMessage) {
        String json;
        if (responseMessage != null) {
            json = messageConverter.toJson(responseMessage);

            // 송신 로깅
            byte[] responseBytes = json.getBytes(config.getCharset());
            messageLogger.logSend(responseMessage, layoutManager.getLayout(responseMessage.getMessageCode()), responseBytes);
        } else {
            json = "";
        }

        ByteBuf content = Unpooled.copiedBuffer(json, config.getCharset());

        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status, content);

        response.headers()
                .set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=" + config.getCharset().name())
                .set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());

        // CORS 헤더 추가
        if (config.isCorsEnabled()) {
            addCorsHeaders(response);
        }

        // Keep-Alive 처리
        boolean keepAlive = HttpUtil.isKeepAlive(request);
        if (keepAlive) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            ctx.writeAndFlush(response);
        } else {
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }

    /**
     * 에러 응답 전송
     */
    private void sendErrorResponse(ChannelHandlerContext ctx, FullHttpRequest request,
                                    HttpResponseStatus status, String message) {
        String json = messageConverter.createErrorJson(String.valueOf(status.code()), message);
        ByteBuf content = Unpooled.copiedBuffer(json, config.getCharset());

        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status, content);

        response.headers()
                .set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=" + config.getCharset().name())
                .set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());

        if (config.isCorsEnabled()) {
            addCorsHeaders(response);
        }

        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * 헬스 체크 응답 전송
     */
    private void sendHealthCheckResponse(ChannelHandlerContext ctx, FullHttpRequest request) {
        String json = messageConverter.createHealthCheckJson("UP");
        ByteBuf content = Unpooled.copiedBuffer(json, config.getCharset());

        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);

        response.headers()
                .set(HttpHeaderNames.CONTENT_TYPE, "application/json")
                .set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());

        if (config.isCorsEnabled()) {
            addCorsHeaders(response);
        }

        boolean keepAlive = HttpUtil.isKeepAlive(request);
        if (keepAlive) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            ctx.writeAndFlush(response);
        } else {
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }

    /**
     * CORS Preflight 응답 전송
     */
    private void sendCorsPreflightResponse(ChannelHandlerContext ctx, FullHttpRequest request) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

        addCorsHeaders(response);

        response.headers()
                .set(HttpHeaderNames.CONTENT_LENGTH, 0);

        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * CORS 헤더 추가
     */
    private void addCorsHeaders(FullHttpResponse response) {
        if (config.getCorsAllowedOrigins() != null && !config.getCorsAllowedOrigins().isEmpty()) {
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN,
                    String.join(", ", config.getCorsAllowedOrigins()));
        } else {
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        }

        response.headers()
                .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, OPTIONS")
                .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type, Authorization")
                .set(HttpHeaderNames.ACCESS_CONTROL_MAX_AGE, "86400");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.debug("HTTP client connected: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.debug("HTTP client disconnected: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("HTTP handler exception", cause);
        ctx.close();
    }
}
