package database;

import api.model.Friend;
import api.model.FriendInvite;
import api.socket.SocketHandler;
import org.jooq.impl.DSL;
import test.generated.tables.records.FriendinviteRecord;

import java.util.List;

import static test.generated.Tables.FRIEND;
import static test.generated.Tables.FRIENDINVITE;
import static util.CommonUtil.hasUpdated;


public class FriendManager {
    public static List<Friend> getFriends(int userID) {
        return SQLService.getService(s ->
                s.fetch(FRIEND, FRIEND.FIRSTUSERID
                                .eq(userID)
                                .or(FRIEND.SECONDUSERID.eq(userID))
                        )
                        .map(record -> Friend.fromRecord(userID, record))
        );
    }

    public static boolean isFriend(int firstUserID, int secondUserID) {
        return SQLService.getService(s ->
                s.fetchExists(FRIEND,
                        FRIEND.FIRSTUSERID.eq(firstUserID).and(FRIEND.SECONDUSERID.eq(secondUserID))
                                .or(
                                        FRIEND.SECONDUSERID.eq(firstUserID).and(FRIEND.FIRSTUSERID.eq(secondUserID))
                                )
                )
        );
    }

    public static void addFriend(int firstUserID, int secondUserID, int privateGroupID) {
        SQLService.getService(s ->
                s.insertInto(FRIEND, FRIEND.FIRSTUSERID, FRIEND.SECONDUSERID, FRIEND.PRIVATEGROUPID)
                        .values(firstUserID, secondUserID, privateGroupID)
                        .execute()
        );
    }

    public static boolean removeFriend(int firstUserID, int secondUserID) {
        return hasUpdated(SQLService.getService(s ->
                s.delete(FRIEND)
                        .where(
                                FRIEND.FIRSTUSERID.eq(firstUserID).and(FRIEND.SECONDUSERID.eq(secondUserID))
                                        .or(
                                                FRIEND.SECONDUSERID.eq(firstUserID).and(FRIEND.FIRSTUSERID.eq(secondUserID))
                                        )
                        )
                        .execute()
        ));
    }

    public static List<FriendInvite> getFriendInvites(int userID) {
        return SQLService.getService(s ->
                s.fetch(FRIENDINVITE, FRIENDINVITE.TARGETID.eq(userID))
                        .map(FriendInvite::fromRecord)
        );
    }

    public static FriendInvite addFriendInvite(int senderID, int targetID) {
        FriendinviteRecord record = SQLService.getService(s ->
                s.insertInto(FRIENDINVITE, FRIENDINVITE.SENDERID, FRIENDINVITE.TARGETID)
                        .values(senderID, targetID)
                        .onDuplicateKeyUpdate()
                        .set(FRIENDINVITE.INVITEDATE, DSL.currentLocalDateTime())
                        .returning()
                        .fetchOne()
        );
        return record == null? null : FriendInvite.fromRecord(record);
    }

    public static boolean removeFriendInvite(int senderID, int targetID) {
        return hasUpdated(SQLService.getService(s ->
                s.delete(FRIENDINVITE)
                        .where(FRIENDINVITE.SENDERID.eq(senderID), FRIENDINVITE.TARGETID.eq(targetID))
                        .execute()
        ));
    }

    public static boolean hasInvite(int senderID, int targetID) {
        return SQLService.getService(s ->
                s.fetchExists(FRIENDINVITE, FRIENDINVITE.SENDERID.eq(senderID), FRIENDINVITE.TARGETID.eq(targetID))
        );
    }
}
