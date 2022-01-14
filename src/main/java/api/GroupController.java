package api;

import api.model.Group;
import api.model.GroupInvite;
import api.model.TextMessage;
import api.socket.RoomType;
import api.socket.SocketHandler;
import api.socket.SocketService;
import api.socket.SocketUtil;
import com.corundumstudio.socketio.BroadcastOperations;
import com.fasterxml.jackson.core.JsonProcessingException;
import database.AccountManager;
import database.GroupManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;
import org.jsoup.safety.Safelist;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;
import test.generated.tables.records.TextmessageRecord;
import util.FileUtil;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static api.APIController.*;
import static api.socket.SocketHandler.*;
import static api.socket.SocketService.server;
import static util.CommonUtil.*;

@RestController
public class GroupController {
    @GetMapping("/group/join")
    public List<Integer> getJoinedGroup(@RequestHeader("token") String token) {
        return GroupManager.getJoinedGroup(AccountManager.getUserID(token));
    }

    @PostMapping("/group/join")
    public ResponseEntity<String> JoinGroup(@RequestHeader("token") String token,
                                            @RequestParam(value = "code", required = false) String code,
                                            @RequestParam(value = "id", required = false) Integer groupID,
                                            @RequestParam(value = "sender", required = false) Integer senderID) {
        int userID = AccountManager.getUserID(token);

        return code == null? JoinGroupFromInvite(userID, groupID, senderID) : JoinGroup(userID, code);
    }

    public ResponseEntity<String> JoinGroup(int userID, String code) {
        int groupID = GroupManager.getInviteCodeGroupID(code);

        if (GroupManager.joinGroup(userID, groupID)) {
            SocketHandler.onJoinedGroup(userID, groupID);
            return OK_REQUEST;
        }
        else return BAD_REQUEST;
    }

    public ResponseEntity<String> JoinGroupFromInvite(int userID, int groupID, int senderID) {
        if (GroupManager.deleteInvite(groupID, userID, senderID) && GroupManager.joinGroup(userID, groupID)) {

            BroadcastOperations privateRoom = SocketHandler.onJoinedGroup(userID, groupID);

            SocketService.sendPrivateEvent(privateRoom, GROUP_INVITE_REMOVED_EVENT, groupID, senderID);

            return OK_REQUEST;
        }
        else return BAD_REQUEST;
    }

    @GetMapping("/group/member")
    public ResponseEntity<List<Integer>> getGroupMembers(@RequestHeader("token") String token, @RequestParam("id") int groupID) {
        if (GroupManager.hasJoined(groupID, AccountManager.getUserID(token)))
            return ResponseEntity.ok(GroupManager.getGroupMembers(groupID));
        else return ResponseEntity.badRequest().build();
    }

    @GetMapping("/group/messages")
    public ResponseEntity<String> getGroupMessages(@RequestHeader("token") String token,
                                                   @RequestParam("id") int groupID,
                                                   @RequestParam("offset") int offset) throws JsonProcessingException {
        if (GroupManager.hasJoined(groupID, AccountManager.getUserID(token)))
            return ResponseEntity.ok(mapper.writeValueAsString(GroupManager.getGroupMessages(groupID, 30, offset)));
        else return BAD_REQUEST;
    }

    @GetMapping("/group/messages/unread")
    public int getUnreadCount(@RequestHeader("token") String token, @RequestParam("id") int groupID) {
        int userID = AccountManager.getUserID(token);

        if (!SocketService.hasClientReadingGroup(groupID, userID)) {
            LocalDateTime time = GroupManager.getLastCheckTime(groupID, userID);

            if (time != null)
                return GroupManager.getMessageCountAfter(groupID, time);
        }

        return 0;
    }

