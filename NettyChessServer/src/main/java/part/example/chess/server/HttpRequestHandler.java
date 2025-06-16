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
import part.example.chess.model.figures.Piece;
import part.example.chess.model.genuine.Game;
import part.example.chess.model.genuine.Move;
import part.example.chess.model.genuine.Player;
import part.example.chess.model.genuine.Position;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;



public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final GameEngine gameEngine = GameEngine.getInstance();
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        try {
            // Разделяем обработку публичных и защищенных запросов
            if (isPublicRequest(request)) {
                handlePublicRequest(ctx, request);
            } else {
                handleProtectedRequest(ctx, request);
            }
        } catch (Exception e) {
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    /**
     * Обрабатывает публичные запросы (логин, регистрация и т.д.)
     */
    private void handlePublicRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        try {
            String uri = request.uri();
            HttpMethod method = request.method();

            if (method == HttpMethod.OPTIONS) {
                handleOptionsRequest(ctx);
            } else if (method == HttpMethod.POST && "/api/auth/login".equals(uri)) {
                handleLoginRequest(ctx, request);
            } else if (method == HttpMethod.POST && "/api/auth/register".equals(uri)) {
                handleRegisterRequest(ctx, request);
            } else {
                sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, "Not found");
            }
        } catch (Exception e) {
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Request processing failed");
        }
    }

    /**
     * Обрабатывает защищенные запросы (требующие аутентификации)
     */
    private void handleProtectedRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        try {
            if (!validateToken(request)) {
                sendErrorResponse(ctx, HttpResponseStatus.UNAUTHORIZED, "Invalid token");
                return;
            }

            String uri = request.uri();
            HttpMethod method = request.method();

            if (method == HttpMethod.GET && uri.startsWith("/api/game/status/")) {
                handleGameStatusRequest(ctx, uri);
            } else if (method == HttpMethod.POST && uri.equals("/api/game/move")) {
                handleGameMoveRequest(ctx, request);
            } else {
                sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, "Not found");
            }
        } catch (Exception e) {
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Request processing failed");
        }
    }

    /**
     * Проверяет, является ли запрос публичным
     */
    private boolean isPublicRequest(FullHttpRequest request) {
        String uri = request.uri();
        return request.method() == HttpMethod.OPTIONS ||
                uri.startsWith("/api/auth/login") ||
                uri.startsWith("/api/auth/register");
    }

    // Далее реализация конкретных обработчиков:

    private void handleOptionsRequest(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK
        );
        setCorsHeaders(response);
        ctx.writeAndFlush(response);
    }

    private void handleGameMoveRequest(ChannelHandlerContext ctx, FullHttpRequest request) throws IOException {
        // 1. Парсим JSON запроса
        String content = request.content().toString(CharsetUtil.UTF_8);
        GameMoveRequest gameMoveRequest;

        try {
            gameMoveRequest = mapper.readValue(content, GameMoveRequest.class);
        } catch (JsonProcessingException e) {
            sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, "Invalid JSON format");
            return;
        }

        // 2. Извлекаем данные из запроса
        String gameId = gameMoveRequest.getGameId();
        Move move = gameMoveRequest.getMove();

        if (gameId == null || move == null || move.getFrom() == null || move.getTo() == null) {
            sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, "Missing required fields");
            return;
        }

        // 3. Проверяем токен и извлекаем username
        String username;
        try {
            username = extractUsernameFromToken(request);
        } catch (IllegalArgumentException e) {
            sendErrorResponse(ctx, HttpResponseStatus.UNAUTHORIZED, "Invalid token");
            return;
        }

        // 4. Обрабатываем ход через GameEngine
        boolean moveSuccess = gameEngine.makeMove(gameId, move, username);

        if (moveSuccess) {
            // 5. Если ход успешен - возвращаем обновленное состояние игры
            Game game = gameEngine.getGame(gameId);
            GameStatus status = new GameStatus(
                    game.getBoardState(),
                    game.getTurn(),
                    game.getStatus()
            );

            try {
                String jsonResponse = mapper.writeValueAsString(status);
                sendJsonResponse(ctx, HttpResponseStatus.OK, jsonResponse);
            } catch (JsonProcessingException e) {
                sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Error processing game state");
            }
        } else {
            sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, "Invalid move");
        }
    }

    private String extractUsernameFromToken(FullHttpRequest request) {
        String authHeader = request.headers().get("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing Authorization header");
        }

        String token = authHeader.substring(7);
        // Здесь должна быть ваша реализация извлечения username из токена
        // Например:
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor("your-secret-key".getBytes()))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid token", e);
        }
    }

    private void handleLoginRequest(ChannelHandlerContext ctx, FullHttpRequest request) throws IOException {
        String content = request.content().toString(CharsetUtil.UTF_8);
        LoginRequest loginRequest = mapper.readValue(content, LoginRequest.class);

        // Здесь должна быть логика проверки логина/пароля
        String token = "generated-jwt-token"; // Замените реальной генерацией

        sendJsonResponse(ctx, HttpResponseStatus.OK, "{\"token\":\"" + token + "\"}");
    }

    private void handleGameStatusRequest(ChannelHandlerContext ctx, String uri) {
        String[] parts = uri.split("/");
        String gameId = parts[4];
        Game game = gameEngine.getGame(gameId);

        if (game != null) {
            String jsonResponse = "{\"status\":\"" + game.getStatus() + "\"}";
            sendJsonResponse(ctx, HttpResponseStatus.OK, jsonResponse);
        } else {
            sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, "Game not found");
        }
    }

    // Вспомогательные методы:

    private void sendJsonResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String json) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                status,
                Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        setCorsHeaders(response);
        ctx.writeAndFlush(response);
    }

    private void sendErrorResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String message) {
        String json = "{\"error\":\"" + message + "\"}";
        sendJsonResponse(ctx, status, json);
    }

    private void setCorsHeaders(FullHttpResponse response) {
        response.headers()
                .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS")
                .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type, Authorization")
                .set(HttpHeaderNames.ACCESS_CONTROL_MAX_AGE, "3600");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
