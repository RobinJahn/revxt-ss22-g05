package src;

import java.io.File;
import javax.swing.JFileChooser;

public class Dialogfenster {

   

    public String oeffnen() {
        final JFileChooser chooser = new JFileChooser("Verzeichnis w�hlen");
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setFileHidingEnabled(false);
        final File file = new File("/home");

        chooser.setCurrentDirectory(file);

        chooser.setVisible(true);
        
        chooser.showOpenDialog(null);

        if(chooser.getSelectedFile() != null)
        {
        	 File inputVerzFile = chooser.getSelectedFile();
             String inputVerzStr = inputVerzFile.getPath();
             System.out.println(inputVerzStr);
             return inputVerzStr;
        }
        else
        {
        	System.out.println("Keine Datei gew�hlt");
        	return null;
        }
    }
} 