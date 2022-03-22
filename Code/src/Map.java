package src;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

public class Map {
    private char[][] map; //main data structure to store the Map Infos

    //Data Structure and needed Variables to store Transitions
    ArrayList<char[]> transitions = new ArrayList<>(); //TODO: austauschen durch hash map
    int[] transitionsBuffer = new int[3];
    private boolean isFirst = true;

    //General Map Infos
    private int anzPlayers;
    private int[] overwriteStonesPerPlayer = new int[]{-1, -1, -1, -1, -1, -1, -1, -1};
    private int[] bombsPerPlayer = new int[]{-1, -1, -1, -1, -1, -1, -1, -1};
    private int explosionRadius;
    private int height;
    private int width;

    /**
     * Constructor imports Map from given Filepath
     */
    public Map() {
    	Dialogfenster Mapöffnen = new Dialogfenster();
    	boolean importedCorrectly = importMap(Mapöffnen.oeffnen());
        if (!importedCorrectly) {
            System.err.println("Map didn't import correctly.");
        }
    }
    
    //public Methods

    /**
     * @param x x corrdinate
     * @param y y coordinate
     * @return Returns the Character at the given x and y position in the Map.
     */
    public char getCharAt(int x, int y){
        return map[y][x];
    }

    /**
     * Method Imports a Map from the given File Name.
     * @param fileName Path and Name of the file to import.
     * @return Returns true if map was imported correctly and false otherwise.
     */
    public boolean importMap(String fileName) {
        //Variables
        FileReader fr = null;
        BufferedReader br = null;
        StreamTokenizer st = null;
        int tokenCounter = 0;
        int x = 0, y = 0;
        boolean readTransitionsNow = false;
        boolean noErrorsInMethod = true;

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
     * @throws IOException
     */
    public boolean exportMap(String fileName) throws IOException {
        File newFile;
        FileWriter fw;

        //setup File and File Writer
        newFile = new File(fileName);
        newFile.createNewFile();
        fw = new FileWriter(fileName, false);

        //write infos
        fw.write(((Integer)anzPlayers).toString() + '\n');
        fw.write( ((Integer)overwriteStonesPerPlayer[0]).toString() + '\n');
        fw.write(((Integer)bombsPerPlayer[0]).toString()  + ' ' + ((Integer)explosionRadius).toString() + '\n');
        fw.write( ((Integer)height).toString() + ' ' + ((Integer)width).toString() + '\n');

        //write map
        for (int y = 0; y < height; y++){
            for (int x = 0; x < width; x++){
                fw.write(((Character)getCharAt(x,y)).toString() + ' ');
            }
            fw.write('\n');
        }

        //write transitions
        for (char[] pair : transitions){
            int x1 = Transitions.getX(pair[0]);
            int y1 = Transitions.getY(pair[0]);
            int r1 = Transitions.getR(pair[0]);

            int x2 = Transitions.getX(pair[1]);
            int y2 = Transitions.getY(pair[1]);
            int r2 = Transitions.getR(pair[1]);

            fw.write(x1 + " " + y1 + " " + r1 + " <-> " + x2 + " " + y2 + " " + r2 + '\n');
        }

        fw.close();

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

        for (char[] pair : transitions){
            int x1 = Transitions.getX(pair[0]);
            int y1 = Transitions.getY(pair[0]);
            int r1 = Transitions.getR(pair[0]);

            int x2 = Transitions.getX(pair[1]);
            int y2 = Transitions.getY(pair[1]);
            int r2 = Transitions.getR(pair[1]);

            mapString += x1 + " " + y1 + " " + r1 + " <-> " + x2 + " " + y2 + " " + r2 + "\n";
        }
        return mapString;
    }

    //private Methodes

    /**
     * Sets given Char at the given x and y position in the Map.
     * Handles if Map wasn't initialized.
     * @param y y coordinate
     * @param x x corrdinate
     * @param charToChangeTo character to set at the given position
     * @return returns true if char was set correctly
     */
    private boolean setCharAt(int x, int y, char charToChangeTo){
        if (map == null) {
            System.err.println("The Map wasn't yet initialized");
            return false;
        }
        map[y][x] = charToChangeTo;
        return true;
    }

    /**
     * In progress...
     * @param st Stream Tokenizer to read the Char out of
     * @return Returns the current Token as a char
     */
    private char getCharFromStreamTokenizer(StreamTokenizer st){
        char result = 0;
        if (st.ttype == StreamTokenizer.TT_WORD) result = st.sval.charAt(0);
        if (st.ttype == StreamTokenizer.TT_NUMBER) result = ((Integer)((Double)st.nval).intValue()).toString().charAt(0); //TODO: check if all that is neccessarry
        if (st.ttype == 45) result = '-'; //TODO: do that right

        return result;
    }

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

        int posInTransitionBuffer = (tokenCounter - (width*height)+6) % 3; //TODO: Übersprint <-> sind  3 - besser machen
        char buffer;
        int currentNumber = ((Double)st.nval).intValue();

        //check for valid number
        switch (posInTransitionBuffer){
            case 0: //represents x
                if (currentNumber >= width) {
                    System.err.println("x Value of transition out of range");
                    return false;
                }
                break;
            case 1: //represents y
                if (currentNumber >= height) {
                    System.err.println("y Value of transition out of range");
                    return false;
                }
                break;
            case 2: //represents rotation
                if (currentNumber > 7) {
                    System.err.println("rotation can't be greater than 7");
                    return false;
                }
                break;
        }

        transitionsBuffer[posInTransitionBuffer] = currentNumber; //saves the 3 values of one transition end in the buffer

        //if one transition end is complete
        if (posInTransitionBuffer == 2) {
            //convert transition infos into a char
            buffer = Transitions.saveInChar(transitionsBuffer[0],transitionsBuffer[1],transitionsBuffer[2]);
            //Checks if transition end is the first or second one
            if (isFirst) { //if it's the first one it creates a new transition pair and saves it in the transition List
                char[] pair = new char[2];
                pair[0] = buffer;
                transitions.add(pair);
            }
            else { //If it's the second one it adds the second transition end to the pair in the transition List
                transitions.get(transitions.size()-1)[1] = buffer;
            }

            //toggle if transition is the first end or the second one
            isFirst = !isFirst;
        }
        return true;
    }

}
