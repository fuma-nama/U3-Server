package api.socket;

import com.corundumstudio.socketio.BroadcastOperations;
import com.corundumstudio.socketio.HandshakeData;
import com.corundumstudio.socketio.SocketIOClient;

import static api.socket.SocketHandler.TOKEN;
import static api.socket.SocketHandler.USER_ID;
import static api.socket.SocketService.server;

public class SocketUtil {
    public static String getToken(SocketIOClient client) {
        return getToken(client.getHandshakeData());
    }

    public static String getToken(HandshakeData data) {
        return data.getSingleUrlParam("token");
    }

    public static BroadcastOperations getPrivateRoom(int userID) {
        return getRoom(RoomType.SELF_CHANNEL, userID);
    }

    public static BroadcastOperations getPrivateRoom(SocketIOClient client) {
        return getPrivateRoom((int) client.get(USER_ID));
    }

    public static BroadcastOperations getRoom(RoomType type, int id) {
        return getRoom(type.getRoom(id));
    }

    public static BroadcastOperations getRoom(String name) {
        return server.getRoomOperations(name);
    }

    public static boolean hasJoinedRoom(SocketIOClient client, String room) {
        return client.getAllRooms().contains(room);
    }
}