package src;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

public class Map {
    private char[][] map; //main data structure to store the Map Infos

    //Data Structure and needed Variables to store Transitions
    //ArrayList<char[]> transitions = new ArrayList<>(); //TODO: austauschen durch hash map
    HashMap<Character,Character> transitionen = new HashMap<Character,Character>();
    
    
    int[] transitionsBuffer = new int[9];
    int posInTransitionBuffer=0;
    
    //General Map Infos
    private int anzPlayers;
    private int[] overwriteStonesPerPlayer = new int[]{-1, -1, -1, -1, -1, -1, -1, -1};
    private int[] bombsPerPlayer = new int[]{-1, -1, -1, -1, -1, -1, -1, -1};
    private int explosionRadius;
    private int height;
    private int width;

    //Extended Map Info
    private int currentlyPlaying = 1;

    /**
     * Constructor imports Map from given Filepath
     */
    public Map() {
    	Dialogfenster openMap = new Dialogfenster();
    	boolean importedCorrectly = importMap(openMap.oeffnen());
        if (!importedCorrectly) {
            System.err.println("Map didn't import correctly.");
        }
    }
    
    //public Methods

    /**
     * @param x x corrdinate
     * @param y y coordinate
     * @return Returns the Character at the given x and y position in the Map. If it is out of boundaries it returns '-'
     */
    public char getCharAt(int x, int y){
        if (x >= width || y >= height) return '-';
        return map[y][x];
    }

    /**
     * Method Imports a Map from the given File Path.
     * @param fileName Path and Name of the file to import.
     * @return Returns true if map was imported correctly and false otherwise.
     */
    public boolean importMap(String fileName) {
        //Variables
        FileReader fr;
        BufferedReader br;
        StreamTokenizer st;
        int tokenCounter;
        boolean noErrorsInMethod;

        //Set up File reader
        try {
            fr = new FileReader(fileName);
        } catch (FileNotFoundException e) {
            System.err.println("File with the Name: " + fileName + " couldn't be found");
            return false;
        }
        br = new BufferedReader(fr);
        st = new StreamTokenizer(br);
        st.whitespaceChars(' ', ' ');
        st.wordChars('-','-');

        //read file
        tokenCounter = 0; //counts which token it is currently at
        while (true){
            //catch end of file and other exceptions
            try {
                if (st.nextToken() == StreamTokenizer.TT_EOF) break;
            } catch (IOException e) {
                e.printStackTrace(); //prints error to stderr
                return false;
            }
            //handle false Tokens - generally defined
            if (st.ttype == StreamTokenizer.TT_WORD){
                char cur = st.sval.charAt(0);
                if (cur != 'c' && cur != 'i' && cur != 'b' && cur != 'x'){
                    System.err.println("Read invalid char. Valid chars: {c, i, b, x}");
                    return false;
                }
            }
            if (st.ttype == StreamTokenizer.TT_NUMBER){
                int currentNumber = ((Double)st.nval).intValue();
                if (currentNumber < 0){
                    System.err.println("No negative Numbers allowed");
                }
            }

            //handle read token
            if (tokenCounter < 6) {
                noErrorsInMethod = handleFirst5(st, tokenCounter);
                if (!noErrorsInMethod) {
                    System.err.println("Method handleFirst5() failed");
                    return false;
                }
            }
            else if (tokenCounter < (width*height)+6) {
                noErrorsInMethod = handleMap(st, tokenCounter);
                if (!noErrorsInMethod) {
                    System.err.println("Method handleMap() failed");
                    return false;
                }
            }
            else {
                noErrorsInMethod = handleTransitions(st, tokenCounter);
                if (!noErrorsInMethod) {
                    System.err.println("Method handleTransitions() failed");
                    return false;
                }
            }

            tokenCounter++;
        }
        //TODO: check wether map was imported correctly
        return true;
    }