    @PostMapping("/group/messages")
    public ResponseEntity<String> sendMessage(@RequestHeader("token") String token,
                                              @RequestParam("id") int groupID,
                                              @RequestParam("context") String context,
                                              @RequestParam("html") boolean isHTML,
                                              @RequestParam(value = "reply", required = false) Integer replyTo,
                                              HttpServletRequest request) throws IOException {
        if (!isValidContext(context)) return BAD_REQUEST;

        System.out.println("A");

        if (!isHTML) {
            context = removeHTML(context);
            if (context.isBlank()) return BAD_REQUEST;
        }

        int senderID = AccountManager.getUserID(token);

        if (GroupManager.hasJoined(groupID, senderID)) {
            System.out.println("B");
            List<String> files = new ArrayList<>();

            //Store message files
            if (request instanceof StandardMultipartHttpServletRequest fileRequest) {

                for (List<MultipartFile> parts : fileRequest.getMultiFileMap().values()) {
                    for (MultipartFile filePart : parts) {
                        String fileUrl = FileUtil.saveFile(groupID, filePart.getOriginalFilename(), FileUtil.MESSAGE_FILE, filePart);
                        files.add(fileUrl);
                    }
                }
            }

            TextmessageRecord record =
                    replyTo == null?
                            GroupManager.storeMessage(groupID, senderID, context, files, isHTML)
                            :
                            GroupManager.storeMessage(groupID, senderID, context, files, replyTo, isHTML);

            SocketService.sendGroupEvent(groupID,
                    MESSAGE_EVENT,
                    mapper.writeValueAsString(TextMessage.fromRecord(record))
            );

            return OK_REQUEST;
        } else return BAD_REQUEST;
    }

    @PutMapping("/group/messages")
    public ResponseEntity<String> editMessage(@RequestHeader("token") String token,
                                              @RequestParam("id") int messageID,
                                              @RequestParam("context") String context) {
        if (isValidContext(context)) {
            int userID = AccountManager.getUserID(token);

            //MySQL doesn't support {update ... returning} so we need to fetch it before using update
            TextmessageRecord record = GroupManager.getMessage(messageID, userID);

            if (record != null) {
                if (!toBoolean(record.getIshtml())) {
                    context = removeHTML(context);
                }

                GroupManager.updateMessage(messageID, context);

                SocketService.sendGroupEvent(record.getGroupid(),
                        MESSAGE_UPDATED_EVENT,
                        record.getGroupid(),
                        messageID,
                        context
                );
                return OK_REQUEST;
            }
        }

        return BAD_REQUEST;
    }

    private static String removeHTML(String s) {
        return Jsoup.clean(s, "", Safelist.none(), new Document.OutputSettings().prettyPrint(false).escapeMode(Entities.EscapeMode.xhtml));
    }

    @DeleteMapping("/group/messages")
    public ResponseEntity<String> deleteMessage(@RequestHeader("token") String token, @RequestParam("id") int messageID) throws IOException {
        int userID = AccountManager.getUserID(token);
        TextmessageRecord message = GroupManager.getMessage(messageID);

        if (message != null && (GroupManager.isOwnerOf(message.getGroupid(), userID) || message.getSenderid() == userID)) {

            GroupManager.deleteMessage(messageID);

            server.getRoomOperations(String.valueOf(message.getGroupid())).sendEvent(MESSAGE_DELETE_EVENT,
                    message.getGroupid(),
                    messageID
            );

            for (String fileUrl : mapper.readValue(message.getFile().data(), String[].class)) {
                FileUtil.deleteFile(fileUrl);
            }
            return OK_REQUEST;
        } else return BAD_REQUEST;
    }

    @GetMapping("/group/voice")
    public Integer[] getVoiceCallMembers(@RequestParam("id") int groupID) {
        return SocketUtil.getRoom(RoomType.VOICE, groupID)
                .getClients()
                .stream()
                .map(client-> (Integer) client.get(USER_ID))
                .filter(Objects::nonNull)
                .toArray(Integer[]::new);
    }

    @GetMapping("/group")
    public Group getGroupProfile(@RequestParam("id") Integer groupID) {
        return GroupManager.getGroup(groupID);
    }

