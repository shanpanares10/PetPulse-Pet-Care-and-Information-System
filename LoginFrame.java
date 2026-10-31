package petpulse.ui;

import java.awt.Color;
import java.awt.Cursor;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import petpulse.data.Db;
import petpulse.util.Branding;
import petpulse.util.Theme;

public class LoginFrame extends javax.swing.JFrame {

    /** Creates new form LoginFrame */
    public LoginFrame() {
        initComponents();
        postInit();
    }

    /** Code that is not part of the visual design (logo, hand cursor, database check). */
    private void postInit() {
        setIconImage(Branding.appIcon());
        setLocationRelativeTo(null);
        lblLogo.setIcon(Branding.badge(120));
        lblLogo.setText(null);
        btnLogin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogin.setBorderPainted(false);
        btnLogin.setOpaque(true);
        btnDbSettings.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        txtUser.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(Theme.CARD_BORDER, 1, true),
                javax.swing.BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        txtPass.setBorder(txtUser.getBorder());
        getRootPane().setDefaultButton(btnLogin);
        checkDatabase();
    }

    /** Connects (and creates the database / tables the first time) without freezing the window. */
    private void checkDatabase() {
        lblStatus.setForeground(Theme.MUTED);
        lblStatus.setText("Checking database...");
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
                    if (err == null && Db.getInitWarning() != null) {
                        lblStatus.setForeground(Theme.DANGER);
                        lblStatus.setText("\u25CF Connected, but history triggers are missing (hover)");
                        lblStatus.setToolTipText(Db.getInitWarning());
                    } else if (err == null) {
                        lblStatus.setForeground(Theme.ACCENT);
                        lblStatus.setText("\u25CF Database connected (" + Db.getName() + ")");
                    } else {
                        lblStatus.setForeground(Theme.DANGER);
                        lblStatus.setText("\u25CF Database offline - start MySQL, then Database Settings");
                        lblStatus.setToolTipText(err);
                    }
                } catch (Exception ignored) {
                    // ignore
                }
            }
        }.execute();
    }

    private void doLogin() {
        String user = txtUser.getText().trim();
        String pass = new String(txtPass.getPassword());
        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter your username and password.",
                    "PetPulse", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            String fullName = Db.authenticate(user, pass);
            if (fullName == null) {
                JOptionPane.showMessageDialog(this, "Wrong username or password.",
                        "PetPulse", JOptionPane.ERROR_MESSAGE);
                txtPass.setText("");
                txtPass.requestFocus();
                return;
            }
            Db.setCurrentUser(fullName);
            new MainFrame(fullName).setVisible(true);
            dispose();
        } catch (java.sql.SQLException ex) {
            Theme.showError(this, ex);
            checkDatabase();
        }
    }

    private void openDbSettings() {
        DbSettingsDialog dlg = new DbSettingsDialog(this);
        dlg.setVisible(true);
        checkDatabase();
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
    @SuppressWarnings("unchecked")
    private void initComponents() {

        leftPanel = new javax.swing.JPanel();
        brandBox = new javax.swing.JPanel();
        lblLogo = new javax.swing.JLabel();
        lblBrand = new javax.swing.JLabel();
        lblTagline = new javax.swing.JLabel();
        rightPanel = new javax.swing.JPanel();
        formPanel = new javax.swing.JPanel();
        lblSignIn = new javax.swing.JLabel();
        lblHint = new javax.swing.JLabel();
        userGroup = new javax.swing.JPanel();
        lblUser = new javax.swing.JLabel();
        txtUser = new javax.swing.JTextField();
        passGroup = new javax.swing.JPanel();
        lblPass = new javax.swing.JLabel();
        txtPass = new javax.swing.JPasswordField();
        loginWrap = new javax.swing.JPanel();
        btnLogin = new javax.swing.JButton();
        dbPanel = new javax.swing.JPanel();
        lblStatus = new javax.swing.JLabel();
        btnDbSettings = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("PetPulse - Login"); // NOI18N
        setPreferredSize(new java.awt.Dimension(860, 540));
        setResizable(false);

        lblLogo.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        lblLogo.setForeground(new java.awt.Color(255, 255, 255));
        lblLogo.setText(" "); // NOI18N
        lblLogo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        lblBrand.setFont(new java.awt.Font("Segoe UI", 1, 38)); // NOI18N
        lblBrand.setForeground(new java.awt.Color(255, 255, 255));
        lblBrand.setText("PetPulse"); // NOI18N
        lblBrand.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        lblTagline.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        lblTagline.setForeground(new java.awt.Color(214, 240, 222));
        lblTagline.setText("Pet Care and Information System"); // NOI18N
        lblTagline.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        brandBox.setOpaque(false);
        brandBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(70, 30, 70, 30));
        brandBox.setLayout(new java.awt.GridLayout(0, 1));
        brandBox.add(lblLogo);
        brandBox.add(lblBrand);
        brandBox.add(lblTagline);

        leftPanel.setBackground(new java.awt.Color(46, 125, 79));
        leftPanel.setPreferredSize(new java.awt.Dimension(360, 0));
        leftPanel.setLayout(new java.awt.BorderLayout());
        leftPanel.add(brandBox, java.awt.BorderLayout.CENTER);

        lblSignIn.setFont(new java.awt.Font("Segoe UI", 1, 30)); // NOI18N
        lblSignIn.setForeground(new java.awt.Color(20, 83, 45));
        lblSignIn.setText("Sign In"); // NOI18N

        lblHint.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        lblHint.setForeground(new java.awt.Color(91, 138, 107));
        lblHint.setText("Enter your administrator account"); // NOI18N

        lblUser.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblUser.setForeground(new java.awt.Color(20, 83, 45));
        lblUser.setText("Username"); // NOI18N

        txtUser.setFont(new java.awt.Font("Segoe UI", 0, 15)); // NOI18N
        txtUser.setText("admin"); // NOI18N

        userGroup.setOpaque(false);
        userGroup.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 0, 8, 0));
        userGroup.setLayout(new java.awt.BorderLayout());
        userGroup.add(lblUser, java.awt.BorderLayout.NORTH);
        userGroup.add(txtUser, java.awt.BorderLayout.CENTER);

        lblPass.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblPass.setForeground(new java.awt.Color(20, 83, 45));
        lblPass.setText("Password"); // NOI18N

        txtPass.setFont(new java.awt.Font("Segoe UI", 0, 15)); // NOI18N

        passGroup.setOpaque(false);
        passGroup.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 0, 8, 0));
        passGroup.setLayout(new java.awt.BorderLayout());
        passGroup.add(lblPass, java.awt.BorderLayout.NORTH);
        passGroup.add(txtPass, java.awt.BorderLayout.CENTER);

        btnLogin.setBackground(new java.awt.Color(46, 125, 79));
        btnLogin.setFont(new java.awt.Font("Segoe UI", 1, 15)); // NOI18N
        btnLogin.setForeground(new java.awt.Color(255, 255, 255));
        btnLogin.setText("LOGIN"); // NOI18N
        btnLogin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLoginActionPerformed(evt);
            }
        });

        loginWrap.setOpaque(false);
        loginWrap.setBorder(javax.swing.BorderFactory.createEmptyBorder(14, 0, 0, 0));
        loginWrap.setLayout(new java.awt.BorderLayout());
        loginWrap.add(btnLogin, java.awt.BorderLayout.CENTER);

        formPanel.setOpaque(false);
        formPanel.setLayout(new java.awt.GridLayout(0, 1));
        formPanel.add(lblSignIn);
        formPanel.add(lblHint);
        formPanel.add(userGroup);
        formPanel.add(passGroup);
        formPanel.add(loginWrap);

        lblStatus.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblStatus.setForeground(new java.awt.Color(91, 138, 107));
        lblStatus.setText("Checking database..."); // NOI18N

        btnDbSettings.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnDbSettings.setForeground(new java.awt.Color(46, 125, 79));
        btnDbSettings.setText("Database Settings"); // NOI18N
        btnDbSettings.setBorderPainted(false);
        btnDbSettings.setContentAreaFilled(false);
        btnDbSettings.setFocusPainted(false);
        btnDbSettings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDbSettingsActionPerformed(evt);
            }
        });

        dbPanel.setOpaque(false);
        dbPanel.setLayout(new java.awt.BorderLayout());
        dbPanel.add(lblStatus, java.awt.BorderLayout.CENTER);
        dbPanel.add(btnDbSettings, java.awt.BorderLayout.EAST);

        rightPanel.setBackground(new java.awt.Color(255, 255, 255));
        rightPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(40, 56, 28, 56));
        rightPanel.setLayout(new java.awt.BorderLayout());
        rightPanel.add(formPanel, java.awt.BorderLayout.NORTH);
        rightPanel.add(dbPanel, java.awt.BorderLayout.SOUTH);

        getContentPane().add(leftPanel, java.awt.BorderLayout.WEST);
        getContentPane().add(rightPanel, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>                        


    private void btnLoginActionPerformed(java.awt.event.ActionEvent evt) {                                         
        doLogin();
    }                                        

    private void btnDbSettingsActionPerformed(java.awt.event.ActionEvent evt) {                                              
        openDbSettings();
    }                                             

    // Variables declaration - do not modify                     
    private javax.swing.JPanel brandBox;
    private javax.swing.JButton btnDbSettings;
    private javax.swing.JButton btnLogin;
    private javax.swing.JPanel dbPanel;
    private javax.swing.JPanel formPanel;
    private javax.swing.JLabel lblBrand;
    private javax.swing.JLabel lblHint;
    private javax.swing.JLabel lblLogo;
    private javax.swing.JLabel lblPass;
    private javax.swing.JLabel lblSignIn;
    private javax.swing.JLabel lblStatus;
    private javax.swing.JLabel lblTagline;
    private javax.swing.JLabel lblUser;
    private javax.swing.JPanel leftPanel;
    private javax.swing.JPanel loginWrap;
    private javax.swing.JPanel passGroup;
    private javax.swing.JPanel rightPanel;
    private javax.swing.JPasswordField txtPass;
    private javax.swing.JTextField txtUser;
    private javax.swing.JPanel userGroup;
    // End of variables declaration                   
}
