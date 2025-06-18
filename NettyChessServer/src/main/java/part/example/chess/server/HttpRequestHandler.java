package part.example.chess.server;

import ch.qos.logback.core.encoder.JsonEscapeUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.Unpooled;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.CharsetUtil;
import part.example.chess.DTO.LoginRequest;
import part.example.chess.model.*;

import java.util.*;

public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final GameEngine gameEngine = GameEngine.getInstance();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String AUTH_SERVER_URL = "http://localhost:8080";
    private static final String JWT_SECRET = "your_jwt_secret_key";

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        String uri = request.uri();

        try {
            if (uri.startsWith("/api/auth")) {
                handleAuthRequest(ctx, request);
            } else if (uri.startsWith("/api/game")) {
                handleGameRequest(ctx, request);
            } else if (uri.equals("/api/status")) {
                handleStatusRequest(ctx);
            } else {
                sendError(ctx, HttpResponseStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
    }

    private void handleAuthRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        JSONObject responseJson = new JSONObject();
        responseJson.put("status", "success");
        responseJson.put("message", "Auth request received");
        sendJsonResponse(ctx, HttpResponseStatus.OK, responseJson);
    }

    private void handleGameRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        String authHeader = request.headers().get(HttpHeaderNames.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(ctx, HttpResponseStatus.UNAUTHORIZED);
            return;
        }

        String token = authHeader.substring(7);
        if (!validateToken(token)) {
            sendError(ctx, HttpResponseStatus.UNAUTHORIZED);
            return;
        }

        String username = extractUsernameFromToken(token);
        if (username == null) {
            sendError(ctx, HttpResponseStatus.UNAUTHORIZED);
            return;
        }

        try {
            String content = request.content().toString(CharsetUtil.UTF_8);
            JSONObject requestJson = new JSONObject(content);

            String gameState = gameEngine.getGameState(username);

            JSONObject responseJson = new JSONObject();
            responseJson.put("status", "success");
            responseJson.put("gameState", gameState);
            sendJsonResponse(ctx, HttpResponseStatus.OK, responseJson);
        } catch (Exception e) {
            sendError(ctx, HttpResponseStatus.BAD_REQUEST);
        }
    }

    private void handleStatusRequest(ChannelHandlerContext ctx) {
        try {
            JSONObject status = new JSONObject();
            status.put("activeGames", gameEngine.getActiveGamesCount());
            status.put("playersOnline", gameEngine.getPlayersOnline());

            JSONObject responseJson = new JSONObject();
            responseJson.put("status", "success");
            responseJson.put("data", status);
            sendJsonResponse(ctx, HttpResponseStatus.OK, responseJson);
        } catch (Exception e) {
            sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean validateToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return false;

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JSONObject payloadJson = new JSONObject(payload);

            long exp = payloadJson.getLong("exp");
            return System.currentTimeMillis() / 1000 <= exp;
        } catch (Exception e) {
            return false;
        }
    }

    private String extractUsernameFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            return new JSONObject(payload).getString("sub");
        } catch (Exception e) {
            return null;
        }
    }

    private void sendJsonResponse(ChannelHandlerContext ctx, HttpResponseStatus status, JSONObject json) {
        String responseText = json.toString();
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                status,
                Unpooled.copiedBuffer(responseText, CharsetUtil.UTF_8));

        response.headers()
                .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                .set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

        ctx.writeAndFlush(response);
    }

    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        JSONObject errorJson = new JSONObject();
        errorJson.put("status", "error");
        errorJson.put("message", status.reasonPhrase());
        sendJsonResponse(ctx, status, errorJson);
    }
}
