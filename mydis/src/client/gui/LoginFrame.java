package client.gui;

import client.BankClientManager;
import client.UserSession;
import common.Constants;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.ButtonGroup;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class LoginFrame extends JFrame {
    private final JTextField serverIpField = new JTextField("127.0.0.1", 15);
    private final JTextField serverPortField = new JTextField(String.valueOf(Constants.SERVER_PORT), 6);

    private final JTextField accountIdField = new JTextField(10);
    private final JTextField nameField = new JTextField(15);
    private final JPasswordField passwordField = new JPasswordField(15);

    private final JTextField balanceField = new JTextField("0", 10);
    private final JComboBox<String> currencyBox = new JComboBox<>(new String[]{"USD", "RMB", "SGD", "JPY", "BPD"});

    private final JRadioButton loginRadio = new JRadioButton("Login", true);
    private final JRadioButton openRadio = new JRadioButton("Open New Account");

    private final JCheckBox retryCheck = new JCheckBox("Enable At-Least-Once Retry", false);
    private final JButton connectButton = new JButton("Connect & Login");
    private final JButton openMonitorButton = new JButton("Open Monitor Window");

    public LoginFrame() {
        super("Bank Client Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridBagLayout());
        setResizable(false);

        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(loginRadio);
        modeGroup.add(openRadio);

        JPanel panel = GuiUtils.createStandardPanel();

        int row = 0;

        panel.add(new JLabel("Server IP:"), GuiUtils.createLabelGbc(0, row));
        panel.add(serverIpField, GuiUtils.createGbc(1, row));
        panel.add(new JLabel("Port:"), GuiUtils.createLabelGbc(2, row));
        panel.add(serverPortField, GuiUtils.createGbc(3, row));

        row++;
        panel.add(new JLabel("Mode:"), GuiUtils.createLabelGbc(0, row));
        panel.add(loginRadio, GuiUtils.createGbc(1, row));
        panel.add(openRadio, GuiUtils.createGbc(2, row));

        row++;
        panel.add(new JLabel("Account ID:"), GuiUtils.createLabelGbc(0, row));
        panel.add(accountIdField, GuiUtils.createGbc(1, row));

        row++;
        panel.add(new JLabel("Name:"), GuiUtils.createLabelGbc(0, row));
        panel.add(nameField, GuiUtils.createGbc(1, row, 3));

        row++;
        panel.add(new JLabel("Password:"), GuiUtils.createLabelGbc(0, row));
        panel.add(passwordField, GuiUtils.createGbc(1, row, 3));

        row++;
        panel.add(new JLabel("Initial Balance:"), GuiUtils.createLabelGbc(0, row));
        panel.add(balanceField, GuiUtils.createGbc(1, row));
        panel.add(new JLabel("Currency:"), GuiUtils.createLabelGbc(2, row));
        panel.add(currencyBox, GuiUtils.createGbc(3, row));

        row++;
        panel.add(retryCheck, GuiUtils.createGbc(0, row, 4));

        row++;
        panel.add(connectButton, GuiUtils.createGbc(0, row, 4));

        row++;
        panel.add(openMonitorButton, GuiUtils.createGbc(0, row, 4));

        add(panel);

        updateOpenAccountFields();
        loginRadio.addActionListener(e -> updateOpenAccountFields());
        openRadio.addActionListener(e -> updateOpenAccountFields());

        connectButton.addActionListener(e -> onConnect());
        openMonitorButton.addActionListener(e -> onOpenMonitorWindow());

        pack();
        setLocationRelativeTo(null);
    }

    private void updateOpenAccountFields() {
        boolean isOpen = openRadio.isSelected();
        accountIdField.setEnabled(!isOpen);
        balanceField.setEnabled(isOpen);
        currencyBox.setEnabled(true);
    }

    private void onConnect() {
        String host = serverIpField.getText().trim();
        String portText = serverPortField.getText().trim();
        String accountIdText = accountIdField.getText().trim();
        String name = nameField.getText().trim();
        String password = new String(passwordField.getPassword());
        boolean enableRetry = retryCheck.isSelected();
        boolean isOpen = openRadio.isSelected();

        if (host.isEmpty() || portText.isEmpty() || name.isEmpty() || password.isEmpty()) {
            GuiUtils.showError(this, "Please fill in all required fields.");
            return;
        }

        int accountId = -1;
        if (!isOpen) {
            if (accountIdText.isEmpty()) {
                GuiUtils.showError(this, "Please enter account ID.");
                return;
            }
            try {
                accountId = Integer.parseInt(accountIdText);
            } catch (NumberFormatException ex) {
                GuiUtils.showError(this, "Invalid account ID.");
                return;
            }
        }

        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException ex) {
            GuiUtils.showError(this, "Invalid port.");
            return;
        }

        float balanceValue = 0f;
        String currencyValue = (String) currencyBox.getSelectedItem();
        if (isOpen) {
            try {
                balanceValue = Float.parseFloat(balanceField.getText().trim());
            } catch (NumberFormatException ex) {
                GuiUtils.showError(this, "Invalid initial balance.");
                return;
            }
        }

        final int finalAccountId = accountId;
        final float balance = balanceValue;
        final String currency = currencyValue;

        connectButton.setEnabled(false);

        SwingWorker<BankClientManager.Result, Void> worker = new SwingWorker<>() {
            @Override
            protected BankClientManager.Result doInBackground() throws Exception {
                BankClientManager manager = BankClientManager.getInstance(host, port);
                manager.setInvocationSemantics(enableRetry);
                if (isOpen) {
                    return manager.openAccount(name, password, currency, balance);
                }
                return manager.login(finalAccountId, name, password, currency);
            }

            @Override
            protected void done() {
                connectButton.setEnabled(true);
                try {
                    BankClientManager.Result result = get();
                    if (result.isSuccess()) {
                        BankClientManager manager = BankClientManager.getInstance();
                        UserSession session = manager.getCurrentUser();
                        if (session == null) {
                            GuiUtils.showError(LoginFrame.this, "Login success but session not created.");
                            return;
                        }
                        dispose();
                        MainFrame mainFrame = new MainFrame(manager);
                        mainFrame.setVisible(true);
                    } else {
                        JOptionPane.showMessageDialog(LoginFrame.this, result.getMessage(), "Login Failed", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(LoginFrame.this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    private void onOpenMonitorWindow() {
        String host = serverIpField.getText().trim();
        String portText = serverPortField.getText().trim();
        boolean enableRetry = retryCheck.isSelected();

        if (host.isEmpty() || portText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in server IP and port.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid port.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        MonitorFrame monitorFrame = new MonitorFrame(host, port, enableRetry);
        monitorFrame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
