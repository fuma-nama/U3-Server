package database;

import api.model.Emoji;
import org.jooq.Record;

import java.util.List;

import static test.generated.Tables.CUSTOMEMOJI;
import static test.generated.Tables.EMOJIORDERS;

public class EmojiManager {
    private static final int MAX_FETCH_COUNT = 30;

    public static List<Emoji> getEmojis(int offset) {
        return SQLService.getService(s->
                s.select().from(CUSTOMEMOJI)
                        .orderBy(CUSTOMEMOJI.ID.desc())
                        .offset(offset)
                        .limit(MAX_FETCH_COUNT)
                        .fetch(Emoji::fromRecord)
        );
    }

    public static List<Emoji> getEmojis(int offset, int creatorID) {
        return SQLService.getService(s->
                s.select().from(CUSTOMEMOJI)
                        .where(CUSTOMEMOJI.CREATORID.eq(creatorID))
                        .orderBy(CUSTOMEMOJI.ID.desc())
                        .offset(offset)
                        .limit(MAX_FETCH_COUNT)
                        .fetch(Emoji::fromRecord)
        );
    }

    public static List<Emoji> getEmojis(int offset, String name) {
        return SQLService.getService(s->
                s.select().from(CUSTOMEMOJI)
                        .where(CUSTOMEMOJI.NAME.contains(name))
                        .orderBy(CUSTOMEMOJI.ID.desc())
                        .offset(offset)
                        .limit(MAX_FETCH_COUNT)
                        .fetch(Emoji::fromRecord)
        );
    }

    public static Record addEmoji(int creatorID, String name) {
        return SQLService.getService(s->
                s.insertInto(CUSTOMEMOJI, CUSTOMEMOJI.CREATORID, CUSTOMEMOJI.NAME)
                        .values(creatorID, name)
                        .returningResult(CUSTOMEMOJI.ID)
                        .fetchOne()
        );
    }

    public static void addEmojiOrder(int userID, int emojiID) {
        SQLService.getService(s->
                s.insertInto(EMOJIORDERS, EMOJIORDERS.USERID, EMOJIORDERS.EMOJIID)
                        .values(userID, emojiID)
                        .onDuplicateKeyIgnore()
                        .execute()
        );
    }

    public static void removeEmojiOrder(int userID, int emojiID) {
        SQLService.getService(s->
                s.delete(EMOJIORDERS)
                        .where(EMOJIORDERS.USERID.eq(userID), EMOJIORDERS.EMOJIID.eq(emojiID))
                        .execute()
        );
    }

    public static List<Integer> getEmojiOrders(int userID) {
        return SQLService.getService(s->
                s.fetch(EMOJIORDERS, EMOJIORDERS.USERID.eq(userID))
                        .getValues(EMOJIORDERS.EMOJIID)
        );
    }

    public static boolean deleteEmoji(int creatorID, int ID) {
        boolean deleted = SQLService.getService(s->
                s.delete(CUSTOMEMOJI)
                        .where(CUSTOMEMOJI.ID.eq(ID), CUSTOMEMOJI.CREATORID.eq(creatorID))
                        .execute()
        ) != 0;

        if (deleted) {
            SQLService.getService(s->
                    s.delete(EMOJIORDERS)
                            .where(EMOJIORDERS.EMOJIID.eq(ID))
                            .execute()
            );
        }

        return deleted;
    }
}
