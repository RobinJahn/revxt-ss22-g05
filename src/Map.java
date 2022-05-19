package src;

import java.io.*;
import java.util.*;

public class Map{
    //main data structure to store the Map Infos
    private char[][] map;

    //Data Structure and needed Variables to store Transitions
    private HashMap<Character,Character> transitions = new HashMap<>();
    private int[] transitionsBuffer = new int[9];
    private int posInTransitionBuffer = 0;
    
    //General Map Infos
    private int anzPlayers;
    private int[] overwriteStonesPerPlayer = new int[]{-1, -1, -1, -1, -1, -1, -1, -1};
    private int[] bombsPerPlayer = new int[]{-1, -1, -1, -1, -1, -1, -1, -1};
    private int explosionRadius;
    private int height;
    private int width;

    public boolean phaseOne;
    private boolean importedCorrectly = false;

    //Extended Map Info
    private int currentlyPlaying = 1;
    private boolean[] disqualifiedPlayers = new boolean[]{false, false, false, false, false, false, false, false}; //TODO: maybe it's not needed

    private ArrayList<HashSet<Position>> stonesPerPlayer = new ArrayList<>(8);
    private HashSet<Position> expansionFields = new HashSet<>();

    //Valid Moves Varables
    private ArrayList<HashMap<Position, Integer>> ValidMoves = new ArrayList<>(8);
    private ArrayList<HashMap<Position, Integer>> OverwriteMoves = new ArrayList<>(8);

    private Arrow[][][][] AffectedArrows;
    private Arrow[][][] StartingArrows;



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
        transitions = (HashMap<Character, Character>) mapToCopy.transitions.clone();
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

        //Stones per player
        initStonesPerPlayerSet();
        for (int playerNr = 1; playerNr <= anzPlayers; playerNr++){
            stonesPerPlayer.set(playerNr-1, (HashSet<Position>) mapToCopy.getStonesOfPlayer(playerNr).clone());
        }
        expansionFields = (HashSet<Position>) mapToCopy.expansionFields.clone();

        //Move carry along //TODO: do better

        for (int playerNr = 0; playerNr < anzPlayers; playerNr++) {
            ValidMoves.add(new HashMap<>());
            //ValidMoves.get(playerNr).putAll(mapToCopy.ValidMoves.get(playerNr));
            for (Position pos : mapToCopy.ValidMoves.get(playerNr).keySet()){
                ValidMoves.get(playerNr).put(pos.clone(), mapToCopy.ValidMoves.get(playerNr).get(pos));
            }
            OverwriteMoves.add(new HashMap<>());
            //OverwriteMoves.get(playerNr).putAll(mapToCopy.OverwriteMoves.get(playerNr));
            for (Position pos : mapToCopy.OverwriteMoves.get(playerNr).keySet()){
                OverwriteMoves.get(playerNr).put(pos.clone(), mapToCopy.OverwriteMoves.get(playerNr).get(pos));
            }
        }

        ArrayList<Arrow> arrowsToUpdate = new ArrayList<>();
        ArrayList<Integer> playerOfArrow = new ArrayList<>();
        Arrow newArrow = null;
        int[] posAndR;
        int currPlayer;
        Arrow arrow;

        StartingArrows = new Arrow[height][width][8];
        for (int y = 0; y < height; y++){
            for (int x = 0; x < width; x++){
                for (int i = 0; i < 8; i++){
                    if (mapToCopy.StartingArrows[y][x][i] != null) {
                        newArrow = mapToCopy.StartingArrows[y][x][i].clone();
                        StartingArrows[y][x][i] = newArrow;
                        arrowsToUpdate.add(newArrow);
                        playerOfArrow.add(map[y][x] - '0');
                    }
                }
            }
        }

