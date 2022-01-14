package api.socket;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.BroadcastOperations;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import database.AccountManager;
import database.GroupManager;

import static api.socket.SocketService.sendGroupEvent;
import static api.socket.SocketUtil.*;

public class SocketHandler {
    public static final String
            MESSAGE_EVENT = "message",
            MESSAGE_UPDATED_EVENT = "message_update",
            MESSAGE_DELETE_EVENT = "message_delete",
            TOKEN = "token",
            USER_ID = "user_id",
            READING_GROUP = "reading_group",
            JOIN_GROUP_EVENT = "join_group",
            LEAVE_GROUP_EVENT = "leave_group",
            USER_PROFILE_UPDATE = "user_update",
            GROUP_PROFILE_UPDATE = "group_update",
            EMOJI_SAVED_EVENT = "emoji_saved",
            EMOJI_REMOVED_EVENT = "emoji_removed",
            UPDATE_READING_GROUP = "update_reading_group",
            REMOVE_READING_GROUP = "remove_reading_group",
            GROUP_INVITE_ADDED_EVENT = "group_invite_add",
            GROUP_INVITE_REMOVED_EVENT = "group_invite_remove",
            FRIEND_INVITE_ADDED_EVENT = "friend_invite_add",
            FRIEND_INVITE_REMOVED_EVENT = "friend_invite_remove",
            FRIEND_ADDED_EVENT = "friend_add",
            FRIEND_REMOVED_EVENT = "friend_remove",
            VOICE_EVENT = "voice",
            JOIN_VOICE_EVENT = "join_voice",
            LEAVE_VOICE_EVENT = "leave_voice";

    @OnConnect
    public void onConnect(SocketIOClient client) {
        String token = getToken(client);
        int userID = AccountManager.getUserID(token);

        client.set(TOKEN, token);
        client.set(USER_ID, userID);
        client.joinRoom(RoomType.SELF_CHANNEL.getRoom(userID)); //Join user update event channel

        for (int groupID : GroupManager.getJoinedGroup(userID, true)) {
            client.joinRoom(RoomType.TEXT.getRoom(groupID));
        }
    }

    @OnEvent(UPDATE_READING_GROUP)
    public void onUpdateReadingGroup(SocketIOClient client, int groupID, AckRequest ackRequest) {
        Integer readingGroup = client.get(READING_GROUP), userID = client.get(USER_ID);
        if (userID == null) return;

        if (readingGroup != null) {
            GroupManager.updateLastCheck(readingGroup, userID);
        }

        client.set(READING_GROUP, groupID);
    }

    @OnEvent(REMOVE_READING_GROUP)
    public void onRemoveReadingGroup(SocketIOClient client, AckRequest ackRequest) {
        Integer readingGroup = client.get(READING_GROUP), userID = client.get(USER_ID);
        if (userID == null) return;

        if (readingGroup != null) {
            GroupManager.updateLastCheck(readingGroup, userID);
        }

        client.del(READING_GROUP);
    }

    @OnEvent(LEAVE_GROUP_EVENT)
    public void onLeaveGroup(SocketIOClient client, int groupID, AckRequest ackRequest) {
        GroupManager.leaveGroup(client.get(USER_ID), groupID);

        client.leaveRoom(RoomType.TEXT.getRoom(groupID));
        getPrivateRoom(client).sendEvent(LEAVE_GROUP_EVENT, groupID);
    }

    @OnEvent(JOIN_VOICE_EVENT)
    public void joinVoiceCall(SocketIOClient client, int groupID, AckRequest ackRequest) {
        int userID = client.get(USER_ID);

        if (GroupManager.hasJoined(groupID, userID)) {
            String room = RoomType.VOICE.getRoom(groupID);

            client.joinRoom(room);

            sendGroupEvent(groupID, JOIN_VOICE_EVENT, groupID, userID);
        }
    }

    @OnEvent(VOICE_EVENT)
    public void sendAudio(SocketIOClient client, int groupID, byte[] data, int bufferSize, AckRequest ackRequest) {
        String room = RoomType.VOICE.getRoom(groupID);

        if (hasJoinedRoom(client, room))
            getRoom(room).sendEvent(VOICE_EVENT, data, bufferSize);
    }

    @OnEvent(LEAVE_VOICE_EVENT)
    public void leaveVoiceCall(SocketIOClient client, int groupID, AckRequest ackRequest) {
        int userID = client.get(USER_ID);

        String room = RoomType.VOICE.getRoom(groupID);

        if (hasJoinedRoom(client, room)) {
            client.leaveRoom(room);

            sendGroupEvent(groupID, LEAVE_VOICE_EVENT, groupID, userID);
        }
    }

    public static BroadcastOperations onJoinedGroup(int userID, int groupID) {
        BroadcastOperations room = getPrivateRoom(userID);

        for (SocketIOClient client : room.getClients()) {
            client.joinRoom(RoomType.TEXT.getRoom(groupID));
        }
        room.sendEvent(JOIN_GROUP_EVENT, groupID);

        return room;
    }

    public static BroadcastOperations onJoinedPrivateGroup(int userID, int groupID) {
        BroadcastOperations room = getPrivateRoom(userID);

        for (SocketIOClient client : room.getClients()) {
            client.joinRoom(RoomType.TEXT.getRoom(groupID));
        }

        return room;
    }
}