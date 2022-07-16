package src;

import java.io.*;
import java.util.*;

public class StaticMap {
    //main data structure to store the Map Infos
    public char[][] map;
    public char[][] reachableFieldMatrix;

    //Data Structure and needed Variables to store Transitions
    public HashMap<Character,Character> transitions = new HashMap<>();
    private final int[] transitionsBuffer = new int[9];
    private int posInTransitionBuffer = 0;

    //General Map Infos
    public int anzPlayers;
    public int initialOverwriteStones;
    public int initialBombs;
    public int explosionRadius;
    public int height;
    public int width;
    public int countOfReachableFields;

    //private
    private final boolean importedCorrectly;
    private ArrayList<HashSet<Position>> initialStonesPerPlayer = new ArrayList<>();
    private HashSet<Position> initialExpansionFields = new HashSet<>();


    /**
     * This Constructor initializes the Static Map
     * @param mapByteArray The Map in a ByteArray Format
     * @param serverLog Indicates if we use Server Log
     */
    public StaticMap(byte[] mapByteArray, boolean serverLog){
        importedCorrectly = importMap(mapByteArray);
        if (!importedCorrectly) {
            System.err.println("Map didn't import correctly.");
        }
        //countOfReachableFields = (height-2)*(width-2); //approximation
        countOfReachableFields = findReachableFields(serverLog);

    }

    //PUBLIC METHODS

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
     * imports Map after a stream tokenizer was created - which changes if you have a file or a stream
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

