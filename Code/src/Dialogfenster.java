package src;

import java.io.File;
import java.nio.file.Paths;
import javax.swing.JFileChooser;

public class Dialogfenster {

    public String oeffnen() {
        final JFileChooser chooser = new JFileChooser("Verzeichnis wählen");
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setFileHidingEnabled(false);

        String currentPath = Paths.get("./Maps").toAbsolutePath().normalize().toString();
        final File file = new File(currentPath);

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
        	System.out.println("Keine Datei gewählt");
        	return null;
        }
    }
} 