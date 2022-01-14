package database;

import lombok.SneakyThrows;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLService {
    private static final String
            USERNAME = "root",
            PASSWORD = "10124Lol",
            url = "jdbc:mysql://localhost:3306/login?serverTimezone=UTC";

    private static final Connection conn;

    static {
        try {
            conn = DriverManager.getConnection(url, USERNAME, PASSWORD);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T getService(ExceptionFunction<DSLContext, T> function) {
        try {
            return function.apply(DSL.using(conn, SQLDialect.MYSQL));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public interface ExceptionFunction<T, R> {
        /**
         * Applies this function to the given argument.
         *
         * @param t the function argument
         * @return the function result
         */
        @SneakyThrows
        R apply(T t) throws Exception;
    }
}
