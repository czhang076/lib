package client.gui;

import client.BankClientManager;
import client.UserSession;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class MainFrame extends JFrame {
    private final BankClientManager manager;
    private final JLabel headerLabel = new JLabel("", SwingConstants.LEFT);
    private final JLabel statusBar = new JLabel(" ");
    private final JLabel balanceLabel = new JLabel("Balance: --", SwingConstants.CENTER);
    private final JComboBox<String> balanceCurrencyBox = new JComboBox<>(new String[]{"USD", "RMB", "SGD", "JPY", "BPD"});

    private final JTextField depositAmountField = new JTextField(12);
    private final JComboBox<String> depositCurrencyBox = new JComboBox<>(new String[]{"USD", "RMB", "SGD", "JPY", "BPD"});

    private final JTextField withdrawAmountField = new JTextField(12);
    private final JComboBox<String> withdrawCurrencyBox = new JComboBox<>(new String[]{"USD", "RMB", "SGD", "JPY", "BPD"});

    private final JTextField transferReceiverField = new JTextField(10);
    private final JTextField transferAmountField = new JTextField(12);
    private final JComboBox<String> transferCurrencyBox = new JComboBox<>(new String[]{"USD", "RMB", "SGD", "JPY", "BPD"});

    private final JTextField exchangeAmountField = new JTextField(12);
    private final JComboBox<String> exchangeFromBox = new JComboBox<>(new String[]{"USD", "RMB", "SGD", "JPY", "BPD"});
    private final JComboBox<String> exchangeToBox = new JComboBox<>(new String[]{"USD", "RMB", "SGD", "JPY", "BPD"});

    public MainFrame(BankClientManager manager) {
        super("Bank Client");
        this.manager = manager;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);
        add(buildTabs(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        updateHeader();
        setSize(600, 400);
        setLocationRelativeTo(null);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> handleLogout());

        header.add(headerLabel, BorderLayout.CENTER);
        header.add(logoutButton, BorderLayout.EAST);
        return header;
    }

    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Home", buildHomeTab());
        tabs.addTab("Deposit", buildDepositTab());
        tabs.addTab("Withdraw", buildWithdrawTab());
        tabs.addTab("Transfer", buildTransferTab());
        tabs.addTab("Exchange", buildExchangeTab());
        tabs.addTab("Close Account", buildCloseTab());
        return tabs;
    }

    private JPanel buildHomeTab() {
        JPanel panel = new JPanel(new BorderLayout());
        balanceLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        balanceLabel.setFont(balanceLabel.getFont().deriveFont(20f));

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshBalance());

        panel.add(balanceLabel, BorderLayout.CENTER);
        JPanel footer = new JPanel();
        footer.add(new JLabel("Currency:"));
        footer.add(balanceCurrencyBox);
        footer.add(refreshButton);
        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildDepositTab() {
        JPanel panel = GuiUtils.createStandardPanel();

        panel.add(new JLabel("Amount:"), GuiUtils.createLabelGbc(0, 0));
        panel.add(depositAmountField, GuiUtils.createGbc(1, 0));

        panel.add(new JLabel("Currency:"), GuiUtils.createLabelGbc(0, 1));
        panel.add(depositCurrencyBox, GuiUtils.createGbc(1, 1));

        JButton submit = new JButton("Submit");
        submit.addActionListener(e -> submitDeposit());
        panel.add(submit, GuiUtils.createGbc(0, 2, 2));

        return panel;
    }

    private JPanel buildWithdrawTab() {
        JPanel panel = GuiUtils.createStandardPanel();

        panel.add(new JLabel("Amount:"), GuiUtils.createLabelGbc(0, 0));
        panel.add(withdrawAmountField, GuiUtils.createGbc(1, 0));

        panel.add(new JLabel("Currency:"), GuiUtils.createLabelGbc(0, 1));
        panel.add(withdrawCurrencyBox, GuiUtils.createGbc(1, 1));

        JButton submit = new JButton("Submit");
        submit.addActionListener(e -> submitWithdraw());
        panel.add(submit, GuiUtils.createGbc(0, 2, 2));

        return panel;
    }

    private JPanel buildTransferTab() {
        JPanel panel = GuiUtils.createStandardPanel();

        panel.add(new JLabel("Receiver ID:"), GuiUtils.createLabelGbc(0, 0));
        panel.add(transferReceiverField, GuiUtils.createGbc(1, 0));

        panel.add(new JLabel("Amount:"), GuiUtils.createLabelGbc(0, 1));
        panel.add(transferAmountField, GuiUtils.createGbc(1, 1));

        panel.add(new JLabel("Currency:"), GuiUtils.createLabelGbc(0, 2));
        panel.add(transferCurrencyBox, GuiUtils.createGbc(1, 2));

        JButton submit = new JButton("Submit");
        submit.addActionListener(e -> submitTransfer());
        panel.add(submit, GuiUtils.createGbc(0, 3, 2));

        return panel;
    }

    private JPanel buildExchangeTab() {
        JPanel panel = GuiUtils.createStandardPanel();

        panel.add(new JLabel("Amount (target):"), GuiUtils.createLabelGbc(0, 0));
        panel.add(exchangeAmountField, GuiUtils.createGbc(1, 0));

        panel.add(new JLabel("From:"), GuiUtils.createLabelGbc(0, 1));
        panel.add(exchangeFromBox, GuiUtils.createGbc(1, 1));

        panel.add(new JLabel("To:"), GuiUtils.createLabelGbc(0, 2));
        panel.add(exchangeToBox, GuiUtils.createGbc(1, 2));

        JButton submit = new JButton("Submit");
        submit.addActionListener(e -> submitExchange());
        panel.add(submit, GuiUtils.createGbc(0, 3, 2));

        return panel;
    }

    private JPanel buildCloseTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JLabel warning = new JLabel("This will permanently close your account.", SwingConstants.CENTER);
        warning.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JButton closeButton = new JButton("Close Account");
        closeButton.addActionListener(e -> submitCloseAccount());

        panel.add(warning, BorderLayout.CENTER);
        JPanel footer = new JPanel();
        footer.add(closeButton);
        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }


    private JPanel buildStatusBar() {
        JPanel status = new JPanel(new BorderLayout());
        status.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        statusBar.setHorizontalAlignment(SwingConstants.LEFT);
        status.add(statusBar, BorderLayout.CENTER);
        return status;
    }

    private void updateHeader() {
        UserSession session = manager.getCurrentUser();
        if (session == null) {
            headerLabel.setText("Welcome");
            return;
        }
        headerLabel.setText("Welcome, " + session.getName() + " (Account ID: " + session.getAccountId() + ")");
    }

    private void handleLogout() {
        manager.logout();
        manager.setServerMessageListener(null);
        dispose();
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }

    private void refreshBalance() {
        String currency = (String) balanceCurrencyBox.getSelectedItem();
        runAsync("Refreshing balance...", () -> manager.checkBalance(currency), result -> {
            if (result.isSuccess()) {
                String balanceText = extractNumber(result.getMessage());
                if (balanceText == null || balanceText.isEmpty()) {
                    balanceLabel.setText("Balance: " + result.getMessage());
                } else {
                    balanceLabel.setText("Balance: " + balanceText + " " + currency);
                }
                showSuccess("Balance updated.");
            } else {
                showError(result.getMessage());
            }
        });
    }

    private void submitDeposit() {
        UserSession session = manager.getCurrentUser();
        if (session == null) {
            showError("Please login again.");
            return;
        }
        float amount;
        try {
            amount = Float.parseFloat(depositAmountField.getText().trim());
        } catch (NumberFormatException e) {
            showError("Invalid amount.");
            return;
        }
        String currency = (String) depositCurrencyBox.getSelectedItem();

        runAsync("Submitting deposit...", () -> manager.deposit(session.getAccountId(), currency, amount), result -> {
            if (result.isSuccess()) {
                showSuccess("Deposit success: " + result.getMessage());
                refreshBalance();
            } else {
                showError(result.getMessage());
            }
        });
    }

    private void submitWithdraw() {
        UserSession session = manager.getCurrentUser();
        if (session == null) {
            showError("Please login again.");
            return;
        }
        float amount;
        try {
            amount = Float.parseFloat(withdrawAmountField.getText().trim());
        } catch (NumberFormatException e) {
            showError("Invalid amount.");
            return;
        }

        String currency = (String) withdrawCurrencyBox.getSelectedItem();
        runAsync("Submitting withdraw...", () -> manager.withdraw(session.getAccountId(), currency, amount), result -> {
            if (result.isSuccess()) {
                showSuccess("Withdraw success: " + result.getMessage());
                refreshBalance();
            } else {
                showError(result.getMessage());
            }
        });
    }

    private void submitTransfer() {
        int receiverId;
        try {
            receiverId = Integer.parseInt(transferReceiverField.getText().trim());
        } catch (NumberFormatException e) {
            showError("Invalid receiver ID.");
            return;
        }

        float amount;
        try {
            amount = Float.parseFloat(transferAmountField.getText().trim());
        } catch (NumberFormatException e) {
            showError("Invalid amount.");
            return;
        }

        String currency = (String) transferCurrencyBox.getSelectedItem();
        runAsync("Submitting transfer...", () -> manager.transfer(receiverId, currency, amount), result -> {
            if (result.isSuccess()) {
                showSuccess("Transfer success: " + result.getMessage());
                refreshBalance();
            } else {
                showError(result.getMessage());
            }
        });
    }

    private void submitExchange() {
        float amount;
        try {
            amount = Float.parseFloat(exchangeAmountField.getText().trim());
        } catch (NumberFormatException e) {
            showError("Invalid amount.");
            return;
        }

        String fromCurrency = (String) exchangeFromBox.getSelectedItem();
        String toCurrency = (String) exchangeToBox.getSelectedItem();
        runAsync("Submitting exchange...", () -> manager.exchange(fromCurrency, toCurrency, amount), result -> {
            if (result.isSuccess()) {
                showSuccess("Exchange success: " + result.getMessage());
                refreshBalance();
            } else {
                showError(result.getMessage());
            }
        });
    }

    private void submitCloseAccount() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to close your account?",
                "Confirm Close Account",
                JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        runAsync("Closing account...", () -> manager.closeAccount(), result -> {
            if (result.isSuccess()) {
                JOptionPane.showMessageDialog(this, "Account closed successfully.", "Closed", JOptionPane.INFORMATION_MESSAGE);
                handleLogout();
            } else {
                showError(result.getMessage());
            }
        });
    }


    private void runAsync(String loadingText, Task task, ResultHandler handler) {
        JDialog loading = createLoadingDialog(loadingText);
        SwingWorker<BankClientManager.Result, Void> worker = new SwingWorker<>() {
            @Override
            protected BankClientManager.Result doInBackground() throws Exception {
                return task.run();
            }

            @Override
            protected void done() {
                loading.dispose();
                try {
                    handler.handle(get());
                } catch (Exception ex) {
                    showError(ex.getMessage());
                }
            }
        };
        worker.execute();
        loading.setVisible(true);
    }

    private JDialog createLoadingDialog(String text) {
        JDialog dialog = new JDialog(this, "Please wait", true);
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.add(new JLabel(text, SwingConstants.CENTER), BorderLayout.NORTH);
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        panel.add(bar, BorderLayout.CENTER);
        dialog.setContentPane(panel);
        dialog.setSize(300, 120);
        dialog.setLocationRelativeTo(this);
        return dialog;
    }

    private void setStatus(String text) {
        statusBar.setText(text == null ? " " : text);
    }

    private void showError(String message) {
        setStatus("Error: " + message);
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showSuccess(String message) {
        setStatus("Success: " + message);
        JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private String extractNumber(String text) {
        if (text == null) {
            return "";
        }
        String num = text.replaceAll("[^0-9.]+", "");
        return num;
    }

    private interface Task {
        BankClientManager.Result run() throws Exception;
    }

    private interface ResultHandler {
        void handle(BankClientManager.Result result);
    }
}
