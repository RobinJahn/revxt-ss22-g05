package src;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ClientServerComparator {

    private String line;
    private int ErrorCount = 0;
    private BufferedReader br;
    ArrayList<String> ServerLines = new ArrayList<>();
    ArrayList<String> ClientLines = new ArrayList<>();
    int width;

    public ClientServerComparator(int width) {
        this.width = width-2;
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
        String currLine;

        try {
            TimeUnit.MILLISECONDS.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        while (true)
        {
            try {
                line = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (line == null) break;

            try {
                if(!line.isEmpty() && line.contains("|"))
                {
                    br.mark(2*width);

                    currLine = line.substring( line.lastIndexOf("|") + 1 ).trim();

                    if (currLine.length() < width) {
                        br.reset();
                        TimeUnit.MILLISECONDS.sleep(10);
                        continue;
                    }

                    ServerLines.add(currLine);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void setClientString(String clientString){
        ClientLines = new ArrayList<>( List.of(clientString.split("\n")) );
    }

    public int getErrorCount()
    {
        return ErrorCount;
    }

    public boolean compare(int moveCounter){

        int indexInServer;

        for(int i = 0;i< ClientLines.size();i++)
        {
            indexInServer = i + ClientLines.size() * moveCounter;

            while (indexInServer >= ServerLines.size()){
                readServer();
            }

            if(!ServerLines.get(indexInServer).equalsIgnoreCase( ClientLines.get(i).trim() ))
            {
                System.out.println("Server ("+i+"): " + ServerLines.get(indexInServer));
                System.out.println("Client ("+i+"): " + ClientLines.get(i));
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

    public String getMapFromServer(int moveCounter){
        int indexInServer;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < ClientLines.size(); i++){
            indexInServer = i + ClientLines.size() * moveCounter;
            sb.append( ServerLines.get(indexInServer) ).append("\n");
        }

        return sb.toString();
    }

}
