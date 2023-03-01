package src.View;

import javax.swing.*;
import java.awt.*;

public class PlayerCircle extends JPanel {

    Color color;
    int size;

    public PlayerCircle(Color color, int size){
        super();
        this.setOpaque(false);
        this.setPreferredSize(new Dimension(size, size));
        this.color = color;
        this.size = size;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(color);
        g.fillOval(0, 0, size, size); // x, y, width, height
    }
}
