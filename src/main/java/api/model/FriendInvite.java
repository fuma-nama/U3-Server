package api.model;

import test.generated.tables.records.FriendinviteRecord;

import java.time.LocalDateTime;

public record FriendInvite(int senderID, LocalDateTime inviteTime) {
    
    public static FriendInvite fromRecord(FriendinviteRecord record) {
        return new FriendInvite(record.getSenderid(), record.getInvitedate());
    }
}
