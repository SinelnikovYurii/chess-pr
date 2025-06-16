package part.example.chess.model.genuine;

public class Move {

    private String from;
    private String to;
    private String piece;


    public Move() {}

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getPiece() {
        return piece;
    }

    public void setPiece(String piece) {
        this.piece = piece;
    }

    public Move(String from, String to, String piece) {
        this.from = from;
        this.to = to;
        this.piece = piece;

    }


}
