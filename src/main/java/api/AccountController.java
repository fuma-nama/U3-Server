package api;

import api.model.LoginEntry;
import api.model.UserProfile;
import api.socket.SocketService;
import database.AccountManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import test.generated.tables.records.LoginentryRecord;
import util.FileUtil;
import util.MailManager;


import java.io.IOException;

import static api.APIController.*;
import static api.socket.SocketHandler.USER_PROFILE_UPDATE;
import static database.AccountManager.*;
import static util.CommonUtil.isValidPassword;
import static util.CommonUtil.notValidName;

@RestController
public class AccountController {
    @GetMapping("/account/login")
    public ResponseEntity<LoginEntry> getLoginEntry(@RequestHeader("token") String token) {
        LoginentryRecord record = AccountManager.getLoginEntry(token);
        if (record == null)
            return ResponseEntity.badRequest().build();
        else
            return ResponseEntity.ok(LoginEntry.fromRecord(record));
    }

    @PutMapping("/account/login/email")
    public ResponseEntity<String> updateEmail(@RequestHeader("token") String token,
                                              @RequestParam("email") String email,
                                              @RequestParam("password") String password,
                                              @RequestParam("code") String code) {
        if (isEmailUsed(email) || !isTrueVerifyCode(email, code)) return BAD_REQUEST;

        boolean success = AccountManager.setEmail(token, password, email);

        return success? OK_REQUEST : BAD_REQUEST;
    }

    @PutMapping("/account/login/password")
    public ResponseEntity<String> updatePassword(@RequestHeader("token") String token,
                                              @RequestParam("old") String oldPassword,
                                              @RequestParam("new") String newPassword) {
        if (isValidPassword(newPassword)) {
            boolean success = AccountManager.setPassword(token, oldPassword, newPassword);

            if (success)
                return OK_REQUEST;
        }

        return BAD_REQUEST;
    }

    @PostMapping("/account/login")
    public ResponseEntity<String> login(@RequestParam(name = "email") String email, @RequestParam(name = "password") String password) {
        try {
            String token = AccountManager.loginEmail(email, password);
            return new ResponseEntity<>(token, HttpStatus.OK);
        } catch (NullPointerException e) {
            return BAD_REQUEST;
        }
    }

    /**
     * Send verify code to email and save the code in database
     * If the email isn't exists, return 400
     * */
    @PostMapping("/account/verify")
    public ResponseEntity<String> sendVerifyCode(@RequestParam(name = "email") String email) {
        if (!AccountManager.isEmailUsed(email)) return BAD_REQUEST;
        String code = AccountManager.updateVerifyCode(email);
        MailManager.sendMail("Your U3 verify code", "Your verify code is: " + code, email);
        return OK_REQUEST;
    }

    /**
     * If the email has been used, return 400
     * Else send verify code to email and save the code in database
     * */
    @PostMapping("/account/verify/email")
    public ResponseEntity<String> verifyEmail(@RequestParam(name = "email") String email) {
        if (AccountManager.isEmailUsed(email)) return BAD_REQUEST;
        String code = AccountManager.createVerifyCode(email);
        MailManager.sendMail("Your U3 verify code", "Your verify code is: " + code, email);
        return OK_REQUEST;
    }

    @GetMapping("/account/verify/code")
    public boolean isTureCode(@RequestParam(name = "email") String email, @RequestParam(name = "code") String code) {
        return AccountManager.isTrueVerifyCode(email, code);
    }

    @DeleteMapping("/account")
    public ResponseEntity<String> deleteAccount(@RequestHeader("token") String token) {
        AccountManager.deleteAccount(token);
        return OK_REQUEST;
    }

    @PostMapping("/account")
    public ResponseEntity<String> signUp(@RequestParam("email") String email,
                                         @RequestParam("name") String name,
                                         @RequestParam("password") String password,
                                         @RequestParam("code") String code,
                                         @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {

        if (notValidName(name) || !isValidPassword(password) || isEmailUsed(email) || !isTureCode(email, code)) return BAD_REQUEST;

        String url = null, token = AccountManager.generateToken();

        if (file != null) {
            url = FileUtil.saveFileByID(token, FileUtil.USER_AVATAR, file);
        }

        int userID = AccountManager.createUserData(name, url);


        AccountManager.createLoginEntry(token, email, password, userID);

        return ResponseEntity.ok(token);
    }

    @PutMapping("/account")
    public ResponseEntity<String> updateAccount(@RequestHeader("token") String token,
                                         @RequestParam(name = "name") String name,
                                         @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {

        if (notValidName(name)) return BAD_REQUEST;

        int userID = AccountManager.getUserID(token);

        if (file != null) {
            String url = FileUtil.saveFileByID(token, FileUtil.USER_AVATAR, file);
            AccountManager.updateUserData(userID, name, url);
            SocketService.sendPrivateEvent(userID, USER_PROFILE_UPDATE, name, url);
        } else {
            AccountManager.updateUserData(userID, name);
            SocketService.sendPrivateEvent(userID, USER_PROFILE_UPDATE, name, null);
        }

        return OK_REQUEST;
    }

    @GetMapping("/account")
    public UserProfile getUser(@RequestParam(name = "id") int userID) {
        return AccountManager.getUser(userID);
    }

    @GetMapping("/account/id")
    public int getUserID(@RequestParam(name = "token") String token) {
        return AccountManager.getUserID(token);
    }

    @PostMapping("/account/reset")
    public ResponseEntity<String> resetPassword(@RequestParam(name = "email") String email,
                                                @RequestParam(name = "code") String code,
                                                @RequestParam(name = "new") String newPassword) {
        if (isValidPassword(newPassword) && AccountManager.isTrueVerifyCode(email, code)) {
            AccountManager.setPassword(email, newPassword);
            return OK_REQUEST;
        } else {
            return BAD_REQUEST;
        }
    }
}
