package src.View;

import javax.swing.*;
import java.awt.*;

public class BonusCircle extends JPanel {

    private Color color1 = Color.RED;
    private Color color2 = Color.BLUE;

    public BonusCircle(int size) {
        super();
        this.setOpaque(false);
        this.setPreferredSize(new Dimension(size, size));
    }

    @Override
    protected void paintComponent(Graphics g) {
        int width = getWidth();
        int height = getHeight();
        int diameter = Math.min(width, height);
        int x = (width - diameter) / 2;
        int y = (height - diameter) / 2;

        Graphics2D g2d = (Graphics2D) g;
        GradientPaint gradient = new GradientPaint(
                x, y, color1,
                x + diameter, y + diameter, color2);
        g2d.setPaint(gradient);
        g2d.fillOval(x, y, diameter, diameter);
    }

}
