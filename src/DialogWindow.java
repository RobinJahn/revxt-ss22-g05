package src;

import java.io.File;
import java.nio.file.Paths;
import javax.swing.JFileChooser;

/**
 * This class provides the functionality so that a file-window is opened in which the user can select a file which then is returned.
 */
public class DialogWindow {

    static File savedPath = new File(Paths.get("./Maps").toAbsolutePath().normalize().toString());

    /**
     * This method opens a file dialog window.
     * @return Returns the complete file path of the selected file as a string.
     */
    public String open() {
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
                 File inputFolderFile = chooser.getSelectedFile();
                 String inputFolderStr = inputFolderFile.getPath();
                 System.out.println(inputFolderStr);
                 savedPath = new File(chooser.getSelectedFile().toString());
                 return inputFolderStr;
            }
        }
        System.out.println("No file selected");
        return null;
    }
} 