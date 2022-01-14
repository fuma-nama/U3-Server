package api.model;

import org.jooq.Record;

import static test.generated.Tables.CUSTOMEMOJI;

public record Emoji(int ID, int creatorID, String name) {

    public static Emoji fromRecord(Record record) {
        return new Emoji(
                record.getValue(CUSTOMEMOJI.ID),
                record.getValue(CUSTOMEMOJI.CREATORID),
                record.getValue(CUSTOMEMOJI.NAME)
        );
    }
}
