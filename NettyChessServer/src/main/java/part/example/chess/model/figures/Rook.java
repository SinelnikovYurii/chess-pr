package part.example.chess.model.figures;

import part.example.chess.model.genuine.Board;
import part.example.chess.model.genuine.Position;

public class Rook extends Piece {
    public Rook(String color, Position position) {
        super(color, position);
    }

    @Override
    public boolean isValidMove(Position target, Board board) {
        int dx = target.getX() - position.getX();
        int dy = target.getY() - position.getY();

        // Проверяем, что ход либо по горизонтали, либо по вертикали
        if (dx != 0 && dy != 0) return false;

        // Проверяем путь
        if (!isPathClear(board,target)) return false;

        // Проверяем цвет фигуры на целевой позиции
        if (!board.isEmpty(target)) {
            Piece targetPiece = board.getPieceAt(target);
            if (targetPiece.getColor().equals(this.color)) return false;
        }

        return true;
    }

    private boolean isPathClear(Board board, Position target) {
        int x1 = Math.min(position.getX(), target.getX());
        int y1 = Math.min(position.getY(), target.getY());
        int x2 = Math.max(position.getX(), target.getX());
        int y2 = Math.max(position.getY(), target.getY());

        int dx = Integer.compare(x2, x1);
        int dy = Integer.compare(y2, y1);

        if (dx == 0) { // Вертикальный ход
            for (int i = 1; i < Math.abs(y2 - y1); i++) {
                Position pos = new Position(x1, y1 + i * dy);
                if (!board.isEmpty(pos)) {
                    return false;
                }
            }
        } else { // Горизонтальный ход
            for (int i = 1; i < Math.abs(x2 - x1); i++) {
                Position pos = new Position(x1 + i * dx, y1);
                if (!board.isEmpty(pos)) {
                    return false;
                }
            }
        }

        return true;
    }


}
