package part.example.chess.DTO;

import part.example.chess.model.genuine.Move;

public class GameMoveRequest {
    private String gameId;
    private Move move;

    // Геттеры и сеттеры
    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }
    public Move getMove() { return move; }
    public void setMove(Move move) { this.move = move; }
}