    /**
     * For Testing Purposes. Exports Map to given FileName in the directory of the Project
     * @param fileName Name of the File
     * @return returns true if the file was created successfully and otherwise false
     */
    public boolean exportMap(String fileName) {
        File newFile;
        FileWriter fw;

        //setup File and File Writer
        newFile = new File(fileName);

        try {
            newFile.createNewFile();
            fw = new FileWriter(fileName, false);

            //write infos
            fw.write("" + anzPlayers + '\n');
            fw.write( "" + overwriteStonesPerPlayer[0] + '\n');
            fw.write("" + bombsPerPlayer[0]  + ' ' + explosionRadius + '\n');
            fw.write( "" + height + ' ' + width + '\n');

            //write map
            for (int y = 0; y < height; y++){
                for (int x = 0; x < width; x++){
                    fw.write(((Character)getCharAt(x,y)).toString() + ' ');
                }
                fw.write('\n');
            }

            //write transitions
            fw.write(Transitions.AllToString(transitionen));
            
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * Returns Infos, Map and Transitions in a formatted String
     */
    public String toString() {
        String mapString = "";
        mapString += "Player count: " + anzPlayers + "\n";
        mapString += "Overwrite Stones per Player:\n";
        for (int i = 0; i < anzPlayers; i++) {
            mapString += "\tPlayer " + i + ": " + overwriteStonesPerPlayer[i] + "\n";
        }
        mapString += "Bombs per Player:\n";
        for (int i = 0; i < anzPlayers; i++) {
            mapString += "\tPlayer " + i + ": " + bombsPerPlayer[i] + "\n";
        }
        mapString += "Explosion radius: " + explosionRadius + "\n";
        mapString += "Height: " + height + ", Width: " + width + "\n\n";

        mapString += Arrays.deepToString(map).replaceAll("],","]\n");

        mapString += "\n\n";
        
        mapString += Transitions.AllToString(transitionen);
        
        return mapString;
    }


    //getter
    public int getCurrentlyPlayingI() {
        return currentlyPlaying;
    }
    public char getCurrentlyPlayingC() {
        return Integer.toString(currentlyPlaying).charAt(0);
    }

    public int getAnzPlayers() {
        return anzPlayers;
    }

    public int getOverwriteStonesForPlayer(int playerId) {
        return overwriteStonesPerPlayer[playerId];
    }

    public int getBombsForPlayer(int playerId) {
        return bombsPerPlayer[playerId];
    }

    public int getExplosionRadius() {
        return explosionRadius;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    //setter

    /**
     * Sets given Char at the given x and y position in the Map.
     * Handles if Map wasn't initialized.
     * @param y y coordinate
     * @param x x corrdinate
     * @param charToChangeTo character to set at the given position
     * @return returns true if char was set correctly
     */
    public boolean setCharAt(int x, int y, char charToChangeTo){
        if (map == null) {
            System.err.println("The Map wasn't yet initialized");
            return false;
        }
        map[y][x] = charToChangeTo;
        return true;
    }

    //private Methodes

    private boolean handleFirst5(StreamTokenizer st, int tokenCounter) {
        int currentNumber = ((Double)st.nval).intValue();
        switch (tokenCounter) {
            case 0:
                if (currentNumber > 8) { //check for valid number
                    System.err.println("Count of Players cant be over 8");
                    return false;
                }
                anzPlayers = currentNumber;
                break;
            case 1:
                for (int i = 0; i < anzPlayers; i++)
                    overwriteStonesPerPlayer[i] = currentNumber;
                break;
            case 2:
                for (int i = 0; i < anzPlayers; i++)
                    bombsPerPlayer[i] = currentNumber;
                break;
            case 3:
                explosionRadius = currentNumber;
                break;
            case 4:
                if (currentNumber > 50) { //check for valid number
                    System.err.println("Map height cant be over 50");
                    return false; //check for valid number
                }
                height = currentNumber;
                break;
            case 5:
                if (currentNumber > 50) { //check for valid number
                    System.err.println("Map width cant be over 50");
                    return false; //check for valid number
                }
                width = currentNumber;
                map = new char[height][width];
                break;
        }
        return true;
    }

    private boolean handleMap(StreamTokenizer st, int tokenCounter) {
        char minus = '-';
        //calculates x and y coordinates out of token counter, width and height
        int x = (tokenCounter-6)%width;
        int y = (tokenCounter-6)/width;
        //save char in map
        if (st.ttype == StreamTokenizer.TT_WORD) {
            setCharAt(x, y, st.sval.charAt(0));
        }
        if (st.ttype == StreamTokenizer.TT_NUMBER) {
            int currentNumber = ((Double)st.nval).intValue();
            if (currentNumber > anzPlayers){
                System.err.println("No values over " + anzPlayers + " allowed");
                return false;
            }
            setCharAt(x, y, Integer.toString(currentNumber).charAt(0));
        }
        if (st.ttype == minus) { //TODO: check if thats ok
            setCharAt(x, y, '-');
        }
        return true;
    }

    private boolean handleTransitions(StreamTokenizer st, int tokenCounter) {
        if (st.ttype != StreamTokenizer.TT_NUMBER) {
            char greater = '>';
            char less = '<';
            char minus = '-';
            if (st.ttype != greater && st.ttype != less && st.ttype != minus) { //TODO: check if thats ok
                System.err.println("No characters allowed in the transition section except <, - ,>");
                return false;
            }
        }

        char buffer;
        char buffer2;
        int currentNumber = ((Double)st.nval).intValue();

        //check for valid number
        switch (posInTransitionBuffer){
            case 0:
            case 6: //represents x
                if (currentNumber >= width) {
                    System.err.println("x Value of transition out of range");
                    return false;
                }
                break;
            case 1:
            case 7: //represents y
                if (currentNumber >= height) {
                    System.err.println("y Value of transition out of range");
                    return false;
                }
                break;
            case 2:
            case 8: //represents rotation
                if (currentNumber > 7) {
                    System.err.println("rotation can't be greater than 7");
                    return false;
                }
                break;
        }

        transitionsBuffer[posInTransitionBuffer] = currentNumber; //saves the values of the transition in the buffer

        //if one transition is complete
        if (posInTransitionBuffer == 8) {
            //convert transition infos into a char
        	buffer = Transitions.saveInChar(transitionsBuffer[0],transitionsBuffer[1],transitionsBuffer[2]);
        	buffer2 = Transitions.saveInChar(transitionsBuffer[6],transitionsBuffer[7],transitionsBuffer[8]);
        	/*
        	for(int i = 0; i< transitionsBuffer.length;i++)
        	{
        		System.out.println(transitionsBuffer[i]);
        	}
        	
        	System.out.println((int)buffer + " , " + (int)buffer2);
        	*/
            transitionen.put(buffer, buffer2);
            transitionen.put(buffer2, buffer);
            
            posInTransitionBuffer = 0;
        }
        else 
        {
        posInTransitionBuffer++;	
        }
        return true;
    }

}