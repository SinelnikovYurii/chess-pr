package part.example.chess.model.genuine;

import part.example.chess.model.figures.Piece;
import part.example.chess.model.figures.PieceInfo;

import java.util.HashMap;
import java.util.Map;

public class Board {
    private final Map<Position, Piece> pieces = new HashMap<>();

    public void addPiece(Piece piece) {
        pieces.put(piece.getPosition(), piece);
    }

    public Piece getPieceAt(Position position) {
        if (pieces.containsKey(position)) {
            return pieces.get(position);
        } else {

            return null;
        }
    }

    public boolean isEmpty(Position position) {
        return !pieces.containsKey(position);
    }

    public void undoMove(Piece piece, Position from) {
        pieces.remove(piece.getPosition());
        piece.setPosition(from);
        pieces.put(from, piece);
    }

    public Map<String, PieceInfo> getBoardStateForClient() {
        Map<String, PieceInfo> state = new HashMap<>();

        for (Map.Entry<Position, Piece> entry : getPieces().entrySet()) {
            Position pos = entry.getKey();
            Piece piece = entry.getValue();

            String key = pos.getX() + "" + pos.getY(); // Например: "04"
            state.put(key, new PieceInfo(piece.getType(), piece.getColor()));
        }

        return state;
    }

    public boolean movePiece(Piece piece, Position target) {
        if (!piece.isValidMove(target, this)) return false;

        if (!isEmpty(target)) {
            Piece targetPiece = getPieceAt(target);
            if (targetPiece.getColor().equals(piece.getColor())) return false;
        }

        Position oldPosition = new Position(piece.getPosition().getX(), piece.getPosition().getY());
        piece.setPosition(target);

        pieces.remove(oldPosition);
        pieces.put(target, piece);

        return true;
    }

    public synchronized Map<Position, Piece> getPieces() {
        return new HashMap<>(pieces); // ← Возвращаем копию
    }
}