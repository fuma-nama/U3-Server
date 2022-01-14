package util;

import java.util.Random;

public class CommonUtil {
    private static final String CHARS = "QWERTYUIOPASDFGJKLZXCVBNM1234567890";

    public static boolean notValidName(String name) {
        return name.isBlank() || name.length() > 50;
    }

    public static boolean isValidContext(String message) {
        return !message.isBlank() && message.length() <= 2000;
    }

    public static boolean isValidContext(String message, boolean allowBlank) {
        return (allowBlank || !message.isBlank()) && message.length() <= 2000;
    }

    public static boolean isValidPassword(String password) {
        return !password.isEmpty() && password.length() <= 50;
    }

    public static String randomString(int length) {
        StringBuilder result = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(CHARS.length());
            char c = CHARS.charAt(index);
            result.append(c);
        }
        return result.toString();
    }

    public static boolean toBoolean(byte b) {
        return b == 1;
    }

    public static boolean hasUpdated(int result) {
        return result != 0;
    }
}
