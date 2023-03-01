package src.View;

import src.Map;
import javax.swing.*;
import java.awt.*;

public class MapPanel extends JPanel {

    int zoom = 0;
    int height;
    int width;
    int defaultCellSize = 25; // default cell size

    JPanel[][] panelMap;

    public MapPanel(Map map){
        super(new GridLayout(map.getWidth(),map.getHeight()));
        this.setBorder(BorderFactory.createLineBorder(Color.RED, 4));
        this.height = map.getHeight();
        this.width = map.getWidth();
        panelMap = new JPanel[height][width];
        fillPanelMap();
        addAllCells();
    }

    public MapPanel(int height, int width){
        super(new GridLayout(height,width));
        this.setBorder(BorderFactory.createLineBorder(Color.RED, 4));
        this.height = height;
        this.width = width;
        panelMap = new JPanel[height][width];
        fillPanelMap();
        addAllCells();
    }

    private void fillPanelMap() {
        JPanel cellPanel;

        // Create a flag to alternate between black and white cells
        boolean isBlack = false;

        // Add labels or components to the grid panel here
        for (int row = 0; row < height; row++) {

            isBlack = row % 2 == 0;

            for (int col = 0; col < width; col++) {
                cellPanel = new MapCell('-', isBlack ? Color.GRAY : Color.WHITE, defaultCellSize);

                // Toggle the flag for the next cell
                isBlack = !isBlack;

                // Add the cell panel to the grid panel
                panelMap[row][col] = cellPanel;
            }
        }
    }

    private void addAllCells(){
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                this.add(panelMap[row][col]);
            }
        }
    }

    public void zoom(double delta){
        this.setPreferredSize(new Dimension((int) (this.getWidth()*delta), (int) (this.getHeight()*delta)));

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                panelMap[row][col].setPreferredSize(
                        new Dimension(
                                (int) (panelMap[row][col].getWidth()*delta),
                                (int) (panelMap[row][col].getHeight()*delta)
                        )
                );
            }
        }

        System.out.println("Zooom");
    }

    public void printMap(Map map){
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                char c = map.getCharAt(col, row); //TODO check: if x and y match
                panelMap[row][col] = new MapCell(c, panelMap[row][col].getBackground(), panelMap[row][col].getHeight());
            }
        }
        //panelMap[0][0] = new MapCell('1', panelMap[0][0].getBackground(), panelMap[0][0].getHeight());
        updateComponentsInGrid();
        repaint();
    }

    public void updateComponentsInGrid() {
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int i = width * row + col;
                this.remove(i);
                this.add(panelMap[row][col], i);
            }
        }
    }


}
