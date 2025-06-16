package part.example.chess.model.genuine;

public class Player {
    
    private String id;
    private String username;
    private String color;

    public Player(String id, String username, String color) {
        this.id = id;
        this.username = username;
        this.color = color;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
