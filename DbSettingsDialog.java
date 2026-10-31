package petpulse.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import petpulse.data.Db;
import petpulse.util.Theme;

/** Lets the user type the MySQL host / port / database / user / password, test it and save it. */
public class DbSettingsDialog extends JDialog {

    private final JTextField txtHost = new JTextField(Db.getHost(), 20);
    private final JTextField txtPort = new JTextField(Db.getPort(), 20);
    private final JTextField txtName = new JTextField(Db.getName(), 20);
    private final JTextField txtUser = new JTextField(Db.getUser(), 20);
    private final JPasswordField txtPass = new JPasswordField(Db.getPassword(), 20);
    private final JLabel lblResult = new JLabel(" ");
    private boolean saved;

    public DbSettingsDialog(Window owner) {
        super(owner, "Database Settings (MySQL)", ModalityType.APPLICATION_MODAL);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 6, 24));
        addRow(form, 0, "Host", txtHost);
        addRow(form, 1, "Port", txtPort);
        addRow(form, 2, "Database name", txtName);
        addRow(form, 3, "User", txtUser);
        addRow(form, 4, "Password", txtPass);

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 5;
        gc.gridwidth = 2;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(10, 0, 0, 0);
        lblResult.setFont(Theme.font(Font.BOLD, 12));
        form.add(lblResult, gc);

        JButton btnTest = Theme.button("Test Connection", Theme.SIDEBAR);
        JButton btnSave = Theme.button("Save", Theme.ACCENT);
        JButton btnCancel = Theme.button("Cancel", Theme.MUTED);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        buttons.setBackground(Color.WHITE);
        buttons.setBorder(BorderFactory.createEmptyBorder(0, 16, 8, 16));
        buttons.add(btnTest);
        buttons.add(btnCancel);
        buttons.add(btnSave);

        btnTest.addActionListener(e -> test());
        btnSave.addActionListener(e -> save());
        btnCancel.addActionListener(e -> dispose());

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);
    }

    private void addRow(JPanel form, int row, String label, java.awt.Component field) {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = row;
        gc.anchor = GridBagConstraints.WEST;
        JLabel l = new JLabel(label);
        l.setFont(Theme.font(Font.BOLD, 13));
        l.setForeground(Theme.DARK);
        gc.gridx = 0;
        gc.insets = new Insets(6, 0, 6, 16);
        form.add(l, gc);
        gc.gridx = 1;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(6, 0, 6, 0);
        form.add(field, gc);
    }

    private void apply() {
        Db.configure(txtHost.getText(), txtPort.getText(), txtName.getText(),
                txtUser.getText(), new String(txtPass.getPassword()));
    }

    private void test() {
        apply();
        lblResult.setForeground(Theme.MUTED);
        lblResult.setText("Connecting...");
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                try {
                    Db.initialize();
                    return null;
                } catch (Exception ex) {
                    return ex.getMessage();
                }
            }

            @Override
            protected void done() {
                try {
                    String err = get();
                    if (err == null) {
                        lblResult.setForeground(Theme.ACCENT);
                        lblResult.setText("Connected! Database and tables are ready.");
                    } else {
                        lblResult.setForeground(Theme.DANGER);
                        lblResult.setText("<html>Failed: " + err.replace("<", "&lt;") + "</html>");
                        pack();
                    }
                } catch (Exception ignored) {
                    // ignore
                }
            }
        }.execute();
    }

    private void save() {
        apply();
        try {
            Db.save();
            saved = true;
            dispose();
        } catch (java.io.IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not save db.properties:\n" + ex.getMessage(),
                    "PetPulse", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isSaved() {
        return saved;
    }
}
