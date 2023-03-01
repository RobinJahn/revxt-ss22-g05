package src.View;

import src.Map;

import javax.swing.*;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MapViewer extends JFrame {

    MapPanel mapPanel;
    MapViewWrapper mapViewWrapper;

    public MapViewer(int height, int width) { //TODO: Remove length argument
        super("Map View");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        //mapPanel = new MapPanel(map);
        mapPanel = new MapPanel(height, width);
        mapViewWrapper = new MapViewWrapper(mapPanel);

        // Create the menu bar and menu items
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Options");
        JMenuItem doubleSizeMenuItem = new JMenuItem("Double Size");
        JMenuItem halfSizeMenuItem = new JMenuItem("Half Size");

        doubleSizeMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mapViewWrapper.zoom(2);
                revalidate();
                repaint();
            }
        });

        halfSizeMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mapViewWrapper.zoom(0.5);
                revalidate();
                repaint();
            }
        });

        // Add the menu items to the menu and the menu to the menu bar
        menu.add(doubleSizeMenuItem);
        menu.add(halfSizeMenuItem);
        menuBar.add(menu);

        setJMenuBar(menuBar);

        add(mapViewWrapper, BorderLayout.CENTER);
        // Pack the frame and set it visible
        pack();
        setVisible(true);
    }

    public void printMap(Map map){
        mapPanel.printMap(map);
        mapViewWrapper.update();
        repaint();
    }

    public static void main(String[] args) {
        MapViewer mapViewer = new MapViewer(5, 10);
        mapViewer.printMap(null);
    }
}
