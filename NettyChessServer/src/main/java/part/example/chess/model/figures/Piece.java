package part.example.chess.model.figures;

import part.example.chess.model.genuine.Board;
import part.example.chess.model.genuine.Position;

public abstract class Piece {
    protected String color;
    protected Position position;

    public Piece(String color, Position position) {
        this.color = color;
        this.position = position;
    }

    public String getColor() { return color; }
    public Position getPosition() { return position; }
    public void setPosition(Position position) { this.position = position; }

    public abstract boolean isValidMove(Position target, Board board);

    public String getType() {
        return getClass().getSimpleName().toLowerCase(); // pawn, rook, knight и т.д.
    }
}
