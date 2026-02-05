package client.gui;

import client.BankClientManager;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class MonitorFrame extends JFrame {
    private final JTextField serverIpField;
    private final JTextField serverPortField;
    private final JTextField durationField = new JTextField("60000", 8);
    private final JTextArea monitorTextArea = new JTextArea(10, 40);
    private final JButton startButton = new JButton("Start Monitoring");

    private final boolean enableRetry;

    public MonitorFrame(String host, int port, boolean enableRetry) {
        super("Monitor Window");
        this.enableRetry = enableRetry;

        this.serverIpField = new JTextField(host, 12);
        this.serverPortField = new JTextField(String.valueOf(port), 6);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        add(buildControls(), BorderLayout.NORTH);
        add(buildOutput(), BorderLayout.CENTER);

        startButton.addActionListener(e -> startMonitoring());

        pack();
        setLocationRelativeTo(null);
    }

    private JPanel buildControls() {
        JPanel panel = GuiUtils.createStandardPanel();

        panel.add(new JLabel("Server IP:"), GuiUtils.createLabelGbc(0, 0));
        panel.add(serverIpField, GuiUtils.createGbc(1, 0));
        panel.add(new JLabel("Port:"), GuiUtils.createLabelGbc(2, 0));
        panel.add(serverPortField, GuiUtils.createGbc(3, 0));

        panel.add(new JLabel("Duration (ms):"), GuiUtils.createLabelGbc(0, 1));
        panel.add(durationField, GuiUtils.createGbc(1, 1));
        panel.add(startButton, GuiUtils.createGbc(2, 1, 2));

        return panel;
    }

    private JScrollPane buildOutput() {
        monitorTextArea.setEditable(false);
        return new JScrollPane(monitorTextArea);
    }

    private void startMonitoring() {
        String host = serverIpField.getText().trim();
        String portText = serverPortField.getText().trim();
        String durationText = durationField.getText().trim();

        if (host.isEmpty() || portText.isEmpty() || durationText.isEmpty()) {
            GuiUtils.showError(this, "Please fill in server and duration.");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException ex) {
            GuiUtils.showError(this, "Invalid port.");
            return;
        }

        long durationMillis;
        try {
            durationMillis = Long.parseLong(durationText);
        } catch (NumberFormatException ex) {
            GuiUtils.showError(this, "Invalid duration.");
            return;
        }

        startButton.setEnabled(false);

        SwingWorker<BankClientManager.Result, Void> worker = new SwingWorker<>() {
            @Override
            protected BankClientManager.Result doInBackground() throws Exception {
                BankClientManager manager = BankClientManager.getInstance(host, port);
                manager.setServer(host, port);
                manager.setInvocationSemantics(enableRetry);
                manager.setServerMessageListener(msg -> SwingUtilities.invokeLater(() -> {
                    monitorTextArea.append(msg + "\n");
                    monitorTextArea.setCaretPosition(monitorTextArea.getDocument().getLength());
                }));
                return manager.startMonitor(durationMillis);
            }

            @Override
            protected void done() {
                startButton.setEnabled(true);
                try {
                    BankClientManager.Result result = get();
                    if (result.isSuccess()) {
                        monitorTextArea.append("[Monitor] Started for " + durationMillis + " ms.\n");
                        monitorTextArea.setCaretPosition(monitorTextArea.getDocument().getLength());
                    } else {
                        GuiUtils.showError(MonitorFrame.this, result.getMessage());
                    }
                } catch (Exception ex) {
                    GuiUtils.showError(MonitorFrame.this, ex.getMessage());
                }
            }
        };

        worker.execute();
    }
}
