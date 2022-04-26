package src;

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;

public class Map{
    //main data structure to store the Map Infos
    private char[][] map;

    //Data Structure and needed Variables to store Transitions
    public HashMap<Character,Character> transitionen = new HashMap<>();
    int[] transitionsBuffer = new int[9];
    int posInTransitionBuffer=0;
    
    //General Map Infos
    private int anzPlayers;
    private int[] overwriteStonesPerPlayer = new int[]{-1, -1, -1, -1, -1, -1, -1, -1};
    private int[] bombsPerPlayer = new int[]{-1, -1, -1, -1, -1, -1, -1, -1};
    private int explosionRadius;
    private int height;
    private int width;

    public boolean importedCorrectly = false;

    //Extended Map Info
    private int currentlyPlaying = 1;
    private boolean[] disqualifiedPlayers = new boolean[]{false, false, false, false, false, false, false, false}; //maybe it's not needed

    private ArrayList<HashSet<Position>> stonesPerPlayer = new ArrayList<>(8);
    private HashSet<Position> expansionFields = new HashSet<>();


    /**
     * Constructor.
     * Opens File Dialog to Import a Map
     */
    public Map() {
    	Dialogfenster openMap = new Dialogfenster();
        String Filepath = openMap.oeffnen();
        if(Filepath != null)
        {
            importedCorrectly = importMap(Filepath);
            if (!importedCorrectly) {
                System.err.println("Map didn't import correctly.");
                return;
            }
        }
    }

    public Map(byte[] mapByteArray){
        importedCorrectly = importMap(mapByteArray);
        if (!importedCorrectly) {
            System.err.println("Map didn't import correctly.");
            return;
        }
    }

    public Map(String fileName){
        importedCorrectly = importMap(fileName);
        if (!importedCorrectly) {
            System.err.println("Map didn't import correctly.");
            return;
        }
    }

    public Map(Map mapToCopy){
        map = new char[mapToCopy.map.length][mapToCopy.map[0].length];
        for (int y = 0; y < mapToCopy.map.length; y++){ //inner array can be copied this way
            System.arraycopy(mapToCopy.map[y], 0, map[y], 0, mapToCopy.map[0].length);
        }
        transitionen = (HashMap<Character, Character>) mapToCopy.transitionen.clone();
        anzPlayers = mapToCopy.anzPlayers;
        overwriteStonesPerPlayer = mapToCopy.overwriteStonesPerPlayer.clone(); //see if that creates a new object
        bombsPerPlayer = mapToCopy.bombsPerPlayer.clone();
        explosionRadius = mapToCopy.explosionRadius;
        height = mapToCopy.height;
        width = mapToCopy.width;
        importedCorrectly = true;
        currentlyPlaying = mapToCopy.currentlyPlaying;
        disqualifiedPlayers = mapToCopy.disqualifiedPlayers;

        transitionsBuffer = new int[9];
        posInTransitionBuffer = 0;

        //TODO: check if you can clone this
        initStonesPerPlayerSet();
        for (int playerNr = 1; playerNr <= anzPlayers; playerNr++){
            stonesPerPlayer.set(playerNr-1, (HashSet<Position>) mapToCopy.getStonesOfPlayer(playerNr).clone());
        }
    }

    
    //PUBLIC METHODS

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

        //Set up File reader
        try {
            fr = new FileReader(fileName);
        } catch (FileNotFoundException e) {
            System.err.println("File with the Name: " + fileName + " couldn't be found");
            return false;
        }
        br = new BufferedReader(fr);
        st = new StreamTokenizer(br);

