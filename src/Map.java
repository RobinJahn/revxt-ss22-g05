package src;

import java.util.*;

public class Map{

    //this class contains all the color codes
    private static class Colors {
        final static String ANSI_RESET = "\u001B[0m";
        final static String ANSI_BLACK = "\u001B[30m";
        final static String ANSI_RED = "\u001B[31m";
        final static String ANSI_GREEN = "\u001B[32m";
        final static String ANSI_YELLOW = "\u001B[33m";
        final static String ANSI_BLUE = "\u001B[34m";
        final static String ANSI_PURPLE = "\u001B[35m";
        final static String ANSI_BRIGHT_CYAN = "\u001B[96m";
        final static String ANSI_WHITE = "\u001B[37m";
        final static String ANSI_BRIGHT_YELLOW = "\u001B[93m";
        final static String ANSI_BRIGHT_GREEN = "\u001B[92m";

        final static String ANSI_BLACK_BACKGROUND = "\u001B[40m";
        final static String ANSI_RED_BACKGROUND = "\u001B[41m";
        final static String ANSI_GREEN_BACKGROUND = "\u001B[42m";
        final static String ANSI_YELLOW_BACKGROUND = "\u001B[43m";
        final static String ANSI_BLUE_BACKGROUND = "\u001B[44m";
        final static String ANSI_PURPLE_BACKGROUND = "\u001B[45m";
        final static String ANSI_CYAN_BACKGROUND = "\u001B[46m";
        final static String ANSI_WHITE_BACKGROUND = "\u001B[47m";
    }

    //variables that controll the arrows
    public static boolean useArrows = false; //needs to stay false because it can be set by parameter
    final static private boolean checkIfAllArrowsAreCorrect = false;

    //static Map
    private final StaticMap staticMap;

    //map
    private final char[][] map;

    //General Map Infos
    private final int[] overwriteStonesPerPlayer;
    private final int[] bombsPerPlayer;

    //Extended Map Info
    private int currentlyPlaying = 1;
    private final boolean[] disqualifiedPlayers; //TODO: maybe this isn't needed

    //Stones on the board
    private final ArrayList<HashSet<Position>> stonesPerPlayer;
    private final HashSet<Position> expansionFields;

    //Valid Moves Variables
    private ArrayList<HashMap<Position, Integer>> ValidMoves = null;
    private ArrayList<HashMap<Position, Integer>> OverwriteMoves = null;

    //Arrow Data Structures
    private Arrow[][][][] AffectedArrows;
    private Arrow[][][] StartingArrows; //TODO: could be 2 elements shorter on each side


    /**
     * Constructor of the Map class.
     * Creates a Map-object by reading the static map
     * @param staticMap the static map the map class bases on
     */
    public Map(StaticMap staticMap){
        //static Map
        this.staticMap = staticMap;

        //map
        map = new char[staticMap.height][staticMap.width];
        for (int i = 0; i < staticMap.height; i++){
            map[i] = staticMap.map[i].clone();
        }

        //General Map info + disqualified Players
        overwriteStonesPerPlayer = new int[staticMap.anzPlayers];
        bombsPerPlayer = new int[staticMap.anzPlayers];
        disqualifiedPlayers = new boolean[staticMap.anzPlayers];

        for (int playerNr = 0; playerNr < staticMap.anzPlayers; playerNr++){
            overwriteStonesPerPlayer[playerNr] = staticMap.initialOverwriteStones;
            bombsPerPlayer[playerNr] = staticMap.initialBombs;
            disqualifiedPlayers[playerNr] = false;
        }

        //Extended Map info

        //Stones on the Board
        stonesPerPlayer = staticMap.getInitialStonesPerPlayer();
        expansionFields = staticMap.getInitialExpansionFields();

        //Valid Moves
        ValidMoves = new ArrayList<>(staticMap.anzPlayers);
        OverwriteMoves = new ArrayList<>(staticMap.anzPlayers);
        initValidMoveArrays();

        //Arrow Data Structures
        AffectedArrows = new Arrow[staticMap.height][staticMap.width][staticMap.anzPlayers][8];
        StartingArrows = new Arrow[staticMap.height][staticMap.width][8];

        //after import create first few arrows
        for (int playerNr = 1; playerNr <= staticMap.anzPlayers; playerNr++) {
            for (Position pos : stonesPerPlayer.get(playerNr-1)){
                firstCreation(pos.x, pos.y, playerNr);
            }
        }
    }

    /**
     * Copy constructor. Copys the given Map.
     * Appart from the Arrow arrays it's a deep copy
     * @param mapToCopy map to copy
     * @param phaseOne phase the map is in
     */
    public Map(Map mapToCopy, boolean phaseOne){
        //static Map
        staticMap = mapToCopy.staticMap;

        //map
        map = new char[staticMap.height][staticMap.width];
        for (int i = 0; i < staticMap.height; i++){
            map[i] = mapToCopy.map[i].clone();
        }

        //General Map info
        overwriteStonesPerPlayer = mapToCopy.overwriteStonesPerPlayer.clone(); //shallow clone because we need a new Array
        bombsPerPlayer = mapToCopy.bombsPerPlayer.clone();

        //Extended Map info
        currentlyPlaying = mapToCopy.currentlyPlaying;
        disqualifiedPlayers = mapToCopy.disqualifiedPlayers.clone();

        //Stones per player
        stonesPerPlayer = new ArrayList<>(staticMap.anzPlayers);
        initStonesPerPlayerSet();

        for (int playerNr = 0; playerNr < staticMap.anzPlayers; playerNr++){
            stonesPerPlayer.set(playerNr, (HashSet<Position>) mapToCopy.stonesPerPlayer.get(playerNr).clone());
        }
        expansionFields = (HashSet<Position>) mapToCopy.expansionFields.clone();

        //  only needed in phase one because in phase two the arrows aren't needed
        if (phaseOne && useArrows) {

            //Valid Moves Variables
            ValidMoves = new ArrayList<>(staticMap.anzPlayers);
            OverwriteMoves = new ArrayList<>(staticMap.anzPlayers);
            //initValidMoveArrays();

            for (int playerNr = 0; playerNr < staticMap.anzPlayers; playerNr++) {
                ValidMoves.add( (HashMap<Position, Integer>) mapToCopy.ValidMoves.get(playerNr).clone() );
                OverwriteMoves.add( (HashMap<Position, Integer>) mapToCopy.OverwriteMoves.get(playerNr).clone() );
            }

            //Arrow Data structures
            AffectedArrows = new Arrow[staticMap.height][staticMap.width][staticMap.anzPlayers][8];
            StartingArrows = new Arrow[staticMap.height][staticMap.width][8];

            //Copy starting Arrows
            for (int y = 0; y < staticMap.height; y++) {
                for (int x = 0; x < staticMap.width; x++) {
                    StartingArrows[y][x] = mapToCopy.StartingArrows[y][x].clone();
                }
            }

            //Copy Affected Arrows
            for (int y = 0; y < staticMap.height; y++) {
                for (int x = 0; x < staticMap.width; x++) {
                    for (int playerNr = 0; playerNr < staticMap.anzPlayers; playerNr++) {
                        AffectedArrows[y][x][playerNr] = mapToCopy.AffectedArrows[y][x][playerNr].clone();
                    }
                }
            }
        }
    }

    // TO STRING METHODS

    /**
     * Prints Infos, Map and Transitions in a formatted way to the outputs
     */
    public String toString() {
        return this.toString(null,true,false);
    }

    /**
     * Retturns a map in a style that can be compared to the server output
     * @param everyPossibleMove List of int[] (x, y, info) that specifys the possible positions a stone can be placed
     * @return formatted string
     */
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

