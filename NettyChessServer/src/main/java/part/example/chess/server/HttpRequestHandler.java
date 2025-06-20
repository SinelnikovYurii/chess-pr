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
import part.example.chess.model.*;
import part.example.chess.model.figures.Piece;
import part.example.chess.model.genuine.Game;
import part.example.chess.model.genuine.Move;
import part.example.chess.model.genuine.Player;
import part.example.chess.model.genuine.Position;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.nio.charset.StandardCharsets;




public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final GameEngine gameEngine = GameEngine.getInstance();
    private final ObjectMapper mapper = new ObjectMapper();
    private final TokenValidator tokenValidator = new TokenValidator();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        System.out.println(request.uri());
        if (request.method() == HttpMethod.OPTIONS) {
            handlePreflightRequest(ctx, request);
            return;
        }

        if (!validateRequest(request)) {
            sendJsonResponse(ctx, HttpResponseStatus.UNAUTHORIZED, "{\"error\": \"Invalid token\"}");
            return;
        }


        switch (request.getMethod().name()) {
            case "POST":
                handlePostRequest(ctx, request);
                break;
            case "GET":
                handleGetRequest(ctx, request);
                break;
            default:
                sendErrorResponse(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED, "Unsupported method");
        }
    }

    private boolean isPreflightRequest(FullHttpRequest request) {
        return request.method() == HttpMethod.OPTIONS;
    }

    private boolean validateRequest(FullHttpRequest request) {

        String authHeader = request.headers().get("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }

        String token = authHeader.substring(7);
        return tokenValidator.validateToken(token);
    }

    private void handlePreflightRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK
        );

        setCommonHeaders(response);
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, OPTIONS");

        ctx.writeAndFlush(response);
    }

    private void handlePostRequest(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        String uri = request.uri();

        if ("/api/game/create".equals(uri)) {
            handleCreateGame(ctx, request);
        } else if (uri.startsWith("/api/game/move")) {
            handleMoveGame(ctx, request);
        }else if(uri.startsWith("/api/game/join")){
            handleJoinGame(ctx, request);
        }else {
            sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, "Endpoint not found");
        }
    }

    private void handleGetRequest(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        String uri = request.uri();

        if ("/api/games".equals(uri)) {
            handleGetGames(ctx);
        } else if (uri.startsWith("/api/game/status/")) {
            handleGameStatus(ctx, request);
        } else if (uri.startsWith("/api/game/moves/")) {
            handlePossibleMoves(ctx, request);
        } else {
            sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, "Endpoint not found");
        }
    }

    private void handleJoinGame(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        String[] parts = request.uri().split("/");
        if (parts.length < 5) {
            sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, "Invalid URL");
            return;
        }

        String gameId = parts[4];
        Game game = gameEngine.getGame(gameId);
        if (game == null) {
            sendJsonResponse(ctx, HttpResponseStatus.NOT_FOUND, "{\"error\": \"Game not found\"}");
            return;
        }

        if (game.getBlackPlayer() != null) {
            sendJsonResponse(ctx, HttpResponseStatus.CONFLICT, "{\"error\": \"Game already has two players\"}");
            return;
        }

        String authHeader = request.headers().get("Authorization");
        String token = authHeader.substring(7);
        String username = extractUsernameFromToken(token);

        if (username == null) {
            sendJsonResponse(ctx, HttpResponseStatus.UNAUTHORIZED, "{\"error\": \"Invalid token\"}");
            return;
        }

        Player player2 = new Player("2", username, "black");
        game.setBlackPlayer(player2);
        String responseStr = "{\"message\": \"Вы присоединились к игре\"}";
        sendJsonResponse(ctx, HttpResponseStatus.OK, responseStr);
    }

    private void handleCreateGame(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        String authHeader = request.headers().get("Authorization");
        String token = authHeader.substring(7);
        String username = extractUsernameFromToken(token);

        if (username == null) {
            sendJsonResponse(ctx, HttpResponseStatus.UNAUTHORIZED, "{\"error\": \"Invalid token\"}");
            return;
        }

        Player player1 = new Player("1", username, "white");
        String gameId = gameEngine.createGame(player1);
        String responseStr = "{\"gameId\": \"" + gameId + "\"}";
        sendJsonResponse(ctx, HttpResponseStatus.CREATED, responseStr);
    }

    private void handleMoveGame(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        String body = request.content().toString(StandardCharsets.UTF_8);
        GameMoveRequest gameMoveRequest = mapper.readValue(body, GameMoveRequest.class);

        String gameId = gameMoveRequest.getGameId();
        Move move = gameMoveRequest.getMove();

        System.out.println("Новый ход = " + gameId + " " + move.getFrom() + " " + move.getTo() + " " + move.getPiece());

        String authHeader = request.headers().get("Authorization");
        String token = authHeader.substring(7); // Bearer <token>
        String username = extractUsernameFromToken(token);

        if (username == null) {
            System.out.println("username is null");
            sendJsonResponse(ctx, HttpResponseStatus.UNAUTHORIZED, "{\"error\": \"Invalid token\"}");
            return;
        }

        Game game = gameEngine.getGame(gameId);
        if (game == null) {
            sendJsonResponse(ctx, HttpResponseStatus.NOT_FOUND, "{\"error\": \"Game not found\"}");
            return;
        }

        if (!game.isPlayersTurn(username)) {
            sendJsonResponse(ctx, HttpResponseStatus.FORBIDDEN, "{\"error\": \"Not your turn\"}");
            return;
        }


        System.out.println("Начало обработки хода");
        boolean success = gameEngine.makeMove(gameId, move, username);
        if (!success) {
            sendJsonResponse(ctx, HttpResponseStatus.FORBIDDEN, "{\"error\": \"Неверный ход\"}");
            return;
        }

        if (game.hasTwoPlayers()) {
            String json = mapper.writeValueAsString(game.getBoardStateForClient());
            sendJsonResponse(ctx, HttpResponseStatus.OK, json);
        } else {
            sendJsonResponse(ctx, HttpResponseStatus.CONFLICT, "{\"error\": \"Ожидается второй игрок\"}");
        }
        String responseStr = "{\"success\": " + success + "}";
        sendJsonResponse(ctx, HttpResponseStatus.OK, responseStr);
    }

    private String extractUsernameFromToken(String token) {
        final String AUTH_SERVICE_URL = "http://localhost:8081/api/auth";
        try {
            URL url = new URL(AUTH_SERVICE_URL + "/user-info");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + token);
            connection.setRequestProperty("Content-Type", "application/json");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(response.toString());
                return jsonNode.path("username").asText();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private void handleGetGames(ChannelHandlerContext ctx) throws Exception {
        List<Game> allGames = GameEngine.getInstance().getAllGames();
        sendJsonResponse(ctx, HttpResponseStatus.OK, mapper.writeValueAsString(allGames));
    }

    private void handleGameStatus(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        String[] parts = request.uri().split("/");
        System.out.println(Arrays.toString(parts));
        if (parts.length < 4) {
            sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, "Invalid URL");
            return;
        }

        String gameId = parts[4];
        Game game = gameEngine.getGame(gameId);

        if (game != null) {
            GameStatus status = new GameStatus(
                    game.getBoardState(),
                    game.getTurn(),
                    game.getStatus()
            );
            sendJsonResponse(ctx, HttpResponseStatus.OK, mapper.writeValueAsString(status));
        } else {
            sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, "Game not found");
        }
    }

    private void handlePossibleMoves(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        String[] parts = request.uri().split("/");
        if (parts.length < 5) {
            sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, "Invalid URL");
            return;
        }

        String gameId = parts[4];
        Game game = gameEngine.getGame(gameId);

        if (game == null) {
            sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, "Game not found");
            return;
        }

        // Создаём копию для безопасного итерирования
        Map<Position, Piece> piecesCopy = new HashMap<>(game.getBoard().getPieces());

        Map<String, List<String>> possibleMoves = new HashMap<>();
        for (Map.Entry<Position, Piece> entry : piecesCopy.entrySet()) {
            Piece piece = entry.getValue();
            if (!piece.getColor().equals(game.getTurn())) continue;

            Position from = piece.getPosition();
            String fromKey = gameEngine.positionToString(from);
            List<String> moves = gameEngine.getValidMoves(piece, game);
            if (!moves.isEmpty()) {
                possibleMoves.put(fromKey, moves);
            }
        }

        String json = mapper.writeValueAsString(possibleMoves);
        sendJsonResponse(ctx, HttpResponseStatus.OK, json);
    }

    private void sendJsonResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String json) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                status,
                Unpooled.copiedBuffer(json, StandardCharsets.UTF_8)
        );

        setCommonHeaders(response);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

        ctx.writeAndFlush(response);
    }

    private void sendErrorResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String message) {
        String json = "{\"error\": \"" + message + "\"}";
        sendJsonResponse(ctx, status, json);
    }

    private void setCommonHeaders(FullHttpResponse response) {
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, OPTIONS");

        response.headers().add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type");
        response.headers().add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Authorization");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}