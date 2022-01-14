package api.model;

import test.generated.tables.records.FriendRecord;

public record Friend(int friendID, Integer privateGroupID) {
    public static Friend fromRecord(int userID, FriendRecord record) {
        int friendID, firstUser = record.getFirstuserid();

        if (firstUser == userID) {
            friendID = record.getSeconduserid();
        } else {
            friendID = firstUser;
        }

        return new Friend(friendID, record.getPrivategroupid());
    }
}
