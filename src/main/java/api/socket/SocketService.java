package api.socket;

import com.corundumstudio.socketio.BroadcastOperations;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import database.AccountManager;

import static api.socket.SocketHandler.READING_GROUP;
import static api.socket.SocketUtil.getPrivateRoom;
import static api.socket.SocketUtil.getToken;

public class SocketService {
    public static SocketIOServer server;

    public static void init() {
        System.out.println("Loading Socket");
        Configuration config = new Configuration();
        config.setHostname("localhost");
        config.setPort(9092);
        config.setAuthorizationListener(data-> AccountManager.isTokenExists(getToken(data)));

        server = new SocketIOServer(config);
        server.addListeners(new SocketHandler());
        server.start();
    }

    public static void sendGroupEvent(int groupID, String event, Object... data) {
        server.getRoomOperations(RoomType.TEXT.getRoom(groupID)).sendEvent(event, data);
    }

    public static void sendPrivateEvent(int userID, String event, Object... data) {
        getPrivateRoom(userID).sendEvent(event, data);
    }

    public static void sendPrivateEvent(BroadcastOperations room, String event, Object... data) {
        room.sendEvent(event, data);
    }

    public static boolean hasClientReadingGroup(int groupID, int userID) {
        for (SocketIOClient client : getPrivateRoom(userID).getClients()) {
            Integer readingGroup = client.get(READING_GROUP);
            if (readingGroup != null && readingGroup == groupID)
                return true;
        }
        return false;
    }
}