package server;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class BankService {
    private final Map<Integer, Account> accounts = new HashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(1000); // Start IDs from 1000

    public int createAccount(String name, String password, String currency, double initialBalance) {
        int id = idGenerator.getAndIncrement();
        Account account = new Account(id, name, password, currency, initialBalance);
        accounts.put(id, account);
        System.out.println("[BankService] Created account: " + account);
        return id;
    }

    public double deposit(int accountId, String password, double amount) throws Exception {
        Account account = accounts.get(accountId);
        if (account == null) {
            throw new Exception("Account does not exist");
        }
        if (!account.getPassword().equals(password)) {
             throw new Exception("Wrong password");
        }
        if (amount <= 0) {
             throw new Exception("Invalid amount");
        }

        account.setBalance(account.getBalance() + amount);
        System.out.println("[BankService] Deposit " + amount + " to account " + accountId + ". New Balance: " + account.getBalance());
        return account.getBalance();
    }

    public double checkBalance(int accountId, String password) throws Exception {
        Account account = accounts.get(accountId);
        if (account == null) {
            throw new Exception("Account does not exist");
        }
        if (!account.getPassword().equals(password)) {
             throw new Exception("Wrong password");
        }
        return account.getBalance();
    }

    public Account getAccount(int accountId) {
        return accounts.get(accountId);
    }
}
