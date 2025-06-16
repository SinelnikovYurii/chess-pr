package part.example.chess.model.figures;

import part.example.chess.model.genuine.Board;
import part.example.chess.model.genuine.Position;

public class Bishop extends Piece {
    public Bishop(String color, Position position) {
        super(color, position);
    }

    @Override
    public boolean isValidMove(Position target, Board board) {
        int dx = Math.abs(target.getX() - position.getX());
        int dy = Math.abs(target.getY() - position.getY());

        // Проверка: ход по диагонали
        if (dx != dy) return false;

        // Проверка: нет ли фигур на пути
        if (!isPathClear(board, target)) return false;

        // Проверка цвета фигуры на целевой позиции
        if (!board.isEmpty(target)) {
            Piece targetPiece = board.getPieceAt(target);
            if (targetPiece.getColor().equals(this.color)) {
                return false; // Нельзя брать свою же фигуру
            }
        }

        return true;
    }

    private boolean isPathClear(Board board, Position target) {
        int fromX = position.getX();
        int fromY = position.getY();
        int toX = target.getX();
        int toY = target.getY();

        // Проверяем, что ход по диагонали
        if (Math.abs(toX - fromX) != Math.abs(toY - fromY)) {
            return false;
        }

        // Определяем шаги по X и Y
        int stepX = Integer.compare(toX, fromX);
        int stepY = Integer.compare(toY, fromY);

        // Перебираем все клетки между начальной и целевой
        int steps = Math.abs(toX - fromX);
        for (int i = 1; i < steps; i++) {
            Position pos = new Position(fromX + i * stepX, fromY + i * stepY);
            if (!board.isEmpty(pos)) {
                return false;
            }
        }

        return true;
    }


}
