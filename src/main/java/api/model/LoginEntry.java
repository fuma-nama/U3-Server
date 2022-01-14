package api.model;

import test.generated.tables.records.LoginentryRecord;

public record LoginEntry(String email, int passwordLength) {
    public static LoginEntry fromRecord(LoginentryRecord record) {
        return new LoginEntry(record.getEmail(), record.getPassword().length());
    }
}
