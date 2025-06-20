package part.example.chess.model;

import part.example.chess.model.figures.PieceInfo;
import part.example.chess.model.genuine.Game;

import java.util.Map;

public class GameStatus {
    private final Map<String, PieceInfo> board;
    private final String turn;
    private final String status;

    public GameStatus(Map<String, PieceInfo> board, String turn, String status) {
        this.board = board;
        this.turn = turn;
        this.status = status;
    }

    public Map<String, PieceInfo> getBoard() { return board; }
    public String getTurn() { return turn; }
    public String getStatus() { return status; }
}
