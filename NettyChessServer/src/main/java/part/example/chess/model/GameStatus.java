package part.example.chess.model;

import part.example.chess.model.genuine.Game;

import java.util.Map;

public class GameStatus {
    private final Map<String, Game.PieceInfo> board;
    private final String turn;
    private final String status;

    public GameStatus(Map<String, Game.PieceInfo> board, String turn, String status) {
        this.board = board;
        this.turn = turn;
        this.status = status;
    }

    public Map<String, Game.PieceInfo> getBoard() { return board; }
    public String getTurn() { return turn; }
    public String getStatus() { return status; }
}
