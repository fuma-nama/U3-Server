package api;

import api.model.Emoji;
import api.socket.SocketService;
import database.AccountManager;
import database.EmojiManager;
import org.jooq.Record;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import util.FileUtil;

import java.io.IOException;
import java.util.List;

import static api.APIController.*;
import static api.socket.SocketHandler.EMOJI_REMOVED_EVENT;
import static api.socket.SocketHandler.EMOJI_SAVED_EVENT;
import static test.generated.Tables.CUSTOMEMOJI;
import static util.CommonUtil.notValidName;

@RestController
public class EmojiController {

    @GetMapping("/emoji")
    public List<Emoji> getEmojis(@RequestParam(value = "creator", required = false) Integer creatorID,
                                 @RequestParam(value = "name", required = false) String emojiName,
                                 @RequestParam("offset") int offset) {

        if (emojiName != null)
            return EmojiManager.getEmojis(offset, emojiName);
        else if (creatorID != null)
            return EmojiManager.getEmojis(offset, creatorID);
        else
            return EmojiManager.getEmojis(offset);
    }

    @PostMapping("/emoji")
    public ResponseEntity<String> addEmoji(@RequestHeader("token") String token,
                                                  @RequestParam("name") String name,
                                                  @RequestPart("file") MultipartFile file) throws IOException {
        if (notValidName(name)) return BAD_REQUEST;

        int userID = AccountManager.getUserID(token);
        Record record = EmojiManager.addEmoji(userID, name);
        if (record != null) {
            int emojiID = record.getValue(CUSTOMEMOJI.ID);
            FileUtil.saveFile(String.valueOf(emojiID), FileUtil.EMOJI, file);
            return OK_REQUEST;
        } else
            return BAD_REQUEST;
    }

    @GetMapping("/emoji/save")
    public ResponseEntity<List<Integer>> getSavedEmojis(@RequestHeader("token") String token) {
        int userID = AccountManager.getUserID(token);

        return ResponseEntity.ok(EmojiManager.getEmojiOrders(userID));
    }

    @PostMapping("/emoji/save")
    @ResponseStatus(HttpStatus.OK)
    public void saveEmoji(@RequestHeader("token") String token, @RequestParam("id") int emojiID) {
        int userID = AccountManager.getUserID(token);
        EmojiManager.addEmojiOrder(userID, emojiID);

        SocketService.sendPrivateEvent(userID, EMOJI_SAVED_EVENT, emojiID);
    }

    @DeleteMapping("/emoji/save")
    @ResponseStatus(HttpStatus.OK)
    public void unSaveEmoji(@RequestHeader("token") String token, @RequestParam("id") int emojiID) {
        int userID = AccountManager.getUserID(token);
        EmojiManager.removeEmojiOrder(userID, emojiID);

        SocketService.sendPrivateEvent(userID, EMOJI_REMOVED_EVENT, emojiID);
    }

    @DeleteMapping("/emoji")
    public ResponseEntity<String> deleteEmoji(@RequestHeader("token") String token,
                                                   @RequestParam("id") int emojiID) throws IOException {
        int userID = AccountManager.getUserID(token);

        boolean deleted = EmojiManager.deleteEmoji(userID, emojiID);

        if (deleted) {
            FileUtil.deleteFile(String.valueOf(emojiID), FileUtil.EMOJI);
            return OK_REQUEST;
        } else
            return BAD_REQUEST;
    }
}
