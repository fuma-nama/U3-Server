package database;

import api.model.Group;
import api.model.GroupInvite;
import api.model.TextMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.Condition;
import org.jooq.JSON;
import org.jooq.impl.DSL;
import org.springframework.lang.Nullable;
import test.generated.tables.records.GroupdataRecord;
import test.generated.tables.records.InvitecodeRecord;
import test.generated.tables.records.JoingrouplogRecord;
import test.generated.tables.records.TextmessageRecord;
import util.CommonUtil;

import java.time.LocalDateTime;
import java.util.List;

import static api.APIController.mapper;
import static database.AccountManager.FALSE;
import static database.AccountManager.TRUE;
import static org.jooq.impl.DSL.*;
import static test.generated.Tables.*;
import static util.CommonUtil.hasUpdated;

public class GroupManager {
    /**Join user to the group**/
    public static boolean joinGroup(int userID, int groupID) {
        return joinGroup(userID, groupID, false);
    }

    /**Join user to the group**/
    public static boolean joinGroup(int userID, int groupID, boolean isGroupPrivate) {
        return hasUpdated(SQLService.getService(s ->
                s.insertInto(JOINGROUPLOG, JOINGROUPLOG.USERID, JOINGROUPLOG.GROUPID, JOINGROUPLOG.ISGROUPPRIVATE)
                        .values(userID, groupID, toByte(isGroupPrivate))
                        .onDuplicateKeyIgnore()
                        .execute()
        ));
    }

    public static void leaveGroup(int userID, int groupID) {
        SQLService.getService(s ->
                s.delete(JOINGROUPLOG)
                        .where(JOINGROUPLOG.USERID.eq(userID), JOINGROUPLOG.GROUPID.eq(groupID))
                        .execute()
        );
    }

    public static void deleteGroup(int groupID, int creatorID) {
        boolean deleted = hasUpdated(
                SQLService.getService(s ->
                        s.delete(GROUPDATA)
                                .where(GROUPDATA.ID.eq(groupID), GROUPDATA.CREATORID.eq(creatorID))
                                .execute()
                )
        );

        if (deleted) //Remove all members If the group has been deleted
            SQLService.getService(s ->
                    s.delete(JOINGROUPLOG)
                            .where(JOINGROUPLOG.GROUPID.eq(groupID))
                            .execute()
            );
    }

    public static void deleteGroup(int groupID) {
        boolean deleted = hasUpdated(
                SQLService.getService(s ->
                        s.delete(GROUPDATA)
                                .where(GROUPDATA.ID.eq(groupID))
                                .execute()
                )
        );

        if (deleted) //Remove all members If the group has been deleted
            SQLService.getService(s ->
                    s.delete(JOINGROUPLOG)
                            .where(JOINGROUPLOG.GROUPID.eq(groupID))
                            .execute()
            );
    }

    public static boolean hasJoined(int groupID, int userID) {
        return SQLService.getService(s ->
                s.fetchExists(JOINGROUPLOG,
                        JOINGROUPLOG.GROUPID.eq(groupID),
                        JOINGROUPLOG.USERID.eq(userID)
                ));
    }

    public static void updateLastCheck(int groupID, int userID) {
        SQLService.getService(s ->
                s.update(JOINGROUPLOG)
                        .set(JOINGROUPLOG.LASTCHECKTIME, currentLocalDateTime())
                        .where(JOINGROUPLOG.GROUPID.eq(groupID), JOINGROUPLOG.USERID.eq(userID))
                        .execute()
        );
    }

    /**@return The time of user's last check, Null if row doesn't exist**/
    public static LocalDateTime getLastCheckTime(int groupID, int userID) {
        JoingrouplogRecord record = SQLService.getService(s ->
                s.fetchOne(JOINGROUPLOG,
                        JOINGROUPLOG.GROUPID.eq(groupID),
                        JOINGROUPLOG.USERID.eq(userID)
                )
        );

        return record == null? null : record.getLastchecktime();
    }

    public static int getMessageCountAfter(int groupID, LocalDateTime time) {
        return SQLService.getService(s->
                s.fetchCount(TEXTMESSAGE, TEXTMESSAGE.GROUPID.eq(groupID), TEXTMESSAGE.DATE.greaterThan(time))
        );
    }

    public static boolean isOwnerOf(int groupID, int userID) {
        return SQLService.getService(s->
                s.fetchExists(GROUPDATA, GROUPDATA.ID.eq(groupID), GROUPDATA.CREATORID.eq(userID))
        );
    }

    @Nullable
    public static TextmessageRecord getMessage(int messageID) {
        return SQLService.getService(s->
                s.fetchOne(TEXTMESSAGE, TEXTMESSAGE.MESSAGEID.eq(messageID))
        );
    }

