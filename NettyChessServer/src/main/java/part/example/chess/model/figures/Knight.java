package part.example.chess.model.figures;

import part.example.chess.model.genuine.Board;
import part.example.chess.model.genuine.Position;

public class Knight extends Piece {
    public Knight(String color, Position position) {
        super(color, position);
    }

    @Override
    public boolean isValidMove(Position target, Board board) {
        int dx = Math.abs(target.getX() - position.getX());
        int dy = Math.abs(target.getY() - position.getY());

        if ((dx == 2 && dy == 1) || (dx == 1 && dy == 2)) {
            if (board.isEmpty(target)) {
                return true;
            } else {
                Piece targetPiece = board.getPieceAt(target);
                return !targetPiece.getColor().equals(this.color); // Можно взять фигуру противника
            }
        }
        return false;
    }


}
