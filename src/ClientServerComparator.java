package src;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ClientServerComparator {

    private String line;
    private BufferedReader br;
    private final int width;
    ArrayList<String> ServerLines = new ArrayList<>();
    ArrayList<String> ClientLines = new ArrayList<>();

    /**
     * This Constructor creates a clientServerComperator Object.
     * With this it's possible to compare the output of the client to the one of the server.
     * For this the server needs to direct its output in a File named "Server_View.txt"
     * @param width the width of the playing field.
     */
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

    /**
     * This method takes a String from the Client and processes it to later compare to the server.
     * The string should come from the method Map.toString_Server()
     * @param clientString The String that gets parsed
     */
    public void setClientString(String clientString){
        ClientLines = new ArrayList<>( List.of(clientString.split("\n")) );
    }

    /**
     * This method compares if the server, and the client have the same output.
     * @param moveCounter This number indicates the number of the move.
     * @return returns true if the two maps match and false otherwise
     */
    public ArrayList<Position> compare(int moveCounter){

        int yIndexInServer;
        String currServerLine;
        String currClientLine;
        String currFieldServer;
        String currFieldClient;
        int xIndexServer;
        int xIndexClient;
        ArrayList<Position> differentPositions = new ArrayList<>();

        for(int y = 0; y < ClientLines.size(); y++)
        {
            yIndexInServer = y + ClientLines.size() * moveCounter;

            while (yIndexInServer >= ServerLines.size()){
                readServer();
            }

            currServerLine = ServerLines.get(yIndexInServer);
            currClientLine = ClientLines.get(y).trim();

            if(!currServerLine.equalsIgnoreCase( currClientLine ))
            {
                System.out.println("Server ("+y+"): " + ServerLines.get(yIndexInServer));
                System.out.println("Client ("+y+"): " + ClientLines.get(y));

                //get concrete Position where the error is
                xIndexClient = 0;
                xIndexServer = 0;
                for (int x = 0; x < width; x++){
                    currFieldClient = "" + currClientLine.charAt(xIndexClient);
                    currFieldServer = "" + currServerLine.charAt(xIndexServer);


                    if (xIndexClient + 1 < currClientLine.length() && currClientLine.charAt(xIndexClient + 1) == '\'') {
                        currFieldClient += '\'';
                    }

                    if (xIndexServer + 1 < currServerLine.length() && currServerLine.charAt(xIndexServer + 1) == '\'') {
                        currFieldServer += '\'';
                    }

                    if (!currFieldClient.equals(currFieldServer)) {
                        differentPositions.add(new Position(x+1,y+1)); //index shift for client
                    }

                    //skip space and '
                    xIndexServer+=2;
                    xIndexClient+=2;

                }
            }
        }

        return differentPositions;
    }

    /**
     * This method returns the map that the server gave out in a formatted String.
     * @param moveCounter This number indicates the number of the move.
     * @return returns the map as a string
     */
    public String getMapFromServer(int moveCounter){
        int indexInServer;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < ClientLines.size(); i++){
            indexInServer = i + ClientLines.size() * moveCounter;
            sb.append( ServerLines.get(indexInServer) ).append("\n");
        }

        return sb.toString();
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
}
