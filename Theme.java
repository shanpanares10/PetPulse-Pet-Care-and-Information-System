package petpulse.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;

public final class Theme {

    public static final Color SIDEBAR        = new Color(0x2E7D4F);
    public static final Color SIDEBAR_ACTIVE = new Color(0x5BAF7C);
    public static final Color TOPBAR         = new Color(0xA8DDB5);
    public static final Color BG             = new Color(0xF4FAF5);
    public static final Color CARD           = new Color(0xE2F3E6);
    public static final Color CARD_BORDER    = new Color(0xB6DDC0);
    public static final Color DARK           = new Color(0x14532D);
    public static final Color ACCENT         = new Color(0x22A05A);
    public static final Color MUTED          = new Color(0x5B8A6B);
    public static final Color DANGER         = new Color(0xC0392B);
    public static final Color WARN           = new Color(0xD68910);

    public static final String FONT = "Segoe UI";

    private Theme() {
    }

    public static Font font(int style, int size) {
        return new Font(FONT, style, size);
    }

    public static void installLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
            // keep default
        }
        UIManager.put("OptionPane.messageFont", font(Font.PLAIN, 14));
        UIManager.put("OptionPane.buttonFont", font(Font.PLAIN, 13));
        UIManager.put("Button.font", font(Font.PLAIN, 13));
        UIManager.put("Label.font", font(Font.PLAIN, 13));
        UIManager.put("TextField.font", font(Font.PLAIN, 14));
        UIManager.put("ComboBox.font", font(Font.PLAIN, 14));
        UIManager.put("Table.font", font(Font.PLAIN, 13));
    }

    /** Rounded light-green "card" border (used on the dashboard). */
    public static void cardBorder(JComponent c) {
        c.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(CARD_BORDER, 1, true),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
    }

    public static JButton button(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(font(Font.BOLD, 13));
        b.setForeground(Color.WHITE);
        b.setBackground(bg);
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        return b;
    }

    public static void styleTable(JTable t) {
        t.setRowHeight(30);
        t.setFont(font(Font.PLAIN, 13));
        t.setGridColor(new Color(0xDCEFE2));
        t.setShowVerticalLines(false);
        t.setIntercellSpacing(new java.awt.Dimension(0, 1));
        t.setSelectionBackground(new Color(0xC8E6D0));
        t.setSelectionForeground(DARK);
        t.setFillsViewportHeight(true);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel,
                    boolean foc, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tb, v, sel, false, r, c);
                if (!sel) {
                    l.setBackground(r % 2 == 0 ? Color.WHITE : new Color(0xF1F8F3));
                    l.setForeground(Color.DARK_GRAY);
                }
                l.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                return l;
            }
        });

        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setPreferredSize(new java.awt.Dimension(0, 34));
        t.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel,
                    boolean foc, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tb, v, false, false, r, c);
                l.setOpaque(true);
                l.setBackground(SIDEBAR);
                l.setForeground(Color.WHITE);
                l.setFont(font(Font.BOLD, 13));
                l.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                return l;
            }
        });
    }

    public static JScrollPane scroll(JTable t) {
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(new LineBorder(CARD_BORDER, 1));
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    public static void showError(Component parent, Throwable ex) {
        JOptionPane.showMessageDialog(parent,
                "Database error:\n" + ex.getMessage()
                + "\n\nCheck that MySQL is running (XAMPP > Start MySQL) and that the\n"
                + "Database Settings on the login screen are correct.",
                "PetPulse", JOptionPane.ERROR_MESSAGE);
    }
}
