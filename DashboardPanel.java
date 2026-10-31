package petpulse.ui;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import petpulse.data.Db;
import petpulse.util.Theme;

public class DashboardPanel extends javax.swing.JPanel implements Refreshable {

    private final DefaultTableModel model = new DefaultTableModel(
            new String[] {"Time", "Pet", "Owner", "Service", "Veterinarian", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tblToday = new JTable(model);

    /** Creates new form DashboardPanel */
    public DashboardPanel() {
        initComponents();
        Theme.cardBorder(statsPanel);
        Theme.styleTable(tblToday);
        tableHolder.add(Theme.scroll(tblToday), java.awt.BorderLayout.CENTER);
        // thin dividers between the four counters
        for (javax.swing.JPanel c : new javax.swing.JPanel[] {cardAppts, cardPayments, cardVacc}) {
            c.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 1, 0, 0, Theme.CARD_BORDER));
        }
    }

    public void setUserName(String name) {
        lblWelcome.setText("Welcome, " + name + "!");
    }

    @Override
    public void refresh() {
        try {
            lblPetsValue.setText(Db.scalar("SELECT COUNT(*) FROM pets WHERE archived = 0"));
            lblApptsValue.setText(Db.scalar("SELECT COUNT(*) FROM appointments "
                    + "WHERE archived = 0 AND appt_date = CURDATE() AND status <> 'Cancelled'"));
            String total = Db.scalar("SELECT COALESCE(SUM(amount), 0) FROM payments WHERE archived = 0");
            lblPaymentsValue.setText("\u20B1" + String.format("%,.2f", new java.math.BigDecimal(total)));
            lblVaccValue.setText(Db.scalar("SELECT COUNT(*) FROM vaccinations "
                    + "WHERE archived = 0 AND status = 'Pending'"));

            try (Connection c = Db.getConnection(); Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT COALESCE(a.appt_time, ''), p.pet_name, p.owner_name, a.service, "
                         + "COALESCE(a.veterinarian, ''), a.status "
                         + "FROM appointments a JOIN pets p ON p.id = a.pet_id "
                         + "WHERE a.archived = 0 AND a.appt_date = CURDATE() ORDER BY a.appt_time, a.id")) {
                model.setRowCount(0);
                while (rs.next()) {
                    model.addRow(new Object[] {rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6)});
                }
            }
        } catch (SQLException ex) {
            Theme.showError(this, ex);
        }
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
    @SuppressWarnings("unchecked")
    private void initComponents() {

        headerPanel = new javax.swing.JPanel();
        lblWelcome = new javax.swing.JLabel();
        lblSubtitle = new javax.swing.JLabel();
        bodyPanel = new javax.swing.JPanel();
        statsPanel = new javax.swing.JPanel();
        cardPets = new javax.swing.JPanel();
        lblPetsTitle = new javax.swing.JLabel();
        lblPetsValue = new javax.swing.JLabel();
        cardAppts = new javax.swing.JPanel();
        lblApptsTitle = new javax.swing.JLabel();
        lblApptsValue = new javax.swing.JLabel();
        cardPayments = new javax.swing.JPanel();
        lblPaymentsTitle = new javax.swing.JLabel();
        lblPaymentsValue = new javax.swing.JLabel();
        cardVacc = new javax.swing.JPanel();
        lblVaccTitle = new javax.swing.JLabel();
        lblVaccValue = new javax.swing.JLabel();
        todayPanel = new javax.swing.JPanel();
        lblToday = new javax.swing.JLabel();
        tableHolder = new javax.swing.JPanel();

        setBackground(new java.awt.Color(244, 250, 245));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(26, 30, 22, 30));
        setLayout(new java.awt.BorderLayout());

        lblWelcome.setFont(new java.awt.Font("Segoe UI", 1, 34)); // NOI18N
        lblWelcome.setForeground(new java.awt.Color(20, 83, 45));
        lblWelcome.setText("Welcome, Admin!"); // NOI18N

        lblSubtitle.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        lblSubtitle.setForeground(new java.awt.Color(91, 138, 107));
        lblSubtitle.setText("Pet Care and Information System"); // NOI18N

        headerPanel.setOpaque(false);
        headerPanel.setLayout(new java.awt.GridLayout(0, 1));
        headerPanel.add(lblWelcome);
        headerPanel.add(lblSubtitle);

        lblPetsTitle.setFont(new java.awt.Font("Segoe UI", 0, 15)); // NOI18N
        lblPetsTitle.setForeground(new java.awt.Color(20, 83, 45));
        lblPetsTitle.setText("Total Pets"); // NOI18N
        lblPetsTitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        lblPetsValue.setFont(new java.awt.Font("Segoe UI", 1, 36)); // NOI18N
        lblPetsValue.setForeground(new java.awt.Color(20, 83, 45));
        lblPetsValue.setText("0"); // NOI18N
        lblPetsValue.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        cardPets.setOpaque(false);
        cardPets.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 6, 6, 6));
        cardPets.setLayout(new java.awt.GridLayout(0, 1));
        cardPets.add(lblPetsTitle);
        cardPets.add(lblPetsValue);

        lblApptsTitle.setFont(new java.awt.Font("Segoe UI", 0, 15)); // NOI18N
        lblApptsTitle.setForeground(new java.awt.Color(20, 83, 45));
        lblApptsTitle.setText("Today's Appointments"); // NOI18N
        lblApptsTitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        lblApptsValue.setFont(new java.awt.Font("Segoe UI", 1, 36)); // NOI18N
        lblApptsValue.setForeground(new java.awt.Color(20, 83, 45));
        lblApptsValue.setText("0"); // NOI18N
        lblApptsValue.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        cardAppts.setOpaque(false);
        cardAppts.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 6, 6, 6));
        cardAppts.setLayout(new java.awt.GridLayout(0, 1));
        cardAppts.add(lblApptsTitle);
        cardAppts.add(lblApptsValue);

        lblPaymentsTitle.setFont(new java.awt.Font("Segoe UI", 0, 15)); // NOI18N
        lblPaymentsTitle.setForeground(new java.awt.Color(20, 83, 45));
        lblPaymentsTitle.setText("Total Payments"); // NOI18N
        lblPaymentsTitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        lblPaymentsValue.setFont(new java.awt.Font("Segoe UI", 1, 36)); // NOI18N
        lblPaymentsValue.setForeground(new java.awt.Color(20, 83, 45));
        lblPaymentsValue.setText("0"); // NOI18N
        lblPaymentsValue.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        cardPayments.setOpaque(false);
        cardPayments.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 6, 6, 6));
        cardPayments.setLayout(new java.awt.GridLayout(0, 1));
        cardPayments.add(lblPaymentsTitle);
        cardPayments.add(lblPaymentsValue);

        lblVaccTitle.setFont(new java.awt.Font("Segoe UI", 0, 15)); // NOI18N
        lblVaccTitle.setForeground(new java.awt.Color(20, 83, 45));
        lblVaccTitle.setText("Pending Vaccinations"); // NOI18N
        lblVaccTitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        lblVaccValue.setFont(new java.awt.Font("Segoe UI", 1, 36)); // NOI18N
        lblVaccValue.setForeground(new java.awt.Color(20, 83, 45));
        lblVaccValue.setText("0"); // NOI18N
        lblVaccValue.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        cardVacc.setOpaque(false);
        cardVacc.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 6, 6, 6));
        cardVacc.setLayout(new java.awt.GridLayout(0, 1));
        cardVacc.add(lblVaccTitle);
        cardVacc.add(lblVaccValue);

        statsPanel.setBackground(new java.awt.Color(226, 243, 230));
        statsPanel.setPreferredSize(new java.awt.Dimension(0, 132));
        statsPanel.setLayout(new java.awt.GridLayout(1, 0));
        statsPanel.add(cardPets);
        statsPanel.add(cardAppts);
        statsPanel.add(cardPayments);
        statsPanel.add(cardVacc);

        lblToday.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lblToday.setForeground(new java.awt.Color(20, 83, 45));
        lblToday.setText("Today's Appointments"); // NOI18N
        lblToday.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 10, 0));

        tableHolder.setOpaque(false);
        tableHolder.setLayout(new java.awt.BorderLayout());

        todayPanel.setOpaque(false);
        todayPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(24, 0, 0, 0));
        todayPanel.setLayout(new java.awt.BorderLayout());
        todayPanel.add(lblToday, java.awt.BorderLayout.NORTH);
        todayPanel.add(tableHolder, java.awt.BorderLayout.CENTER);

        bodyPanel.setOpaque(false);
        bodyPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(22, 0, 0, 0));
        bodyPanel.setLayout(new java.awt.BorderLayout());
        bodyPanel.add(statsPanel, java.awt.BorderLayout.NORTH);
        bodyPanel.add(todayPanel, java.awt.BorderLayout.CENTER);

        add(headerPanel, java.awt.BorderLayout.NORTH);
        add(bodyPanel, java.awt.BorderLayout.CENTER);
    }// </editor-fold>                        



    // Variables declaration - do not modify                     
    private javax.swing.JPanel bodyPanel;
    private javax.swing.JPanel cardAppts;
    private javax.swing.JPanel cardPayments;
    private javax.swing.JPanel cardPets;
    private javax.swing.JPanel cardVacc;
    private javax.swing.JPanel headerPanel;
    private javax.swing.JLabel lblApptsTitle;
    private javax.swing.JLabel lblApptsValue;
    private javax.swing.JLabel lblPaymentsTitle;
    private javax.swing.JLabel lblPaymentsValue;
    private javax.swing.JLabel lblPetsTitle;
    private javax.swing.JLabel lblPetsValue;
    private javax.swing.JLabel lblSubtitle;
    private javax.swing.JLabel lblToday;
    private javax.swing.JLabel lblVaccTitle;
    private javax.swing.JLabel lblVaccValue;
    private javax.swing.JLabel lblWelcome;
    private javax.swing.JPanel statsPanel;
    private javax.swing.JPanel tableHolder;
    private javax.swing.JPanel todayPanel;
    // End of variables declaration                   
}
