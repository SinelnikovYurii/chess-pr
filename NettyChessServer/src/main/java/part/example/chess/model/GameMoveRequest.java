package part.example.chess.model;

import part.example.chess.model.genuine.Move;
import part.example.chess.model.genuine.Player;

public class GameMoveRequest {
    private String gameId;
    private Move move;
    private Player player;

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }
    // Getter'ы и Setter'ы
    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }

    public Move getMove() { return move; }
    public void setMove(Move move) { this.move = move; }
}