    @Nullable
    public static void updateMessage(int messageID, String newContext) {
        SQLService.getService(s->
                s.update(TEXTMESSAGE)
                        .set(TEXTMESSAGE.CONTEXT, newContext)
                        .set(TEXTMESSAGE.EDITED, TRUE)
                        .where(TEXTMESSAGE.MESSAGEID.eq(messageID))
                        .execute()
        );
    }

    public static void deleteMessage(int messageID) {
        SQLService.getService(s->
                s.delete(TEXTMESSAGE)
                        .where(TEXTMESSAGE.MESSAGEID.eq(messageID))
                        .execute()
        );
    }

    /**
     * Create a new group
     * @return ID of the new group
     * **/
    public static int createGroup(int creatorID, String name, String description) {
        return SQLService.getService(s ->
                s.insertInto(GROUPDATA, GROUPDATA.CREATORID, GROUPDATA.NAME, GROUPDATA.DESCRIPTION, GROUPDATA.ISPRIVATE)
                        .values(creatorID, name, description, FALSE)
                        .returningResult(GROUPDATA.ID)
                        .fetchOne()
        ).getValue(GROUPDATA.ID);
    }

    /**
     * Create a new private group
     * @apiNote Private group has no profile
     * @return ID of the new group
     * **/
    public static int createPrivateGroup() {
        return SQLService.getService(s ->
                s.insertInto(GROUPDATA, GROUPDATA.ISPRIVATE)
                        .values(TRUE)
                        .returningResult(GROUPDATA.ID)
                        .fetchOne()
        ).getValue(GROUPDATA.ID);
    }

    public static boolean updateGroup(int groupID, int creatorID, String name, String description) {
        return hasUpdated(SQLService.getService(s ->
                s.update(GROUPDATA)
                        .set(GROUPDATA.NAME, name)
                        .set(GROUPDATA.DESCRIPTION, description)
                        .where(GROUPDATA.ID.eq(groupID), GROUPDATA.CREATORID.eq(creatorID))
                        .execute()
        ));
    }

    public static boolean updateGroup(int groupID, int creatorID, String name, String description, String iconUrl) {
        return hasUpdated(SQLService.getService(s ->
                s.update(GROUPDATA)
                        .set(GROUPDATA.NAME, name)
                        .set(GROUPDATA.DESCRIPTION, description)
                        .set(GROUPDATA.AVATAR, iconUrl)
                        .where(GROUPDATA.ID.eq(groupID), GROUPDATA.CREATORID.eq(creatorID))
                        .execute()
        ));
    }

    /**Get joined groups of user
     * @return A list of group IDs**/
    public static List<Integer> getJoinedGroup(int userID) {
        return getJoinedGroup(userID, false);
    }

    /**Get joined groups of user
     * @return A list of group IDs**/
    public static List<Integer> getJoinedGroup(int userID, boolean includePrivate) {
        Condition[] conditions;

        if (includePrivate) {
            conditions = new Condition[] {
                    JOINGROUPLOG.USERID.eq(userID)
            };
        } else
            conditions = new Condition[] {
                    JOINGROUPLOG.USERID.eq(userID),
                    JOINGROUPLOG.ISGROUPPRIVATE.eq(FALSE)
        };

        return SQLService.getService(s ->
                s.selectFrom(JOINGROUPLOG)
                        .where(conditions)
                        .orderBy(JOINGROUPLOG.LASTCHECKTIME)
                        .fetch()
                        .getValues(JOINGROUPLOG.GROUPID)
        );
    }

    /**Get group members
     * @return A list of user IDs**/
    public static List<Integer> getGroupMembers(int groupID) {
        return SQLService.getService(s ->
                s.fetch(JOINGROUPLOG, JOINGROUPLOG.GROUPID.eq(groupID))
                        .getValues(JOINGROUPLOG.USERID)
        );
    }

    public static List<TextMessage> getGroupMessages(int groupID, int limit, int offset) {
        return SQLService.getService(s ->
                s.select().from(TEXTMESSAGE)
                        .where(TEXTMESSAGE.GROUPID.eq(groupID))
                        .orderBy(TEXTMESSAGE.DATE.desc())
                        .offset(offset)
                        .limit(limit)
                        .fetch(TextMessage::fromRecord)
        );
    }

    /**
     * Get Group Details
     * @throws NullPointerException If Group doesn't exist or the group is private
     * **/
    public static Group getGroup(int groupID) throws NullPointerException {
        GroupdataRecord record = SQLService.getService(s ->
                s.fetchOne(GROUPDATA, GROUPDATA.ID.eq(groupID), GROUPDATA.ISPRIVATE.eq(FALSE))
        );

        if (record == null) throw new NullPointerException("Group doesn't exist");

        return new Group(record.getCreatorid(), record.getAvatar(), record.getName(), record.getDescription());
    }

