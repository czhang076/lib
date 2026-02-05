package client.gui;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class GuiUtils {

    /**
     * Creates a standard GridBagConstraints object with default padding and alignment.
     */
    public static GridBagConstraints createGbc(int x, int y) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.insets = new Insets(6, 6, 6, 6); // Standard padding
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        return gbc;
    }

    /**
     * Creates a GridBagConstraints object with specific span and fill properties.
     */
    public static GridBagConstraints createGbc(int x, int y, int width) {
        GridBagConstraints gbc = createGbc(x, y);
        gbc.gridwidth = width;
        return gbc;
    }

    /**
     * Creates a GridBagConstraints object for a label (no fill).
     */
    public static GridBagConstraints createLabelGbc(int x, int y) {
        GridBagConstraints gbc = createGbc(x, y);
        gbc.fill = GridBagConstraints.NONE;
        return gbc;
    }

    /**
     * Helper to create a panel with GridBagLayout and consistent border.
     */
    public static JPanel createStandardPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        return panel;
    }

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void showSuccess(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }
}