        return importMapWithStreamTokenizer(st);
    }

    /**
     * Method Imports a Map from the given input Stream.
     * @param mapByteArray byte array to import from
     * @return Returns true if map was imported correctly and false otherwise.
     */
    public boolean importMap(byte[] mapByteArray) {
        //Variables
        BufferedReader br;
        StreamTokenizer st;

        String mapString = new String(mapByteArray);
        char[] mapCharArray = mapString.toCharArray();

        CharArrayReader car = new CharArrayReader(mapCharArray);
        br = new BufferedReader(car);
        st = new StreamTokenizer(br);

        return importMapWithStreamTokenizer(st);
    }

    /**
     * imports Map after a stream tokenizer was created - wich changes if you have a file or a stream
     * @param st Stream Tokenizer that is set to read the map
     * @return Returns true if map was imported correctly and false otherwise.
     */
    private boolean importMapWithStreamTokenizer(StreamTokenizer st){
        //Variables
        int tokenCounter;
        boolean noErrorsInMethod;

        st.whitespaceChars(' ', ' ');
        st.wordChars('-','-'); //TODO: doesn't really work

        //Read file
        tokenCounter = 0; //counts which token it is currently at
        while (true){
            //Catch end of file and other exceptions
            try {
                if (st.nextToken() == StreamTokenizer.TT_EOF) break;
            } catch (IOException e) {
                e.printStackTrace(); //prints error to stderr
                return false;
            }
            //Handle false Tokens - generally false
            //  Handle false chars
            if (st.ttype == StreamTokenizer.TT_WORD){
                char cur = st.sval.charAt(0);
                if (cur != 'c' && cur != 'i' && cur != 'b' && cur != 'x'){ //an imported map can't have a t to mark transitions
                    System.err.println("Read invalid char! Valid chars: {c, i, b, x}");
                    return false;
                }
            }
            //  Handle false Numbers
            if (st.ttype == StreamTokenizer.TT_NUMBER){
                int currentNumber = ((Double)st.nval).intValue();
                if (currentNumber < 0){
                    System.err.println("No negative Numbers allowed");
                }
            }

            //Handle read token
            //First read infos
            if (tokenCounter < 6) {
                noErrorsInMethod = handleFirst5(st, tokenCounter);
                if (!noErrorsInMethod) {
                    System.err.println("Method handleFirst5() failed.");
                    return false;
                }
            }
            //then read map
            else if (tokenCounter < ((width-2)*(height-2))+6) { //-2 because of the ring around the map
                noErrorsInMethod = handleMap(st, tokenCounter);
                if (!noErrorsInMethod) {
                    System.err.println("Method handleMap() failed");
                    return false;
                }
            }
            //and then read transitions
            else {
                noErrorsInMethod = handleTransitions(st);
                if (!noErrorsInMethod) {
                    System.err.println("Method handleTransitions() failed");
                    return false;
                }
            }

            tokenCounter++;
        }
        //TODO: check weather map was imported correctly
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
        char currChar;
        //setup File and File Writer
        newFile = new File(fileName);

        try {
            newFile.createNewFile();
            fw = new FileWriter(fileName, false);

            //write infos
            fw.write("" + anzPlayers + '\n');
            fw.write( "" + overwriteStonesPerPlayer[0] + '\n');
            fw.write("" + bombsPerPlayer[0]  + ' ' + explosionRadius + '\n');
            fw.write( "" + (height-2) + ' ' + (width-2) + '\n');

            //write map
            for (int y = 1; y < height-1; y++){
                for (int x = 1; x < width-1; x++){
                    currChar = getCharAt(x,y);
                    if (currChar == 't') currChar = '-';
                    fw.write(Character.toString(currChar) + ' ');
                }
                fw.write('\n');
            }

            //write transitions
            fw.write(Transitions.AllToStringWithIndexShift(transitionen));
            
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
        mapString += "currently playing: " + currentlyPlaying + "\n";
        mapString += "Overwrite Stones per Player:\n";
        for (int i = 1; i <= anzPlayers; i++) {
            mapString += "\tPlayer " + i + ": " + overwriteStonesPerPlayer[i-1] + "\n";
        }
        mapString += "Bombs per Player:\n";
        for (int i = 1; i <= anzPlayers; i++) {
            mapString += "\tPlayer " + i + ": " + bombsPerPlayer[i-1] + "\n";
        }
        mapString += "Explosion radius: " + explosionRadius + "\n";
        mapString += "Height: " + height + ", Width: " + width + "\n\n";

        mapString += Arrays.deepToString(map).replaceAll("],","]\n");

        mapString += "\n\n";
        
        mapString += Transitions.AllToString(transitionen);
        
        return mapString;
    }


    //Kopieren und umbauen zur toString_Server Methode

    public String toString_Server(ArrayList<int[]> everyPossibleMove)
    {
        String mapString = "";
        String bufferString;

        ArrayList<Position> possibleMoves = null;
        if (everyPossibleMove != null) {
            possibleMoves = new ArrayList<>();
            for (int[] posAndInfo : everyPossibleMove) {
                possibleMoves.add(new Position(posAndInfo[0], posAndInfo[1]));
            }
        }

        for (int y = 1; y < height-1; y++){
            for (int x = 1; x < width-1; x++){
                bufferString = "";
                bufferString += getCharAt(x,y);
                if (possibleMoves != null && possibleMoves.contains(new Position(x,y))) {
                    bufferString += "'";
                }
                else
                {
                    bufferString += " ";
                }

                mapString += bufferString;
            }
            mapString += "\n";
        }
        return mapString;
    }

    //not very clean code - just for testing purposes
    public String toString(ArrayList<int[]> everyPossibleMove, boolean showTransitions, boolean addColorsForIntelliJ){
        final String ANSI_RESET = "\u001B[0m";
        final String ANSI_BLACK = "\u001B[30m";
        final String ANSI_RED = "\u001B[31m";
        final String ANSI_GREEN = "\u001B[32m";
        final String ANSI_YELLOW = "\u001B[33m";
        final String ANSI_BLUE = "\u001B[34m";
        final String ANSI_PURPLE = "\u001B[35m";
        final String ANSI_BRIGHT_CYAN = "\u001B[96m";
        final String ANSI_WHITE = "\u001B[37m";
        final String ANSI_BRIGHT_YELLOW = "\u001B[93m";
        final String ANSI_BRIGHT_GREEN = "\u001B[92m";

        String mapString = "";
        String bufferString;

        ArrayList<Position> possibleMoves = null;
        if (everyPossibleMove != null) {
            possibleMoves = new ArrayList<>();
            for (int[] posAndInfo : everyPossibleMove) {
                possibleMoves.add(new Position(posAndInfo[0], posAndInfo[1]));
            }
        }

        mapString += "Player count: " + anzPlayers + "\n";
        mapString += "currently playing: " + currentlyPlaying + "\n";
        mapString += "Overwrite Stones per Player:\n";
        for (int i = 1; i <= anzPlayers; i++) {
            mapString += "\tPlayer " + i + ": " + overwriteStonesPerPlayer[i-1] + "\n";
        }
        mapString += "Bombs per Player:\n";
        for (int i = 1; i <= anzPlayers; i++) {
            mapString += "\tPlayer " + i + ": " + bombsPerPlayer[i-1] + "\n";
        }
        mapString += "Explosion radius: " + explosionRadius + "\n";
        mapString += "Height: " + height + ", Width: " + width + "\n\n";

        for (int i = -1 ; i < width; i++) mapString += i + "\t";
        mapString += "\n";
        for (int y = 0; y < height; y++){
            mapString += y + "\t";
            for (int x = 0; x < width; x++){
                bufferString = "";

                if (addColorsForIntelliJ) {
                    switch (getCharAt(x,y)){
                        case '1':
                            bufferString += ANSI_RED;
                            break;
                        case '2':
                            bufferString += ANSI_BLUE;
                            break;
                        case '3':
                            bufferString += ANSI_GREEN;
                            break;
                        case '4':
                            bufferString += ANSI_YELLOW;
                            break;
                        case '5':
                            bufferString += ANSI_BRIGHT_CYAN;
                            break;
                        case '6':
                            bufferString += ANSI_PURPLE;
                            break;
                        case '7':
                            bufferString += ANSI_BRIGHT_YELLOW;
                            break;
                        case '8':
                            bufferString += ANSI_BRIGHT_GREEN;
                            break;
                    }
                }
                bufferString += getCharAt(x,y);
                if (possibleMoves != null && possibleMoves.contains(new Position(x,y))) bufferString += "'";

                if (addColorsForIntelliJ) bufferString += ANSI_RESET;

                mapString += bufferString + "\t";
            }
            mapString += "\n";
        }

        if (showTransitions) {
            mapString += '\n';
            mapString += Transitions.AllToString(transitionen);
        }

        return mapString;
    }

    /**
     * Swaps Stones of the currently playing player with the specified player
     * @param playerId ID of the player with whom to swap stones
     */
    public void swapStonesWithOnePlayer(int playerId) {
        //error handling
        if (playerId < 1 || playerId > anzPlayers) {
            System.err.println("A Player with ID " + playerId + " doesn't exist");
            return;
        }
        if (playerId == currentlyPlaying) {
            // if the player chose himself do nothing
            return;
        }

        //index shift
        playerId -= 1;
        int currentlyPlaying = this.currentlyPlaying-1;

        //swap stones with player
        HashSet<Position> buffer;
        buffer = stonesPerPlayer.get(currentlyPlaying);
        stonesPerPlayer.set(currentlyPlaying, stonesPerPlayer.get(playerId));
        stonesPerPlayer.set(playerId, buffer);

        //color the new stones of the player in its color
        for (int playerNr : new int[]{playerId, currentlyPlaying}){
            for (Position pos : stonesPerPlayer.get(playerNr)) {
                setCharAtWithoutSetUpdate(pos, (char)('0'+playerNr+1)); //reverse index shift
            }
        }
    }

    /**
     * Die Farben aller Spieler werden um eins verschoben
     */
    public void Inversion() {
        HashSet<Position> buffer;

        //adds the last element at the front, so it shifts all the other elements one further and deletes the last element
        buffer = stonesPerPlayer.remove(stonesPerPlayer.size()-1);
        stonesPerPlayer.add(0,buffer);


        //colors the new stones of the player in its color
        for (int playerNr = 1; playerNr <= anzPlayers; playerNr++){
            for (Position pos : stonesPerPlayer.get(playerNr-1)){
                setCharAtWithoutSetUpdate(pos, (char)('0'+playerNr));
            }
        }
    }

    //GETTER
    /**
     * @param x x coordinate
     * @param y y coordinate
     * @return Returns the Character at the given x and y position in the Map. If it is out of boundaries it returns '-'
     */
    public char getCharAt(int x, int y){
        //check boundaries
        if (x >= width || y >= height || x < 0 || y < 0) return '-';
        //return value
        return map[y][x];
    }

    public char getCharAt(Position pos){
        //check boundaries
        if (pos.x > width || pos.y > height || pos.x < 0 || pos.y < 0) return '-';
        //return value
        return map[pos.y][pos.x];
    }

    public int getCurrentlyPlayingI() {
        return currentlyPlaying;
    }
    public char getCurrentlyPlayingC() {
        return (char) ('0'+currentlyPlaying);
    }

    public int getAnzPlayers() {
        return anzPlayers;
    }


    public int getOverwriteStonesForPlayer(int playerId) {
        return overwriteStonesPerPlayer[playerId-1];
    }

    public int getBombsForPlayer(int playerId) {
        return bombsPerPlayer[playerId-1];
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

    public HashSet<Position> getStonesOfPlayer(int playerNr){
        if (playerNr < 0 || playerNr > anzPlayers) {
            System.err.println("Player with that number isn't in the Game");
            return null;
        }

        return stonesPerPlayer.get(playerNr-1);
    }

    public HashSet<Position> getExpansionFields(){
        return expansionFields;
    }

    //SETTER

    /**
     * Sets given Char at the given x and y position in the Map.
     * Handles if Map wasn't initialized.
     * @param y y coordinate
     * @param x x coordinate
     * @param charToChangeTo character to set at the given position
     * @return returns true if char was set correctly
     */
    public boolean setCharAt(int x, int y, char charToChangeTo){
        char charAtPos;
        Position posToSetChar = new Position(x,y);

        //check if map is initialized
        if (map == null) {
            System.err.println("The Map wasn't yet initialized");
            return false;
        }
        //check boundaries
        if (x >= width || y >= height || x < 0 || y < 0) {
            System.err.println("Position out of Map");
            return false;
        }
        //get what is there
        charAtPos = getCharAt(x,y);
        //if there was a player remove stone from his stone set
        if (charAtPos != '0' && Character.isDigit(charAtPos)) {
            stonesPerPlayer.get(charAtPos-'0'-1).remove(posToSetChar); //-'0' to convert to int
        }
        if (charAtPos == 'x') expansionFields.remove(posToSetChar);

        //set char
        map[y][x] = charToChangeTo;

        //add char to set of player
        if (charToChangeTo != '0' && Character.isDigit(charToChangeTo)) {
            stonesPerPlayer.get(charToChangeTo-'0'-1).add(posToSetChar); //-'0' to convert to int
        }

        return true;
    }

    private void setCharAtWithoutSetUpdate(Position posToSetKeystone, char charToChangeTo){
        //check if map is initialized
        if (map == null) {
            System.err.println("The Map wasn't yet initialized");
            return;
        }
        //check boundaries
        if (posToSetKeystone.x >= width || posToSetKeystone.y >= height || posToSetKeystone.x < 0 || posToSetKeystone.y < 0) {
            System.err.println("Position out of Map");
            return;
        }

        //set char
        map[posToSetKeystone.y][posToSetKeystone.x] = charToChangeTo;
    }

    public void increaseBombsOfPlayer(){
        bombsPerPlayer[currentlyPlaying-1]++;
    }

    public void decreaseBombsOfPlayer(){
        bombsPerPlayer[currentlyPlaying-1]--;
    }

    public void increaseOverrideStonesOfPlayer(){
        overwriteStonesPerPlayer[currentlyPlaying-1]++;
    }

    public void decreaseOverrideStonesOfPlayer(){
        overwriteStonesPerPlayer[currentlyPlaying-1]--;
    }

    public void nextPlayer(){
        currentlyPlaying++;
        if (currentlyPlaying == anzPlayers+1) currentlyPlaying = 1;
    }

    public void setPlayer(int PlayerID) {
        currentlyPlaying = PlayerID;
    }

    public void disqualifyPlayer(int playerNr){
        disqualifiedPlayers[playerNr-1] = false;
    }

    //PRIVATE METHODS

    private void initStonesPerPlayerSet(){
        for (int playerNr = 0; playerNr < anzPlayers; playerNr++) {
            stonesPerPlayer.add(new HashSet<Position>());
        }
    }

    private boolean handleFirst5(StreamTokenizer st, int tokenCounter) {
        int currentNumber = ((Double)st.nval).intValue();
        switch (tokenCounter) {
            case 0:
                if (currentNumber > 8) { //check for valid number
                    System.err.println("Count of Players can't be over 8");
                    return false;
                }
                anzPlayers = currentNumber;
                initStonesPerPlayerSet();
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
                height = currentNumber + 2; //+2 because of the ring around the map
                break;
            case 5:
                if (currentNumber > 50) { //check for valid number
                    System.err.println("Map width cant be over 50");
                    return false; //check for valid number
                }
                width = currentNumber + 2; //+2 because of the ring around the map
                //create map
                map = new char[height][width];
                initializeMap();
                break;
        }
        return true;
    }

    private void initializeMap(){
        for (int y = 0; y < height; y++){
            for (int x = 0; x < width; x++){
                setCharAt(x,y,'-');
            }
        }
    }

    private boolean handleMap(StreamTokenizer st, int tokenCounter) {
        char minus = '-';
        char charAtPos;
        char charToImport;

        //calculates x and y coordinates out of token counter, width and height
        int x = (tokenCounter-6)%(width-2) + 1; //-2 and +1 is index shift
        int y = (tokenCounter-6)/(width-2) + 1; //-2 and +1 is index shift

        //save char in map
        //if it's a char
        if (st.ttype == StreamTokenizer.TT_WORD) {
            charToImport = st.sval.charAt(0);
            setCharAt(x, y, charToImport);
            if (charToImport == 'x') expansionFields.add(new Position(x,y));
        }
        //if it's a number
        if (st.ttype == StreamTokenizer.TT_NUMBER) {
            int currentNumber = ((Double)st.nval).intValue();
            if (currentNumber > anzPlayers){
                System.err.println("No values over " + anzPlayers + " allowed!");
                return false;
            }
            charAtPos = (char)('0'+currentNumber);
            setCharAt(x, y, charAtPos);

            //if it's a player add the position his stones
            if (currentNumber != 0) stonesPerPlayer.get(currentNumber-1).add(new Position(x,y));
        }
        if (st.ttype == minus) {
            setCharAt(x, y, '-');
        }
        return true;
    }

    private boolean handleTransitions(StreamTokenizer st) {
        if (st.ttype != StreamTokenizer.TT_NUMBER) {
            char greater = '>';
            char less = '<';
            char minus = '-';
            if (st.ttype != greater && st.ttype != less && st.ttype != minus) { //TODO: check if that's ok
                System.err.println("No characters allowed in the transition section except <, - ,>");
                return false;
            }
        }

        char buffer;
        char buffer2;
        int currentNumber;
        if (posInTransitionBuffer == 2 || posInTransitionBuffer == 8) { //special handling of rotation value and position value
            currentNumber = ((Double) st.nval).intValue();
        }
        else {
            currentNumber = ((Double) st.nval).intValue() + 1;  //+1 is index shift
        }

        //check for valid number
        switch (posInTransitionBuffer){
            case 0:
            case 6: //represents x
                if (currentNumber >= width) { //no index shift needed here
                    System.err.println("x Value of transition out of range");
                    return false;
                }
                break;
            case 1:
            case 7: //represents y
                if (currentNumber >= height) { //no index shift needed here
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
            //Add transition to hash Map
            transitionen.put(buffer, buffer2);
            transitionen.put(buffer2, buffer);

            //Add Transitions to Map
            //first end
            Position endOne = new Position(transitionsBuffer[0], transitionsBuffer[1]);
            endOne = Position.goInR(endOne, transitionsBuffer[2]);
            setCharAt(endOne.x, endOne.y, 't');

            //second end
            Position endTwo = new Position(transitionsBuffer[6], transitionsBuffer[7]);
            endTwo = Position.goInR(endTwo, transitionsBuffer[8]);
            setCharAt(endTwo.x, endTwo.y, 't');
            
            posInTransitionBuffer = 0;
        }
        else 
        {
        posInTransitionBuffer++;	
        }
        return true;
    }

}
