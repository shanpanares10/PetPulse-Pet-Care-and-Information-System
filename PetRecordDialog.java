package petpulse.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Window;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import petpulse.data.Db;
import petpulse.util.Theme;

/** Shows the complete history of one pet (info, appointments, vaccinations, payments). */
final class PetRecordDialog {

    private PetRecordDialog() {
    }

    static void show(Window owner, int petId) {
        StringBuilder sb = new StringBuilder();
        try (Connection c = Db.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement("SELECT * FROM pets WHERE id = ?")) {
                ps.setInt(1, petId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        sb.append("PET INFORMATION\n---------------------------------------------\n");
                        line(sb, "Name", rs.getString("pet_name"));
                        line(sb, "Species", rs.getString("species"));
                        line(sb, "Breed", rs.getString("breed"));
                        line(sb, "Gender", rs.getString("gender"));
                        line(sb, "Age", rs.getString("age"));
                        line(sb, "Owner", rs.getString("owner_name"));
                        line(sb, "Contact", rs.getString("contact_no"));
                        line(sb, "Address", rs.getString("address"));
                    }
                }
            }
            section(sb, c, petId, "APPOINTMENTS",
                    "SELECT appt_date, appt_time, service, status FROM appointments "
                    + "WHERE pet_id = ? AND archived = 0 ORDER BY appt_date DESC", 4);
            section(sb, c, petId, "VACCINATIONS",
                    "SELECT vaccine_name, date_given, next_due, status FROM vaccinations "
                    + "WHERE pet_id = ? AND archived = 0 ORDER BY id DESC", 4);
            section(sb, c, petId, "PAYMENTS",
                    "SELECT payment_date, description, method, amount FROM payments "
                    + "WHERE pet_id = ? AND archived = 0 ORDER BY payment_date DESC", 4);
        } catch (SQLException ex) {
            Theme.showError(owner, ex);
            return;
        }

        JTextArea area = new JTextArea(sb.toString());
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        area.setCaretPosition(0);

        JDialog dlg = new JDialog(owner, "Pet Record", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        JButton close = Theme.button("Close", Theme.ACCENT);
        close.addActionListener(e -> dlg.dispose());
        JPanel south = new JPanel();
        south.add(close);
        dlg.getContentPane().setLayout(new BorderLayout());
        dlg.getContentPane().add(new JScrollPane(area), BorderLayout.CENTER);
        dlg.getContentPane().add(south, BorderLayout.SOUTH);
        dlg.setSize(620, 560);
        dlg.setLocationRelativeTo(owner);
        dlg.setVisible(true);
    }

    private static void line(StringBuilder sb, String label, String value) {
        sb.append(String.format("%-10s: %s%n", label, value == null ? "-" : value));
    }

    private static void section(StringBuilder sb, Connection c, int petId, String title, String sql, int cols)
            throws SQLException {
        sb.append("\n").append(title).append("\n---------------------------------------------\n");
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, petId);
            try (ResultSet rs = ps.executeQuery()) {
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    StringBuilder row = new StringBuilder();
                    for (int i = 1; i <= cols; i++) {
                        String v = rs.getString(i);
                        row.append(i > 1 ? "  |  " : "").append(v == null ? "-" : v);
                    }
                    sb.append(row).append("\n");
                }
                if (!any) {
                    sb.append("(none)\n");
                }
            }
        }
    }
}
