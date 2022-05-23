package src;

import java.util.*;

public class Map{

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

    //Valid Moves Varables
    private final ArrayList<HashMap<Position, Integer>> ValidMoves;
    private final ArrayList<HashMap<Position, Integer>> OverwriteMoves;

    //Arrow Data Structures
    private Arrow[][][][] AffectedArrows;
    private Arrow[][][] StartingArrows; //TODO: could be 2 elements shorter on each side


    public Map(StaticMap staticMap){
        //static Map
        this.staticMap = staticMap;

        //map
        map = new char[staticMap.height][staticMap.width];
        for (int i = 0; i < staticMap.height; i++){
            map[i] = staticMap.map[i].clone();
        }

        //General Map info + disualified Players
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
     * Copy constructor
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
        overwriteStonesPerPlayer = mapToCopy.overwriteStonesPerPlayer.clone(); //shallow clone because we need a new Array TODO: check that
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

        //Valid Moves Varables
        ValidMoves = new ArrayList<>(staticMap.anzPlayers);
        OverwriteMoves = new ArrayList<>(staticMap.anzPlayers);
        //initValidMoveArrays();

        for (int playerNr = 0; playerNr < staticMap.anzPlayers; playerNr++) {
            ValidMoves.add( (HashMap<Position, Integer>) mapToCopy.ValidMoves.get(playerNr).clone() );
            OverwriteMoves.add( (HashMap<Position, Integer>) mapToCopy.OverwriteMoves.get(playerNr).clone() );
        }

        //  only needed in phase one because in phase two the arrows aren't needed
        if (phaseOne) {
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
    
    // PUBLIC METHODES

    /**
     * Returns Infos, Map and Transitions in a formatted String
     */
    public String toString() {
        StringBuilder mapString = new StringBuilder();

        appendMapInfosForStringBuilder(mapString);

        mapString.append(Arrays.deepToString(map).replaceAll("],", "]\n"));
        mapString.append("\n\n");
        
        mapString.append(Transitions.AllToString(staticMap.transitions));
        
        return mapString.toString();
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

                //add char itself
                bufferString += currChar;

                //reset color
                if (useColors) bufferString += ANSI_RESET;

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
                buffer = AffectedArrows[y][x][staticMap.anzPlayers-1];
                //shift all arrow lists one further
                for (int playerNr = staticMap.anzPlayers-1; playerNr >= 1; playerNr--) {
                    AffectedArrows[y][x][playerNr] = AffectedArrows[y][x][playerNr-1];
                }
                //set elements of first player to the ones from the last one
                AffectedArrows[y][x][0] = buffer;
            }
        }

        //colors the new stones of the player in its color
        for (int playerNr = 1; playerNr <= staticMap.anzPlayers; playerNr++){
            for (Position pos : stonesPerPlayer.get(playerNr-1)){
                setCharAtWithoutSetUpdate(pos, (char)('0'+playerNr));
            }
        }
    }

    /* GETTER */
    /**
     * @param x x coordinate
     * @param y y coordinate
     * @return Returns the Character at the given x and y position in the Map. If it is out of boundaries it returns '-'
     */
    public char getCharAt(int x, int y){
        //check boundaries
        if (x >= staticMap.width || y >= staticMap.height || x < 0 || y < 0) return '-';
        //return value
        return map[y][x];
    }

    public char getCharAt(Position pos){
        //check boundaries
        if (pos.x > staticMap.width || pos.y > staticMap.height || pos.x < 0 || pos.y < 0) return '-';
        //return value
        return map[pos.y][pos.x];
    }

    public int getAnzPlayers() {
        return staticMap.anzPlayers;
    }

    public int getExplosionRadius() {
        return staticMap.explosionRadius;
    }

    public int getHeight() {
        return staticMap.height;
    }

    public int getWidth() {
        return staticMap.width;
    }

    public boolean checkForTransition(Position pos, int rotation){
        return staticMap.checkForTransition(pos, rotation);
    }

    public int getCurrentlyPlayingI() {
        return currentlyPlaying;
    }
    public char getCurrentlyPlayingC() {
        return (char) ('0'+currentlyPlaying);
    }

    public int getOverwriteStonesForPlayer(int playerId) {
        return overwriteStonesPerPlayer[playerId-1];
    }

    public int getBombsForPlayer(int playerId) {
        return bombsPerPlayer[playerId-1];
    }