        return true;
    }

    /**
     * Returns Infos, Map and Transitions in a formatted String
     */
    public String toString() {
        String mapString = "";
        mapString += "Player count: " + anzPlayers + "\n";
        mapString += "Initial Overwrite Stones per Player: " + initialOverwriteStones;
        mapString += "Initial Bombs per Player: " + initialBombs;
        mapString += "Explosion radius: " + explosionRadius + "\n";
        mapString += "Height: " + height + ", Width: " + width + "\n\n";

        mapString += Arrays.deepToString(map).replaceAll("],","]\n");

        mapString += "\n\n";

        mapString += Transitions.AllToString(transitions);

        return mapString;
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
        if (pos.x == this.width-1){
            if (r == 1 || r == 2 || r == 3) return null;
        }
        //check if step is valid in y direction
        if (pos.y == 0) {
            if (r == 7 || r == 0 || r == 1) return null;
        }
        if (pos.y == this.height-1){
            if (r == 3 || r == 4 || r == 5) return null;
        }

        //do the step
        newPos = Position.goInR(pos, r);

        charAtPos = map[newPos.y][newPos.x];

        //check if there's a wall
        if (charAtPos == '-') return null;

        //check if there is a transition
        if (charAtPos == 't') {
            //check if the direction matches the current one
            transitionLookup = Transitions.saveInChar(pos.x,pos.y,r); //pos is the old position
            transitionEnd = transitions.get(transitionLookup);
            if (transitionEnd == null) return null; //if there isn't an entry

            //go through the transition
            newPos.x = Transitions.getX(transitionEnd);
            newPos.y= Transitions.getY(transitionEnd);
            newR = Transitions.getR(transitionEnd);
            newR = (newR+4)%8; //flips direction because transition came out of that direction, so you go through the other way

            //if on the other side is a -
            if (map[newPos.y][newPos.x] == '-') return null; //only relevant in bomb phase
        }

        //sets the position to the new One (call by reference)
        pos.x = newPos.x;
        pos.y = newPos.y;
        return newR;
    }

    //GETTER

    /**
     * Returns if the Map was Imported Correctly or not.
     * @return Returns true if the Map was imported Correctly and false otherwise
     */
    public boolean wasImportedCorrectly() {
        return importedCorrectly;
    }

    /**
     * This Function returns the Initial Stones Per Player in an ArrayList of Hashsets of Positions.
     * @return Returns an Arraylist of one Hashset per Player with their Initial Stones.
     */
    public ArrayList<HashSet<Position>> getInitialStonesPerPlayer(){
        ArrayList<HashSet<Position>> result = initialStonesPerPlayer;
        initialStonesPerPlayer = null;
        return result;
    }

    /**
     * This Function returns the Initial ExpansionFields in a HashSet
     * @return Returns a Hashset with the Initial ExpansionFields
     */
    public HashSet<Position> getInitialExpansionFields(){
        HashSet<Position> result = initialExpansionFields;
        initialExpansionFields = null;
        return result;
    }

    /**
     * Checks for a Transition given the Position and Rotation.
     * @param pos The Current Position
     * @param rotation The Rotation in which we are going.
     * @return Returns true if a Transition starts from the given Position in the given Direction
     */
    public boolean checkForTransition(Position pos, int rotation){
        Character transitionStart = Transitions.saveInChar(pos.x, pos.y, rotation);
        Character transitionEnd;

        transitionEnd = transitions.get(transitionStart);

        return transitionEnd != null;
    }

    //IMPORT FUNCTIONS

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
                initialOverwriteStones = currentNumber;
                break;
            case 2:
                initialBombs = currentNumber;
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
                initializeMap(); //TODO check if that's needed
                initStonesPerPlayerSet();
                break;
        }
        return true;
    }

    private void initializeMap(){
        for (int y = 0; y < height; y++){
            for (int x = 0; x < width; x++){
                map[y][x] = '-';
            }
        }
    }

    private boolean handleMap(StreamTokenizer st, int tokenCounter) {
        char charAtPos;
        char charToImport;

        //calculates x and y coordinates out of token counter, width and height
        int x = (tokenCounter-6)%(width-2) + 1; //-2 and +1 is index shift
        int y = (tokenCounter-6)/(width-2) + 1; //-2 and +1 is index shift

        //save char in map
        switch (st.ttype){

            //if it's a char
            case StreamTokenizer.TT_WORD: {
                charToImport = st.sval.charAt(0);
                map[y][x] = charToImport;
                if (charToImport == 'x') initialExpansionFields.add(new Position(x, y));
                break;
            }

            //if it's a number
            case StreamTokenizer.TT_NUMBER: {
                int currentNumber = ((Double) st.nval).intValue();
                if (currentNumber > anzPlayers) {
                    System.err.println("No values over " + anzPlayers + " allowed!");
                    return false;
                }
                charAtPos = (char) ('0' + currentNumber);
                map[y][x] = charAtPos;

                //if it's a player add the position his stones
                if (currentNumber != 0) initialStonesPerPlayer.get(currentNumber - 1).add(new Position(x, y));
                break;
            }

            //if it's a minus
            case '-': {
                map[y][x] =  '-';
                break;
            }

            default:
                System.err.println("Read char that couldn't be handled");
                return false;
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
            //Add transition to hash Map
            transitions.put(buffer, buffer2);
            transitions.put(buffer2, buffer);

            //Add Transitions to Map
            //first end
            Position endOne = new Position(transitionsBuffer[0], transitionsBuffer[1]);
            endOne = Position.goInR(endOne, transitionsBuffer[2]);
            map[endOne.y][endOne.x] = 't';

            //second end
            Position endTwo = new Position(transitionsBuffer[6], transitionsBuffer[7]);
            endTwo = Position.goInR(endTwo, transitionsBuffer[8]);
            map[endTwo.y][endTwo.x] = 't';

            posInTransitionBuffer = 0;
        }
        else
        {
            posInTransitionBuffer++;
        }
        return true;
    }

    private void initStonesPerPlayerSet(){
        for (int playerNr = 0; playerNr < anzPlayers; playerNr++) {
            initialStonesPerPlayer.add(new HashSet<>());
        }
    }

    //Helper

    /**
     * This function Calculates the Amount of Reachable Fields in the Map.
     * @param serverLog Indicates whether we compare to the Server or not.
     * @return The Number of ReachableFields
     */

    private int findReachableFields(boolean serverLog)
    {
        char[][] map = new char[height][width];
        for (int i = 0; i < height; i++){
            map[i] = this.map[i].clone();
        }
        Integer newR;
        PriorityQueue<Integer> Queue = new PriorityQueue<>();
        int countOfReachableFields = 0;
        //Fill Queue with Starting Positions

        //TODO: check for initial overwrite stones and bonusFields
        for (Position p : initialExpansionFields) {
            map[p.y][p.x] = 'R';
            Queue.add(p.x*100+p.y);
        }


        for(int i = 0;i<anzPlayers;i++)
        {
            for(Position p : initialStonesPerPlayer.get(i))
            {
                map[p.y][p.x] = 'R';
                Queue.add(p.x*100+p.y);
            }
        }

        Integer location;

        countOfReachableFields += Queue.size();

        while((location = Queue.poll() )!= null)
        {

            for(int r = 0; r<8;r++)
            {
                char value1;
                char value2;
                Position startPos = new Position(location / 100,location % 100);
                Position pos1 = startPos.clone();
                Position pos2 = startPos.clone();

                //1. termination condition
                //  get pos and char at pos
                Integer wall2 = doAStep(pos2, (r+4)%8);
                value2 = map[pos2.y][pos2.x];
                if (wall2 == null || value2 != 'R')
                {
                    continue;
                }


                //2. termination condition
                newR = r;
                while (true) {
                    newR = doAStep(pos1, newR);

                    if (newR == null || startPos.equals(pos1)) {
                        break;
                    }

                    value1 = map[pos1.y][pos1.x];
                    if (value1 == '0' || value1 == 'b' || value1 == 'i' || value1 == 'c') {
                        countOfReachableFields++;
                        map[pos1.y][pos1.x] = 'R';
                        Queue.add(pos1.x * 100 + pos1.y);
                        break;
                    }
                }
            }
        }

        reachableFieldMatrix = map;

        if(!serverLog) {
            for (int y = 0; y < reachableFieldMatrix.length; y++) {
                for (int x = 0; x < reachableFieldMatrix[0].length; x++) {
                    if (reachableFieldMatrix[y][x] != '-' && reachableFieldMatrix[y][x] != 'R' && reachableFieldMatrix[y][x] != 't') {
                        this.map[y][x] = '-';
                    }
                }
            }
        }

        return countOfReachableFields;
    }

}
