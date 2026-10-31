package petpulse.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import petpulse.data.Db;
import petpulse.util.Theme;

/**
 * One reusable screen = table + search + Add / Edit / Archive buttons.
 * It is used for Pet Information, Appointments, Vaccinations and Payments.
 * Records are never deleted here - "Archive" hides them and they appear in the Archive screen.
 */
public class CrudPanel extends JPanel implements Refreshable {

    /** Item of the "Pet" drop-down. */
    static final class PetItem {
        final int id;
        final String name;

        PetItem(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final String table;
    private final FieldDef[] fields;
    private final Runnable onChange;
    private final String selectSql;

    private final DefaultTableModel model;
    private final JTable tbl;
    private final TableRowSorter<DefaultTableModel> sorter;
    private final JTextField txtSearch = new JTextField(16);
    private final JLabel lblCount = new JLabel(" ");
    private final String title;

    public CrudPanel(String title, String table, String orderBy, Runnable onChange, FieldDef... fields) {
        this.title = title;
        this.table = table;
        this.fields = fields;
        this.onChange = onChange;

        boolean pet = false;
        StringBuilder sb = new StringBuilder("SELECT t.id");
        String[] cols = new String[fields.length + 1];
        cols[0] = "ID";
        for (int i = 0; i < fields.length; i++) {
            cols[i + 1] = fields[i].label;
            if (fields[i].type == FieldDef.Type.PET) {
                pet = true;
                sb.append(", p.pet_name");
            } else {
                sb.append(", t.").append(fields[i].column);
            }
        }
        sb.append(" FROM ").append(table).append(" t");
        if (pet) {
            sb.append(" LEFT JOIN pets p ON p.id = t.pet_id");
        }
        sb.append(" WHERE t.archived = 0 ORDER BY ").append(orderBy);
        this.selectSql = sb.toString();

        setLayout(new BorderLayout(0, 12));
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(22, 28, 18, 28));

        // ---- header: title + search + buttons
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(Theme.font(Font.BOLD, 26));
        lblTitle.setForeground(Theme.DARK);
        header.add(lblTitle, BorderLayout.WEST);

        JPanel tools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        tools.setOpaque(false);
        JLabel lblSearch = new JLabel("Search:");
        lblSearch.setForeground(Theme.DARK);
        JButton btnAdd = Theme.button("+ Add", Theme.ACCENT);
        JButton btnEdit = Theme.button("Edit", Theme.SIDEBAR);
        JButton btnArchive = Theme.button("Archive", Theme.WARN);
        JButton btnRefresh = Theme.button("Refresh", Theme.MUTED);
        tools.add(lblSearch);
        tools.add(txtSearch);
        tools.add(btnAdd);
        tools.add(btnEdit);
        tools.add(btnArchive);
        tools.add(btnRefresh);
        header.add(tools, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // ---- table
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tbl = new JTable(model);
        Theme.styleTable(tbl);
        tbl.removeColumn(tbl.getColumnModel().getColumn(0)); // hide ID (kept in the model)
        sorter = new TableRowSorter<>(model);
        tbl.setRowSorter(sorter);
        add(Theme.scroll(tbl), BorderLayout.CENTER);

        lblCount.setForeground(Theme.MUTED);
        add(lblCount, BorderLayout.SOUTH);

        // ---- actions
        btnAdd.addActionListener(e -> doAdd());
        btnEdit.addActionListener(e -> doEdit());
        btnArchive.addActionListener(e -> doArchive());
        btnRefresh.addActionListener(e -> refresh());
        tbl.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    doEdit();
                }
            }
        });
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { applyFilter(); }
            @Override
            public void removeUpdate(DocumentEvent e) { applyFilter(); }
            @Override
            public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
    }

    private void applyFilter() {
        String s = txtSearch.getText().trim();
        sorter.setRowFilter(s.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(s)));
        updateCount();
    }

    private void updateCount() {
        lblCount.setText(tbl.getRowCount() + " record(s)");
    }

    // ------------------------------------------------------------------ data

    @Override
    public void refresh() {
        try (Connection c = Db.getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(selectSql)) {
            model.setRowCount(0);
            int n = fields.length + 1;
            while (rs.next()) {
                Object[] row = new Object[n];
                for (int i = 0; i < n; i++) {
                    row[i] = rs.getString(i + 1);
                }
                model.addRow(row);
            }
            updateCount();
        } catch (SQLException ex) {
            Theme.showError(this, ex);
        }
    }

    private int selectedId() {
        int row = tbl.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a record first.", title,
                    JOptionPane.INFORMATION_MESSAGE);
            return -1;
        }
        return Integer.parseInt(String.valueOf(model.getValueAt(tbl.convertRowIndexToModel(row), 0)));
    }

    private void changed() {
        refresh();
        if (onChange != null) {
            onChange.run();
        }
    }

    private void doAdd() {
        Map<String, String> v = new FormDialog("Add " + title, null).open();
        if (v == null) {
            return;
        }
        StringBuilder cols = new StringBuilder();
        StringBuilder qs = new StringBuilder();
        for (FieldDef f : fields) {
            cols.append(cols.length() > 0 ? "," : "").append(f.column);
            qs.append(qs.length() > 0 ? "," : "").append("?");
        }
        String sql = "INSERT INTO " + table + " (" + cols + ") VALUES (" + qs + ")";
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, v);
            ps.executeUpdate();
            changed();
        } catch (SQLException ex) {
            Theme.showError(this, ex);
        }
    }

    private void doEdit() {
        int id = selectedId();
        if (id < 0) {
            return;
        }
        Map<String, String> current = loadRow(id);
        if (current == null) {
            return;
        }
        Map<String, String> v = new FormDialog("Edit " + title, current).open();
        if (v == null) {
            return;
        }
        StringBuilder set = new StringBuilder();
        for (FieldDef f : fields) {
            set.append(set.length() > 0 ? ", " : "").append(f.column).append(" = ?");
        }
        String sql = "UPDATE " + table + " SET " + set + " WHERE id = ?";
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            int next = bind(ps, v);
            ps.setInt(next, id);
            ps.executeUpdate();
            changed();
        } catch (SQLException ex) {
            Theme.showError(this, ex);
        }
    }

    private void doArchive() {
        int id = selectedId();
        if (id < 0) {
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this,
                "Move this record to the Archive?\nYou can restore it later from the Archive screen.",
                "Archive", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) {
            return;
        }
        String sql = "UPDATE " + table + " SET archived = 1, archived_at = NOW() WHERE id = ?";
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            changed();
        } catch (SQLException ex) {
            Theme.showError(this, ex);
        }
    }

    private Map<String, String> loadRow(int id) {
        StringBuilder cols = new StringBuilder();
        for (FieldDef f : fields) {
            cols.append(cols.length() > 0 ? "," : "").append(f.column);
        }
        String sql = "SELECT " + cols + " FROM " + table + " WHERE id = ?";
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Map<String, String> m = new LinkedHashMap<>();
                for (FieldDef f : fields) {
                    m.put(f.column, rs.getString(f.column));
                }
                return m;
            }
        } catch (SQLException ex) {
            Theme.showError(this, ex);
            return null;
        }
    }

    /** Binds the form values to the statement; returns the next free parameter index. */
    private int bind(PreparedStatement ps, Map<String, String> v) throws SQLException {
        int i = 1;
        for (FieldDef f : fields) {
            String s = v.get(f.column);
            if (s == null || s.trim().isEmpty()) {
                ps.setNull(i++, Types.VARCHAR);
                continue;
            }
            s = s.trim();
            switch (f.type) {
                case NUMBER:
                    ps.setBigDecimal(i++, new BigDecimal(s));
                    break;
                case DATE:
                    ps.setDate(i++, java.sql.Date.valueOf(LocalDate.parse(s)));
                    break;
                case PET:
                    ps.setInt(i++, Integer.parseInt(s));
                    break;
                default:
                    ps.setString(i++, s);
            }
        }
        return i;
    }

    private List<PetItem> loadPets() {
        List<PetItem> list = new ArrayList<>();
        try (Connection c = Db.getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT id, CONCAT(pet_name, ' (', owner_name, ')') FROM pets "
                     + "WHERE archived = 0 ORDER BY pet_name")) {
            while (rs.next()) {
                list.add(new PetItem(rs.getInt(1), rs.getString(2)));
            }
        } catch (SQLException ex) {
            Theme.showError(this, ex);
        }
        return list;
    }

    // ---------------------------------------------------------------- dialog

    private final class FormDialog extends JDialog {

        private final Map<FieldDef, JComponent> inputs = new LinkedHashMap<>();
        private Map<String, String> result;

        FormDialog(String heading, Map<String, String> initial) {
            super(SwingUtilities.getWindowAncestor(CrudPanel.this), heading, ModalityType.APPLICATION_MODAL);

            JPanel form = new JPanel(new GridBagLayout());
            form.setBackground(Color.WHITE);
            form.setBorder(BorderFactory.createEmptyBorder(20, 24, 8, 24));
            GridBagConstraints gc = new GridBagConstraints();
            gc.anchor = GridBagConstraints.WEST;

            int row = 0;
            for (FieldDef f : fields) {
                JLabel l = new JLabel(f.label + (f.required ? " *" : ""));
                l.setFont(Theme.font(Font.BOLD, 13));
                l.setForeground(Theme.DARK);
                JComponent in = createInput(f, initial);
                inputs.put(f, in);

                gc.gridx = 0;
                gc.gridy = row;
                gc.weightx = 0;
                gc.fill = GridBagConstraints.NONE;
                gc.insets = new Insets(6, 0, 6, 16);
                form.add(l, gc);
                gc.gridx = 1;
                gc.weightx = 1;
                gc.fill = GridBagConstraints.HORIZONTAL;
                gc.insets = new Insets(6, 0, 6, 0);
                form.add(in, gc);
                row++;
            }

            JButton save = Theme.button("Save", Theme.ACCENT);
            JButton cancel = Theme.button("Cancel", Theme.MUTED);
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
            buttons.setBackground(Color.WHITE);
            buttons.setBorder(BorderFactory.createEmptyBorder(0, 16, 8, 16));
            buttons.add(cancel);
            buttons.add(save);

            save.addActionListener(e -> onSave());
            cancel.addActionListener(e -> dispose());
            getRootPane().setDefaultButton(save);

            getContentPane().setLayout(new BorderLayout());
            getContentPane().add(form, BorderLayout.CENTER);
            getContentPane().add(buttons, BorderLayout.SOUTH);
            pack();
            setMinimumSize(new java.awt.Dimension(440, getHeight()));
            setLocationRelativeTo(getOwner());
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private JComponent createInput(FieldDef f, Map<String, String> initial) {
            String value = initial == null ? null : initial.get(f.column);
            switch (f.type) {
                case COMBO: {
                    JComboBox<String> cb = new JComboBox<>(f.options);
                    if (value != null) {
                        cb.setSelectedItem(value);
                    }
                    return cb;
                }
                case PET: {
                    JComboBox<PetItem> cb = new JComboBox<>();
                    for (PetItem p : loadPets()) {
                        cb.addItem(p);
                    }
                    if (value != null) {
                        boolean found = false;
                        for (int i = 0; i < cb.getItemCount(); i++) {
                            if (String.valueOf(cb.getItemAt(i).id).equals(value)) {
                                cb.setSelectedIndex(i);
                                found = true;
                            }
                        }
                        if (!found) {
                            PetItem p = new PetItem(Integer.parseInt(value), "(archived pet #" + value + ")");
                            cb.addItem(p);
                            cb.setSelectedItem(p);
                        }
                    }
                    return cb;
                }
                default: {
                    JTextField tf = new JTextField(22);
                    if (value != null) {
                        tf.setText(value);
                    } else if (f.defaultToday) {
                        tf.setText(f.todayText());
                    }
                    if (f.type == FieldDef.Type.DATE) {
                        tf.setToolTipText("Format: yyyy-MM-dd  (example 2026-10-13)");
                    }
                    return tf;
                }
            }
        }

        private void onSave() {
            Map<String, String> v = new LinkedHashMap<>();
            for (Map.Entry<FieldDef, JComponent> en : inputs.entrySet()) {
                FieldDef f = en.getKey();
                JComponent in = en.getValue();
                String s;
                if (f.type == FieldDef.Type.PET) {
                    Object sel = ((JComboBox<?>) in).getSelectedItem();
                    s = sel == null ? "" : String.valueOf(((PetItem) sel).id);
                    if (s.isEmpty()) {
                        warn("Please add a pet first (Pet Information), then choose it here.");
                        return;
                    }
                } else if (f.type == FieldDef.Type.COMBO) {
                    s = String.valueOf(((JComboBox<?>) in).getSelectedItem());
                } else {
                    s = ((JTextField) in).getText().trim();
                }
                if (f.required && s.isEmpty()) {
                    warn(f.label + " is required.");
                    in.requestFocus();
                    return;
                }
                try {
                    if (f.type == FieldDef.Type.NUMBER && !s.isEmpty()) {
                        new BigDecimal(s);
                    } else if (f.type == FieldDef.Type.DATE && !s.isEmpty()) {
                        LocalDate.parse(s);
                    }
                } catch (NumberFormatException ex) {
                    warn(f.label + " must be a number (example 350.00).");
                    return;
                } catch (DateTimeParseException ex) {
                    warn(f.label + " must be a date like 2026-10-13 (yyyy-MM-dd).");
                    return;
                }
                v.put(f.column, s);
            }
            result = v;
            dispose();
        }

        private void warn(String msg) {
            JOptionPane.showMessageDialog(this, msg, "Please check", JOptionPane.WARNING_MESSAGE);
        }

        Map<String, String> open() {
            setVisible(true);
            return result;
        }
    }
}
