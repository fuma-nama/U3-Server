package api.model;

import test.generated.tables.records.GroupinviteRecord;

public record GroupInvite(int groupID, int senderID) {
    public static GroupInvite fromRecord(GroupinviteRecord record) {
        return new GroupInvite(record.getGroupid(), record.getSenderid());
    }
}
