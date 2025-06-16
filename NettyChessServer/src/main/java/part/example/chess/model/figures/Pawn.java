package part.example.chess.model.figures;

import part.example.chess.model.genuine.Board;
import part.example.chess.model.genuine.Position;

public class Pawn extends Piece {

    public Pawn(String color, Position position) {
        super(color, position);
    }

    @Override
    public boolean isValidMove(Position target, Board board) {
        int dx = Math.abs(target.getX() - position.getX());
        int dy = target.getY() - position.getY();

        if (!color.equals("white") && !color.equals("black")) return false;

        boolean isWhite = color.equals("white");

        // Проверяем направление
        if (isWhite) {
            if (dy < 1) return false; // Не может двигаться вниз

            // Начальный ход на две клетки
            if (position.getY() == 1 && dy == 2 && dx == 0) {
                if (board.getPieceAt(new Position(position.getX(), position.getY() + 1)) != null) {
                    return false; // Путь заблокирован
                }
                return true;
            }

            if (dy != 1) return false; // Обычный ход — только на одну клетку
        } else {
            if (dy > -1) return false; // Не может двигаться вверх

            if (position.getY() == 6 && dy == -2 && dx == 0) {
                if (board.getPieceAt(new Position(position.getX(), position.getY() - 1)) != null) {
                    return false; // Путь заблокирован
                }
                return true;
            }

            if (dy != -1) return false; // Обычный ход — только на одну клетку
        }

        // Прямой ход
        if (dx == 0) {
            if (board.getPieceAt(target) != null) {
                return false; // Нельзя ходить через фигуру
            }
            return true;
        }

        // Диагональный ход (взятие)
        if (dx == 1) {
            if (board.getPieceAt(target) == null) {
                return false; // Нечего брать
            }
            return true;
        }

        return false;
    }


}
