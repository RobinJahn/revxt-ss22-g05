package src.View;

import javax.swing.*;
import java.awt.*;

public class InversionCircle extends JPanel {

    private Color color1 = Color.RED;
    private Color color2 = Color.BLUE;

    public InversionCircle(int size) {
        super();
        this.setOpaque(false);
        this.setPreferredSize(new Dimension(size, size));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(color1);
        g2d.setStroke(new BasicStroke(this.getWidth() / 10f));
        int[] xPoints = { 50, 20, 80, 0, 100 };
        int[] yPoints = { 0, 100, 100, 40, 40 };
        g2d.drawPolygon(xPoints, yPoints, 5);
    }

}
