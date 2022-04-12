package src;

import java.io.File;
import java.nio.file.Paths;
import javax.swing.JFileChooser;

public class Dialogfenster {

    static File savedPath = new File(Paths.get("./Maps").toAbsolutePath().normalize().toString());

    public String oeffnen() {
        final JFileChooser chooser = new JFileChooser("Select Path");
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setFileHidingEnabled(false);

        String currentPath = Paths.get("./Maps").toAbsolutePath().normalize().toString();
        final File file = new File(currentPath);

        chooser.setCurrentDirectory(savedPath);

        chooser.setVisible(true);

        int returnVal = chooser.showOpenDialog(null);

        if(returnVal == JFileChooser.APPROVE_OPTION)
        {
            if(chooser.getSelectedFile() != null)
            {
                 File inputVerzFile = chooser.getSelectedFile();
                 String inputVerzStr = inputVerzFile.getPath();
                 System.out.println(inputVerzStr);
                 savedPath = new File(chooser.getSelectedFile().toString());
                 return inputVerzStr;
            }
        }
        System.out.println("No file selected");
        return null;
    }
} 