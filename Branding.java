package petpulse.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;

public final class Branding {

    private static final String LOGO_PATH = "/petpulse/resources/logo.png";
    private static BufferedImage logo;

    private Branding() {
    }

    private static synchronized BufferedImage load() {
        if (logo == null) {
            try (InputStream in = Branding.class.getResourceAsStream(LOGO_PATH)) {
                if (in != null) {
                    logo = ImageIO.read(in);
                }
            } catch (IOException ignored) {
                // draw fallback below
            }
            if (logo == null) {
                logo = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            }
        }
        return logo;
    }

    /** Logo on a white rounded badge, size x size pixels. */
    public static Icon badge(int size) {
        return new ImageIcon(badgeImage(size));
    }

    public static Image appIcon() {
        return badgeImage(64);
    }

    private static BufferedImage badgeImage(int size) {
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setColor(Color.WHITE);
        g.fillRoundRect(0, 0, size, size, size / 4, size / 4);

        BufferedImage src = load();
        int pad = Math.max(2, size / 8);
        int box = size - 2 * pad;
        double scale = Math.min((double) box / src.getWidth(), (double) box / src.getHeight());
        int w = Math.max(1, (int) Math.round(src.getWidth() * scale));
        int h = Math.max(1, (int) Math.round(src.getHeight() * scale));
        g.drawImage(src, (size - w) / 2, (size - h) / 2, w, h, null);
        g.dispose();
        return out;
    }
}
