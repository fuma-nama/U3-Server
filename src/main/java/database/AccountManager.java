package database;

import api.model.UserProfile;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.TableField;
import test.generated.tables.records.LoginentryRecord;
import test.generated.tables.records.UserdataRecord;
import test.generated.tables.records.VerifyemailRecord;
import util.CommonUtil;
import static test.generated.Tables.*;

public class AccountManager {
    protected static final byte TRUE = 1, FALSE = 0;

    public static void deleteAccount(String token) {
        SQLService.getService(s->{
            int userID = getUserID(token);
            //Remove login entry
            s.delete(LOGINENTRY)
                    .where(LOGINENTRY.TOKEN.eq(token))
                    .execute();
            //Leave groups
            s.delete(JOINGROUPLOG)
                    .where(JOINGROUPLOG.USERID.eq(userID))
                    .execute();

            //Remove user profile
            s.delete(USERDATA)
                    .where(USERDATA.ID.eq(userID))
                    .execute();
            return null;
        });
    }

    /**
     * @return User token
     * @throws NullPointerException if email doesn't exists
     * **/
    public static String loginEmail(String email, String password) throws NullPointerException {
        return SQLService.getService(s ->
                s.fetchOne(LOGINENTRY,
                        LOGINENTRY.EMAIL.eq(email),
                        LOGINENTRY.PASSWORD.eq(password))
        ).getToken();
    }

    public static LoginentryRecord getLoginEntry(String token) throws NullPointerException {
        return SQLService.getService(s ->
                s.fetchOne(LOGINENTRY, LOGINENTRY.TOKEN.eq(token))
        );
    }

    public static boolean isEmailUsed(String email) {
        return SQLService.getService(s->s.fetchExists(LOGINENTRY, LOGINENTRY.EMAIL.eq(email)));
    }

    /**
     * Create verify code and store new verify code to email
     * **/
    public static String createVerifyCode(String email) {
        String code = createVerifyCode();

        SQLService.getService(s ->
                s.insertInto(VERIFYEMAIL, VERIFYEMAIL.EMAIL, VERIFYEMAIL.CODE)
                        .values(email, code)
                        .onDuplicateKeyUpdate()
                        .set(VERIFYEMAIL.CODE, code)
                        .execute()
        );
        return code;
    }

    /**Create verify code and store new verify code to email
     * <br>If verified is {@code true}, reset verified to {@code false}**/
    public static String updateVerifyCode(String email) {
        String code = createVerifyCode();

        SQLService.getService(s ->
                s.update(VERIFYEMAIL)
                        .set(VERIFYEMAIL.CODE, code)
                        .where(VERIFYEMAIL.EMAIL.eq(email))
                        .execute()
        );
        return code;
    }

    /**Create a Verity code, the length is 8**/
    private static String createVerifyCode() {
        return CommonUtil.randomString(8);
    }

    public static boolean isTrueVerifyCode(String email, String code) {
        return SQLService.getService(s->{
            VerifyemailRecord record = s.fetchOne(VERIFYEMAIL, VERIFYEMAIL.EMAIL.eq(email));
            return record != null && record.getCode().equals(code);
        });
    }

    /** Create a login entry that contains a token
     * **/
    public static void createLoginEntry(String token, String email, String password, int userID) {
        SQLService.getService(s->
                s.insertInto(LOGINENTRY, LOGINENTRY.EMAIL, LOGINENTRY.PASSWORD, LOGINENTRY.TOKEN, LOGINENTRY.USERID)
                        .values(email, password, token, userID)
                        .execute()
        );
    }

    /**@return a token that doesn't exist**/
    public static String generateToken() {
        return generateID(LOGINENTRY, LOGINENTRY.TOKEN, 30);
    }

    private static <T extends Record> String generateID(Table<T> table, TableField<T, String> field, int length) {
        String token = CommonUtil.randomString(length);
        String finalToken = token;
        while (SQLService.getService(s->s.fetchExists(table, field.eq(finalToken)))) {
            token = CommonUtil.randomString(length);
        }
        return token;
    }

    /**@return UserID
     * @throws NullPointerException if failed to insert user data into database**/
    public static int createUserData(String name, String avatarUrl) throws NullPointerException {
        Record record = SQLService.getService(s ->
                s.insertInto(USERDATA, USERDATA.NAME, USERDATA.AVATAR)
                        .values(name, avatarUrl)
                        .returningResult(USERDATA.ID)
                        .fetchOne()
        );
        return record.getValue(USERDATA.ID);
    }

    public static void updateUserData(int userID, String name) {
        SQLService.getService(s ->
                s.update(USERDATA)
                        .set(USERDATA.NAME, name)
                        .where(USERDATA.ID.eq(userID))
                        .execute()
        );
    }

    public static void updateUserData(int userID, String name, String avatarUrl) {
        SQLService.getService(s ->
                s.update(USERDATA)
                        .set(USERDATA.NAME, name)
                        .set(USERDATA.AVATAR, avatarUrl)
                        .where(USERDATA.ID.eq(userID))
                        .execute()
        );
    }

    public static boolean setEmail(String token, String password, String newEmail) {
        return SQLService.getService(s->
                s.update(LOGINENTRY)
                        .set(LOGINENTRY.EMAIL, newEmail)
                        .where(LOGINENTRY.TOKEN.eq(token), LOGINENTRY.PASSWORD.eq(password))
                        .execute()
        ) != 0;
    }

    /**Update password of account**/
    public static void setPassword(String email, String newPassword) {
        SQLService.getService(s->
                s.update(LOGINENTRY)
                        .set(LOGINENTRY.PASSWORD, newPassword)
                        .where(LOGINENTRY.EMAIL.eq(email))
                        .execute()
        );
    }

    public static boolean setPassword(String token, String oldPassword, String newPassword) {
        return SQLService.getService(s->
                s.update(LOGINENTRY)
                        .set(LOGINENTRY.PASSWORD, newPassword)
                        .where(LOGINENTRY.TOKEN.eq(token), LOGINENTRY.PASSWORD.eq(oldPassword))
                        .execute()
        ) != 0;
    }

    public static boolean isTokenExists(String token) {
        return SQLService.getService(s->s.fetchExists(LOGINENTRY, LOGINENTRY.TOKEN.eq(token)));
    }

    /**@return UserID of token
     * @throws NullPointerException if token has no userID**/
    public static int getUserID(String token) throws NullPointerException {
        LoginentryRecord record = SQLService.getService(s->
             s.fetchOne(LOGINENTRY, LOGINENTRY.TOKEN.eq(token))
        );
        return record.getUserid();
    }

    /**@return User profile of id
     * @throws NullPointerException if user doesn't exist**/
    public static UserProfile getUser(int userID) throws NullPointerException {
        UserdataRecord record = SQLService.getService(s ->
                s.fetchOne(USERDATA, USERDATA.ID.eq(userID))
        );
        if (record == null) throw new NullPointerException("User doesn't exist");

        return new UserProfile(record.getName(), record.getAvatar());
    }
}
