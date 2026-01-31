package server;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import common.CurrencyType;

public class BankService {
    private final Map<Integer, Account> accounts = new HashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(1000); // Start IDs from 1000

    public int createAccount(String name, String password, String currency, double initialBalance) throws Exception {
        if (name == null || name.isEmpty()) {
            throw new Exception("Name is required");
        }
        if (password == null || password.isEmpty()) {
            throw new Exception("Password is required");
        }
        CurrencyType currencyType = CurrencyType.fromString(currency);
        int id = idGenerator.getAndIncrement();
        Account account = new Account(id, name, password, currencyType, initialBalance);
        accounts.put(id, account);
        System.out.println("[BankService] Created account: " + account);
        return id;
    }

    public double deposit(int accountId, String name, String password, String currency, double amount) throws Exception {
        Account account = accounts.get(accountId);
        if (account == null) {
            throw new Exception("Account does not exist");
        }
        if (!account.getName().equals(name)) {
            throw new Exception("Name mismatch");
        }
        if (!account.getPassword().equals(password)) {
             throw new Exception("Wrong password");
        }
        if (!account.getCurrencyType().name().equalsIgnoreCase(currency)) {
            throw new Exception("Currency mismatch");
        }
        if (amount <= 0) {
             throw new Exception("Invalid amount");
        }
        synchronized (account) {
            account.setBalance(account.getBalance() + amount);
        }
        System.out.println("[BankService] Deposit " + amount + " to account " + accountId + ". New Balance: " + account.getBalance());
        return account.getBalance();
    }

    public double withdraw(int accountId, String name, String password, String currency, double amount) throws Exception {
        Account account = accounts.get(accountId);
        if (account == null) {
            throw new Exception("Account does not exist");
        }
        if (!account.getName().equals(name)) {
            throw new Exception("Name mismatch");
        }
        if (!account.getPassword().equals(password)) {
             throw new Exception("Wrong password");
        }
        if (!account.getCurrencyType().name().equalsIgnoreCase(currency)) {
            throw new Exception("Currency mismatch");
        }
        if (amount <= 0) {
            throw new Exception("Invalid amount");
        }
        synchronized (account) {
            if (account.getBalance() < amount) {
                throw new Exception("Insufficient balance");
            }
            account.setBalance(account.getBalance() - amount);
        }
        System.out.println("[BankService] Withdraw " + amount + " from account " + accountId + ". New Balance: " + account.getBalance());
        return account.getBalance();
    }

    public double checkBalance(int accountId, String name, String password) throws Exception {
        Account account = accounts.get(accountId);
        if (account == null) {
            throw new Exception("Account does not exist");
        }
        if (!account.getName().equals(name)) {
            throw new Exception("Name mismatch");
        }
        if (!account.getPassword().equals(password)) {
             throw new Exception("Wrong password");
        }
        return account.getBalance();
    }

    public Account getAccountInfo(int accountId, String name, String password) throws Exception {
        Account account = accounts.get(accountId);
        if (account == null) {
            throw new Exception("Account does not exist");
        }
        if (!account.getName().equals(name)) {
            throw new Exception("Name mismatch");
        }
        if (!account.getPassword().equals(password)) {
            throw new Exception("Wrong password");
        }
        return account;
    }

    public Account closeAccount(int accountId, String name, String password) throws Exception {
        Account account = accounts.get(accountId);
        if (account == null) {
            throw new Exception("Account does not exist");
        }
        if (!account.getName().equals(name)) {
            throw new Exception("Name mismatch");
        }
        if (!account.getPassword().equals(password)) {
            throw new Exception("Wrong password");
        }
        accounts.remove(accountId);
        System.out.println("[BankService] Closed account: " + accountId);
        return account;
    }

    public double transfer(int fromId, String fromName, String fromPassword, int toId, double amount) throws Exception {
        if (fromId == toId) {
            throw new Exception("Cannot transfer to the same account");
        }
        Account from = accounts.get(fromId);
        Account to = accounts.get(toId);
        if (from == null || to == null) {
            throw new Exception("Account does not exist");
        }
        if (!from.getName().equals(fromName)) {
            throw new Exception("Name mismatch");
        }
        if (!from.getPassword().equals(fromPassword)) {
            throw new Exception("Wrong password");
        }
        if (!from.getCurrencyType().equals(to.getCurrencyType())) {
            throw new Exception("Currency mismatch between accounts");
        }
        if (amount <= 0) {
            throw new Exception("Invalid amount");
        }
        synchronized (from) {
            if (from.getBalance() < amount) {
                throw new Exception("Insufficient balance");
            }
            from.setBalance(from.getBalance() - amount);
        }
        synchronized (to) {
            to.setBalance(to.getBalance() + amount);
        }
        System.out.println("[BankService] Transfer " + amount + " from account " + fromId + " to " + toId);
        return from.getBalance();
    }

    public Account getAccount(int accountId) {
        return accounts.get(accountId);
    }
}
