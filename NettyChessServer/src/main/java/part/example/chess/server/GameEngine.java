package part.example.chess.server;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.encoder.JsonEscapeUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import part.example.chess.model.figures.*;
import part.example.chess.model.genuine.*;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.*;

public class GameEngine {

    private static GameEngine instance = new GameEngine();
    private final Map<String, Game> games = new HashMap<>();
    private final Map<String, String> playerToGame = new HashMap<>();

    private GameEngine() {}

    public static GameEngine getInstance() {
        return instance;
    }

    public List<Game> getAllGames() {
        return new ArrayList<>(games.values());
    }

    public String createGame(String player1Id, String player2Id) {
        String gameId = "game-" + System.currentTimeMillis();
        Game game = new Game(gameId, new Player(player1Id, "Player1", "white"),
                new Player(player2Id, "Player2", "black"));
        games.put(gameId, game);
        playerToGame.put(player1Id, gameId);
        playerToGame.put(player2Id, gameId);
        return gameId;
    }

    public String findOpponent(String playerId) {
        // Простейший вариант: ищем первого ожидающего игрока
        for (Map.Entry<String, String> entry : playerToGame.entrySet()) {
            String otherPlayerId = entry.getKey();
            if (!otherPlayerId.equals(playerId)) {
                return otherPlayerId;
            }
        }
        return null;
    }

    public synchronized boolean makeMove(String gameId, Move move, String username) {
        Game game = games.get(gameId);
        if (game == null) return false;

        // 1. Проверяем, что игра еще не завершена
        if (!game.getStatus().equals("ongoing")) {
            return false;
        }

        // 2. Проверяем, что ход делает правильный игрок
        String currentTurn = game.getTurn();
        Player currentPlayer = currentTurn.equals("white") ? game.getWhitePlayer() : game.getBlackPlayer();

        if (!currentPlayer.getUsername().equals(username)) {
            return false;
        }

        // 3. Преобразуем шахматные координаты в позиции
        Position from = stringToPosition(move.getFrom());
        Position to = stringToPosition(move.getTo());

        Board board = game.getBoard();
        Piece piece = board.getPieceAt(from);

        // 4. Проверяем, что фигура существует и принадлежит текущему игроку
        if (piece == null || !piece.getColor().equals(currentTurn)) {
            return false;
        }

        // 5. Создаем копию игры для проверки хода
        Game tempGame = copyGame(game);
        Board tempBoard = tempGame.getBoard();
        Position tempFrom = new Position(from.getX(), from.getY());
        Position tempTo = new Position(to.getX(), to.getY());
        Piece tempPiece = tempBoard.getPieceAt(tempFrom);

        // 6. Пытаемся сделать ход во временной копии
        if (!tempBoard.movePiece(tempPiece, tempTo)) {
            return false;
        }

        // 7. Проверяем, остается ли король под шахом после хода
        if (isInCheck(tempGame, currentTurn)) {
            return false; // Ход не снимает шах - недопустим
        }

        // 8. Если все проверки пройдены, делаем ход в реальной игре
        boolean moveSuccess = board.movePiece(piece, to);
        if (!moveSuccess) {
            return false;
        }

        // 9. Проверяем состояние игры после хода
        String opponentColor = currentTurn.equals("white") ? "black" : "white";

        if (isInCheck(game, opponentColor)) {
            if (isCheckmate(game, opponentColor)) {
                game.setStatus(currentTurn + "_wins");
            } else {
                game.setStatus(opponentColor + "_in_check");
            }
        } else if (isStalemate(game, opponentColor)) {
            game.setStatus("draw");
        }

        // 10. Меняем очередь хода, если игра продолжается
        if (game.getStatus().equals("ongoing") || game.getStatus().endsWith("_in_check")) {
            game.setTurn(opponentColor);
        }

        return true;
    }

