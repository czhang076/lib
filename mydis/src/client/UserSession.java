package client;

public class UserSession {
    private final int accountId;
    private final String name;
    private final String password;

    public UserSession(int accountId, String name, String password) {
        this.accountId = accountId;
        this.name = name;
        this.password = password;
    }

    public int getAccountId() {
        return accountId;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }
}
