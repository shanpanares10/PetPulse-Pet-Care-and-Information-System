package petpulse;

import petpulse.ui.LoginFrame;
import petpulse.util.Theme;

public class Main {
    public static void main(String[] args) {
        Theme.installLookAndFeel();
        java.awt.EventQueue.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