    public ArrayList<Position> getStonesOfPlayer(int playerNr){
        if (playerNr < 0 || playerNr > staticMap.anzPlayers) {
            System.err.println("Player with that number isn't in the Game");
            return null;
        }

        ArrayList<Position> result = new ArrayList<>( stonesPerPlayer.get(playerNr-1) );

        for (int i = 0; i < result.size(); i++){
            result.set(i, result.get(i).clone());
        }

        return result;
    }

    public int getCountOfStonesOfPlayer(int playerNr){
        return stonesPerPlayer.get(playerNr-1).size();
    }

    public ArrayList<Position> getExpansionFields(){
        ArrayList<Position> result = new ArrayList<>( expansionFields );

        for (int i = 0; i < result.size(); i++){
            result.set(i, result.get(i).clone());
        }

        return result;
    }

    public ArrayList<int[]> getValidMoves(boolean phaseOne){
        return getValidMoves(currentlyPlaying, phaseOne);
    }

    public ArrayList<int[]> getValidMoves(int playerId, boolean phaseOne){
        ArrayList<int[]> resultList = new ArrayList<>();
        char currChar;

        if (phaseOne) {
            //get Valid Moves
            for (Position pos : ValidMoves.get(playerId - 1).keySet()) {
                currChar = getCharAt(pos);
                switch (currChar) {
                    case '0':
                    case 'i':
                        resultList.add(new int[]{pos.x, pos.y, 0});
                        break;
                    case 'b':
                        resultList.add(new int[]{pos.x, pos.y, 20});
                        resultList.add(new int[]{pos.x, pos.y, 21});
                        break;
                    case 'c':
                        for (int playerNr = 1; playerNr <= staticMap.anzPlayers; playerNr++) {
                            if (playerNr == playerId) continue;
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
                    resultList.add(new int[]{pos.x, pos.y, 0});
                }

                for (Position pos : expansionFields) {
                    resultList.add(new int[]{pos.x, pos.y, 0});
                }
            }
        }

        //if we are in the bomb Phase
        else {
            char fieldValue;
            int accuracy = 2;

            //if player has no bomb's return empty array
            if (getBombsForPlayer(playerId) == 0) {
                return resultList; //returns empty array
            }

            while (resultList.isEmpty()) {
                //gets the possible positions to set a bomb at
                for (int y = 0; y < staticMap.height; y += accuracy) {
                    for (int x = 0; x < staticMap.width; x += accuracy) {
                        fieldValue = getCharAt(x, y);
                        if (fieldValue != '-' && fieldValue != 't') {
                            resultList.add(new int[]{x, y});
                        }
                    }
                }
                accuracy = 1; //could be changed to accuracy-- if it should have more proceduaral steps
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
        if (x >= staticMap.width || y >= staticMap.height || x < 0 || y < 0) {
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
        if (charAtPos != '-' && charAtPos != '+' && charAtPos != 't' && charToChangeTo != '-' && charToChangeTo != '+' && charToChangeTo != 't' && charToChangeTo != '0') {
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
        //TODO: check if player can make a move
        currentlyPlaying++;
        if (currentlyPlaying == staticMap.anzPlayers+1) currentlyPlaying = 1;
    }

    public void setPlayer(int PlayerID) {
        currentlyPlaying = PlayerID;
    }

    public void disqualifyPlayer(int playerNr){
        disqualifiedPlayers[playerNr-1] = false;
    }

    //PRIVATE METHODS

    //  Methodes for Move carry along

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

        System.out.println("Check if right: [List: " + checkForReferenceInAffectedArrows() + ", Valid Moves: " + checkValidMoves() + ", Overwrite Moves " + checkOverwriteMoves() + "] ");
        return;
    }

    private Arrow deleteArrowFrom(Arrow oldArrow, int arrowOfPlayer, int from, int arrowPointedOnPlayer){
        int[] posAndR;
        Position currPos;

        Arrow copyOfArrow = oldArrow.clone();

        //delete valid move created by this arrow
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
                    AffectedArrows[posAndR[1]][posAndR[0]][arrowOfPlayer - 1][posAndR[2]] = null;
                }
            }

            //delete overwrite moves created by this arrow - index 0 and 1 don't create an overwrite move - index = size-1 sometimes
            if (counter >= 2){
                currPos = new Position(posAndR[0],posAndR[1]);
                //checks if the position the arrow points to is one of the players stones -> if so delete overwrite move
                if (counter == oldArrow.positionsWithDirection.size()-1){
                    if (arrowPointedOnPlayer == arrowOfPlayer+'0'){
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
                    AffectedArrows[posAndR[1]][posAndR[0]][arrowOfPlayer - 1][posAndR[2]] = copyOfArrow;
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
            //  create Arrow from last position on foreward
            createArrowFrom(currArrow, arrowOfPlayer);
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

    private void createArrowFrom(Arrow arrow, int arrowOfPlayer){
        int[] posAndR = arrow.positionsWithDirection.get( arrow.positionsWithDirection.size()-1 );
        int direction = posAndR[2];
        Position currPos = new Position(posAndR[0], posAndR[1]);
        Position StartingPos = currPos.clone();
        Integer newR = direction;
        Integer nextR;
        char currChar;
        boolean firstStep = true;

        //check if it continiues the arrow
        if (arrow.positionsWithDirection.size() >= 2){
            firstStep = false;
        }

        //go in one direction
        while (true) {
            //does one step
            nextR = staticMap.doAStep(currPos, newR); //currPos is changed here

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
                newR = staticMap.doAStep(currPos, (direction+4)%8);
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
                    addOverwritePosition(arrowOfPlayer, currPos.clone());
                }
                break;
            }

            //if it's another player or 'x'
            arrow.positionsWithDirection.add(new int[]{currPos.x, currPos.y, newR});

            //if it's not the first step an overwrite move can be made
            if (!firstStep) {
                addOverwritePosition(arrowOfPlayer, currPos.clone());
            }

            firstStep = false;
        }

        //add last position after break
        arrow.positionsWithDirection.add(new int[]{currPos.x, currPos.y, newR});
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
        //  increase count of arrows poining to this direction
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

    //      Checking Methodes for Move Carry Along

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

    private boolean checkForReferenceInAffectedArrows(){
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

    private boolean checkValidMoves(){
        ArrayList<Arrow> validArows = getAllValidArrows();

        int[] posAndR;
        boolean correct = true;
        boolean isOneOfThem;
        int arrowOfPlayer;

        for (Arrow arrow : validArows){
            //get player
            posAndR = arrow.positionsWithDirection.get(0);
            arrowOfPlayer = map[posAndR[1]][posAndR[0]]-'0';
            //get last position
            posAndR = arrow.positionsWithDirection.get( arrow.positionsWithDirection.size()-1 );
            //reset
            isOneOfThem = false;

            for (Position validPos : ValidMoves.get(arrowOfPlayer-1).keySet()){
                if (validPos.x == posAndR[0] && validPos.y == posAndR[1]) isOneOfThem = true;
            }

            if (!isOneOfThem) {
                return false;
            }
        }
        return true;
    }

    private boolean checkOverwriteMoves(){
        ArrayList<Arrow> allArrows = getAllArrows();
        int[] posAndR;
        boolean correct = true;
        boolean isOneOfThem;
        int arrowOfPlayer;

        //for every arrow
        for (Arrow arrow : allArrows){
            //get player
            posAndR = arrow.positionsWithDirection.get(0);
            arrowOfPlayer = map[posAndR[1]][posAndR[0]]-'0';

            //for every overwrite position
            for (int i = 2; i < arrow.positionsWithDirection.size(); i++) {
                //get position
                posAndR = arrow.positionsWithDirection.get(i);
                //reset
                isOneOfThem = false;

                if (i == arrow.positionsWithDirection.size()-1){
                    char charAtPos = map[posAndR[1]][posAndR[0]];
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

    //  Other

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


    //function to color the map

    /**
     * colors the map when the keystone is placed in the specified position
     * @param pos position where the keystone is placed
     * @param map the map on wich it is placed
     */
    public static void colorMap(Position pos, Map map){
        Position StartingPos;
        Position currPos;
        Integer newR;
        boolean wasFirstStep;
        boolean foundEnd;
        char currChar;
        LinkedHashSet<Position> positionsToColor = new LinkedHashSet<>(); //doesn't store duplicates
        ArrayList<Position> positionsAlongOneDirection;

        if (map.getCharAt(pos) == 'x' && map.getOverwriteStonesForPlayer(map.getCurrentlyPlayingI()) > 0) {
            map.setCharAt(pos.x, pos.y, map.getCurrentlyPlayingC());
        }

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

        //colors the positions
        for (Position posToColor : positionsToColor) {
            map.setCharAt(posToColor.x, posToColor.y, map.getCurrentlyPlayingC());
        }
    }

    //function to do a step on the map

    public Integer doAStep(Position pos, int r){
        return staticMap.doAStep(pos,r);
    }
}