    private boolean isInCheck(Game game, String kingColor) {
        Board board = game.getBoard();
        Position kingPosition = findKing(board, kingColor);
        if (kingPosition == null) return false;

        String opponentColor = kingColor.equals("white") ? "black" : "white";

        for (Map.Entry<Position, Piece> entry : board.getPieces().entrySet()) {
            Piece piece = entry.getValue();
            if (piece.getColor().equals(opponentColor) &&
                    piece.isValidMove(kingPosition, board)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCheckmate(Game game, String kingColor) {
        if (!isInCheck(game, kingColor)) {
            return false;
        }

        Board board = game.getBoard();
        Position kingPosition = findKing(board, kingColor);
        King king = (King) board.getPieceAt(kingPosition);

        // 1. Проверяем, может ли король сдвинуться
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;

                int newX = kingPosition.getX() + dx;
                int newY = kingPosition.getY() + dy;

                if (newX >= 0 && newX < 8 && newY >= 0 && newY < 8) {
                    Position newPos = new Position(newX, newY);
                    if (king.isValidMove(newPos, board)) {
                        Game tempGame = copyGame(game);
                        Board tempBoard = tempGame.getBoard();
                        tempBoard.movePiece(tempBoard.getPieceAt(kingPosition), newPos);

                        if (!isInCheck(tempGame, kingColor)) {
                            return false; // Король может уйти от шаха
                        }
                    }
                }
            }
        }

        // 2. Проверяем, можно ли заблокировать шах другой фигурой
        Position attackerPosition = findAttacker(game, kingPosition, kingColor);
        if (attackerPosition != null) {
            // Проверяем все фигуры защищающегося игрока
            for (Map.Entry<Position, Piece> entry : board.getPieces().entrySet()) {
                Piece piece = entry.getValue();
                if (piece.getColor().equals(kingColor) && !(piece instanceof King)) {
                    // Проверяем, может ли фигура перехватить атаку
                    if (piece.isValidMove(attackerPosition, board)) {
                        Game tempGame = copyGame(game);
                        Board tempBoard = tempGame.getBoard();
                        tempBoard.movePiece(
                                tempBoard.getPieceAt(entry.getKey()),
                                attackerPosition
                        );

                        if (!isInCheck(tempGame, kingColor)) {
                            return false; // Атаку можно заблокировать
                        }
                    }

                    // Проверяем, может ли фигура встать между атакующим и королем
                    if (isLinearAttack(attackerPosition, kingPosition)) {
                        List<Position> path = getPath(attackerPosition, kingPosition);
                        for (Position pos : path) {
                            if (piece.isValidMove(pos, board)) {
                                Game tempGame = copyGame(game);
                                Board tempBoard = tempGame.getBoard();
                                tempBoard.movePiece(
                                        tempBoard.getPieceAt(entry.getKey()),
                                        pos
                                );

                                if (!isInCheck(tempGame, kingColor)) {
                                    return false; // Атаку можно перехватить
                                }
                            }
                        }
                    }
                }
            }
        }

        return true; // Некуда ходить - мат
    }

    private boolean isStalemate(Game game, String currentColor) {
        if (isInCheck(game, currentColor)) {
            return false;
        }

        Board board = game.getBoard();

        // Проверяем, есть ли у игрока допустимые ходы
        for (Map.Entry<Position, Piece> entry : board.getPieces().entrySet()) {
            Piece piece = entry.getValue();
            if (piece.getColor().equals(currentColor)) {
                for (int x = 0; x < 8; x++) {
                    for (int y = 0; y < 8; y++) {
                        Position to = new Position(x, y);
                        if (piece.isValidMove(to, board)) {
                            Game tempGame = copyGame(game);
                            Board tempBoard = tempGame.getBoard();
                            if (tempBoard.movePiece(tempBoard.getPieceAt(entry.getKey()), to)) {
                                if (!isInCheck(tempGame, currentColor)) {
                                    return false; // Найден допустимый ход
                                }
                            }
                        }
                    }
                }
            }
        }

        return true; // Нет допустимых ходов - пат
    }

// Вспомогательные методы:

    private Position findAttacker(Game game, Position kingPosition, String kingColor) {
        Board board = game.getBoard();
        String opponentColor = kingColor.equals("white") ? "black" : "white";

        for (Map.Entry<Position, Piece> entry : board.getPieces().entrySet()) {
            Piece piece = entry.getValue();
            if (piece.getColor().equals(opponentColor) &&
                    piece.isValidMove(kingPosition, board)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private boolean isLinearAttack(Position from, Position to) {
        return from.getX() == to.getX() ||  // Вертикальная атака
                from.getY() == to.getY() ||  // Горизонтальная атака
                Math.abs(from.getX() - to.getX()) == Math.abs(from.getY() - to.getY()); // Диагональная атака
    }

    private List<Position> getPath(Position from, Position to) {
        List<Position> path = new ArrayList<>();
        int dx = Integer.compare(to.getX(), from.getX());
        int dy = Integer.compare(to.getY(), from.getY());

        int x = from.getX() + dx;
        int y = from.getY() + dy;

        while (x != to.getX() || y != to.getY()) {
            path.add(new Position(x, y));
            x += dx;
            y += dy;
        }

        return path;
    }

    private Game copyGame(Game original) {
        Game copy = new Game(original.getId(),
                original.getWhitePlayer(),
                original.getBlackPlayer());

        copy.setBoard(new Board());
        copy.setTurn(original.getTurn());
        copy.setStatus(original.getStatus());

        for (Map.Entry<Position, Piece> entry : original.getBoard().getPieces().entrySet()) {
            Piece originalPiece = entry.getValue();
            Piece copiedPiece = createPieceCopy(originalPiece);
            copy.getBoard().addPiece(copiedPiece);
        }

        return copy;
    }

    private Piece createPieceCopy(Piece original) {
        Position pos = original.getPosition();
        switch (original.getType()) {
            case "pawn": return new Pawn(original.getColor(), pos);
            case "rook": return new Rook(original.getColor(), pos);
            case "knight": return new Knight(original.getColor(), pos);
            case "bishop": return new Bishop(original.getColor(), pos);
            case "queen": return new Queen(original.getColor(), pos);
            case "king": return new King(original.getColor(), pos);
            default: throw new IllegalArgumentException("Unknown piece type");
        }
    }



    private boolean isPlayerTurn(Piece piece, Game game) {
        String currentTurn = game.getTurn();
        return currentTurn.equals(piece.getColor());
    }

    private void changeTurn(Game game) {
        String currentTurn = game.getTurn();
        game.setTurn(currentTurn.equals("white") ? "black" : "white");
    }


    private Piece createCopyOfPiece(Piece original) {
        try {
            Class<?> clazz = Class.forName(original.getClass().getName());
            Constructor<?> constructor = clazz.getConstructor(String.class, Position.class);
            return (Piece) constructor.newInstance(original.getColor(), original.getPosition());
        } catch (Exception e) {
            throw new RuntimeException("Не удалось скопировать фигуру", e);
        }
    }

    private Position findKing(Board board, String color) {
        for (Map.Entry<Position, Piece> entry : board.getPieces().entrySet()) {
            Piece piece = entry.getValue();
            if ("king".equals(piece.getType()) && color.equals(piece.getColor())) {
                System.out.println("Король найден на позиции: " + piece.getPosition());
                return piece.getPosition();
            }
        }
        System.out.println("Король не найден");
        return null;
    }

    public Game getGame(String gameId) {
        Game game = games.get(gameId);
        if (game == null) {
            // Попробуем загрузить из файла или БД
//            try {
//                game = loadGameFromFile(gameId); // ← реализуй этот метод
//            } catch (IOException e) {
//                System.err.println("Не удалось загрузить игру: " + gameId);
//                e.printStackTrace();
//            }

            if (game == null) {
                // Создаём новую игру, если она не найдена
                game = new Game(gameId, new Player("1","Player1","white"),
                        new Player("2","Player2","black"));
                games.put(gameId, game);

            }
        }

        return game;
    }



    public Position stringToPosition(String posStr) {
        if (posStr == null || posStr.length() < 2) {
            throw new IllegalArgumentException("Неверный формат координат: " + posStr);
        }

        int col = posStr.charAt(0) - 'a';
        int row = Character.getNumericValue(posStr.charAt(1)) - 1;

        return new Position(col, row);
    }

    public String positionToString(Position pos) {
        return String.valueOf((char)('a' + pos.getX())) + (pos.getY() + 1);
    }

    public void sendMoveToClient(ChannelHandlerContext ctx, Move move) {
        String json = "{\"from\":\"" + move.getFrom() +
                "\",\"to\":\"" + move.getTo() +
                "\",\"piece\":\"" + move.getPiece() +
                "\"}";
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        ctx.writeAndFlush(response);
    }
}
