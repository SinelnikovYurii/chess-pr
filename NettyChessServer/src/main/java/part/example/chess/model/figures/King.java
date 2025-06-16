package part.example.chess.model.figures;

import part.example.chess.model.genuine.Board;
import part.example.chess.model.genuine.Position;

public class King extends Piece {
    public King(String color, Position position) {
        super(color, position);
    }

    @Override
    public boolean isValidMove(Position target, Board board) {
        int dx = Math.abs(target.getX() - position.getX());
        int dy = Math.abs(target.getY() - position.getY());

        // Проверка: шаг не больше одного по всем осям
        if (dx > 1 || dy > 1) {
            return false;
        }

        // Проверка цвета фигуры на целевой позиции
        if (!board.isEmpty(target)) {
            Piece targetPiece = board.getPieceAt(target);
            if (targetPiece.getColor().equals(this.color)) {
                return false; // Нельзя брать свою фигуру
            }
        }

        return true;
    }

}