        AffectedArrows = new Arrow[height][width][anzPlayers][8];
        for (int i = 0; i < arrowsToUpdate.size(); i++){
            arrow = arrowsToUpdate.get(i);
            currPlayer = playerOfArrow.get(i);
            for ( int j = 1; j < arrow.positionsWithDirection.size(); j++){ //start from the first position that needs to be in the affected list
                posAndR = arrow.positionsWithDirection.get(j);
                AffectedArrows[posAndR[1]][posAndR[0]][currPlayer-1][posAndR[2]] = arrow;
            }
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
        //st.wordChars('-','-');

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

        //after import create first few arrows
        for (int playerNr = 1; playerNr <= anzPlayers; playerNr++) {
            for (Position pos : getStonesOfPlayer(playerNr)){
                firstCreation(pos.x, pos.y, playerNr);
            }
        }


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
            fw.write(Transitions.AllToStringWithIndexShift(transitions));
            
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
        
        mapString += Transitions.AllToString(transitions);
        
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
                char temp = getCharAt(x,y);
                if(temp != 't')
                {
                    bufferString += temp;
                }
                else
                {
                    bufferString += "-";
                }

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
    public String toString(List<int[]> everyPossibleMove, boolean showTransitions, boolean useColors){
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

        final String ANSI_BLACK_BACKGROUND = "\u001B[40m";
        final String ANSI_RED_BACKGROUND = "\u001B[41m";
        final String ANSI_GREEN_BACKGROUND = "\u001B[42m";
        final String ANSI_YELLOW_BACKGROUND = "\u001B[43m";
        final String ANSI_BLUE_BACKGROUND = "\u001B[44m";
        final String ANSI_PURPLE_BACKGROUND = "\u001B[45m";
        final String ANSI_CYAN_BACKGROUND = "\u001B[46m";
        final String ANSI_WHITE_BACKGROUND = "\u001B[47m";

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

                if (useColors) {
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
                        case 'b':
                            bufferString += ANSI_BLACK;
                            bufferString += ANSI_GREEN_BACKGROUND;
                            break;
                        case 'i':
                            bufferString += ANSI_BLACK;
                            bufferString += ANSI_PURPLE_BACKGROUND;
                            break;
                        case 'c':
                            bufferString += ANSI_BLACK;
                            bufferString += ANSI_CYAN_BACKGROUND;
                            break;
                        case 'x':
                            bufferString += ANSI_BLACK;
                            bufferString += ANSI_WHITE_BACKGROUND;
                            break;
                    }
                }
                bufferString += getCharAt(x,y);
                if (possibleMoves != null && possibleMoves.contains(new Position(x,y))) bufferString += "'";

                if (useColors) bufferString += ANSI_RESET;

                mapString += bufferString + "\t";
            }
            mapString += "\n";
        }

        if (showTransitions) {
            mapString += '\n';
            mapString += Transitions.AllToString(transitions);
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

    /**
     * goes one step in the specified direction. If there's a wall or the end of the map it returns null if there's a transition it goes through it
     * @param pos start position
     * @param r direction to do the step in
     * @return returns null if move isn't possible and the direction after the move if it is possible. If a transition changes the direction this is where to get the new one
     */
    public Integer doAStep(Position pos, int r){
        char transitionLookup;
        char charAtPos;
        Character transitionEnd;
        int newR = r;
        Position newPos;

        //check if step is valid in x direction
        if (pos.x == 0){
            if (r == 7 || r == 6 || r == 5) return null;
        }
        if (pos.x == this.getWidth()-1){
            if (r == 1 || r == 2 || r == 3) return null;
        }
        //check if step is valid in y direction
        if (pos.y == 0) {
            if (r == 7 || r == 0 || r == 1) return null;
        }
        if (pos.y == this.getHeight()-1){
            if (r == 3 || r == 4 || r == 5) return null;
        }

        //do the step
        newPos = Position.goInR(pos, r);

        charAtPos = this.getCharAt(newPos);

        //check if there's a wall
        if (charAtPos == '-') return null;

        //check if there is a transition
        if (charAtPos == 't') {
            //check if the direction matches the current one
            transitionLookup = Transitions.saveInChar(pos.x,pos.y,r); //pos is the old position
            transitionEnd = this.getTransitions().get(transitionLookup);
            if (transitionEnd == null) return null; //if there isn't an entry

            //go through the transition
            newPos.x = Transitions.getX(transitionEnd);
            newPos.y= Transitions.getY(transitionEnd);
            newR = Transitions.getR(transitionEnd);
            newR = (newR+4)%8; //flips direction because transition came out of that direction, so you go through the other way

            if (this.getCharAt(newPos) == '-') return null; //only relevant in bomb phase
        }

        //sets the position to the new One (call by reference)
        pos.x = newPos.x;
        pos.y = newPos.y;
        return newR;
    }


    /* GETTER */
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

    public HashMap<Character, Character> getTransitions() {
        return transitions;
    }

    public boolean wasImportedCorrectly() {
        return importedCorrectly;
    }

    public ArrayList<int[]> getValidMoves(){
        ArrayList<int[]> resultList = new ArrayList<>();
        char currChar;

        for (Position pos : ValidMoves.get(currentlyPlaying-1).keySet()){
            currChar = getCharAt(pos);
            switch (currChar){
                case '0':
                case 'i':
                    resultList.add(new int[]{pos.x, pos.y, 0});
                    break;
                case 'b':
                    resultList.add(new int[]{pos.x, pos.y, 20});
                    resultList.add(new int[]{pos.x, pos.y, 21});
                    break;
                case 'c':
                    for (int playerNr = 1; playerNr <= anzPlayers; playerNr++) {
                        if (playerNr == currentlyPlaying) continue;
                        resultList.add(new int[]{pos.x, pos.y, playerNr});
                    }
                    break;
                default:
                    System.err.println("Valid Move was on another field: " + pos + "=" + currChar);
                    break;
            }
        }

        if (getOverwriteStonesForPlayer(currentlyPlaying) > 0) {
            for (Position pos : OverwriteMoves.get(currentlyPlaying - 1).keySet()) {
                resultList.add(new int[]{pos.x, pos.y, 0});
            }

            for (Position pos : expansionFields){
                resultList.add(new int[]{pos.x, pos.y, 0});
            }
        }

        return resultList;
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
            stonesPerPlayer.get(charAtPos-'0'-1).remove(posToSetChar); //-'0' to convert to int; -1 because of index shift
        }
        if (charAtPos == 'x') expansionFields.remove(posToSetChar);

        //set char
        map[y][x] = charToChangeTo;

        //add char to set of player
        if (charToChangeTo != '0' && Character.isDigit(charToChangeTo)) {
            stonesPerPlayer.get(charToChangeTo-'0'-1).add(posToSetChar); //-'0' to convert to int; -1 because of index shift
        }

        //update valid moves
        if (charAtPos != '-' && charAtPos != '+' && charAtPos != 't' && charToChangeTo != '0' && charToChangeTo != '-' && charToChangeTo != '+' && charToChangeTo != 't')
            fieldChange(x,y,charAtPos-'0',charToChangeTo-'0');

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

    //Methodes for Move carry along

    private void firstCreation(int x, int y, int newPlayer){
        for (int r = 0; r <= 7; r++){
            addNewArrow(x, y, r, newPlayer);
        }
    }

    private void fieldChange(int x, int y, int oldPlayer, int newPlayer){
        Arrow currArrow;

        //update every arrow for every player that is affected by this field
        for (int playerNr = 1; playerNr <= anzPlayers; playerNr++){
            for (int r = 0; r <= 7; r++){
                currArrow = AffectedArrows[y][x][playerNr-1][r];
                if (currArrow != null) {
                    updateArrow(currArrow, playerNr, x, y, r);
                }
            }
        }

        //delete arrows that start on this position
        //and add new ones for the new player
        for (int r = 0; r <= 7; r++){
            currArrow = StartingArrows[y][x][r];
            if (currArrow != null) {
                deleteArrowFrom(currArrow, oldPlayer, 0);
            }
            addNewArrow(x, y, r, newPlayer);
        }

        System.out.println("List is correkt: " + checkForBugs());
        //return;
    }

    private void deleteArrowFrom(Arrow oldArrow, int arrowOfPlayer, int from){
        int[] posAndR;
        Position currPos;
        Integer arrowsPointingToPos;

        //delete valid move created by this arrow
        if (oldArrow.createsValidMove){
            //get last element
            posAndR = oldArrow.positionsWithDirection.get(oldArrow.positionsWithDirection.size()-1);
            currPos = new Position(posAndR[0],posAndR[1]);
            removeValidPosition(arrowOfPlayer, currPos);
            oldArrow.createsValidMove = false;
        }

        //starts from the end
        for (int counter =  oldArrow.positionsWithDirection.size()-1; counter >= from; counter--){
            //get position and direction
            posAndR = oldArrow.positionsWithDirection.get(counter);
            //delete references out of affected arrows or starting arrows
            if (counter == 0){
                StartingArrows[posAndR[1]][posAndR[0]][posAndR[2]] = null;
            }
            else {
                if (AffectedArrows[posAndR[1]][posAndR[0]][arrowOfPlayer-1][posAndR[2]] == oldArrow) {
                    AffectedArrows[posAndR[1]][posAndR[0]][arrowOfPlayer - 1][posAndR[2]] = null;
                }
            }
            //delete overwrite moves created by this arrow
            if (counter >= 2){
                currPos = new Position(posAndR[0],posAndR[1]);

                removeOverwritePosition(arrowOfPlayer, currPos);
            }
            //delete last position out of list
            oldArrow.positionsWithDirection.remove(counter);
        }
    }

    private void addNewArrow(int x, int y, int direction, int arrowOfPlayer){
        Arrow newArrow = new Arrow();
        int[] posAndR;

        //add starting position
        newArrow.positionsWithDirection.add(new int[]{x,y,direction});

        //create Arrow on map
        createArrowFrom(newArrow, x, y, direction, arrowOfPlayer);

        //add references
        //  start position
        posAndR = newArrow.positionsWithDirection.get(0);
        StartingArrows[posAndR[1]][posAndR[0]][posAndR[2]] = newArrow;

        //  middle and end positions
        for (int i = 1; i < newArrow.positionsWithDirection.size(); i++){
            posAndR = newArrow.positionsWithDirection.get(i);
            //save reference in affected arrow list
            AffectedArrows[posAndR[1]][posAndR[0]][arrowOfPlayer-1][posAndR[2]] = newArrow;
        }
    }

    private void updateArrow(Arrow currArrow, int arrowOfPlayer, int x, int y, int direction){
        //TODO: use binary search
        int i = 0;
        int[] posAndR;

        for (; i < currArrow.positionsWithDirection.size(); i++){
            posAndR = currArrow.positionsWithDirection.get(i);
            if (posAndR[0] == x && posAndR[1] == y && posAndR[2] == direction) break;
        }
        if (i >= currArrow.positionsWithDirection.size()) System.err.println("Something went wrong - Position not found");
        //deletes all positions after the one we got
        deleteArrowFrom(currArrow, arrowOfPlayer, i);

        //check further
        //  get last position
        if (currArrow.positionsWithDirection.size()-1 >= 0) {
            posAndR = currArrow.positionsWithDirection.get(currArrow.positionsWithDirection.size() - 1);
            //  create Arrow from last position on foreward
            createArrowFrom(currArrow, posAndR[0], posAndR[1], posAndR[2], arrowOfPlayer);
        }
        else {
            System.err.println("The Size of the arrow is less then 1");
        }

        //add new references

        for (int j = i; j < currArrow.positionsWithDirection.size(); j++) {
            //  start position
            if (j == 0) {
                System.err.println("Something went wrong - While Updating Arrow it got deleted");
                posAndR = currArrow.positionsWithDirection.get(0);
                StartingArrows[posAndR[1]][posAndR[0]][posAndR[2]] = currArrow;
            }

            //  middle and end positions
            else {
                posAndR = currArrow.positionsWithDirection.get(j);
                //save reference in affected arrow list
                AffectedArrows[posAndR[1]][posAndR[0]][arrowOfPlayer - 1][posAndR[2]] = currArrow;
            }
        }

    }

    private void createArrowFrom(Arrow arrow, int x, int y, int direction, int arrowOfPlayer){
        Position currPos = new Position(x,y);
        Position StartingPos = currPos.clone();
        Integer newR = direction;
        Integer nextR;
        Integer arrowsPointingToPos;
        char currChar;
        boolean firstStep = true;

        //check if it continiues the arrow
        if (arrow.positionsWithDirection.size() >= 2){
            firstStep = false;
        }

        //go in one direction
        while (true) {
            //does one step
            nextR = doAStep(currPos, newR); //currPos is changed here

            //check for walls
            if (nextR == null) {
                //set end to position it can't go to
                currPos = Position.goInR(currPos, direction);
                break; //if the step wasn't possible
            }
            newR = nextR;

            //check for cykles
            if(currPos.equals(StartingPos)) { //
                //let arrow end one field ahead of the start
                //go one step back
                newR = doAStep(currPos, (direction+4)%8);
                //direction is the opposite of the one we have after going back
                newR = (newR+4)%8;
                break;
            }

            //check what's there
            currChar = getCharAt(currPos);

            //if it's a blank
            if (currChar == '0' || currChar == 'i' || currChar == 'c' ||currChar == 'b'){
                if (!firstStep) {
                    addValidPosition(arrowOfPlayer, currPos);
                    arrow.createsValidMove = true;
                }
                break;
            }

            //if it's me
            if (currChar == '0'+arrowOfPlayer) {
                //overwrite stone can be place on top of first own stone
                if (!firstStep) {
                    addOverwritePosition(arrowOfPlayer, currPos);
                }
                break;
            }

            //if it's another player or 'x'
            arrow.positionsWithDirection.add(new int[]{currPos.x, currPos.y, newR});

            //if it's not the first step an overwrite move can be made
            if (!firstStep) {
                addOverwritePosition(arrowOfPlayer, currPos);
            }

            firstStep = false;
        }

        //add last position after break
        arrow.positionsWithDirection.add(new int[]{currPos.x, currPos.y, newR});
    }


    private void addValidPosition(int arrowOfPlayer, Position currPos) {
        Integer arrowsPointingToPos;
        //add position to valid moves
        //  increase count of arrows poining to this direction
        arrowsPointingToPos = ValidMoves.get(arrowOfPlayer - 1).get(currPos);
        if (arrowsPointingToPos != null) {
            arrowsPointingToPos++;
            //add position and count
            ValidMoves.get(arrowOfPlayer - 1).replace(currPos.clone(), arrowsPointingToPos);
        }
        else {
            arrowsPointingToPos = 1;
            //add position and count
            ValidMoves.get(arrowOfPlayer - 1).put(currPos.clone(), arrowsPointingToPos);
        }

    }

    private void addOverwritePosition(int arrowOfPlayer, Position currPos) {
        Integer arrowsPointingToPos;
        //add position to overwrite moves
        //  increase count of arrows poining to this direction
        arrowsPointingToPos = OverwriteMoves.get(arrowOfPlayer - 1).get(currPos);
        if (arrowsPointingToPos != null) {
            arrowsPointingToPos++;
            //  add position and count
            OverwriteMoves.get(arrowOfPlayer - 1).replace(currPos.clone(),arrowsPointingToPos);
        }
        else {
           arrowsPointingToPos = 1;
            //  add position and count
            OverwriteMoves.get(arrowOfPlayer - 1).put(currPos.clone(),arrowsPointingToPos);
        }
    }

    private void removeValidPosition(int arrowOfPlayer, Position currPos) {
        Integer arrowsPointingToPos;
        //delete valid move
        //  get number of arrows that point to this position
        arrowsPointingToPos = ValidMoves.get(arrowOfPlayer -1).get(currPos);

        if (arrowsPointingToPos == null){
            System.err.println("Valid Position is not in set");
            return;
        }

        //  if it's just this arrow remove the position
        if (arrowsPointingToPos == 1) ValidMoves.get(arrowOfPlayer -1).remove(currPos);
            //if there is another arrow reduce count
        else {
            arrowsPointingToPos--;
            ValidMoves.get(arrowOfPlayer -1).replace(currPos, arrowsPointingToPos);
        }
    }

    private void removeOverwritePosition(int arrowOfPlayer, Position currPos) {
        Integer arrowsPointingToPos;
        //delete ovwerwrite move
        //  get number of arrows that poit to this position
        arrowsPointingToPos = OverwriteMoves.get(arrowOfPlayer -1).get(currPos);

        if (arrowsPointingToPos != null) {
            //  if it's just this arrow remove the position
            if (arrowsPointingToPos == 1) OverwriteMoves.get(arrowOfPlayer - 1).remove(currPos);
                //if there is another arrow reduce count
            else {
                arrowsPointingToPos--;
                OverwriteMoves.get(arrowOfPlayer - 1).replace(currPos, arrowsPointingToPos);
            }
        }
    }

    private ArrayList<Arrow> getAllArrows(){
        ArrayList<Arrow> arrowsInMap = new ArrayList<>();
        Arrow arrow;

        for (int y = 0; y < height; y++){
            for (int x = 0; x < width; x++){
                for (int i = 0; i < 8; i++){
                    if (StartingArrows[y][x][i] != null) {
                        arrow = StartingArrows[y][x][i];
                        arrowsInMap.add(arrow);
                    }
                }
            }
        }
        return arrowsInMap;
    }

    private ArrayList<Arrow> getAllValidArrows(){
        ArrayList<Arrow> arrowsInMap = getAllArrows();
        ArrayList<Arrow> validArrowsInMap = new ArrayList<>();

        for (Arrow arrow : arrowsInMap){
            if (arrow.createsValidMove) validArrowsInMap.add(arrow);
        }
        return  validArrowsInMap;
    }

    private boolean checkForBugs(){
        ArrayList<Arrow> allArrows = getAllArrows();
        boolean correct = true;
        boolean isOneOfThem = false;
        int[] posAndR;

        for (Arrow arrow : allArrows){
            for (int i = 1; i < arrow.positionsWithDirection.size(); i++){
                posAndR = arrow.positionsWithDirection.get(i);
                isOneOfThem = false;
                for (int playerNr = 0; playerNr < anzPlayers; playerNr++) {
                    if (AffectedArrows[posAndR[1]][posAndR[0]][playerNr][posAndR[2]] == arrow) isOneOfThem = true;
                }
                if (!isOneOfThem) {
                    correct = false;
                }
                break;
            }
            if (!correct) break;
        }
        return correct;
    }

    //other

    private void initValidMoveArrays(){
        for (int playerNr = 0; playerNr < anzPlayers; playerNr++) {
            ValidMoves.add(new HashMap<>());
            OverwriteMoves.add(new HashMap<>());
        }

        AffectedArrows = new Arrow[height][width][anzPlayers][8];

        /*for (int y = 0; y < height; y++){
            AffectedArrows[y] = new Arrow[width][anzPlayers][8];
            for (int x = 0; x < width; x++){
                AffectedArrows[y][x] = new Arrow[anzPlayers][8];
                for (int playerNr = 0; playerNr < anzPlayers; playerNr++){
                    AffectedArrows[y][x][playerNr] = new Arrow[8];
                }
            }
        }*/

        StartingArrows = new Arrow[height][width][8];

        /*for (int y = 0; y < height; y++){
            StartingArrows[y] = new Arrow[width][8];
            for (int x = 0; x < width; x++){
                StartingArrows[y][x] = new Arrow[8];
            }
        }

         */
    }

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
                //init
                initStonesPerPlayerSet();
                initValidMoveArrays();
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
            if (st.ttype != greater && st.ttype != less && st.ttype != minus) {
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
            transitions.put(buffer, buffer2);
            transitions.put(buffer2, buffer);

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
