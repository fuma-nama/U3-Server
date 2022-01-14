package api.socket;

public enum RoomType {
    SELF_CHANNEL("Self"), VOICE("Voice"), TEXT("");

    private final String prefix;

    RoomType(String prefix) {
        this.prefix = prefix;
    }

    public String getRoom(int id) {
        return prefix + id;
    }
}