    @PostMapping("/group")
    public ResponseEntity<String> createGroup(@RequestHeader("token") String token,
                               @RequestParam("name") String name,
                               @RequestParam("detail") String description,
                               @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {

        if (notValidName(name) && !isValidContext(description, true)) return BAD_REQUEST;

        int userID = AccountManager.getUserID(token),
                groupID = GroupManager.createGroup(userID, name, description);

        if (file != null) {
            String iconUrl = FileUtil.saveFileByID(groupID, FileUtil.SERVER_ICON, file);
            GroupManager.updateGroupIcon(groupID, iconUrl);
        }

        GroupManager.joinGroup(userID, groupID);

        SocketHandler.onJoinedGroup(userID, groupID);

        return OK_REQUEST;
    }

    @PutMapping("/group")
    public ResponseEntity<String> updateGroup(@RequestHeader("token") String token,
                               @RequestParam("id") int groupID,
                               @RequestParam("name") String name,
                               @RequestParam("detail") String description,
                               @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {

        if (notValidName(name) && !isValidContext(description, true)) return BAD_REQUEST;

        int userID = AccountManager.getUserID(token);

        String iconUrl = null;
        boolean updated;

        if (file != null) {
            iconUrl = FileUtil.saveFileByID(groupID, FileUtil.SERVER_ICON, file);
            updated = GroupManager.updateGroup(groupID, userID, name, description, iconUrl);
        } else
            updated = GroupManager.updateGroup(groupID, userID, name, description);

        if (updated) {
            SocketService.sendGroupEvent(groupID, GROUP_PROFILE_UPDATE, groupID, name, description, iconUrl);
            return OK_REQUEST;
        } else
            return BAD_REQUEST;
    }

    @GetMapping("/group/invite/code")
    public ResponseEntity<String> getInviteCode(@RequestParam("id") int groupID,
                                                   @RequestHeader("token") String token) {
        if (GroupManager.getGroup(groupID).creatorID() == AccountManager.getUserID(token)) {
            String code = GroupManager.getGroupInviteCode(groupID);

            if (code == null) {
                code = GroupManager.createGroupInviteCode(groupID);
            }

            return ResponseEntity.ok(code);
        }
        else {
            return BAD_REQUEST;
        }
    }

    @PostMapping("/group/invite/code")
    public ResponseEntity<String> updateInviteCode(@RequestParam("id") int groupID,
                                                @RequestHeader("token") String token) {
        if (GroupManager.getGroup(groupID).creatorID() == AccountManager.getUserID(token)) {
            String code = GroupManager.updateGroupInviteCode(groupID);

            return ResponseEntity.ok(code);
        }
        else {
            return BAD_REQUEST;
        }
    }

    @GetMapping("/group/invite")
    public List<GroupInvite> getInvites(@RequestHeader("token") String token) {
        int senderID = AccountManager.getUserID(token);

        return GroupManager.getUserInvites(senderID);
    }

    @PostMapping("/group/invite")
    public ResponseEntity<String> sendInvite(@RequestHeader("token") String token,
                                             @RequestParam("id") int groupID,
                                             @RequestParam("target") int targetID) {
        int senderID = AccountManager.getUserID(token);

        if (GroupManager.getGroup(groupID).creatorID() == senderID) {
            GroupManager.sendInviteTo(groupID, senderID, targetID);

            SocketService.sendPrivateEvent(targetID, GROUP_INVITE_ADDED_EVENT, new GroupInvite(groupID, senderID));
            return OK_REQUEST;
        }
        else {
            return BAD_REQUEST;
        }
    }

    @DeleteMapping("/group/invite")
    @ResponseStatus(HttpStatus.OK)
    public void removeInvite(@RequestHeader("token") String token,
                             @RequestParam("id") int groupID,
                             @RequestParam("sender") int senderID) {
        int userID = AccountManager.getUserID(token);

        GroupManager.removeUserInvite(groupID, senderID, userID);

        SocketService.sendPrivateEvent(userID, GROUP_INVITE_REMOVED_EVENT, new GroupInvite(groupID, senderID));
    }
}
