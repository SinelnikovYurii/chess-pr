package part.example.chess.model.figures;

public class PieceInfo {
    private final String type;
    private final String color;

    public PieceInfo(String type, String color) {
        this.type = type;
        this.color = color;
    }

    public String getType() {
        return type;
    }

    public String getColor() {
        return color;
    }
}
