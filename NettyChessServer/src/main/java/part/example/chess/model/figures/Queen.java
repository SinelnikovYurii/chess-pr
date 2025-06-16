package part.example.chess.model.figures;

import part.example.chess.model.genuine.Board;
import part.example.chess.model.genuine.Position;

public class Queen extends Piece {
    public Queen(String color, Position position) {
        super(color, position);
    }

    @Override
    public boolean isValidMove(Position target, Board board) {
        int dx = Math.abs(target.getX() - position.getX());
        int dy = Math.abs(target.getY() - position.getY());

        if (dx == 0 || dy == 0 || dx == dy) {
            // Ход по прямой или диагонали
        } else {
            return false; // Недопустимый путь
        }

        // Проверка пути
        if (!isPathClear(board, target)) {
            return false;
        }

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

        int stepX = Integer.compare(toX, fromX);
        int stepY = Integer.compare(toY, fromY);

        int steps;

        if (stepX != 0 && stepY != 0) { // Диагональный ход
            steps = Math.abs(toX - fromX);
            for (int i = 1; i < steps; i++) {
                Position pos = new Position(fromX + i * stepX, fromY + i * stepY);
                if (!board.isEmpty(pos)) {
                    return false;
                }
            }
        } else if (stepX != 0) { // Горизонтальный ход
            steps = Math.abs(toX - fromX);
            for (int i = 1; i < steps; i++) {
                Position pos = new Position(fromX + i * stepX, fromY);
                if (!board.isEmpty(pos)) {
                    return false;
                }
            }
        } else if (stepY != 0) { // Вертикальный ход
            steps = Math.abs(toY - fromY);
            for (int i = 1; i < steps; i++) {
                Position pos = new Position(fromX, fromY + i * stepY);
                if (!board.isEmpty(pos)) {
                    return false;
                }
            }
        }

        return true;
    }


}