        for (int y = 1; y < staticMap.height-1; y++){
            for (int x = 1; x < staticMap.width-1; x++){
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

    /**
     * Advanced toString() Method. It returns the map in a formatted String that can be changed by the parameters.
     * @param everyPossibleMove List of int[] (x, y, info) that specifys the possible positions a stone can be placed
     * @param showTransitions boolean if the transitions should be appended.
     * @param useColors boolean if the returnd string should have color inforamtions in it to print it.
     * @return formatted string
     */
    public String toString(List<int[]> everyPossibleMove, boolean showTransitions, boolean useColors){

        StringBuilder mapString = new StringBuilder();
        String bufferString;

        char currChar;

        ArrayList<Position> possibleMoves = null;
        if (everyPossibleMove != null) {
            possibleMoves = new ArrayList<>();
            for (int[] posAndInfo : everyPossibleMove) {
                possibleMoves.add(new Position(posAndInfo[0], posAndInfo[1]));
            }
        }

        appendMapInfosForStringBuilder(mapString);

        // print numbers for columns
        for (int i = -1 ; i < staticMap.width; i++) mapString.append(i).append("\t");
        mapString.append("\n");

        //print map
        for (int y = 0; y < staticMap.height; y++){

            //print row number
            mapString.append(y).append("\t");

            //print row of map
            for (int x = 0; x < staticMap.width; x++){
                currChar = map[y][x];
                bufferString = "";
                //get color for the char at the position
                if (useColors) {
                    switch (currChar){
                        case '1':
                            bufferString += Colors.ANSI_RED;
                            break;
                        case '2':
                            bufferString += Colors.ANSI_BLUE;
                            break;
                        case '3':
                            bufferString += Colors.ANSI_GREEN;
                            break;
                        case '4':
                            bufferString += Colors.ANSI_YELLOW;
                            break;
                        case '5':
                            bufferString += Colors.ANSI_BRIGHT_CYAN;
                            break;
                        case '6':
                            bufferString += Colors.ANSI_PURPLE;
                            break;
                        case '7':
                            bufferString += Colors.ANSI_BRIGHT_YELLOW;
                            break;
                        case '8':
                            bufferString += Colors.ANSI_BRIGHT_GREEN;
                            break;
                        case 'b':
                            bufferString += Colors.ANSI_BLACK;
                            bufferString += Colors.ANSI_GREEN_BACKGROUND;
                            break;
                        case 'i':
                            bufferString += Colors.ANSI_BLACK;
                            bufferString += Colors.ANSI_PURPLE_BACKGROUND;
                            break;
                        case 'c':
                            bufferString += Colors.ANSI_BLACK;
                            bufferString += Colors.ANSI_CYAN_BACKGROUND;
                            break;
                        case 'x':
                            bufferString += Colors.ANSI_BLACK;
                            bufferString += Colors.ANSI_WHITE_BACKGROUND;
                            break;
                    }
                }

                //add char itself
                bufferString += currChar;

                //reset color
                if (useColors) bufferString += Colors.ANSI_RESET;

                //add ' if a move can be made to this position
                if (possibleMoves != null && possibleMoves.contains(new Position(x,y))) bufferString += "'";

                //add display of position to map String
                mapString.append(bufferString).append("\t");
            }
            mapString.append("\n");
        }

        if (showTransitions) {
            mapString.append('\n');
            mapString.append(Transitions.AllToString(staticMap.transitions));
        }

        return mapString.toString();
    }

    /**
     * This Method returns the map in a formatted String. It shows all the possible routes you could go from the specified Positions by the positionsToMark List
     * @param everyPossibleMove List of int[] (x, y, info) that specifys the possible positions a stone can be placed
     * @param showTransitions boolean if the transitions should be appended.
     * @param positionsToMark List of Position objects that specify wich positions should be marked.
     * @return formatted string
     */
    public String toString(List<int[]> everyPossibleMove, boolean showTransitions, ArrayList<Position> positionsToMark){
        boolean backGroundSet;
        int spaces = 6;
        int sizeOfBufferString;
        StringBuilder mapString = new StringBuilder();
        String bufferString;

        char currChar;

        ArrayList<Position> possibleMoves = null;
        if (everyPossibleMove != null) {
            possibleMoves = new ArrayList<>();
            for (int[] posAndInfo : everyPossibleMove) {
                possibleMoves.add(new Position(posAndInfo[0], posAndInfo[1]));
            }
        }

        Position currPos;
        Integer newR;
        PositionAndInfo posAndInfo;
        LinkedHashMap<PositionAndInfo, Integer> reachablePositions = new LinkedHashMap<>();
        int stepCounter;
        for (Position pos : positionsToMark) {
            for (int direction = 0; direction < 8; direction++){
                stepCounter = 0;
                currPos = pos.clone();
                newR = direction;
                while (true) {
                    newR = doAStep(currPos, newR);
                    if (newR == null || currPos.equals(pos)) break;
                    posAndInfo = new PositionAndInfo(currPos.x, currPos.y, direction);
                    reachablePositions.put(posAndInfo, stepCounter);
                    stepCounter++;
                }
            }
        }


        appendMapInfosForStringBuilder(mapString);

        // print numbers for columns
        for (int i = -1 ; i < staticMap.width; i++) {
            mapString.append(i);
            //add spaces
            mapString.append(" ".repeat(spaces - Integer.toString(i).length() + 1));
        }
        mapString.append("\n");

        //print map
        for (int y = 0; y < staticMap.height; y++){

            //print row number
            mapString.append(y);
            //add spaces
            mapString.append(" ".repeat(spaces - Integer.toString(y).length() + 1));

            //print row of map
            for (int x = 0; x < staticMap.width; x++){
                currChar = map[y][x];
                bufferString = "";
                sizeOfBufferString = 0;

                backGroundSet = false;

                for (int direction = 0; direction < 8; direction++) {
                    posAndInfo = new PositionAndInfo(x, y, direction);
                    if (reachablePositions.containsKey(posAndInfo)) {
                        switch (direction){
                            case 0:
                                bufferString += Colors.ANSI_RED_BACKGROUND;
                                break;
                            case 1:
                                bufferString += Colors.ANSI_BLUE_BACKGROUND;
                                break;
                            case 2:
                                bufferString += Colors.ANSI_CYAN_BACKGROUND;
                                break;
                            case 3:
                                bufferString += Colors.ANSI_GREEN_BACKGROUND;
                                break;
                            case 4:
                                bufferString += Colors.ANSI_PURPLE_BACKGROUND;
                                break;
                            case 5:
                                bufferString += Colors.ANSI_WHITE_BACKGROUND;
                                break;
                            case 6:
                                bufferString += Colors.ANSI_YELLOW_BACKGROUND;
                                break;
                            case 7:
                                bufferString += Colors.ANSI_BLACK_BACKGROUND;
                                break;
                        }
                        bufferString += reachablePositions.get(posAndInfo);
                        bufferString += Colors.ANSI_RESET;

                        sizeOfBufferString += reachablePositions.get(posAndInfo).toString().length();
                        backGroundSet = true;
                    }
                }

                //get color for the char at the position

                switch (currChar){
                    case '1':
                        bufferString += Colors.ANSI_RED;
                        break;
                    case '2':
                        bufferString += Colors.ANSI_BLUE;
                        break;
                    case '3':
                        bufferString += Colors.ANSI_GREEN;
                        break;
                    case '4':
                        bufferString += Colors.ANSI_YELLOW;
                        break;
                    case '5':
                        bufferString += Colors.ANSI_BRIGHT_CYAN;
                        break;
                    case '6':
                        bufferString += Colors.ANSI_PURPLE;
                        break;
                    case '7':
                        bufferString += Colors.ANSI_BRIGHT_YELLOW;
                        break;
                    case '8':
                        bufferString += Colors.ANSI_BRIGHT_GREEN;
                        break;
                    case 'b':
                        bufferString += Colors.ANSI_BLACK;
                        bufferString += Colors.ANSI_GREEN_BACKGROUND;
                        break;
                    case 'i':
                        bufferString += Colors.ANSI_BLACK;
                        bufferString += Colors.ANSI_PURPLE_BACKGROUND;
                        break;
                    case 'c':
                        bufferString += Colors.ANSI_BLACK;
                        bufferString += Colors.ANSI_CYAN_BACKGROUND;
                        break;
                    case 'x':
                        bufferString += Colors.ANSI_BLACK;
                        bufferString += Colors.ANSI_WHITE_BACKGROUND;
                        break;
                }


                //add char itself
                bufferString += currChar;
                sizeOfBufferString++;

                //reset color
                bufferString += Colors.ANSI_RESET;

                //add ' if a move can be made to this position
                if (possibleMoves != null && possibleMoves.contains(new Position(x,y))) {
                    bufferString += "'";
                    sizeOfBufferString++;
                }

                //add display of position to map String
                mapString.append(bufferString);

                //add spaces
                mapString.append(" ".repeat(Math.max(0, spaces - sizeOfBufferString + 1)));
            }
            mapString.append("\n");
        }

        if (showTransitions) {
            mapString.append('\n');
            mapString.append(Transitions.AllToString(staticMap.transitions));
        }

        return mapString.toString();
    }

    /**
     * This method is used when the Client compares it's output to the one of the server.
     * It formats the output of the Server in a way that it can be compared to the Client's ouput.
     * @param mapAsString The output of the Server in a string
     * @param useColors Boolean if colors should be used
     * @return formatted String
     */
    public static String toString(String mapAsString, boolean useColors){
        StringBuilder mapString = new StringBuilder();
        String bufferString;
        String[] mapLines = mapAsString.split("\n");
        char currChar;
        int widthOfServer = mapLines[0].replaceAll(" ","").replaceAll("'", "").length();

        // print numbers for columns
        for (int i = -1 ; i < widthOfServer+2; i++) mapString.append(i).append("\t");
        mapString.append("\n");

        //print map
        for (int y = -1; y < mapLines.length+1; y++){

            //print row number
            mapString.append(y+1).append("\t");

            if (y == -1 || y == mapLines.length){
                mapString.append("-\t".repeat(widthOfServer+2)).append("\n");
                continue;
            }
            //print row of map
            for (int x = -1; x < mapLines[y].length()+1; x++) {

                if (x == -1 || x == mapLines[y].length()){
                    mapString.append("-").append("\t");
                    continue;
                }

                currChar = mapLines[y].charAt(x);

                if (currChar == ' ') continue;

                bufferString = "";
                //get color for the char at the position
                if (useColors) {
                    switch (currChar) {
                        case '1':
                            bufferString += Colors.ANSI_RED;
                            break;
                        case '2':
                            bufferString += Colors.ANSI_BLUE;
                            break;
                        case '3':
                            bufferString += Colors.ANSI_GREEN;
                            break;
                        case '4':
                            bufferString += Colors.ANSI_YELLOW;
                            break;
                        case '5':
                            bufferString += Colors.ANSI_BRIGHT_CYAN;
                            break;
                        case '6':
                            bufferString += Colors.ANSI_PURPLE;
                            break;
                        case '7':
                            bufferString += Colors.ANSI_BRIGHT_YELLOW;
                            break;
                        case '8':
                            bufferString += Colors.ANSI_BRIGHT_GREEN;
                            break;
                        case 'b':
                            bufferString += Colors.ANSI_BLACK;
                            bufferString += Colors.ANSI_GREEN_BACKGROUND;
                            break;
                        case 'i':
                            bufferString += Colors.ANSI_BLACK;
                            bufferString += Colors.ANSI_PURPLE_BACKGROUND;
                            break;
                        case 'c':
                            bufferString += Colors.ANSI_BLACK;
                            bufferString += Colors.ANSI_CYAN_BACKGROUND;
                            break;
                        case 'x':
                            bufferString += Colors.ANSI_BLACK;
                            bufferString += Colors.ANSI_WHITE_BACKGROUND;
                            break;
                    }
                }

                //add char itself
                bufferString += currChar;

                //reset color
                if (useColors) bufferString += Colors.ANSI_RESET;

                //add ' if a move can be made to this position
                if (x + 1 < mapLines[y].length() && mapLines[y].charAt(x + 1) == '\'') {
                    bufferString += "'";
                    x++;
                }

                //add display of position to map String
                mapString.append(bufferString).append("\t");
            }
            mapString.append("\n");
        }

        return mapString.toString();
    }

    private void appendMapInfosForStringBuilder(StringBuilder mapString) {
        mapString.append("Player count: ").append(staticMap.anzPlayers).append("   ");
        mapString.append("Explosion radius: ").append(staticMap.explosionRadius).append("   ");
        mapString.append("Height: ").append(staticMap.height).append("   ");
        mapString.append("Width: ").append(staticMap.width).append("\n");

        mapString.append("currently playing: ").append(currentlyPlaying).append("\n");

        mapString.append("Overwrite Stones per Player: ");
        for (int i = 1; i <= staticMap.anzPlayers; i++) {
            mapString.append(overwriteStonesPerPlayer[i - 1]).append(", ");
        }
        mapString.append("\n");

        mapString.append("Bombs per Player: ");
        for (int i = 1; i <= staticMap.anzPlayers; i++) {
            mapString.append(bombsPerPlayer[i - 1]).append(", ");
        }
        mapString.append("\n");
    }

    // GETTER ----------------------------------------------------------------------------------------------------------

    /**
     * Gets the Character at the given x and y position in the Map and returns it. If it is out of boundaries it returns '-'
     * @param x x coordinate
     * @param y y coordinate
     * @return the character at the position
     */
    public char getCharAt(int x, int y){
        //check boundaries
        if (x >= staticMap.width || y >= staticMap.height || x < 0 || y < 0) return '-';
        //return value
        return map[y][x];
    }

    /**
     * Gets the Character at the given Position in the Map and returns it. If it is out of boundaries it returns '-'
     * @param pos position to specify coordinates
     * @return the character at the position
     */
    public char getCharAt(Position pos){
        //check boundaries
        if (pos.x >= staticMap.width || pos.y >= staticMap.height || pos.x < 0 || pos.y < 0) return '-';
        //return value
        return map[pos.y][pos.x];
    }

    /**
     * @return returns the ammount of players that play on this map
     */
    public int getAnzPlayers() {
        return staticMap.anzPlayers;
    }

    /**
     * @return returns the explosion radius
     */
    public int getExplosionRadius() {
        return staticMap.explosionRadius;
    }

    /**
     * @return returns the height of the map
     */
    public int getHeight() {
        return staticMap.height;
    }

    /**
     * @return returns the width of the map
     */
    public int getWidth() {
        return staticMap.width;
    }

    /**
     * Checks if there's a transition when you would go one step in specified rotation
     * @param pos position to go from
     * @param rotation rotation to go in
     * @return boolean when there is a transition
     */
    public boolean checkForTransition(Position pos, int rotation){
        return staticMap.checkForTransition(pos, rotation);
    }

    /**
     * @return Returns the number of the player, that is currently playing
     */
    public int getCurrentlyPlayingI() {
        return currentlyPlaying;
    }

    /**
     * @return Returns the number as a char of the player, that is currently playing
     */
    public char getCurrentlyPlayingC() {
        return (char) ('0'+currentlyPlaying);
    }

    /**
     * returns the ammount of overwrite stones the specified player has
     * @param playerId Number of the palyer
     * @return ammount of overwrite stones
     */
    public int getOverwriteStonesForPlayer(int playerId) {
        return overwriteStonesPerPlayer[playerId-1];
    }

    /**
     * returns the ammount of bombs the specified player has
     * @param playerId Number of the palyer
     * @return ammount of bombs
     */
    public int getBombsForPlayer(int playerId) {
        return bombsPerPlayer[playerId-1];
    }

    /**
     * Returns all the Stones one player has.
     * @param playerNr Player to get the stones from
     * @return List of Positions the player has
     */
    public ArrayList<Position> getStonesOfPlayer(int playerNr){
        if (playerNr < 1 || playerNr > staticMap.anzPlayers) {
            System.err.println("Player with that number isn't in the Game");
            return null;
        }

        ArrayList<Position> result = new ArrayList<>( stonesPerPlayer.get(playerNr-1) );

        for (int i = 0; i < result.size(); i++){
            result.set(i, result.get(i).clone());
        }

        return result;
    }

    /**
     * returns the ammount of stones one player has
     * @param playerNr The player to get the ammount of stones from
     * @return The ammount
     */
    public int getCountOfStonesOfPlayer(int playerNr){
        if (playerNr > 0 && playerNr <= stonesPerPlayer.size()) {
            return stonesPerPlayer.get(playerNr - 1).size();
        }
        else {
            return 0;
        }
    }

    /**
     * @return Retunrs a list of all expansion fields that are currently on the map.
     */
    public ArrayList<Position> getExpansionFields(){
        ArrayList<Position> result = new ArrayList<>( expansionFields );

        for (int i = 0; i < result.size(); i++){
            result.set(i, result.get(i).clone());
        }

        return result;
    }

    /**
     * returns all the valid Moves the player currently playing can make. This is done by using arrows. (Does not work right 100%)
     * @param heuristic heurisitc by wich it gets evaluated if an overwrite move should be made.
     * @return List of all the possible Positions the player can move to
     */
    public ArrayList<int[]> getValidMovesByArrows(Heuristic heuristic){
        return getValidMovesByArrows(currentlyPlaying, heuristic);
    }

    private ArrayList<int[]> getValidMovesByArrows(int playerId, Heuristic heuristic){
        ArrayList<int[]> resultList = new ArrayList<>();
        ArrayList<int[]> overwriteMoves = new ArrayList<>();
        char currChar;
        int bombOrOverwrite;


        //get Valid Moves
        for (Position pos : ValidMoves.get(playerId - 1).keySet()) {
            currChar = getCharAt(pos);
            switch (currChar) {
                case '0':
                case 'i':
                    resultList.add(new int[]{pos.x, pos.y, 0});
                    break;
                case 'b':
                    bombOrOverwrite = (heuristic == null) ? 21 : heuristic.selectBombOrOverwrite();
                    resultList.add(new int[]{pos.x, pos.y, bombOrOverwrite});
                    break;
                case 'c':
                    for (int playerNr = 1; playerNr <= staticMap.anzPlayers; playerNr++) {
                        resultList.add(new int[]{pos.x, pos.y, playerNr});
                    }
                    break;
                default:
                    System.err.println("Valid Move was on another field: " + pos + "=" + currChar);
                    break;
            }
        }

        //get Overwrite Moves
        if (getOverwriteStonesForPlayer(playerId) > 0) {
            for (Position pos : OverwriteMoves.get(playerId - 1).keySet()) {
                if (heuristic != null && heuristic.evaluateOverwriteMove(pos))
                    resultList.add(new int[]{pos.x, pos.y, 0});
                else
                    overwriteMoves.add(new int[]{pos.x, pos.y, 0});
            }

            for (Position pos : expansionFields) {
                if (heuristic != null && heuristic.evaluateOverwriteMove(pos))
                    resultList.add(new int[]{pos.x, pos.y, 0});
                else
                    overwriteMoves.add(new int[]{pos.x, pos.y, 0});
            }
        }

        if (resultList.isEmpty()) return overwriteMoves;

        return resultList;
    }

    /**
     * @return returns an array of booleans that specifies wich players are disqualified.
     */
    public boolean[] getDisqualifiedPlayers()
    {
        return disqualifiedPlayers;
    }

    /**
     * gets if the specified player is disqualified
     * @param PlayerNum player number
     * @return true when the player is disqualified and false otherwise
     */
    public boolean getDisqualifiedPlayer(int PlayerNum)
    {
        return disqualifiedPlayers[PlayerNum-1];
    }

    /**
     * @return Returns the ammount of fields that can be reached in this map
     */
    public int getCountOfReachableFields(){
        return staticMap.countOfReachableFields;
    }

    /**
     * @return Returns the ammount of bonus-fields that can be reached in this map
     */
    public int getCountOfReachableBonusFields(Map map)
    {
        int erg = 0;
        for(int y = 0;y< staticMap.height;y++)
        {
            for(int x = 0; x <staticMap.width;x++)
            {
                if(map.getCharAt(x,y) == 'b' && staticMap.reachableFieldMatrix[y][x] == 'R')
                {
                    erg++;
                }
            }
        }
        return erg;
    }

    /**
     * Returns a list of positions where the bonus fields are that can be reached
     * @param map Map this gets checked on
     * @return List of Positions
     */
    public ArrayList<Position> getReachableBonusFields(Map map)
    {
        ArrayList<Position> Fields = new ArrayList<>();
        for(int y = 0;y< staticMap.height;y++)
        {
            for(int x = 0; x <staticMap.width;x++)
            {
                if(map.getCharAt(x,y) == 'b' && staticMap.reachableFieldMatrix[y][x] == 'R')
                {
                    Position pos = new Position(x,y);
                    Fields.add(pos);
                }
            }
        }
        return Fields;
    }

    /**
     * @return returns the percentage by wich the current map is filled
     */
    public double getFillPercentage()
    {
        int occupiedFields = 0;
        for (int playerNR = 1; playerNR <= staticMap.anzPlayers; playerNR++){
            occupiedFields += getCountOfStonesOfPlayer(playerNR);
        }
        //occupiedFields += expansionFields.size(); don't think that's relevant

        return occupiedFields / (double) staticMap.countOfReachableFields;
    }

    /**
     * Prints the map in the ouput in wich you can see wich fields are reachable and wich not
     */
    public void printReachableFields(){
        System.out.println(Arrays.deepToString(staticMap.reachableFieldMatrix).replace("],", "\n"));
    }

    /**
     * @return returns if the map is in a terminal State - in the First Phase. Used by MCTS
     */
    public boolean isTerminal(){
        for (int playerNR = 1; playerNR<=staticMap.anzPlayers; playerNR++){
            try {
                //if getValidMovesByColor is called it returns when it found one move. If getValidMovesByArrow is called it returns the list.
                if (!Map.getValidMoves(this, true, false, false, 0, null).isEmpty()) {
                    return false;
                }
            } catch (ExceptionWithMove e) {
                return false;
            }
            nextPlayer();
        }
        return true;
    }

    /**
     * @return returns if the map is in a terminal State - in the Second Phase. Used by MCTS
     */
    public boolean isTerminalSecondPhase(){
        for (int playerNR = 1; playerNR<=staticMap.anzPlayers; playerNR++){
            if (getBombsForPlayer(getCurrentlyPlayingI()) > 0) return false;
            nextPlayer();
        }
        return true;
    }

    /**
     * @return Returns a random Move that can be made
     */
    public int[] getRandomMove() {
        //-2 because we have a line of walls around all maps and -1 because we don't want to have the (widht-2) itself in the values
        int x = 1 + (int) (Math.random() * (this.getWidth() - 3));
        int y = 1 + (int) (Math.random() * (this.getHeight() - 3));
        ArrayList<Integer> directions = new ArrayList<>() {
            {
                add(0);
                add(1);
                add(2);
                add(3);
                add(4);
                add(5);
                add(6);
                add(7);
            }
        };
        Position randomMove = new Position(x, y);
        int[] result;
        char charAtPos = getCharAt(randomMove);
        int length = 1;
        boolean moveFound = false;
        int direction = 0;
        int randomVal;

        if (charAtPos != '-' && charAtPos != 't') {
            moveFound = checkIfMoveIsPossible(randomMove, directions, this);
        }

        while (!moveFound) {
            for(int i = 0; i < length; i++) {
                randomMove = Position.goInR(randomMove, direction);
                charAtPos = getCharAt(randomMove);
                if (charAtPos != '-' && charAtPos != 't') {
                    moveFound = checkIfMoveIsPossible(randomMove, directions, this);
                    if (moveFound) {
                        break;
                    }
                }
            }

            if (moveFound)
                break;

            direction = (direction+2)%8;

            for(int i = 0; i < length; i++) {
                randomMove = Position.goInR(randomMove, direction);
                charAtPos = getCharAt(randomMove);
                if (charAtPos != '-' && charAtPos != 't') {
                    moveFound = checkIfMoveIsPossible(randomMove, directions, this);
                    if (moveFound) {
                        break;
                    }
                }
            }

            direction = (direction+2)%8;
            length++;

            if (length > 2 + 2 * Math.max(getHeight(), getWidth())) return new int[]{-1, -1, -1};
        }

        charAtPos = getCharAt(randomMove);

        switch (charAtPos) {
            case 'b':
                if (Math.round(Math.random()) == 1)
                    result = new int[]{randomMove.x, randomMove.y, 20};
                else
                    result = new int[]{randomMove.x, randomMove.y, 21};
                return result;

            case 'c':
                randomVal = (int)Math.round( Math.random() * (staticMap.anzPlayers-1) ) + 1;  //-1 to create values from 0 to anzPlayer-1 and +1 to get it from 1 to anzPlayers
                while(getCountOfStonesOfPlayer(randomVal) == 0)
                {
                    randomVal = ++randomVal % staticMap.anzPlayers;
                }
                return new int[]{randomMove.x, randomMove.y, randomVal};

            default:
                return new int[]{randomMove.x, randomMove.y, 0};
        }
    }

    /**
     * @return Returns a random Bomb move that can be made
     */
    public int[] getRandomBombMove(){
        //-2 because we have a line of walls around all maps and -1 because we don't want to have the (widht-2) itself in the values
        int x = 1 + (int) (Math.random() * (this.getWidth() - 3));
        int y = 1 + (int) (Math.random() * (this.getHeight() - 3));
        Position randomMove = new Position(x, y);
        char charAtPos = getCharAt(randomMove);
        int length = 1;
        int direction = 0;

        if (charAtPos != '-' && charAtPos != 't') {
            return new int[]{randomMove.x, randomMove.y, 0};
        }

        for (int loopCount = 0; loopCount < 5 * getWidth() * getHeight(); loopCount++) {
            for(int i = 0; i < length; i++) {
                randomMove = Position.goInR(randomMove, direction);
                charAtPos = getCharAt(randomMove);
                if (charAtPos != '-' && charAtPos != 't') {
                    return new int[]{randomMove.x, randomMove.y, 0};
                }
            }

            direction = (direction+2)%8;

            for(int i = 0; i < length; i++) {
                randomMove = Position.goInR(randomMove, direction);
                charAtPos = getCharAt(randomMove);
                if (charAtPos != '-' && charAtPos != 't') {
                    return new int[]{randomMove.x, randomMove.y, 0};
                }
            }

            direction = (direction+2)%8;
            length++;

            if (length > 2 + 2 * Math.max(getHeight(), getWidth())) return new int[]{-1, -1, -1};
        }

        System.err.println("Something went wrong - getRandomBombMove didn't find a bomb move");
        return new int[]{-1,-1,-1};
    }

    /**
     * Gets the placement of the specified player in the curent map
     * @param myPlayerNr player to get the placement for
     * @return position
     */
    public int getPlacement(int myPlayerNr){
        int placement = 1;
        int score;
        int myScore = getCountOfStonesOfPlayer(myPlayerNr);
        for (int playerNr = 1; playerNr <= staticMap.anzPlayers; playerNr++){
            if (playerNr == myPlayerNr) continue;
            score = getCountOfStonesOfPlayer(playerNr);
            if (score > myScore){
                placement++;
            }
        }
        return placement;
    }

    // SETTER ----------------------------------------------------------------------------------------------------------

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
        if (x >= staticMap.width || y >= staticMap.height || x < 0 || y < 0) {
            System.err.println("Position out of Map");
            return false;
        }

        //get what is there
        charAtPos = getCharAt(x,y);

        //check if something changes
        if (charAtPos == charToChangeTo) return true;

        //if there was a player remove stone from his stone set
        if (charAtPos != '0' && Character.isDigit(charAtPos)) {
            stonesPerPlayer.get(charAtPos-'0'-1).remove(posToSetChar); //-'0' to convert to int; -1 because of index shift
        }
        //if thee was an expansion-field remove it from the list
        if (charAtPos == 'x') expansionFields.remove(posToSetChar);

        //set char
        map[y][x] = charToChangeTo;

        //add char to set of player
        if (charToChangeTo != '0' && Character.isDigit(charToChangeTo)) {
            stonesPerPlayer.get(charToChangeTo-'0'-1).add(posToSetChar); //-'0' to convert to int; -1 because of index shift
        }

        //update valid moves
        if (useArrows && charAtPos != '-' && charAtPos != '+' && charAtPos != 't' && charToChangeTo != '-' && charToChangeTo != '+' && charToChangeTo != 't' && charToChangeTo != '0') {
            //update Arrows
            fieldChange(x, y, charAtPos - '0', charToChangeTo - '0');
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
        if (posToSetKeystone.x >= staticMap.width || posToSetKeystone.y >= staticMap.height || posToSetKeystone.x < 0 || posToSetKeystone.y < 0) {
            System.err.println("Position out of Map");
            return;
        }

        //set char
        map[posToSetKeystone.y][posToSetKeystone.x] = charToChangeTo;
    }

    /**
     * Swaps Stones of the currently playing player with the specified player
     * @param playerId ID of the player with whom to swap stones
     */
    public void swapStonesWithOnePlayer(int playerId) {
        //error handling
        if (playerId < 1 || playerId > staticMap.anzPlayers) {
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
        HashSet<Position> posBuffer;
        HashMap<Position, Integer> validPosBuffer;
        Arrow[] buffer;

        //swap stones with player
        posBuffer = stonesPerPlayer.get(currentlyPlaying);
        stonesPerPlayer.set(currentlyPlaying, stonesPerPlayer.get(playerId));
        stonesPerPlayer.set(playerId, posBuffer);

        if (useArrows){

            //swaps valid positions
            validPosBuffer = ValidMoves.get(currentlyPlaying);
            ValidMoves.set(currentlyPlaying, ValidMoves.get(playerId));
            ValidMoves.set(playerId, validPosBuffer);

            //swaps overwrite positions
            validPosBuffer = OverwriteMoves.get(currentlyPlaying);
            OverwriteMoves.set(currentlyPlaying, OverwriteMoves.get(playerId));
            OverwriteMoves.set(playerId, validPosBuffer);

            //swap arrows
            for (int y = 0; y < staticMap.height; y++) {
                for (int x = 0; x < staticMap.width; x++) {
                    buffer = AffectedArrows[y][x][currentlyPlaying];
                    AffectedArrows[y][x][currentlyPlaying] = AffectedArrows[y][x][playerId];
                    AffectedArrows[y][x][playerId] = buffer;
                }
            }
        }

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
        HashSet<Position> posBuffer;
        HashMap<Position, Integer> validPosBuffer;
        Arrow[] buffer;

        //adds the last element at the front, so it shifts all the other elements one further and deletes the last element
        posBuffer = stonesPerPlayer.remove(stonesPerPlayer.size()-1);
        stonesPerPlayer.add(0,posBuffer);

        if (useArrows) {

            //swap Valid Moves
            validPosBuffer = ValidMoves.remove( stonesPerPlayer.size()-1 );
            ValidMoves.add(0, validPosBuffer);

            //swap overwrite Moves
            validPosBuffer = OverwriteMoves.remove( stonesPerPlayer.size()-1 );
            OverwriteMoves.add(0, validPosBuffer);

            //swap arrows
            for (int y = 0; y < staticMap.height; y++) {
                for (int x = 0; x < staticMap.width; x++) {
                    //get last element
                    buffer = AffectedArrows[y][x][staticMap.anzPlayers - 1];
                    //shift all arrow lists one further
                    for (int playerNr = staticMap.anzPlayers - 1; playerNr >= 1; playerNr--) {
                        AffectedArrows[y][x][playerNr] = AffectedArrows[y][x][playerNr - 1];
                    }
                    //set elements of first player to the ones from the last one
                    AffectedArrows[y][x][0] = buffer;
                }
            }
        }

        //colors the new stones of the player in its color
        for (int playerNr = 1; playerNr <= staticMap.anzPlayers; playerNr++){
            for (Position pos : stonesPerPlayer.get(playerNr-1)){
                setCharAtWithoutSetUpdate(pos, (char)('0'+playerNr));
            }
        }
    }

    /**
     * increases the number of bombs the player currently playing has by 1
     */
    public void increaseBombsOfPlayer(){
        bombsPerPlayer[currentlyPlaying-1]++;
    }

    /**
     * decreases the number of bombs the player currently playing has by 1
     */
    public void decreaseBombsOfPlayer(){
        bombsPerPlayer[currentlyPlaying-1]--;
    }

    /**
     * increases the number of overwrite stones the player currently playing has by 1
     */
    public void increaseOverrideStonesOfPlayer(){
        overwriteStonesPerPlayer[currentlyPlaying-1]++;
    }

    /**
     * decreases the number of overwrite stones the player currently playing has by 1
     */
    public void decreaseOverrideStonesOfPlayer(){
        overwriteStonesPerPlayer[currentlyPlaying-1]--;
    }

    /**
     * sets the player currently playing to the next one.
     * Regards only the disqulaified players and not if the player can make a move
     * @return returns the number of skipped players
     */
    public int nextPlayer(){
        int skippedPlayers = -1;
        //TODO: check if player can make a move
        do {
            currentlyPlaying++;
            if (currentlyPlaying == staticMap.anzPlayers+1) currentlyPlaying = 1;
            skippedPlayers++;
        } while (disqualifiedPlayers[currentlyPlaying - 1]);

        return skippedPlayers;
    }

    /**
     * sets the next player to the specified number
     * @param PlayerID player number it gets set to
     */
    public void setPlayer(int PlayerID) {
        currentlyPlaying = PlayerID;
    }

    /**
     * marks the specified player ass disqualified
     * @param playerNr player number
     */
    public void disqualifyPlayer(int playerNr){
        disqualifiedPlayers[playerNr-1] = false;
    }


    //  METHODS FOR MOVE CARRY ALONG -----------------------------------------------------------------------------------

    private void firstCreation(int x, int y, int newPlayer){
        for (int r = 0; r <= 7; r++){
            addNewArrow(x, y, r, newPlayer);
        }
    }

    private void fieldChange(int x, int y, int oldPlayer, int newPlayer){
        Arrow currArrow;

        //update every arrow for every player that is affected by this field
        for (int playerNr = 1; playerNr <= staticMap.anzPlayers; playerNr++){
            for (int r = 0; r <= 7; r++){
                currArrow = AffectedArrows[y][x][playerNr-1][r];
                if (currArrow != null) {
                    updateArrow(currArrow, playerNr, x, y, r, oldPlayer);
                }
            }
        }

        //delete arrows that start on this position
        //and add new ones for the new player
        for (int r = 0; r <= 7; r++){
            currArrow = StartingArrows[y][x][r];
            if (currArrow != null) {
                int[] posAndR = currArrow.positionsWithDirection.get( currArrow.positionsWithDirection.size()-1 );
                deleteArrowFrom(currArrow, oldPlayer, 0, map[posAndR[1]][posAndR[0]]-'0');
            }
            addNewArrow(x, y, r, newPlayer);
        }

        if (checkIfAllArrowsAreCorrect) {
            System.out.println("Check if right: [List: " + checkForReferenceInAffectedArrows() + ", Valid Moves: " + checkValidMoves() + ", Overwrite Moves " + checkOverwriteMoves() + "] ");
            return;
        }
    }

    private Arrow deleteArrowFrom(Arrow oldArrow, int arrowOfPlayer, int from, int newPlayerOnField){
        int[] posAndR;
        Position currPos;
        Position secondPos = null;
        Arrow copyOfArrow = oldArrow.clone();

        if (oldArrow.positionsWithDirection.size() >= 1){
            posAndR = oldArrow.positionsWithDirection.get(1);
            secondPos = new Position(posAndR[0], posAndR[1]);
        }

        //delete the valid move created by this arrow
        if (oldArrow.createsValidMove){
            //get last element
            posAndR = oldArrow.positionsWithDirection.get(oldArrow.positionsWithDirection.size()-1);
            currPos = new Position(posAndR[0],posAndR[1]);
            removeValidPosition(arrowOfPlayer, currPos);
            copyOfArrow.createsValidMove = false;
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
                    AffectedArrows[posAndR[1]][posAndR[0]][arrowOfPlayer-1][posAndR[2]] = null;
                }
            }

            //delete overwrite moves created by this arrow - index 0 and 1 don't create an OverWrite move - index = size-1 sometimes
            currPos = new Position(posAndR[0],posAndR[1]);
            if (counter >= 2 && !currPos.equals(secondPos)){

                //if it's the last position of the arrow
                if (counter == oldArrow.positionsWithDirection.size()-1){
                    //checks if the Position, the arrow points to is one of the players stones -> if so delete overwrite move

                    // if the position isn't the one we colored in this call get the player from the map
                    // else take the one given. (overwrites it, but it gets called only once anyway)
                    if (from != counter) {
                        newPlayerOnField = map[currPos.y][currPos.x]-'0';
                    }

                    //if the arrow points to a position where we are, delete the position
                    if (newPlayerOnField >= 1 && newPlayerOnField <= getAnzPlayers()){ //if (newPlayerOnField == arrowOfPlayer){
                        removeOverwritePosition(arrowOfPlayer, currPos);
                    }
                }
                else {
                    removeOverwritePosition(arrowOfPlayer, currPos);
                }
            }

            //delete last position out of list - needs the copied variant because otherwise it would change the object
            copyOfArrow.positionsWithDirection.remove(counter);
        }

        //update references on the remaining positions
        for (int counter = from-1; counter >= 0; counter--){
            //get position and direction
            posAndR = copyOfArrow.positionsWithDirection.get(counter);
            //update reference
            if (counter == 0){
                StartingArrows[posAndR[1]][posAndR[0]][posAndR[2]] = copyOfArrow;
            }
            else {
                if (AffectedArrows[posAndR[1]][posAndR[0]][arrowOfPlayer-1][posAndR[2]] == oldArrow) {
                    AffectedArrows[posAndR[1]][posAndR[0]][arrowOfPlayer-1][posAndR[2]] = copyOfArrow;
                }
            }
        }

        return copyOfArrow;
    }

    private void addNewArrow(int x, int y, int direction, int arrowOfPlayer){
        Arrow newArrow = new Arrow();
        int[] posAndR;

        //add starting position
        newArrow.positionsWithDirection.add(new int[]{x,y,direction});

        //create Arrow on map
        createArrowFrom(newArrow, arrowOfPlayer);

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

    private void updateArrow(Arrow currArrow, int arrowOfPlayer, int x, int y, int direction, int oldPlayer){
        //TODO: use binary search
        int i = 0;
        int[] posAndR;

        for (; i < currArrow.positionsWithDirection.size(); i++){
            posAndR = currArrow.positionsWithDirection.get(i);
            if (posAndR[0] == x && posAndR[1] == y && posAndR[2] == direction) break;
        }
        if (i >= currArrow.positionsWithDirection.size()) System.err.println("Something went wrong - Position not found");

        //deletes all positions after the one we got
        currArrow = deleteArrowFrom(currArrow, arrowOfPlayer, i, oldPlayer); //creates new arrow that then replaces the old One

        //check further
        //  get last position
        if (currArrow.positionsWithDirection.size()-1 >= 0) {
            //  create Arrow from last position on forward
            createArrowFrom(currArrow, arrowOfPlayer);
        }
        else {
            System.err.println("The Size of the arrow is less then 1");
        }

        //add new references

        for (int j = i; j < currArrow.positionsWithDirection.size(); j++) {
            //  start position
            if (j == 0) {
                System.err.println("Something went wrong - While Updating Arrow, it got deleted");
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

    private void createArrowFrom(Arrow arrow, int arrowOfPlayer){
        //set posAndR to last position
        int[] posAndR = arrow.positionsWithDirection.get( arrow.positionsWithDirection.size()-1 );
        int direction = posAndR[2];
        Position currPos = new Position(posAndR[0], posAndR[1]);
        //set posAndR to first pos
        posAndR = arrow.positionsWithDirection.get(0);
        Position StartingPos = new Position(posAndR[0], posAndR[1]);

        Position SecondPos = null;
        int newR = direction;
        Integer nextR;
        char currChar;
        boolean firstStep = true;

        //check if it continues the arrow
        if (arrow.positionsWithDirection.size() >= 2){
            firstStep = false;
            //get second position if lenth of arrow is 2 or more
            posAndR = arrow.positionsWithDirection.get(1);
            SecondPos = new Position(posAndR[0], posAndR[1]);
        }

        //go in one direction
        while (true) {
            //does one step
            nextR = staticMap.doAStep(currPos, newR); //currPos is changed here

            //check for walls
            if (nextR == null) {
                //set end to position it can't go to
                currPos = Position.goInR(currPos, newR);
                break;
            }
            newR = nextR;

            //check for cycles
            if(currPos.equals(StartingPos)) {
                //let arrow end one field ahead of the start - and because position one before was already added return
                return;
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
                    addOverwritePosition(arrowOfPlayer, currPos.clone());
                }
                break;
            }

            //if it's another player or 'x'
            arrow.positionsWithDirection.add(new int[]{currPos.x, currPos.y, newR});
            if (firstStep) {
                SecondPos = new Position(currPos.x, currPos.y);
            }

            //if it's not the first step and the currPos is not the second pos an OverWrite move can be made
            if (!firstStep && !currPos.equals(SecondPos)) {
                addOverwritePosition(arrowOfPlayer, currPos.clone());
            }

            firstStep = false;
        }

        //add last position after break
        arrow.positionsWithDirection.add(new int[]{currPos.x, currPos.y, newR});
    }

    /**
     * Colors the map in regard to what move was made and what player has the turn.
     * For this method the Arrows are used
     * @param pos Position the currently playing player set to
     */
    public void colorMapByArrows(Position pos){
        boolean isOverwriteMove;
        char charAtPos = getCharAt(pos);
        Position secondPos;
        Position currPos;
        int[] posAndR;

        isOverwriteMove = (charAtPos != '0' && Character.isDigit(charAtPos)) || getCharAt(pos) == 'x';

        if (getCharAt(pos) == 'x'){
            setCharAt(pos.x, pos.y, (char) ('0' + getCurrentlyPlayingI()));
        }

        //get and clone array and arrows
        Arrow[] arrows = AffectedArrows[pos.y][pos.x][getCurrentlyPlayingI() - 1].clone();
        for (int i = 0; i < arrows.length; i++){
            if (arrows[i] == null) continue;
            arrows[i] = arrows[i].clone(); //needs to be cloned because otherwise the list is changed because of the coloring
        }

        //normal move
        if (!isOverwriteMove) {
            for (int i = 0; i < arrows.length; i++) {
                Arrow arrow = arrows[i];
                if (arrow != null && arrow.createsValidMove) {
                    for (int[] posAndR2 : arrow.positionsWithDirection) {
                        setCharAt(posAndR2[0], posAndR2[1], (char) ('0' + getCurrentlyPlayingI()));
                    }
                }
            }
        }

        //overwrite move
        else {
            for (int arrowIndex = 0; arrowIndex < arrows.length; arrowIndex++) {
                //get arrow
                Arrow arrow = arrows[arrowIndex];
                if (arrow == null) continue;
                //get second Pos
                posAndR = arrow.positionsWithDirection.get(1);
                secondPos = new Position(posAndR[0], posAndR[1]);

                //go through positions to find the index of the position that was set on
                for (int posIndex = 0; posIndex < arrow.positionsWithDirection.size(); posIndex++) {
                    //get position
                    posAndR = arrow.positionsWithDirection.get(posIndex);
                    currPos = new Position(posAndR[0], posAndR[1]);

                    //check position
                    if (posIndex >= 2 && currPos.equals(pos) && !currPos.equals(secondPos)) {

                        //goes from the found position to the first position and colors the fields
                        for (int posToColorIndex = posIndex; posToColorIndex >= 0; posToColorIndex--) {
                            posAndR = arrow.positionsWithDirection.get(posToColorIndex);
                            setCharAt(posAndR[0], posAndR[1], (char) ('0' + getCurrentlyPlayingI()));
                        }
                    }
                }
            }

        }
    }

    //      Helping methods for Move Carry along

    private void addValidPosition(int arrowOfPlayer, Position currPos) {
        Integer arrowsPointingToPos;
        //add position to valid moves
        //  increase count of arrows pointing to this direction
        arrowsPointingToPos = ValidMoves.get(arrowOfPlayer - 1).get(currPos);
        if (arrowsPointingToPos != null) {
            arrowsPointingToPos++; //TODO: check if that creates a problem
            //add position and count
            ValidMoves.get(arrowOfPlayer - 1).replace(currPos, arrowsPointingToPos);
        }
        else {
            arrowsPointingToPos = 1;
            //add position and count
            ValidMoves.get(arrowOfPlayer - 1).put(currPos, arrowsPointingToPos);
        }

    }

    private void addOverwritePosition(int arrowOfPlayer, Position currPos) {
        Integer arrowsPointingToPos;
        //add position to overwrite moves
        //  increase count of arrows pointing to this direction
        arrowsPointingToPos = OverwriteMoves.get(arrowOfPlayer - 1).get(currPos);
        if (arrowsPointingToPos != null) {
            arrowsPointingToPos++;
            //  add position and count
            OverwriteMoves.get(arrowOfPlayer - 1).replace(currPos,arrowsPointingToPos);
        }
        else {
           arrowsPointingToPos = 1;
            //  add position and count
            OverwriteMoves.get(arrowOfPlayer - 1).put(currPos,arrowsPointingToPos);
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
        //delete overwrite move
        //  get number of arrows that point to this position
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

    //      Checking Methods for Move Carry Along

    private ArrayList<Arrow> getAllArrows(){
        ArrayList<Arrow> arrowsInMap = new ArrayList<>();
        Arrow arrow;

        for (int y = 0; y < staticMap.height; y++){
            for (int x = 0; x < staticMap.width; x++){
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

    /**
     * Testing Method to check if every arrow has the correct reference in the affected arrow array
     * @return true if everything is right and false otherwise
     */
    public boolean checkForReferenceInAffectedArrows(){
        ArrayList<Arrow> allArrows = getAllArrows();
        boolean isOneOfThem;
        int[] posAndR;

        for (Arrow arrow : allArrows){
            for (int i = 1; i < arrow.positionsWithDirection.size(); i++){
                posAndR = arrow.positionsWithDirection.get(i);
                isOneOfThem = false;
                for (int playerNr = 0; playerNr < staticMap.anzPlayers; playerNr++) {
                    if (AffectedArrows[posAndR[1]][posAndR[0]][playerNr][posAndR[2]] == arrow) isOneOfThem = true;
                }
                if (!isOneOfThem) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Testing Method to check if every arrow that creates a valid move is considered
     * @return true if everything is right and false otherwise
     */
    public boolean checkValidMoves(){
        ArrayList<Arrow> validArrows = getAllValidArrows();

        int[] posAndR;
        boolean isOneOfThem;
        int arrowOfPlayer;
        ArrayList<ArrayList<Position>> validMovesOfValidArrows = new ArrayList<>(8);
        for (int i = 0; i < 8; i++) validMovesOfValidArrows.add(new ArrayList<>());

        for (Arrow arrow : validArrows){
            //get player
            posAndR = arrow.positionsWithDirection.get(0);
            arrowOfPlayer = map[posAndR[1]][posAndR[0]]-'0';
            //get last position
            posAndR = arrow.positionsWithDirection.get( arrow.positionsWithDirection.size()-1 );
            validMovesOfValidArrows.get(arrowOfPlayer-1).add(new Position(posAndR[0], posAndR[1]));
            //reset
            isOneOfThem = false;

            for (Position validPos : ValidMoves.get(arrowOfPlayer-1).keySet()){
                if (validPos.x == posAndR[0] && validPos.y == posAndR[1]) isOneOfThem = true;
            }

            if (!isOneOfThem) {
                return false;
            }
        }

       for (int playerNr = 0; playerNr < getAnzPlayers(); playerNr++){
           for (Position pos : ValidMoves.get(playerNr).keySet()){

               if (!validMovesOfValidArrows.get(playerNr).contains(pos)) {
                   return false;
               }
           }
       }

        return true;
    }

    /**
     * Testing Method to check if every arrow that creates overwrite moves is considered
     * @return true if everything is right and false otherwise
     */
    public boolean checkOverwriteMoves(){
        ArrayList<Arrow> allArrows = getAllArrows();
        int[] posAndR;
        boolean correct = true;
        boolean isOneOfThem;
        int arrowOfPlayer;
        Position SecondPos = null;


        //for every arrow
        for (Arrow arrow : allArrows){

            //get Second Pos
            if (arrow.positionsWithDirection.size() >= 2) {
                posAndR = arrow.positionsWithDirection.get(1);
                SecondPos = new Position(posAndR[0], posAndR[1]);
            }

            //get player
            posAndR = arrow.positionsWithDirection.get(0);
            arrowOfPlayer = map[posAndR[1]][posAndR[0]]-'0';

            //for every overwrite position
            for (int i = 2; i < arrow.positionsWithDirection.size(); i++) {
                //get position
                posAndR = arrow.positionsWithDirection.get(i);

                if (SecondPos.equals(new Position(posAndR[0], posAndR[1]))) continue;

                //reset
                isOneOfThem = false;

                //if it's the last element
                if (i == arrow.positionsWithDirection.size()-1){
                    char charAtPos = map[posAndR[1]][posAndR[0]];
                    //if the stone at that position is not the color of the current player
                    if (charAtPos != arrowOfPlayer+'0'){
                        continue;
                    }
                }

                for (Position overwritePos : OverwriteMoves.get(arrowOfPlayer - 1).keySet()) {
                    if (overwritePos.x == posAndR[0] && overwritePos.y == posAndR[1]) isOneOfThem = true;
                }

                if (!isOneOfThem) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Testing Method to check if every overwrite move can be made has the corret ammount of arrows that create these moves
     * @return true if everything is right and false otherwise
     */
    public Position checkOverwriteMovesTheOtherWay(){
        int counter;
        int[] currPosAndDir;
        Position currPos;
        Position secondPos;
        char currStartChar;

        for (int playerNr = 1; playerNr <= getAnzPlayers(); playerNr++) {
            for (Position overwritePos : OverwriteMoves.get(playerNr-1).keySet()) {
                counter = 0;
                for (Arrow arrow : getAllArrows()){
                    //to short?
                    if (arrow.positionsWithDirection.size() <= 2) continue;

                    //right player?
                    currPosAndDir = arrow.positionsWithDirection.get(0);
                    currStartChar = map[currPosAndDir[1]][currPosAndDir[0]];
                    if (currStartChar-'0' != playerNr) continue;

                    //set second pos
                    currPosAndDir = arrow.positionsWithDirection.get(1);
                    secondPos = new Position(currPosAndDir[0], currPosAndDir[1]);

                    //go over the relevant positions
                    for (int i = 2; i < arrow.positionsWithDirection.size(); i++){
                        //get position
                        currPosAndDir = arrow.positionsWithDirection.get(i);
                        currPos = new Position(currPosAndDir[0], currPosAndDir[1]);

                        //check if it's relevant
                        if (currPos.equals(overwritePos) && !currPos.equals(secondPos)) counter++;
                    }
                }
                if (counter != OverwriteMoves.get(playerNr-1).get(overwritePos)) {
                    System.out.println("checkOverwriteMovesTheOtherWay() failed with move: " + overwritePos + " for player " + playerNr);
                    System.out.println("Found " + counter + " overwrite positions by the arrows, but in the list there is: " + OverwriteMoves.get(playerNr-1).get(overwritePos));
                    return overwritePos;
                }
            }
        }
        return null;
    }

    //  INIT METHODS

    private void initValidMoveArrays(){
        for (int playerNr = 0; playerNr < staticMap.anzPlayers; playerNr++) {
            ValidMoves.add(new HashMap<>());
            OverwriteMoves.add(new HashMap<>());
        }
    }

    private void initStonesPerPlayerSet(){
        for (int playerNr = 0; playerNr < staticMap.anzPlayers; playerNr++) {
            stonesPerPlayer.add(new HashSet<>());
        }
    }

    // OTHER -----------------------------------------------------------------------------------------------------------

    /**
     * By calling this method you can simulate a step on the map.
     * This step is taken from the specified poition in the specified direction.
     * The position that was given the method contains afterwards the new position after the step
     * @param pos Position to do the step from
     * @param r direction in wich to go
     * @return Integer value wich direction you are now facing or null if the step wasn't possible
     */
    public Integer doAStep(Position pos, int r){
        return staticMap.doAStep(pos,r);
    }

    //STATIC FUNCTIONS --------------------------------------------------------------------------------------------------------------------------------------------

    //  functions to simulate a move

    /**
     * This method simulates a move. The given map is updated in regard to the position the stone was set to.
     * @param posToSetKeystone Position the keystone was set to
     * @param additionalInfo Additional informatin that might be needed (1-9 for a choise move; 20, 21 for a bonus move)
     * @param moveOfPlayer playernumber of the player that made the move
     * @param map Map that is updated
     * @param printOn boolean that specifies if the function should print an output
     */
    public static void updateMapWithMove(Position posToSetKeystone, int additionalInfo, int moveOfPlayer, Map map, boolean printOn) {
        char fieldValue;

        map.setPlayer(moveOfPlayer); //set playing player because server could have skipped some

        //get value of field where next keystone is set
        fieldValue = map.getCharAt(posToSetKeystone.x, posToSetKeystone.y);

        //color the map
        Map.colorMap(posToSetKeystone, map);

        //handle special moves
        switch (additionalInfo){
            case 0: //could be normal move, overwrite move, or inversion move
                //for a normal move there are no further actions necessary
                //overwrite move
                if ((Character.isDigit(fieldValue) && fieldValue != '0') || fieldValue == 'x') {
                    if (printOn) System.out.println("Overwrite Move");
                    map.decreaseOverrideStonesOfPlayer();
                }
                //inversion move
                else if (fieldValue == 'i') {
                    if (printOn) System.out.println("Inversion Move");
                    map.Inversion();
                }
                break;
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8: //choice
                if (printOn) System.out.println("Choice Move");
                map.swapStonesWithOnePlayer(additionalInfo);
                break;
            case 20: //bonus and want a bomb
                if (printOn) System.out.println("Bonus Move - Bomb");
                map.increaseBombsOfPlayer();
                break;
            case 21: //bonus and want an overwrite-stone
                if (printOn) System.out.println("Bonus Move - Overwrite Stone");
                map.increaseOverrideStonesOfPlayer();
                break;
            default:
                System.err.println("Field value is invalid");
                break;
        }

        map.nextPlayer();
    }

    /**
     * Updates Map by breadth-first search
     * @param x x coordinate where the bomb was set
     * @param y y coordinate where the bomb was set
     */
    public static void updateMapAfterBombingBFS(int x, int y, int moveOfPlayer, Map map){
        char charAtPos;
        int explosionRadius = map.getExplosionRadius();

        map.setPlayer(moveOfPlayer); //server may have skipped a player

        if (map.getBombsForPlayer(moveOfPlayer) == 0){
            System.err.println("Something's wrong - Server send a bomb move but Player has no bombs - updating Map anyway");
        }

        //for breadth-first search
        Queue<int[]> posQ = new LinkedList<>(); //int array: [0] == x, [1] == y, [2] == distance from explosion
        int[] currPosAndDist;
        Position nextPos;
        Position posAfterStep;
        int counterForExpRad = 0;

        //for transitions
        Integer newR;

        //first element
        map.setCharAt(x, y, '+'); //TODO: check if + can be removed so only - is used
        posQ.add(new int[]{x,y, counterForExpRad});

        while (!posQ.isEmpty()){
            currPosAndDist = posQ.poll();
            nextPos = new Position(currPosAndDist[0], currPosAndDist[1]);
            counterForExpRad = currPosAndDist[2];

            //go in every possible direction
            for (int r = 0; r <= 7; r++) {
                //get position it will move to
                posAfterStep = Position.goInR(nextPos, r);
                //check what's there
                charAtPos = map.getCharAt(posAfterStep);

                //if there's a transition go through (not the char though)
                if (charAtPos == 't') {
                    //go one step back because we need to come from where the transition points
                    posAfterStep = Position.goInR(posAfterStep, (r + 4) % 8);
                    //tries to go through transition
                    newR = map.doAStep(posAfterStep, r); //takes Position it came from. Because from there it needs to go through
                    if (newR == null) continue;
                }

                if (charAtPos != '+' && charAtPos != '-') { // + is grey, - is black
                    //if explosion radius allows it
                    if (counterForExpRad < explosionRadius) {
                        map.setCharAt(posAfterStep.x, posAfterStep.y, '+');
                        posQ.add(new int[]{posAfterStep.x, posAfterStep.y, counterForExpRad + 1});
                    }
                }
            }
            map.setCharAt(nextPos.x, nextPos.y, '-'); //next position is still the position it came from
        }

        //Decreases the Bombs of the player
        map.decreaseBombsOfPlayer();

        //next player
        map.nextPlayer();
    }


    //  function to color the map

    /**
     * colors the map when the keystone is placed at the specified position
     * @param pos position where the keystone is placed
     * @param map the map on which it is placed on
     */
    public static void colorMap(Position pos, Map map){

        if (useArrows){
            map.colorMapByArrows(pos);
        }
        else {
            Set<Position> positionsToColor; //doesn't store duplicates

            //if the move is a overwrite-move to an expansion-field
            if (map.getCharAt(pos) == 'x' && map.getOverwriteStonesForPlayer(map.getCurrentlyPlayingI()) > 0) {
                map.setCharAt(pos.x, pos.y, map.getCurrentlyPlayingC());
            }

            //get positions to color
            positionsToColor = getPositionsToColor(pos, map);

            //colors the positions
            for (Position posToColor : positionsToColor) {
                map.setCharAt(posToColor.x, posToColor.y, map.getCurrentlyPlayingC());
            }
        }
    }

    private static Set<Position> getPositionsToColor(Position pos, Map map) {
        LinkedHashSet<Position> positionsToColor = new LinkedHashSet<>();
        Position StartingPos;
        char currChar;
        boolean wasFirstStep;
        Position currPos;
        ArrayList<Position> positionsAlongOneDirection;
        Integer newR;
        boolean foundEnd;

        //checks every direction for a connection and adds the positions in between to color them later
        for (int r = 0; r <= 7; r++){
            //reset values
            StartingPos = pos.clone();
            currPos = pos.clone();
            wasFirstStep = true;
            newR = r;
            positionsAlongOneDirection = new ArrayList<>();
            positionsAlongOneDirection.add(currPos.clone());
            foundEnd = false;

            //go in one direction until there is something relevant
            while (true) {
                //does one step
                newR = map.doAStep(currPos, newR); //currPos is changed here
                if (newR == null) break; //if the step wasn't possible

                //detect loop
                if(currPos.equals(StartingPos)) break;

                //check what's there
                currChar = map.getCharAt(currPos);

                //check for blank
                if (currChar == '0' || currChar == 'i' || currChar == 'c' ||currChar == 'b') break;

                //check for players

                //if it's the first move - finding an own keystone isn't a connection but cancels the search in that direction
                if (wasFirstStep) {
                    //if there is a keystone of your own, and it's the first step
                    if (currChar == map.getCurrentlyPlayingC()) break;
                    //if there's another player
                    wasFirstStep = false;
                }

                //if it's not the first move - finding an own keystone is a connection
                else {
                    //if there is a keystone of your own, and it's not the first step
                    if (currChar == map.getCurrentlyPlayingC()) {
                        foundEnd = true;
                        break;
                    }
                }
                //when adding the position to the list HERE it doesn't add the ones that break the loop
                positionsAlongOneDirection.add(currPos.clone());
            }
            //if it found a connection it adds all the moves along the way to the positions to color
            if (foundEnd) positionsToColor.addAll(positionsAlongOneDirection); //doesn't add duplicates because of LinkedHashSet
        }

        return positionsToColor;
    }

    //  functions to calculate all possible moves

    /**
     * returns the possible moves on the specified map for the current player
     * @param map Map that possible moves are calculated on
     * @param timed boolean if there is a time limit
     * @param printOn boolean if the method should print information
     * @param serverLog boolean if only the neccessary inforamtion should be printed
     * @param upperTimeLimit if timed is enabled this is the time limit the method needs to considder
     * @param heuristic heuristic to evaluate if overwrite moves should be made
     * @return List of possible Moves. The moves are speccisfied in an int[] as (x,y,info)
     * @throws ExceptionWithMove if the time is over this exception is thrown. It contains a move that can be made
     */
    public static ArrayList<int[]> getValidMoves(Map map, boolean timed, boolean printOn, boolean serverLog, long upperTimeLimit, Heuristic heuristic) throws ExceptionWithMove {
        ArrayList<int[]> everyPossibleMove;

        //Out of Time ?
        if(timed && (upperTimeLimit - System.nanoTime()<0)) {
            if (printOn || serverLog) System.out.println("Out of Time - get Valid Moves");
            throw new ExceptionWithMove(map.getRandomMove());
        }

        if (useArrows){
            everyPossibleMove = map.getValidMovesByArrows(heuristic);
        }
        else {
            everyPossibleMove = getFieldsByOwnColor(map, timed, printOn, serverLog, upperTimeLimit, heuristic);
        }

        return everyPossibleMove;
    }

    /**
     * returns the possible bomb moves on the specified map for the current player
     * @param map Map that possible moves are calculated on
     * @return List of possible Moves. The moves are speccisfied in an int[] as (x,y,info)
     */
    public static ArrayList<int[]> getPositionsToSetABomb(Map map) {
        ArrayList<int[]> validMoves = new ArrayList<>();
        char fieldValue;

        //gets the possible positions to set a bomb at
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                fieldValue = map.getCharAt(x, y);
                if (fieldValue != '-' && fieldValue != 't') {
                    validMoves.add(new int[]{x, y});
                }
            }
        }

        return validMoves;
    }


    /**
     * goes in every specified direction to check if it's possible to set a keystone at the specified position
     * @param pos pos the keystone would be placed
     * @param directions directions it needs to check
     * @param map the map the check takes place on
     * @return returns true if the move is possible and false otherwise
     */
    public static boolean checkIfMoveIsPossible(Position pos, ArrayList<Integer> directions, Map map){
        Position StartingPos;
        Position currPos;
        Integer newR;
        boolean wasFirstStep;
        boolean isOverwriteMove;
        char currChar = map.getCharAt(pos);

        //check if it's out of the map
        if (currChar == '-' || currChar == 't') return false;

        //check if it's an expansions field
        if (currChar == 'x'){
            if (map.getOverwriteStonesForPlayer(map.getCurrentlyPlayingI()) > 0) return true; //no need to check if ti's a valid move
            else return false;
        }

        //check if it is an overwrite-move and if we have enough overwrite stones
        isOverwriteMove = (currChar != '0' && Character.isDigit(currChar));
        if (isOverwriteMove && map.getOverwriteStonesForPlayer(map.getCurrentlyPlayingI()) <= 0) return false;
        //else check if the overwrite-move is possible




        //go over every direction that needs to be checked
        for (Integer r : directions){
            //reset values
            StartingPos = pos.clone();
            currPos = pos.clone();
            wasFirstStep = true;
            newR = r;

            //go in one direction until there is something relevant
            while (true) {
                //does one step
                newR = map.doAStep(currPos, newR); //currPos is changed here
                if (newR == null) break; //if the step wasn't possible

                //check what's there
                currChar = map.getCharAt(currPos);
                //check for blank
                if (currChar == '0' || currChar == 'i' || currChar == 'c' ||currChar == 'b') break;

                //check for players
                //if it's the first move - finding an own keystone isn't a connection but cancels the search in that direction
                if (wasFirstStep) {
                    if (currChar == map.getCurrentlyPlayingC()) break;
                    wasFirstStep = false;
                }
                //if it's not the first move - finding an own keystone is a connection
                else {
                    if(currPos.equals(StartingPos)) break; //if we would color with the stone we started from

                    if (currChar == map.getCurrentlyPlayingC()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * returns the possible moves on the specified map for the current player. This is done by using the "By own color" Method
     * @param map Map that possible moves are calculated on
     * @param timed boolean if there is a time limit
     * @param printOn boolean if the method should print information
     * @param serverLog boolean if only the neccessary inforamtion should be printed
     * @param upperTimeLimit if timed is enabled this is the time limit the method needs to considder
     * @param heuristic heuristic to evaluate if overwrite moves should be made
     * @return List of possible Moves. The moves are speccisfied in an int[] as (x,y,info)
     * @throws ExceptionWithMove if the time is over this exception is thrown. It contains a move that can be made
     */
    public static ArrayList<int[]> getFieldsByOwnColor(Map map, boolean timed, boolean printOn, boolean serverLog, long upperTimeLimit, Heuristic heuristic) throws ExceptionWithMove{
        HashSet<PositionAndInfo> resultMovesSet = new HashSet<>();
        HashSet<PositionAndInfo> overwriteMovesSet = new HashSet<>();

        ArrayList<int[]> resultPosAndInfo = new ArrayList<>();
        ArrayList<int[]> overwriteMoves = new ArrayList<>();

        HashSet<Position> posVisited;

        int[] saveElement = null;

        PositionAndInfo posAndInfo;
        int r;
        Integer newR;
        Position currPos;
        char currChar;
        int bombOrOverwrite;


        //add x fields
        if (map.getOverwriteStonesForPlayer(map.getCurrentlyPlayingI()) > 0) {
            for (Position pos : map.getExpansionFields()) {
                if (heuristic != null && heuristic.evaluateOverwriteMove(pos)){
                    resultMovesSet.add(new PositionAndInfo(pos.x, pos.y, 0));
                }
                else {
                    overwriteMovesSet.add(new PositionAndInfo(pos.x, pos.y, 0));
                }
            }
        }

        //out of time ?
        if (  !(resultMovesSet.isEmpty() && overwriteMovesSet.isEmpty()) && timed && upperTimeLimit-System.nanoTime() < 0){
            if (printOn || serverLog) System.out.println("Out of time - get Fields by own color");

            //one of them is filled otherwise it wouldn't get in the if
            if (!resultMovesSet.isEmpty()){
                throw new ExceptionWithMove( resultMovesSet.iterator().next().toIntArray() );
            }
            else {
                throw new ExceptionWithMove( overwriteMovesSet.iterator().next().toIntArray() );
            }
        }

        //goes over every position of the current player and checks in all directions if a move is possible
        for (Position pos : map.getStonesOfPlayer(map.getCurrentlyPlayingI())){

            for (r = 0; r <= 7; r++){

                //out of time ?
                if ( !(resultMovesSet.isEmpty() && overwriteMovesSet.isEmpty()) && timed && upperTimeLimit-System.nanoTime() < 0){
                    if (printOn || serverLog) System.out.println("Out of time - get Fields by own color");
                    if (saveElement != null){
                        throw new ExceptionWithMove(saveElement);
                    }
                    else {
                        //one of them is filled otherwise it wouldn't get in the if
                        if (!resultMovesSet.isEmpty()){
                            throw new ExceptionWithMove( resultMovesSet.iterator().next().toIntArray() );
                        }
                        else {
                            throw new ExceptionWithMove( overwriteMovesSet.iterator().next().toIntArray() );
                        }
                    }
                }

                newR = r;
                currPos = pos.clone();
                posVisited = new HashSet<>();

                posVisited.add(currPos.clone()); //add to visited positions

                //do the first step
                newR = map.doAStep(currPos, newR);
                if (newR == null) continue; //if the step wasn't possible

                //detect loop
                if (currPos.equals(pos)){ //only needs to detect the position because it will go in every direction anyway
                    break;
                }

                //check what's there
                currChar = map.getCharAt(currPos);
                if (currChar == 'c' || currChar == 'b' || currChar == 'i' || currChar == '0' || currChar == map.getCurrentlyPlayingC()) continue; //if it's c, b, i, 0, myColor

                posVisited.add(currPos.clone()); //add to visited positions


                //take more steps
                while (true){
                    newR = map.doAStep(currPos, newR);
                    if (newR == null) break; //if the step wasn't possible

                    //detect loop
                    if (currPos.equals(pos)){ //only needs to detect the position because it will go in every direction anyway
                        break;
                    }

                    //check what's there
                    currChar = map.getCharAt(currPos);

                    //player or 0
                    if (Character.isDigit(currChar)){
                        // 0
                        if (currChar == '0') {
                            posAndInfo = new PositionAndInfo(currPos.x, currPos.y, 0);
                            saveElement = posAndInfo.toIntArray();
                            resultMovesSet.add(posAndInfo);
                            break;
                        }
                        //player
                        else {
                            //own or enemy stone -> overwrite move
                            if (map.getOverwriteStonesForPlayer(map.getCurrentlyPlayingI()) > 0 && !posVisited.contains(currPos)) {
                                posAndInfo = new PositionAndInfo(currPos.x, currPos.y, 0);
                                saveElement = posAndInfo.toIntArray();
                                if (heuristic != null && heuristic.evaluateOverwriteMove(new Position(currPos.x, currPos.y))){
                                    resultMovesSet.add(posAndInfo);
                                }
                                else {
                                    overwriteMovesSet.add(posAndInfo);
                                }
                            }
                            //if it's an own stone don't go on
                            if (currChar == map.getCurrentlyPlayingC()) break;
                        }
                    }

                    // c, b, i
                    else if (currChar != 'x') { //x nothing happens
                        switch (currChar){
                            case 'i': {
                                posAndInfo = new PositionAndInfo(currPos.x, currPos.y, 0);
                                saveElement = posAndInfo.toIntArray();
                                resultMovesSet.add(posAndInfo);
                                break;
                            }
                            case 'c': {
                                for (int playerNr = 1; playerNr <= map.getAnzPlayers(); playerNr++) {
                                    posAndInfo = new PositionAndInfo(currPos.x, currPos.y, playerNr);
                                    saveElement = posAndInfo.toIntArray();
                                    resultMovesSet.add(posAndInfo);
                                }
                                break;
                            }
                            case 'b': {
                                bombOrOverwrite = (heuristic == null)? 21 : heuristic.selectBombOrOverwrite();
                                posAndInfo = new PositionAndInfo(currPos.x, currPos.y, bombOrOverwrite);
                                saveElement = posAndInfo.toIntArray();
                                resultMovesSet.add(posAndInfo);
                                break;
                            }
                        }
                        break;
                    }

                    posVisited.add(currPos.clone()); //add to visited positions

                }
            }
        }

        if (heuristic == null){
            resultMovesSet.addAll(overwriteMovesSet);
        }

        if (!resultMovesSet.isEmpty()) {
            for (PositionAndInfo posAndInfo2 : resultMovesSet) {
                resultPosAndInfo.add(posAndInfo2.toIntArray());
            }
            return  resultPosAndInfo;
        }
        else {
            for (PositionAndInfo posAndInfo2 : overwriteMovesSet) {
                overwriteMoves.add(posAndInfo2.toIntArray());
            }
            return overwriteMoves;
        }
    }

    /**
     * This method calculates the stone count after a move was made. This is used by MCTS
     * @param map Map that value is calculated on
     * @param playerNr number of the player that would make the move
     * @param posToMoveTo position the player would place the stone
     * @return double that represents the relation between the own stones and the enemy ones
     */
    public static double getStoneCountAfterMove(Map map, int playerNr, int[] posToMoveTo){
        int myStoneCount = 0; //start value are the current stones of the player and the one we're going to set to
        int enemyStoneCount = 0;
        Arrow[] arrows;
        int[] posAndR;
        char charAtPos = map.map[posToMoveTo[1]][posToMoveTo[0]];
        boolean overwriteMove = false;
        int i;

        if (charAtPos != '0' && Character.isDigit(charAtPos)) overwriteMove = true;

        if (!overwriteMove) myStoneCount++; //when it's no overwrite move the position gets colored
        if (overwriteMove && charAtPos != playerNr+'0') myStoneCount++; //if it's an OverWrite move, and we overwrite an enemies stone

        if (useArrows){
            arrows = map.AffectedArrows[posToMoveTo[1]][posToMoveTo[0]][playerNr-1];

            for (Arrow arrow : arrows){
                if (arrow == null) continue;

                if (overwriteMove){
                    //get index at which position in the arrow the position we set to is
                    for (i = arrow.positionsWithDirection.size()-1; i > 0; i--){
                        posAndR = arrow.positionsWithDirection.get(i);
                        if (posToMoveTo[0] == posAndR[0] && posToMoveTo[1] == posAndR[1]) break;
                    }

                    myStoneCount += i - 1; //-1 because of the start position, -1 because we don't color the position we set to and +1 because it's an index
                }
                else if (arrow.createsValidMove){
                    myStoneCount += arrow.positionsWithDirection.size()-2; //-2 because the end position was already counted and the start position wouldn't be counted
                }
            }
        }

        for (int enemyNr = 1; enemyNr <= map.getAnzPlayers(); enemyNr++){
            if (enemyNr == playerNr) continue;
            enemyStoneCount += map.getCountOfStonesOfPlayer(enemyNr);
        }

        if (overwriteMove) {
            if (charAtPos == playerNr+'0') enemyStoneCount -= myStoneCount-1; //the positions I color without the one I set to
            else enemyStoneCount -= myStoneCount; //the positions I color with the position I set to
        }
        else {
            enemyStoneCount -= myStoneCount-1; //the positions I color without the one I set to
        }

        myStoneCount += map.getCountOfStonesOfPlayer(playerNr);

        return (double) myStoneCount / ((double)enemyStoneCount / (map.staticMap.anzPlayers - 1));
    }

    /**
     * This method returns the count of moves the current player can make
     * @param map Map on wich that is calculated
     * @param timed boolean if the method is times
     * @param upperTimeLimit if timed is enabled this is the time limit the method needs to considder
     * @return count of moves the current player can make
     * @throws ExceptionWithMove throws exception when the time is up.
     */
    public static int getCountOfMovesForPlayer(Map map, boolean timed, long upperTimeLimit) throws ExceptionWithMove {
        int count = 0;

        if (useArrows){
            return map.ValidMoves.get(map.getCurrentlyPlayingI()-1).size();
        }
        else {
            return getValidMoves(map, timed, false, false, upperTimeLimit, null).size();
        }
    }
}


