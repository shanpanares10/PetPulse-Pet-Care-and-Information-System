package petpulse.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.IntConsumer;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import petpulse.data.Db;
import petpulse.util.Theme;

public class ReportPanel extends JPanel implements Refreshable {

    private final String sql;
    private final boolean showTotal;
    private final DefaultTableModel model;
    private final JTable tbl;
    private final TableRowSorter<DefaultTableModel> sorter;
    private final JTextField txtSearch = new JTextField(16);
    private final JLabel lblCount = new JLabel(" ");
    private final String title;
    private final String footerLabel;
    private final String footerSql;
    private String footerText = "";

    public ReportPanel(String title, String sql, String[] headers, boolean showTotal, String openText,
            IntConsumer onOpen) {
        this(title, sql, headers, showTotal, openText, onOpen, null, null);
    }

    /**
     * Same as above, plus a footer figure taken from its own query (footerSql returns ONE value),
     * e.g. the total of all active payments under the transaction ledger.
     */
    public ReportPanel(String title, String sql, String[] headers, boolean showTotal, String openText,
            IntConsumer onOpen, String footerLabel, String footerSql) {
        this.title = title;
        this.footerLabel = footerLabel;
        this.footerSql = footerSql;
        this.sql = sql;
        this.showTotal = showTotal;

        setLayout(new BorderLayout(0, 12));
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(22, 28, 18, 28));

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
        tools.add(lblSearch);
        tools.add(txtSearch);
        if (onOpen != null) {
            JButton btnOpen = Theme.button(openText, Theme.ACCENT);
            btnOpen.addActionListener(e -> open(onOpen));
            tools.add(btnOpen);
        }
        JButton btnRefresh = Theme.button("Refresh", Theme.MUTED);
        btnRefresh.addActionListener(e -> refresh());
        tools.add(btnRefresh);
        header.add(tools, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        String[] cols = new String[headers.length + 1];
        cols[0] = "ID";
        System.arraycopy(headers, 0, cols, 1, headers.length);
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tbl = new JTable(model);
        Theme.styleTable(tbl);
        tbl.removeColumn(tbl.getColumnModel().getColumn(0));
        sorter = new TableRowSorter<>(model);
        tbl.setRowSorter(sorter);
        add(Theme.scroll(tbl), BorderLayout.CENTER);

        lblCount.setForeground(Theme.MUTED);
        lblCount.setFont(Theme.font(Font.BOLD, 13));
        add(lblCount, BorderLayout.SOUTH);

        if (onOpen != null) {
            tbl.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        open(onOpen);
                    }
                }
            });
        }
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { applyFilter(); }
            @Override
            public void removeUpdate(DocumentEvent e) { applyFilter(); }
            @Override
            public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
    }

    private void open(IntConsumer onOpen) {
        int row = tbl.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a row first.", title,
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        onOpen.accept(Integer.parseInt(String.valueOf(model.getValueAt(tbl.convertRowIndexToModel(row), 0))));
    }

    private void applyFilter() {
        String s = txtSearch.getText().trim();
        sorter.setRowFilter(s.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(s)));
        updateCount();
    }

    private void updateCount() {
        String text = tbl.getRowCount() + " record(s)";
        if (showTotal) {
            BigDecimal total = BigDecimal.ZERO;
            int last = model.getColumnCount() - 1;
            for (int r = 0; r < tbl.getRowCount(); r++) {
                try {
                    total = total.add(new BigDecimal(String.valueOf(
                            model.getValueAt(tbl.convertRowIndexToModel(r), last))));
                } catch (NumberFormatException ignored) {
                    // skip non-numeric
                }
            }
            text += "      Total: \u20B1" + String.format("%,.2f", total);
        }
        if (!footerText.isEmpty()) {
            text += "      " + footerText;
        }
        lblCount.setText(text);
    }

    @Override
    public void refresh() {
        try (Connection c = Db.getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            model.setRowCount(0);
            int n = model.getColumnCount();
            while (rs.next()) {
                Object[] row = new Object[n];
                for (int i = 0; i < n; i++) {
                    row[i] = rs.getString(i + 1);
                }
                model.addRow(row);
            }
            footerText = "";
            if (footerSql != null) {
                try {
                    String v = Db.scalar(footerSql);
                    footerText = footerLabel + ": \u20B1" + String.format("%,.2f",
                            new BigDecimal(v == null ? "0" : v));
                } catch (NumberFormatException ignored) {
                    // leave the footer empty
                }
            }
            updateCount();
        } catch (SQLException ex) {
            Theme.showError(this, ex);
        }
    }
}
