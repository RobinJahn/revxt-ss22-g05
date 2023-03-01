package src.View;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class MapViewWrapper extends JPanel {
    MapPanel mapPanel;
    JScrollPane scrollPane;

    public MapViewWrapper(MapPanel mapPanel){
        super();
        this.mapPanel = mapPanel;

        double relationWidthToHeight = (double) mapPanel.height / mapPanel.width;
        int startWidth = 500;

        scrollPane = new JScrollPane(mapPanel);
        scrollPane.setPreferredSize(new Dimension(startWidth, (int) (startWidth*relationWidthToHeight)));
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.ORANGE, 4));

        this.add(scrollPane);
        this.setBorder(BorderFactory.createLineBorder(Color.GREEN, 3));

        // Add a ComponentListener to the parent panel that will update the size of the rectangle panel
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int width = getWidth();
                int height = getHeight();

                int newWith = width;
                int newHeight = (int) (width * relationWidthToHeight);

                if (newHeight > height){
                    newHeight = height;
                    newWith = (int) (height / relationWidthToHeight);
                }

                scrollPane.setPreferredSize(new Dimension(newWith, newHeight));
                scrollPane.revalidate();
                scrollPane.repaint();
            }
        });
    }

    public void zoom(double delta){
        mapPanel.zoom(delta);
        scrollPane.setViewportView(mapPanel);
        scrollPane.revalidate();
        scrollPane.repaint();
        revalidate();
        repaint();
    }

    public void update(){
        scrollPane.revalidate();
        scrollPane.repaint();
        revalidate();
        repaint();
    }
}

