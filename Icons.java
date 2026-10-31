package petpulse.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.Icon;

public final class Icons {

    private Icons() {
    }

    public static Icon of(String name, int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g0, int x, int y) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                g.translate(x, y);
                g.scale(size / 24.0, size / 24.0);
                g.setColor(color);
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                draw(g, name);
                g.dispose();
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    private static Path2D poly(double... p) {
        Path2D path = new Path2D.Double();
        path.moveTo(p[0], p[1]);
        for (int i = 2; i < p.length; i += 2) {
            path.lineTo(p[i], p[i + 1]);
        }
        path.closePath();
        return path;
    }

    private static void draw(Graphics2D g, String name) {
        switch (name) {
            case "home":
                g.fill(poly(12, 3, 22, 12, 19, 12, 19, 21, 14, 21, 14, 15, 10, 15, 10, 21, 5, 21, 5, 12, 2, 12));
                break;
            case "paw":
                g.fill(new Ellipse2D.Double(7, 13, 10, 8));
                g.fill(new Ellipse2D.Double(2.5, 9, 4, 5.5));
                g.fill(new Ellipse2D.Double(7.5, 3.5, 4, 6));
                g.fill(new Ellipse2D.Double(12.5, 3.5, 4, 6));
                g.fill(new Ellipse2D.Double(17.5, 9, 4, 5.5));
                break;
            case "calendar":
                g.draw(new RoundRectangle2D.Double(3.5, 5, 17, 15.5, 3, 3));
                g.fillRect(3, 8, 18, 3);
                g.fillRect(7, 2, 2, 5);
                g.fillRect(15, 2, 2, 5);
                for (int r = 0; r < 2; r++) {
                    for (int c = 0; c < 3; c++) {
                        g.fillRect(7 + c * 4, 13 + r * 4, 2, 2);
                    }
                }
                break;
            case "syringe":
                AffineTransform old = g.getTransform();
                g.rotate(Math.toRadians(-45), 12, 12);
                g.draw(new RoundRectangle2D.Double(6, 9.5, 11, 5, 2, 2));
                g.drawLine(17, 12, 22, 12);
                g.drawLine(6, 12, 3, 12);
                g.drawLine(3, 8, 3, 16);
                g.fillRect(9, 9, 1, 6);
                g.setTransform(old);
                break;
            case "card":
                g.draw(new RoundRectangle2D.Double(2.5, 5, 19, 14, 3, 3));
                g.fillRect(3, 9, 18, 3);
                g.fillRect(6, 15, 5, 2);
                break;
            case "clock":
                g.draw(new Ellipse2D.Double(3, 3, 18, 18));
                g.drawLine(12, 7, 12, 12);
                g.drawLine(12, 12, 16, 14);
                break;
            case "document":
                g.draw(new RoundRectangle2D.Double(5, 2.5, 14, 19, 2, 2));
                g.drawLine(8, 8, 16, 8);
                g.drawLine(8, 12, 16, 12);
                g.drawLine(8, 16, 13, 16);
                break;
            case "archive":
                g.fill(new RoundRectangle2D.Double(2.5, 3.5, 19, 5, 2, 2));
                g.draw(new RoundRectangle2D.Double(4.5, 9, 15, 11.5, 2, 2));
                g.fillRect(9, 12, 6, 2);
                break;
            case "logout":
                Path2D door = new Path2D.Double();
                door.moveTo(10, 3.5);
                door.lineTo(4, 3.5);
                door.lineTo(4, 20.5);
                door.lineTo(10, 20.5);
                g.draw(door);
                g.drawLine(9, 12, 19, 12);
                g.fill(poly(21.5, 12, 16, 7.5, 16, 16.5));
                break;
            case "user":
                g.fill(new Ellipse2D.Double(7.5, 3, 9, 9));
                g.fill(new RoundRectangle2D.Double(4, 14, 16, 8, 8, 8));
                break;
            default:
                g.fill(new Ellipse2D.Double(6, 6, 12, 12));
        }
    }
}
