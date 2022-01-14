package api.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import database.GroupManager;
import org.jooq.Record;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;

import static api.APIController.mapper;
import static test.generated.Tables.TEXTMESSAGE;
import static util.CommonUtil.toBoolean;

public record TextMessage(int ID, int groupID, int senderID, String context, LocalDateTime date, boolean edited, String[] file, TextMessage replyTo, boolean isHTML) {

    public static TextMessage fromRecord(@Nullable Record record) {
        return fromRecord(record, true);
    }

    public static TextMessage fromRecord(@Nullable Record record, boolean setReply) {
        if (record == null) return null;

        try {
            return new TextMessage(
                    record.getValue(TEXTMESSAGE.MESSAGEID),
                    record.getValue(TEXTMESSAGE.GROUPID),
                    record.getValue(TEXTMESSAGE.SENDERID),
                    record.getValue(TEXTMESSAGE.CONTEXT),
                    record.getValue(TEXTMESSAGE.DATE),
                    toBoolean(record.getValue(TEXTMESSAGE.EDITED)),
                    mapper.readValue(record.getValue(TEXTMESSAGE.FILE).data(), String[].class),
                    setReply? getReplyTo(record) : null,
                    toBoolean(record.getValue(TEXTMESSAGE.ISHTML))
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static TextMessage getReplyTo(Record record) {
        int messageID = record.getValue(TEXTMESSAGE.REPLYTO);
        if (messageID == -1) return null;
        else return TextMessage.fromRecord(GroupManager.getMessage(messageID), false);
    }
}