    public static void updateGroupIcon(int groupID, String avatarUrl) {
        SQLService.getService(s ->
            s.update(GROUPDATA)
                    .set(GROUPDATA.AVATAR, avatarUrl)
                    .where(GROUPDATA.ID.eq(groupID))
                    .execute()
        );
    }

    /**@return New invite code**/
    public static String createGroupInviteCode(int groupID) {
        String code = CommonUtil.randomString(15);

        SQLService.getService(s ->
                s.insertInto(INVITECODE, INVITECODE.GROUPID, INVITECODE.CODE)
                        .values(groupID, code)
                        .onDuplicateKeyUpdate()
                        .set(INVITECODE.CODE, code)
                        .execute()
        );

        return code;
    }

    public static String updateGroupInviteCode(int groupID) {
        String code = CommonUtil.randomString(15);

        SQLService.getService(s ->
                s.update(INVITECODE)
                        .set(INVITECODE.CODE, code)
                        .where(INVITECODE.GROUPID.eq(groupID))
                        .execute()
        );

        return code;
    }

    public static String getGroupInviteCode(int groupID) {
        InvitecodeRecord record = SQLService.getService(s ->
                s.fetchOne(INVITECODE, INVITECODE.GROUPID.eq(groupID))
        );

        return record == null? null : record.getCode();
    }

    public static boolean deleteInvite(int groupID, int userID, int senderID) {
        return hasUpdated(SQLService.getService(s ->
                s.delete(GROUPINVITE)
                        .where(GROUPINVITE.GROUPID.eq(groupID),
                                GROUPINVITE.TARGETID.eq(userID),
                                GROUPINVITE.SENDERID.eq(senderID)
                        )
                        .execute()
        ));
    }

    public static void sendInviteTo(int groupID, int senderID, int targetID) {
        SQLService.getService(s ->
                s.insertInto(GROUPINVITE, GROUPINVITE.TARGETID, GROUPINVITE.SENDERID, GROUPINVITE.GROUPID)
                        .values(targetID, senderID, groupID)
                        .onDuplicateKeyIgnore()
                        .execute()
        );
    }

    public static List<GroupInvite> getUserInvites(int targetID) {
        return SQLService.getService(s ->
                s.fetch(GROUPINVITE, GROUPINVITE.TARGETID.eq(targetID)).map(GroupInvite::fromRecord)
        );
    }

    public static void removeUserInvite(int groupID, int senderID, int targetID) {
        SQLService.getService(s ->
                s.delete(GROUPINVITE)
                        .where(GROUPINVITE.GROUPID.eq(groupID),
                                GROUPINVITE.TARGETID.eq(targetID),
                                GROUPINVITE.SENDERID.eq(senderID)
                        )
                        .execute()
        );
    }

    public static int getInviteCodeGroupID(String code) throws NullPointerException {
        InvitecodeRecord record = SQLService.getService(s ->
                s.fetchOne(INVITECODE, INVITECODE.CODE.eq(code))
        );
        if (record != null) return record.getGroupid();
        else throw new NullPointerException("Code doesn't exists");
    }

    public static TextmessageRecord storeMessage(int groupID, int senderID, String message, List<String> files, int replyTo, boolean isHTML) {
        return SQLService.getService(s ->
                s.insertInto(TEXTMESSAGE, TEXTMESSAGE.GROUPID, TEXTMESSAGE.SENDERID, TEXTMESSAGE.CONTEXT, TEXTMESSAGE.FILE, TEXTMESSAGE.REPLYTO, TEXTMESSAGE.ISHTML)
                        .values(groupID, senderID, message, JSON.json(mapper.writeValueAsString(files)), replyTo, toByte(isHTML))
                        .returning()
                        .fetchOne()
        );
    }

    public static TextmessageRecord storeMessage(int groupID, int senderID, String message, List<String> files, boolean isHTML) {
        return SQLService.getService(s ->
                s.insertInto(TEXTMESSAGE, TEXTMESSAGE.GROUPID, TEXTMESSAGE.SENDERID, TEXTMESSAGE.CONTEXT, TEXTMESSAGE.FILE, TEXTMESSAGE.ISHTML)
                        .values(groupID, senderID, message, JSON.json(mapper.writeValueAsString(files)), toByte(isHTML))
                        .returning()
                        .fetchOne()
        );
    }

    public static TextmessageRecord getMessage(int messageID, int senderID) {
        return SQLService.getService(s ->
                s.fetchOne(TEXTMESSAGE, TEXTMESSAGE.MESSAGEID.eq(messageID), TEXTMESSAGE.SENDERID.eq(senderID))
        );
    }

    private static byte toByte(boolean b) {
        return (byte) (b? 1 : 0);
    }
}