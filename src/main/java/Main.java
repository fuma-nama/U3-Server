import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.sql.DriverManager;

import static test.generated.tables.Loginentry.LOGINENTRY;

public class Main {
    /** System
     * Entries:
     * Client, Server, DataBase
     *
     * Login
     *
     * Client
     *   |
     * If have been logged in -> Login with token
     * Else -> Login with email and password -> Get token for next login
     *
     * Server
     *   |
     * If client uses token -> Find user by email -> Check token -> Return user info to client
     * Else if client uses password -> Find user by email -> Check password -> Return user info and token to client
     * (User info must get by token)
     *
     * DataBase
     *   |
     * Table: LoginEntry (Information for login)
     *   Email (String) - User email
     *   Token (String Array) - Tokens, The length of array should same as the device count you logged in to the account
     *   Password (String) - User password
     *
     * Send Message
     *
     * Client
     *   |
     * Do api call with token header
     *
     * Server
     *   |
     * Get user data from token header
     * **/
    public static void mainSQL(String[] args) {
        String userName = "root";
        String upassword = "10124Lol";
        String url = "jdbc:mysql://localhost:3306/login?serverTimezone=UTC";


        try (Connection conn = DriverManager.getConnection(url, userName, upassword)) {
            DSLContext create = DSL.using(conn, SQLDialect.MYSQL);

            try {
                create.insertInto(LOGINENTRY,
                                LOGINENTRY.EMAIL, LOGINENTRY.PASSWORD, LOGINENTRY.TOKEN)
                        .values("god63820869@gmail.com", "a231513", "1234567").execute();
            } catch (DataAccessException e) {
                System.out.println("Email already exists");
            }

            Result<Record> result = create.select().from(LOGINENTRY).fetch();

            for (Record r : result) {
                String email = r.getValue(LOGINENTRY.EMAIL),
                        password = r.getValue(LOGINENTRY.PASSWORD),
                        token = r.getValue(LOGINENTRY.TOKEN);

                System.out.println("Email: " + email + " Password: " + password + " Token: " + token);
            }
        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
