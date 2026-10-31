package petpulse.ui;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import petpulse.util.Branding;
import petpulse.util.Icons;
import petpulse.util.Theme;

public class MainFrame extends javax.swing.JFrame {

    private final Map<String, Refreshable> screens = new LinkedHashMap<>();
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private final CardLayout cards = new CardLayout();
    private final DashboardPanel dashboard = new DashboardPanel();

    public MainFrame(String fullName) {
        initComponents();
        setIconImage(Branding.appIcon());
        setLocationRelativeTo(null);
        lblAdmin.setText(fullName);
        dashboard.setUserName(fullName);
        styleChrome();
        buildScreens();
        navigate("Dashboard");
    }

    /** Icons, hover cursor and the logo (not part of the visual design). */
    private void styleChrome() {
        lblBrand.setIcon(Branding.badge(60));
        lblBrand.setIconTextGap(14);
        lblAdmin.setIcon(Icons.of("user", 28, Theme.DARK));
        lblAdmin.setIconTextGap(10);
        sidebar.setBorder(BorderFactory.createEmptyBorder());
        logoutPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 12, 0, 12),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.SIDEBAR_ACTIVE),
                        BorderFactory.createEmptyBorder(12, 0, 20, 0))));

        String[][] icons = {
            {"Dashboard", "home"}, {"Pet Information", "paw"}, {"Appointments", "calendar"},
            {"Vaccinations", "syringe"}, {"Payments", "card"}, {"Transaction History", "clock"},
            {"Pet Records", "document"}, {"Archive", "archive"}};
        JButton[] btns = {btnDashboard, btnPets, btnAppointments, btnVaccinations, btnPayments,
            btnTransactions, btnRecords, btnArchive};
        for (int i = 0; i < btns.length; i++) {
            btns[i].setIcon(Icons.of(icons[i][1], 24, Color.WHITE));
            btns[i].setIconTextGap(18);
            btns[i].setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            navButtons.put(icons[i][0], btns[i]);
        }
        btnLogout.setIcon(Icons.of("logout", 24, Color.WHITE));
        btnLogout.setIconTextGap(18);
        btnLogout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnTopLogout.setIcon(Icons.of("logout", 26, Theme.DARK));
        btnTopLogout.setIconTextGap(8);
        btnTopLogout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    /** Creates every screen once and puts it in the content area. */
    private void buildScreens() {
        contentPanel.setLayout(cards);
        Runnable refreshAll = this::refreshAll;

        CrudPanel pets = new CrudPanel("Pet Information", "pets", "t.id DESC", refreshAll,
                FieldDef.text("Pet Name", "pet_name", true),
                FieldDef.combo("Species", "species", "Dog", "Cat", "Bird", "Rabbit", "Hamster", "Other"),
                FieldDef.text("Breed", "breed", false),
                FieldDef.combo("Gender", "gender", "Male", "Female"),
                FieldDef.text("Age", "age", false),
                FieldDef.text("Owner Name", "owner_name", true),
                FieldDef.text("Contact No.", "contact_no", false),
                FieldDef.text("Address", "address", false));

        CrudPanel appointments = new CrudPanel("Appointments", "appointments",
                "t.appt_date DESC, t.appt_time DESC, t.id DESC", refreshAll,
                FieldDef.pet(),
                FieldDef.date("Date", "appt_date", true, true),
                FieldDef.text("Time (e.g. 09:30 AM)", "appt_time", false),
                FieldDef.combo("Service", "service", "Check-up", "Vaccination", "Grooming",
                        "Deworming", "Surgery", "Dental", "Other"),
                FieldDef.text("Veterinarian", "veterinarian", false),
                FieldDef.combo("Status", "status", "Scheduled", "Completed", "Cancelled"),
                FieldDef.text("Notes", "notes", false));

        CrudPanel vaccinations = new CrudPanel("Vaccinations", "vaccinations", "t.id DESC", refreshAll,
                FieldDef.pet(),
                FieldDef.text("Vaccine Name", "vaccine_name", true),
                FieldDef.date("Date Given", "date_given", false, false),
                FieldDef.date("Next Due", "next_due", false, false),
                FieldDef.combo("Status", "status", "Pending", "Done"),
                FieldDef.text("Remarks", "remarks", false));

        CrudPanel payments = new CrudPanel("Payments", "payments", "t.payment_date DESC, t.id DESC", refreshAll,
                FieldDef.pet(),
                FieldDef.text("Description", "description", true),
                FieldDef.number("Amount (\u20B1)", "amount", true),
                FieldDef.date("Payment Date", "payment_date", true, true),
                FieldDef.combo("Method", "method", "Cash", "GCash", "Maya", "Bank Transfer", "Card"));

        // Transaction History = the transaction_history ledger table (every payment event,
        // including edits, archives, restores and permanent deletes, with who did it).
        ReportPanel transactions = new ReportPanel("Transaction History",
                "SELECT h.id, h.receipt_no, h.payment_date, h.logged_at, h.action, h.pet_name, h.owner_name, "
                + "h.description, h.method, h.performed_by, h.amount "
                + "FROM transaction_history h ORDER BY h.id DESC",
                new String[] {"Receipt #", "Payment Date", "Logged On", "Action", "Pet", "Owner",
                    "Description", "Method", "By", "Amount (\u20B1)"},
                false, null, null,
                "Total collected (active payments)",
                "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE archived = 0");

        ReportPanel records = new ReportPanel("Pet Records",
                "SELECT p.id, p.pet_name, p.species, p.breed, p.owner_name, p.contact_no, "
                + "(SELECT COUNT(*) FROM appointments a WHERE a.pet_id = p.id AND a.archived = 0), "
                + "(SELECT COUNT(*) FROM vaccinations v WHERE v.pet_id = p.id AND v.archived = 0), "
                + "(SELECT COALESCE(SUM(y.amount), 0) FROM payments y WHERE y.pet_id = p.id AND y.archived = 0) "
                + "FROM pets p WHERE p.archived = 0 ORDER BY p.pet_name",
                new String[] {"Pet", "Species", "Breed", "Owner", "Contact", "Appointments", "Vaccinations", "Total Paid (\u20B1)"},
                false, "View Record", id -> PetRecordDialog.show(this, id));

        ArchivePanel archive = new ArchivePanel(refreshAll);

        addScreen("Dashboard", dashboard, dashboard);
        addScreen("Pet Information", pets, pets);
        addScreen("Appointments", appointments, appointments);
        addScreen("Vaccinations", vaccinations, vaccinations);
        addScreen("Payments", payments, payments);
        addScreen("Transaction History", transactions, transactions);
        addScreen("Pet Records", records, records);
        addScreen("Archive", archive, archive);
    }

    private void addScreen(String key, java.awt.Component view, Refreshable r) {
        contentPanel.add(view, key);
        screens.put(key, r);
    }

    /** Shows one screen, reloads its data and highlights the sidebar button. */
    private void navigate(String key) {
        Refreshable r = screens.get(key);
        if (r == null) {
            return;
        }
        r.refresh();
        cards.show(contentPanel, key);
        lblPageTitle.setText(key);
        setTitle("PetPulse - " + key);
        for (Map.Entry<String, JButton> en : navButtons.entrySet()) {
            boolean on = en.getKey().equals(key);
            en.getValue().setOpaque(on);
            en.getValue().setBackground(Theme.SIDEBAR_ACTIVE);
            en.getValue().repaint();
        }
    }

    /** Called after any add / edit / archive so the Dashboard and every list stay in sync. */
    private void refreshAll() {
        for (Refreshable r : screens.values()) {
            r.refresh();
        }
    }

    private void doLogout() {
        int ok = JOptionPane.showConfirmDialog(this, "Log out of PetPulse?", "Logout",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (ok == JOptionPane.YES_OPTION) {
            petpulse.data.Db.setCurrentUser(null);
            new LoginFrame().setVisible(true);
            dispose();
        }
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
    @SuppressWarnings("unchecked")
    private void initComponents() {

        sidebar = new javax.swing.JPanel();
        brandPanel = new javax.swing.JPanel();
        lblBrand = new javax.swing.JLabel();
        navPanel = new javax.swing.JPanel();
        btnDashboard = new javax.swing.JButton();
        btnPets = new javax.swing.JButton();
        btnAppointments = new javax.swing.JButton();
        btnVaccinations = new javax.swing.JButton();
        btnPayments = new javax.swing.JButton();
        btnTransactions = new javax.swing.JButton();
        btnRecords = new javax.swing.JButton();
        btnArchive = new javax.swing.JButton();
        logoutPanel = new javax.swing.JPanel();
        btnLogout = new javax.swing.JButton();
        mainArea = new javax.swing.JPanel();
        topBar = new javax.swing.JPanel();
        lblPageTitle = new javax.swing.JLabel();
        userPanel = new javax.swing.JPanel();
        lblAdmin = new javax.swing.JLabel();
        btnTopLogout = new javax.swing.JButton();
        contentPanel = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("PetPulse - Dashboard"); // NOI18N
        setMinimumSize(new java.awt.Dimension(1040, 640));
        setPreferredSize(new java.awt.Dimension(1200, 720));

        lblBrand.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        lblBrand.setForeground(new java.awt.Color(255, 255, 255));
        lblBrand.setText("<html><b style='font-size:22px'>PetPulse</b><br><span style='font-size:9px'>Pet Care and Information System</span></html>"); // NOI18N

        brandPanel.setOpaque(false);
        brandPanel.setPreferredSize(new java.awt.Dimension(270, 110));
        brandPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 16, 10, 10));
        brandPanel.setLayout(new java.awt.BorderLayout());
        brandPanel.add(lblBrand, java.awt.BorderLayout.CENTER);

        btnDashboard.setFont(new java.awt.Font("Segoe UI", 0, 15)); // NOI18N
        btnDashboard.setForeground(new java.awt.Color(255, 255, 255));
        btnDashboard.setText("Dashboard"); // NOI18N
        btnDashboard.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        btnDashboard.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 18, 8, 8));
        btnDashboard.setContentAreaFilled(false);
        btnDashboard.setFocusPainted(false);
        btnDashboard.setOpaque(false);
        btnDashboard.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                navActionPerformed(evt);
            }
        });

        btnPets.setFont(new java.awt.Font("Segoe UI", 0, 15)); // NOI18N
        btnPets.setForeground(new java.awt.Color(255, 255, 255));
        btnPets.setText("Pet Information"); // NOI18N
        btnPets.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        btnPets.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 18, 8, 8));
        btnPets.setContentAreaFilled(false);
        btnPets.setFocusPainted(false);
        btnPets.setOpaque(false);
        btnPets.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                navActionPerformed(evt);
            }
        });

        btnAppointments.setFont(new java.awt.Font("Segoe UI", 0, 15)); // NOI18N
        btnAppointments.setForeground(new java.awt.Color(255, 255, 255));
        btnAppointments.setText("Appointments"); // NOI18N
        btnAppointments.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        btnAppointments.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 18, 8, 8));
        btnAppointments.setContentAreaFilled(false);
        btnAppointments.setFocusPainted(false);
        btnAppointments.setOpaque(false);
        btnAppointments.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                navActionPerformed(evt);
            }
        });

        btnVaccinations.setFont(new java.awt.Font("Segoe UI", 0, 15)); // NOI18N
        btnVaccinations.setForeground(new java.awt.Color(255, 255, 255));
        btnVaccinations.setText("Vaccinations"); // NOI18N
        btnVaccinations.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        btnVaccinations.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 18, 8, 8));
        btnVaccinations.setContentAreaFilled(false);
        btnVaccinations.setFocusPainted(false);
        btnVaccinations.setOpaque(false);
        btnVaccinations.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                navActionPerformed(evt);
            }
        });

        btnPayments.setFont(new java.awt.Font("Segoe UI", 0, 15)); // NOI18N
        btnPayments.setForeground(new java.awt.Color(255, 255, 255));
        btnPayments.setText("Payments"); // NOI18N
        btnPayments.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        btnPayments.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 18, 8, 8));
        btnPayments.setContentAreaFilled(false);
        btnPayments.setFocusPainted(false);
        btnPayments.setOpaque(false);
        btnPayments.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                navActionPerformed(evt);
            }
        });

        btnTransactions.setFont(new java.awt.Font("Segoe UI", 0, 15)); // NOI18N
        btnTransactions.setForeground(new java.awt.Color(255, 255, 255));
        btnTransactions.setText("Transaction History"); // NOI18N
        btnTransactions.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        btnTransactions.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 18, 8, 8));
        btnTransactions.setContentAreaFilled(false);
        btnTransactions.setFocusPainted(false);
        btnTransactions.setOpaque(false);
        btnTransactions.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                navActionPerformed(evt);
            }
        });

        btnRecords.setFont(new java.awt.Font("Segoe UI", 0, 15)); // NOI18N
        btnRecords.setForeground(new java.awt.Color(255, 255, 255));
        btnRecords.setText("Pet Records"); // NOI18N
        btnRecords.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        btnRecords.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 18, 8, 8));
        btnRecords.setContentAreaFilled(false);
        btnRecords.setFocusPainted(false);
        btnRecords.setOpaque(false);
        btnRecords.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                navActionPerformed(evt);
            }
        });

        btnArchive.setFont(new java.awt.Font("Segoe UI", 0, 15)); // NOI18N
        btnArchive.setForeground(new java.awt.Color(255, 255, 255));
        btnArchive.setText("Archive"); // NOI18N
        btnArchive.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        btnArchive.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 18, 8, 8));
        btnArchive.setContentAreaFilled(false);
        btnArchive.setFocusPainted(false);
        btnArchive.setOpaque(false);
        btnArchive.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                navActionPerformed(evt);
            }
        });

        navPanel.setOpaque(false);
        navPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 12, 6, 12));
        navPanel.setLayout(new java.awt.GridLayout(0, 1));
        navPanel.add(btnDashboard);
        navPanel.add(btnPets);
        navPanel.add(btnAppointments);
        navPanel.add(btnVaccinations);
        navPanel.add(btnPayments);
        navPanel.add(btnTransactions);
        navPanel.add(btnRecords);
        navPanel.add(btnArchive);

        btnLogout.setFont(new java.awt.Font("Segoe UI", 0, 15)); // NOI18N
        btnLogout.setForeground(new java.awt.Color(255, 255, 255));
        btnLogout.setText("Logout"); // NOI18N
        btnLogout.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        btnLogout.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 18, 8, 8));
        btnLogout.setContentAreaFilled(false);
        btnLogout.setFocusPainted(false);
        btnLogout.setOpaque(false);
        btnLogout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                logoutActionPerformed(evt);
            }
        });

        logoutPanel.setOpaque(false);
        logoutPanel.setPreferredSize(new java.awt.Dimension(270, 84));
        logoutPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 20, 12));
        logoutPanel.setLayout(new java.awt.BorderLayout());
        logoutPanel.add(btnLogout, java.awt.BorderLayout.CENTER);

        sidebar.setBackground(new java.awt.Color(46, 125, 79));
        sidebar.setPreferredSize(new java.awt.Dimension(270, 0));
        sidebar.setLayout(new java.awt.BorderLayout());
        sidebar.add(brandPanel, java.awt.BorderLayout.NORTH);
        sidebar.add(navPanel, java.awt.BorderLayout.CENTER);
        sidebar.add(logoutPanel, java.awt.BorderLayout.SOUTH);

        lblPageTitle.setFont(new java.awt.Font("Segoe UI", 1, 22)); // NOI18N
        lblPageTitle.setForeground(new java.awt.Color(20, 83, 45));
        lblPageTitle.setText("Dashboard"); // NOI18N

        lblAdmin.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        lblAdmin.setForeground(new java.awt.Color(20, 83, 45));
        lblAdmin.setText("Admin"); // NOI18N
        lblAdmin.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 22));

        btnTopLogout.setFont(new java.awt.Font("Segoe UI", 1, 15)); // NOI18N
        btnTopLogout.setForeground(new java.awt.Color(20, 83, 45));
        btnTopLogout.setText("Logout"); // NOI18N
        btnTopLogout.setBorderPainted(false);
        btnTopLogout.setContentAreaFilled(false);
        btnTopLogout.setFocusPainted(false);
        btnTopLogout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                logoutActionPerformed(evt);
            }
        });

        userPanel.setOpaque(false);
        userPanel.setLayout(new java.awt.BorderLayout());
        userPanel.add(lblAdmin, java.awt.BorderLayout.CENTER);
        userPanel.add(btnTopLogout, java.awt.BorderLayout.EAST);

        topBar.setBackground(new java.awt.Color(168, 221, 181));
        topBar.setPreferredSize(new java.awt.Dimension(0, 70));
        topBar.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 28, 0, 24));
        topBar.setLayout(new java.awt.BorderLayout());
        topBar.add(lblPageTitle, java.awt.BorderLayout.WEST);
        topBar.add(userPanel, java.awt.BorderLayout.EAST);

        contentPanel.setBackground(new java.awt.Color(244, 250, 245));
        contentPanel.setLayout(new java.awt.BorderLayout());

        mainArea.setOpaque(false);
        mainArea.setLayout(new java.awt.BorderLayout());
        mainArea.add(topBar, java.awt.BorderLayout.NORTH);
        mainArea.add(contentPanel, java.awt.BorderLayout.CENTER);

        getContentPane().add(sidebar, java.awt.BorderLayout.WEST);
        getContentPane().add(mainArea, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>                        


    private void navActionPerformed(java.awt.event.ActionEvent evt) {                                    
        navigate(((javax.swing.JButton) evt.getSource()).getText());
    }                                   

    private void logoutActionPerformed(java.awt.event.ActionEvent evt) {                                       
        doLogout();
    }                                      

    // Variables declaration - do not modify                     
    private javax.swing.JPanel brandPanel;
    private javax.swing.JButton btnAppointments;
    private javax.swing.JButton btnArchive;
    private javax.swing.JButton btnDashboard;
    private javax.swing.JButton btnLogout;
    private javax.swing.JButton btnPayments;
    private javax.swing.JButton btnPets;
    private javax.swing.JButton btnRecords;
    private javax.swing.JButton btnTopLogout;
    private javax.swing.JButton btnTransactions;
    private javax.swing.JButton btnVaccinations;
    private javax.swing.JPanel contentPanel;
    private javax.swing.JLabel lblAdmin;
    private javax.swing.JLabel lblBrand;
    private javax.swing.JLabel lblPageTitle;
    private javax.swing.JPanel logoutPanel;
    private javax.swing.JPanel mainArea;
    private javax.swing.JPanel navPanel;
    private javax.swing.JPanel sidebar;
    private javax.swing.JPanel topBar;
    private javax.swing.JPanel userPanel;
    // End of variables declaration                   
}
