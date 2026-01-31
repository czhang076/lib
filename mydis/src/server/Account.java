package server;

public class Account {
    private int accountNumber;
    private String name;
    private String password;
    private String currencyType;
    private double balance;
    private long version = 0;

    public Account(int accountNumber, String name, String password, String currencyType, double balance) {
        this.accountNumber = accountNumber;
        this.name = name;
        this.password = password;
        this.currencyType = currencyType;
        this.balance = balance;
        this.version = 0;
    }

    public long getVersion() {
        return version;
    }

    public int getAccountNumber() {
        return accountNumber;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public String getCurrencyType() {
        return currencyType;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
        this.version++;
    }

    @Override
    public String toString() {
        return "Account{" +
                "id=" + accountNumber +
                ", name='" + name + '\'' +
                ", currency='" + currencyType + '\'' +
                ", balance=" + balance +
                '}';
    }
}
