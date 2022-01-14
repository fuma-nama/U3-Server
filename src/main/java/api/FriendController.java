package api;

import api.model.Friend;
import api.model.FriendInvite;
import api.socket.SocketHandler;
import api.socket.SocketService;
import database.AccountManager;
import database.FriendManager;
import database.GroupManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static api.APIController.BAD_REQUEST;
import static api.APIController.OK_REQUEST;
import static api.socket.SocketHandler.*;

@RestController
public class FriendController {
    @GetMapping("/friend")
    public List<Friend> getFriends(@RequestHeader("token") String token) {
        return FriendManager.getFriends(AccountManager.getUserID(token));
    }

    @DeleteMapping("/friend")
    public ResponseEntity<String> removeFriend(@RequestHeader("token") String token, @RequestParam("id") int friendID) {
        int userID = AccountManager.getUserID(token);

        boolean success = FriendManager.removeFriend(userID, friendID);

        if (success) {
            SocketService.sendPrivateEvent(userID, FRIEND_REMOVED_EVENT, friendID);
            SocketService.sendPrivateEvent(friendID, FRIEND_REMOVED_EVENT, userID);
            return OK_REQUEST;
        } else
            return BAD_REQUEST;
    }

    @GetMapping("/friend/invite")
    public List<FriendInvite> getFriendInvites(@RequestHeader("token") String token) {
        return FriendManager.getFriendInvites(AccountManager.getUserID(token));
    }

    @PostMapping("/friend/invite")
    public ResponseEntity<String> sendFriendInvite(@RequestHeader("token") String token, @RequestParam("target") int targetID) {
        int userID = AccountManager.getUserID(token);

        //If target has already sent invite to user, agree his invite
        if (FriendManager.hasInvite(targetID, userID)) {

            return agreeFriendInvite(userID, targetID);
        } else if (targetID != userID && !FriendManager.isFriend(userID, targetID)) {
            FriendInvite invite = FriendManager.addFriendInvite(userID, targetID);

            if (invite != null) {
                SocketService.sendPrivateEvent(targetID, FRIEND_INVITE_ADDED_EVENT, invite);
            }

            return OK_REQUEST;
        }

        return BAD_REQUEST;
    }

    @PostMapping("/friend/invite/agree")
    public ResponseEntity<String> agreeFriendInvite(@RequestHeader("token") String token, @RequestParam("sender") int senderID) {
        int userID = AccountManager.getUserID(token);

        return agreeFriendInvite(userID, senderID);
    }

    public ResponseEntity<String> agreeFriendInvite(int userID, int senderID) {
        if (FriendManager.removeFriendInvite(senderID, userID)) {
            int groupID = GroupManager.createPrivateGroup();

            joinPrivateGroup(groupID, userID, senderID);

            FriendManager.addFriend(userID, senderID, groupID);

            SocketService.sendPrivateEvent(userID, FRIEND_ADDED_EVENT, new Friend(senderID, groupID));

            SocketService.sendPrivateEvent(senderID, FRIEND_ADDED_EVENT, new Friend(userID, groupID));

            return OK_REQUEST;
        } else
            return BAD_REQUEST;
    }

    private static void joinPrivateGroup(int groupID, int... userIDs) {
        for (int userID : userIDs) {
            GroupManager.joinGroup(userID, groupID, true);
            SocketHandler.onJoinedPrivateGroup(userID, groupID);
        }
    }

    @DeleteMapping("/friend/invite")
    public ResponseEntity<String> removeFriendInvite(@RequestHeader("token") String token, @RequestParam("sender") int senderID) {
        int userID = AccountManager.getUserID(token);

        if (FriendManager.removeFriendInvite(userID, senderID)) {

            SocketService.sendPrivateEvent(userID, FRIEND_INVITE_REMOVED_EVENT, senderID);

            return OK_REQUEST;
        } else
            return BAD_REQUEST;
    }
}
