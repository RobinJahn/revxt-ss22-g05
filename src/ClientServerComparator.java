package src;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ClientServerComparator {

    private String line;
    private String Server = "";
    private String Client ="";
    private int ErrorCount = 0;
    private Scanner scan = null;

    private void readServer()
    {
        Server = "";
        try {
            //Wait Time so Server can write his output into the File
            TimeUnit.MILLISECONDS.sleep(1000);
        }
        catch (InterruptedException IE)
        {}

        try
        {
            File IDE = new File("./serverAndAi/Server_View.txt");
            File script = new File("../serverAndAi/Server_View.txt");
            //scan = new Scanner(IDE);      //IDE Version
            scan = new Scanner(script);   //Script Version
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        while (scan.hasNext())
        {
            line = scan.nextLine();

            if(!line.isEmpty() && line.contains("|"))
            {
                String[] toAdd = line.split("[|]");
                Server += toAdd[1].trim() + "\n";
            }
        }
        scan.close();
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

        String ServerLines[] = Server.split("\n");
        String ClientLines[] = Client.split("\n");

        for(int i = 0;i< ClientLines.length;i++)
        {
            if(!ServerLines[i+ ClientLines.length*moveCounter].equalsIgnoreCase(ClientLines[i].trim()))
            {
                System.out.println("Server: " + ServerLines[i]);
                System.out.println("Client: " + ClientLines[i]);
                ErrorCount++;
            }
        }

        if(ErrorCount == 0)
        {
            return true;
        }
        else
        {
            System.out.println(ErrorCount);
            return false;
        }
    }
}
