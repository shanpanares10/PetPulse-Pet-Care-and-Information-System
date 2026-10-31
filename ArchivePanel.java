package petpulse.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import petpulse.data.Db;
import petpulse.util.Theme;

/** Archive screen (formerly "Trash Bin"): restore or permanently delete archived records. */
public class ArchivePanel extends JPanel implements Refreshable {

    private static final String SQL =
        "SELECT 'Pet' AS type, id, CONCAT_WS(' | ', pet_name, species, owner_name) AS details, archived_at "
        + "FROM pets WHERE archived = 1 "
        + "UNION ALL SELECT 'Appointment', a.id, CONCAT_WS(' | ', p.pet_name, a.service, a.appt_date), a.archived_at "
        + "FROM appointments a JOIN pets p ON p.id = a.pet_id WHERE a.archived = 1 "
        + "UNION ALL SELECT 'Vaccination', v.id, CONCAT_WS(' | ', p.pet_name, v.vaccine_name, v.status), v.archived_at "
        + "FROM vaccinations v JOIN pets p ON p.id = v.pet_id WHERE v.archived = 1 "
        + "UNION ALL SELECT 'Payment', y.id, CONCAT_WS(' | ', p.pet_name, y.description, y.amount), y.archived_at "
        + "FROM payments y JOIN pets p ON p.id = y.pet_id WHERE y.archived = 1 "
        + "ORDER BY archived_at DESC";

    private static final String LOG_SQL =
        "SELECT record_type, record_id, details, action, performed_by, logged_at "
        + "FROM archive_log ORDER BY id DESC LIMIT 1000";

    private final Runnable onChange;
    private final DefaultTableModel model;
    private final JTable tbl;
    private final DefaultTableModel logModel;
    private final JTable tblLog;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardHost = new JPanel(cardLayout);
    private final JButton btnRestore;
    private final JButton btnDelete;
    private final JButton btnLog;
    private boolean logMode = false;
    private final JLabel lblCount = new JLabel(" ");

    public ArchivePanel(Runnable onChange) {
        this.onChange = onChange;
        setLayout(new BorderLayout(0, 12));
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(22, 28, 18, 28));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel lblTitle = new JLabel("Archive");
        lblTitle.setFont(Theme.font(Font.BOLD, 26));
        lblTitle.setForeground(Theme.DARK);
        header.add(lblTitle, BorderLayout.WEST);

        JPanel tools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        tools.setOpaque(false);
        btnRestore = Theme.button("Restore", Theme.ACCENT);
        btnDelete = Theme.button("Delete Permanently", Theme.DANGER);
        btnLog = Theme.button("Archive Log", Theme.DARK);
        JButton btnRefresh = Theme.button("Refresh", Theme.MUTED);
        tools.add(btnRestore);
        tools.add(btnDelete);
        tools.add(btnLog);
        tools.add(btnRefresh);
        header.add(tools, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        model = new DefaultTableModel(new String[] {"Type", "ID", "Details", "Archived On"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tbl = new JTable(model);
        Theme.styleTable(tbl);
        tbl.getColumnModel().getColumn(0).setPreferredWidth(90);
        tbl.getColumnModel().getColumn(1).setPreferredWidth(40);
        tbl.getColumnModel().getColumn(2).setPreferredWidth(420);
        tbl.getColumnModel().getColumn(3).setPreferredWidth(150);

        logModel = new DefaultTableModel(
                new String[] {"Type", "ID", "Details", "Action", "By", "Date & Time"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblLog = new JTable(logModel);
        Theme.styleTable(tblLog);
        tblLog.getColumnModel().getColumn(0).setPreferredWidth(90);
        tblLog.getColumnModel().getColumn(1).setPreferredWidth(40);
        tblLog.getColumnModel().getColumn(2).setPreferredWidth(330);
        tblLog.getColumnModel().getColumn(3).setPreferredWidth(90);
        tblLog.getColumnModel().getColumn(4).setPreferredWidth(110);
        tblLog.getColumnModel().getColumn(5).setPreferredWidth(150);

        cardHost.setOpaque(false);
        cardHost.add(Theme.scroll(tbl), "items");
        cardHost.add(Theme.scroll(tblLog), "log");
        add(cardHost, BorderLayout.CENTER);

        lblCount.setForeground(Theme.MUTED);
        add(lblCount, BorderLayout.SOUTH);

        btnRestore.addActionListener(e -> restore());
        btnDelete.addActionListener(e -> deleteForever());
        btnRefresh.addActionListener(e -> refresh());
        btnLog.addActionListener(e -> toggleLog());
    }

    /** Switches between the restorable archive list and the history of archive actions. */
    private void toggleLog() {
        logMode = !logMode;
        cardLayout.show(cardHost, logMode ? "log" : "items");
        btnLog.setText(logMode ? "Back to Archive" : "Archive Log");
        btnRestore.setEnabled(!logMode);
        btnDelete.setEnabled(!logMode);
        updateCount();
    }

    private void updateCount() {
        lblCount.setText(logMode
                ? logModel.getRowCount() + " log entr" + (logModel.getRowCount() == 1 ? "y" : "ies")
                  + " (latest 1000)"
                : model.getRowCount() + " archived record(s)");
    }

    private static String tableFor(String type) {
        switch (type) {
            case "Pet": return "pets";
            case "Appointment": return "appointments";
            case "Vaccination": return "vaccinations";
            default: return "payments";
        }
    }

    private void restore() {
        int row = tbl.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an archived record first.");
            return;
        }
        String table = tableFor(String.valueOf(model.getValueAt(row, 0)));
        int id = Integer.parseInt(String.valueOf(model.getValueAt(row, 1)));
        run("UPDATE " + table + " SET archived = 0, archived_at = NULL WHERE id = ?", id);
    }

    private void deleteForever() {
        int row = tbl.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an archived record first.");
            return;
        }
        String type = String.valueOf(model.getValueAt(row, 0));
        String extra = type.equals("Pet")
                ? "\nThis also deletes all appointments, vaccinations and payments of that pet." : "";
        int ok = JOptionPane.showConfirmDialog(this,
                "Permanently delete this record? This cannot be undone." + extra,
                "Delete Permanently", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) {
            return;
        }
        int id = Integer.parseInt(String.valueOf(model.getValueAt(row, 1)));
        run("DELETE FROM " + tableFor(type) + " WHERE id = ?", id);
    }

    private void run(String sql, int id) {
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            refresh();
            if (onChange != null) {
                onChange.run();
            }
        } catch (SQLException ex) {
            Theme.showError(this, ex);
        }
    }

    @Override
    public void refresh() {
        try (Connection c = Db.getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(SQL)) {
            model.setRowCount(0);
            while (rs.next()) {
                model.addRow(new Object[] {rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)});
            }
            lblCount.setText(model.getRowCount() + " archived record(s)");
        } catch (SQLException ex) {
            Theme.showError(this, ex);
        }
        try (Connection c = Db.getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(LOG_SQL)) {
            logModel.setRowCount(0);
            while (rs.next()) {
                logModel.addRow(new Object[] {rs.getString(1), rs.getString(2), rs.getString(3),
                    rs.getString(4), rs.getString(5), rs.getString(6)});
            }
            updateCount();
        } catch (SQLException ex) {
            Theme.showError(this, ex);
        }
    }
}
