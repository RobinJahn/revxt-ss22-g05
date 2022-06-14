package src;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ClientServerComparator {

    private String line;
    private String Server = "";
    private String Client ="";
    private int ErrorCount = 0;
    private BufferedReader br;

    public ClientServerComparator() {
        try {
            File IDE = new File("./serverAndAi/Server_View.txt"); //IDE Version
            //File script = new File("../serverAndAi/Server_View.txt"); //Script Version

            FileReader fr = new FileReader(IDE);
            //FileReader fr = new FileReader(script);
            br = new BufferedReader(fr);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void readServer()
    {
        String[] toAdd;

        while (true)
        {
            try {
                line = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (line == null) break;

            if(!line.isEmpty() && line.contains("|"))
            {

                toAdd = line.split("[|]");
                Server += toAdd[1].trim() + "\n";
            }
        }
    }

    public void setClientString(String clientString){
        this.Client = clientString;
    }

    public int getErrorCount()
    {
        return ErrorCount;
    }

    public boolean compare(int moveCounter){

        readServer();

        String[] ServerLines = Server.split("\n");;
        String[] ClientLines = Client.split("\n");

        int indexInServer;

        for(int i = 0;i< ClientLines.length;i++)
        {
            indexInServer = i + ClientLines.length * moveCounter;

            if (indexInServer >= ServerLines.length){
                readServer();
                ServerLines = Server.split("\n");
                ClientLines = Client.split("\n");
            }

            if(!ServerLines[ indexInServer ].equalsIgnoreCase( ClientLines[i].trim() ))
            {
                System.out.println("Server ("+i+"): " + ServerLines[i]);
                System.out.println("Client ("+i+"): " + ClientLines[i]);
                ErrorCount++;
            }
        }

        if(ErrorCount == 0)
        {
            return true;
        }
        else
        {
            System.err.println("In ClientServerComparator found " + ErrorCount + " errors");
            return false;
        }
    }
}
