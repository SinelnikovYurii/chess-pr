package part.example.chess.model.genuine;

import part.example.chess.model.figures.Piece;
import part.example.chess.model.figures.*;

import java.util.HashMap;
import java.util.Map;

public class Game {
    private String id;
    private Player whitePlayer;
    private Player blackPlayer;
    private Board board;
    private String turn = "white";
    private String status = "ongoing";


    public Player getWhitePlayer() {
        return whitePlayer;
    }

    public void setWhitePlayer(Player whitePlayer) {
        this.whitePlayer = whitePlayer;
    }

    public Player getBlackPlayer() {
        return blackPlayer;
    }

    public void setBlackPlayer(Player blackPlayer) {
        this.blackPlayer = blackPlayer;
    }

    public Game(String id, Player whitePlayer, Player blackPlayer) {
        this.id = id;
        this.whitePlayer = whitePlayer;
        this.blackPlayer = blackPlayer;
        this.board = new Board();


        initializePieces();
    }

    public String getTurn() {
        return turn;
    }

    public void setTurn(String turn) {
        this.turn = turn;
    }


    private void initializePieces() {
        // Белые фигуры
        board.addPiece(new Rook("white", new Position(0, 0)));
        board.addPiece(new Knight("white", new Position(1, 0)));
        board.addPiece(new Bishop("white", new Position(2, 0)));
        board.addPiece(new Queen("white", new Position(3, 0)));
        board.addPiece(new King("white", new Position(4, 0)));
        board.addPiece(new Bishop("white", new Position(5, 0)));
        board.addPiece(new Knight("white", new Position(6, 0)));
        board.addPiece(new Rook("white", new Position(7, 0)));

        for (int i = 0; i < 8; i++) {
            board.addPiece(new Pawn("white", new Position(i, 1)));
        }


        board.addPiece(new Rook("black", new Position(0, 7)));
        board.addPiece(new Knight("black", new Position(1, 7)));
        board.addPiece(new Bishop("black", new Position(2, 7)));
        board.addPiece(new Queen("black", new Position(3, 7)));
        board.addPiece(new King("black", new Position(4, 7)));
        board.addPiece(new Bishop("black", new Position(5, 7)));
        board.addPiece(new Knight("black", new Position(6, 7)));
        board.addPiece(new Rook("black", new Position(7, 7)));

        for (int i = 0; i < 8; i++) {
            board.addPiece(new Pawn("black", new Position(i, 6)));
        }
    }

    public void setBoard(Board board) {
        this.board = board;
    }

    public Board getBoard() {
        return board;
    }

    public String getId() {
        return id;
    }


    public Map<String, PieceInfo> getBoardState() {
        Map<String, PieceInfo> state = new HashMap<>();
        for (Map.Entry<Position, Piece> entry : board.getPieces().entrySet()) {
            Position pos = entry.getKey();
            Piece piece = entry.getValue();
            state.put(pos.getX() + "" + pos.getY(), new PieceInfo(piece.getType(), piece.getColor()));
        }
        return state;
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public static class PieceInfo {
        private final String type;
        private final String color;

        public PieceInfo(String type, String color) {
            this.type = type;
            this.color = color;
        }

        public String getType() { return type; }
        public String getColor() { return color; }
    }

}
