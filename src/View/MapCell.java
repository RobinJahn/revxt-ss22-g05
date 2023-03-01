package src.View;

import javax.swing.*;
import java.awt.*;

public class MapCell extends JPanel {

    char playerColor;

    public MapCell(char value, Color backgroundColor, int length) {
        super(new GridBagLayout());
        this.playerColor = value;
        this.setBackground(Color.WHITE);
        this.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));

        this.setPreferredSize(new Dimension(length, length));
        int size = (int) (length * 0.8);

        switch (value) {
            case 't':
            case '-':
                this.setBackground(Color.darkGray);
                break;
            case '1':
                this.add(new PlayerCircle(Color.RED, size));
                break;
            case '2':
                this.add(new PlayerCircle(Color.BLUE, size));
                break;
            case '3':
                this.add(new PlayerCircle(new Color(35, 122, 32), size)); //dark
                break;
            case '4':
                this.add(new PlayerCircle(new Color(138, 138, 11), size)); //dark
                break;
            case '5':
                this.add(new PlayerCircle(Color.CYAN, size));
                break;
            case '6':
                this.add(new PlayerCircle(Color.MAGENTA, size));
                break;
            case '7':
                this.add(new PlayerCircle(Color.YELLOW, size));
                break;
            case '8':
                this.add(new PlayerCircle(Color.GREEN, size));
                break;
            case 'b':
                //this.add(createLabel("B", size));
                this.add(new BonusCircle(size));
                break;
            case 'i':
                this.add(createLabel("I", size));
                //this.add(new InversionCircle(size));
                break;
            case 'c':
                this.add(createLabel("C", size));
                break;
            case 'x':
                this.add(new PlayerCircle(Color.GRAY, size));
                break;
        }
    }

    private JLabel createLabel(String text, int size){
        JLabel label = new JLabel(text);
        label.setFont(findMaxFontSize(label, label.getFont()));
        label.setPreferredSize(new Dimension(size, size));
        return label;
    }

    private static Font findMaxFontSize(JLabel label, Font font) {
        int low = 1;
        int high = label.getHeight();
        FontMetrics fm = label.getFontMetrics(font);

        while (low <= high) {
            int mid = (low + high) / 2;
            Font testFont = font.deriveFont(Font.PLAIN, mid);
            int textWidth = fm.stringWidth(label.getText());
            int textHeight = fm.getHeight();
            if (textWidth < label.getWidth() && textHeight < label.getHeight()) {
                font = testFont;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        return font;
    }
}
