package src;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;

public class Heuristic {
    private final boolean printOn;

    private Map map; //is automatically updated because of the reference here
    //the color of the player for wich the map is rated
    private final int myColorI;
    private final char myColorC;
    //the matrix that rates the different fields
    private double[][] matrix;
    //relevant information
    HashSet<Position> uncapturableFields = new HashSet<>();

    //heuristic values
    private final int base = 3;

    //booleans to enable or disable certain elements of the heuristic
    private boolean countStones;
    private boolean countMoves;
    private boolean useFieldValues;

    //multipliers
    public static final int countOfMultipliers = 4;
    private double stoneCountMultiplier;
    private double moveCountMultiplier;
    private double fieldValueMultiplier;
    private double edgeMultiplier;


    /**
     * Constructor - initializes map and my color.
     * Calculates static infos about the map
     * @param map the map in any state. Only the static information are relevant
     * @param myColor the number(color) of the player for wich the map is rated - doesn't change for the client
     * @param printOn boolean that defines if the heuristic should print relevant infos
     * @param multiplier list of double values to define the multipliers for the different heuristically evaluations
     */
    public Heuristic(Map map, int myColor, boolean printOn, double[] multiplier){
        this.printOn = printOn;
        this.map = map;
        this.myColorI = myColor;
        this.myColorC = (char)('0'+myColor);
        matrix = new double[map.getHeight()][map.getWidth()];

        //set multiplier
        //default multiplier
        stoneCountMultiplier = 4;
        moveCountMultiplier = 8;
        fieldValueMultiplier = 1;
        edgeMultiplier = 6;
        //corresponding default enables
        countMoves = true;
        countStones = true;
        useFieldValues = true;

        //set given parameters
        if (multiplier != null) {
            for (int i = 0; i < multiplier.length; i++) {
                switch (i) {
                    case 0:
                        stoneCountMultiplier = multiplier[i];
                        if (stoneCountMultiplier == 0) countStones = false;
                        break;
                    case 1:
                        moveCountMultiplier = multiplier[i];
                        if (moveCountMultiplier == 0) countMoves = false;
                        break;
                    case 2:
                        fieldValueMultiplier = multiplier[i];
                        if (fieldValueMultiplier == 0) useFieldValues = false;
                    case 3:
                        edgeMultiplier = multiplier[i];
                }
            }
        }


        setStaticInfos();
    }

    /**
     * Function to evaluate the map that was given the constructor (call by reference)
     * @return returns the value of the map
     */
    public double evaluate(boolean phaseOne){
        double countOfStonesEvaluation = 0;
        double countOfMovesEvaluation = 0;
        double averageFieldValue = 0;
        double result = 0;

        if (!phaseOne){
            stoneCountMultiplier = 1;
            countStones = true;
            countMoves = false;
            useFieldValues = false;
        }

        //update relevant infos
        if (countStones) countOfStonesEvaluation = countStones();

        if (countMoves) countOfMovesEvaluation = countMoves();

        if (useFieldValues) averageFieldValue = getFieldValues();


        if (countStones) result += countOfStonesEvaluation * stoneCountMultiplier;
        if (countMoves) result += countOfMovesEvaluation * moveCountMultiplier;
        if (useFieldValues) result += averageFieldValue * fieldValueMultiplier;

        if (printOn) {
            System.out.println("Value for stone count = " + countOfStonesEvaluation * stoneCountMultiplier);
            System.out.println("Value for move count = " + countOfMovesEvaluation * moveCountMultiplier);
            System.out.println("Value for field Values = " + averageFieldValue * fieldValueMultiplier);
        }

        //value
        return result;
    }

    public static double fastEvaluate(Map map, int myPlayerNr){

        int myStoneCout = map.getCountOfStonesOfPlayer(myPlayerNr);
        int enemyStoneCount = 0;
        double result;

        for (int playerNr = 1; playerNr < map.getAnzPlayers(); playerNr++){
            if (playerNr == myPlayerNr) continue;
            enemyStoneCount += map.getCountOfStonesOfPlayer(playerNr);
        }

        result = (double)myStoneCout / ((double)enemyStoneCount/ (map.getAnzPlayers() - 1));
        return result;
    }

    public static ArrayList<Double> fastEvalAll(ArrayList<Map> mapList, int myPlayerNr){
        ArrayList<Double> result = new ArrayList<>();
        for (Map map : mapList){
            result.add(fastEvaluate(map, myPlayerNr));
        }
        return result;
    }

    /**
     * prints out the evaluation matrix
     */
    public void printMatrix(){
        System.out.println("Matrix of map:");
        for (int y = 0; y < matrix.length; y++){
            for (int x = 0; x < matrix[y].length; x++){
                if (matrix[y][x] != Double.NEGATIVE_INFINITY) System.out.printf("%4d", (int)matrix[y][x]);
                else  System.out.printf("%4c", '-');
            }
            System.out.println();
        }
        System.out.println();
    }

    /**
     * Updates the Map object (if it was cloned for example).
     * Also updates th dynamic infos.
     * The general Map needs to be the same as the one the heuristic got initialized with
     * @param mapToUpdateWith The new Map object to update the infos with
     */
    public void updateMap(Map mapToUpdateWith){
        this.map = mapToUpdateWith;
        //setDynamicInfos(); not needed because it's called in evaluate
    }

    /**
     * Calculates placement of all players
     * @return returns a value according to the placement of the player
     */
    public double placePlayers(){
        int placement = 1;
        int myScore = map.getCountOfStonesOfPlayer(myColorI);

        for (int playerNr = 1; playerNr <= map.getAnzPlayers(); playerNr++) {
            if (playerNr == myColorI) continue;
            if (map.getCountOfStonesOfPlayer(playerNr) > myScore){
                placement++;
            }
        }

        return 1 - ((double)placement - 1) / ((double)map.getAnzPlayers() -1);
    }


    /**
     * calculates and sets static information about the map.
     * initializes the matrix of values for the map
     *
     * Current heuristik implementations:
     *      check for backed up outgoings
     *
     * Information that are currently saved:
     *      bonus
     *      inversion
     *      choice
     */
    private void setStaticInfos(){
        char currChar;
        Position currPos = new Position(0,0); //position that goes through whole map

        //evaluate every position by its properties
        for (int y = 0; y < map.getHeight(); y++){
            for (int x = 0; x < map.getWidth(); x++) {
                //set new position
                currPos.x = x;
                currPos.y = y;
                //get char at position
                currChar = map.getCharAt(x,y);

                //if it's a valid field
                if (currChar != '-' && currChar != 't'){
                    //calculate value of position
                    matrix[y][x] += calculateValueOfPosition(currPos, currChar);
                }
                //if it's a wall
                else {
                    matrix[y][x] = Double.NEGATIVE_INFINITY;
                }
            }
        }
        if (printOn) System.out.println("Values of Positions");
        if (printOn) printMatrix();

        //evaluate every position by its neighbours
        addWaveMatrix();

        if (printOn) System.out.println("Added Waves");
        if (printOn) printMatrix();

    }

    //methods to return dynamic infos
    private double getFieldValues() {
        double averageFieldValue = 0;

        if (map.getCountOfStonesOfPlayer(myColorI) == 0) return averageFieldValue;

        for (Position pos :  map.getStonesOfPlayer(myColorI)) {
            averageFieldValue += matrix[pos.y][pos.x];
        }

        return averageFieldValue;
    }

    private double countMoves() {
        //Variables
        int myPossibleMoves = 0;
        int possibleMovesOfEnemys = 0;
        double enemyMovesAverage;
        double countOfMovesEvaluation;

        //gets possible moves of all players and adds them to the corresponding move counter
        for (int i = 1; i <= map.getAnzPlayers(); i++) {
            if (myColorI == map.getCurrentlyPlayingI()) {
                myPossibleMoves = Map.getValidMoves(map).size();
            } else {
                possibleMovesOfEnemys += Map.getValidMoves(map).size();
            }
            map.nextPlayer();
        } //resets to currently playing

        //get percentages out of it
        enemyMovesAverage = (double)possibleMovesOfEnemys/((double)map.getAnzPlayers()-1);

        //set possible moves percentage
        countOfMovesEvaluation = (myPossibleMoves - enemyMovesAverage);

        return countOfMovesEvaluation;
    }

    private double countStones() {
        //Variables
        int countOfOwnStones;
        int countOfEnemyStones = 0;
        double enemyStonesAverage;
        double countOfStonesEvaluation;

        //gets count of own stones
        countOfOwnStones = map.getCountOfStonesOfPlayer(myColorI);
        //gets count of enemy stones
        for (int playerNr = 1; playerNr <= map.getAnzPlayers(); playerNr++) {
            if (playerNr == myColorI) continue;
            countOfEnemyStones += map.getCountOfStonesOfPlayer(playerNr);
        }

        //get percentages out of it
        enemyStonesAverage = (double)countOfEnemyStones/((double)map.getAnzPlayers()-1);

        //set stone percentage

        countOfStonesEvaluation = (countOfOwnStones - enemyStonesAverage);
        //else countOfStonesEvaluation = 500; //if enemy has no stones you have 100% stones

        return countOfStonesEvaluation;
    }


    //methods to evaluate value of a Position by properties
    //  main method
    private double calculateValueOfPosition(Position pos, char charAtPos){
        double result = 0;
        boolean[] outgoingDirections;
        boolean isCapturable;
        int[] reachableFields;
        int sumOfReachableFields = 0;
        int backedOuOutgoings;

        //set values
        outgoingDirections = getOutgoingDirections(pos);
        isCapturable = isCapturable(outgoingDirections);
        reachableFields = checkReachableFields(pos, outgoingDirections); //because of the list a distinction between directions is possible
        for (int a : reachableFields) sumOfReachableFields += a;
        backedOuOutgoings = getBackedUpOutgoings(outgoingDirections);

        //get evaluation
        result = sumOfReachableFields;
        // or: result = Math.pow(base, backedOuOutgoings);
        result += bonusFieldValue(charAtPos);
        if (!isCapturable) result *= edgeMultiplier;

        return result;
    }

    //  returning information
    private boolean[] getOutgoingDirections(Position savedPos){
        boolean[] outgoingDirections = new boolean[]{true, true, true, true, true, true, true, true};
        char charAtPos;
        Position pos;

        for (int r = 0; r <= 7; r++){
            //check first direction
            pos = savedPos.clone();
            pos = Position.goInR(pos,r);

            charAtPos = map.getCharAt(pos);
            //check if there's a wall
            if(charAtPos == '-') {
                outgoingDirections[r] = false;
            }
            //check if there's a transition and if it's relevant
            else if(charAtPos == 't'){
                if (!map.checkForTransition(savedPos,r)) outgoingDirections[r] = false;
            }
        }
        return outgoingDirections;
    }

    private boolean isCapturable(boolean[] outgoingDirections){

        int blockedAxisCount = 0;

        for (int r = 0; r < 4; r++){
            if (!outgoingDirections[r] || !outgoingDirections[(r+4)%8]) blockedAxisCount++;
        }

        if (blockedAxisCount ==4) return false;
        else return true;
    }

    private int[] checkReachableFields(Position savedPos, boolean[] outgoingDirections) {
        Integer newR;
        int[] reachableFields = new int[8];
        Position pos;

        for (int r = 0; r <= 7; r++){
            pos = savedPos.clone();
            newR = r;
            while (true) {
                newR = map.doAStep(pos, newR);

                if (newR == null || ( pos.equals(savedPos) && newR == r )){
                    break;
                }
                reachableFields[r]++;

                //safety to not run infinitely
                if (reachableFields[r] > map.getHeight()*map.getWidth()) {
                    System.err.println("This shouldn't happen - checkReachableFields() ran into an infinite loop");
                    reachableFields[r] = -1;
                    break;
                }
            }
        }

        return reachableFields;
    }

    private int getBackedUpOutgoings(boolean[] outgoingDirections) {
        int backedUpOutgoings = 0;

        for (int r = 0; r < 4; r++){
            if (outgoingDirections[r] ^ outgoingDirections[(r+4)%8]) backedUpOutgoings++; // xor - on one side needs to be free the other needs to be a wall
        }

        return backedUpOutgoings;
    }

    //  returning evaluations
    private int bonusFieldValue(char charAtPos){
        switch (charAtPos){
            case 'b':
                return 20;
            case 'c':
                return 40;
            case 'i':
            case 'x':
            default: //includes 0-8
                return 0;
        }
    }


    //methods to evaluate value of a Position by Neighboring positions

    /**
     * takes the greater values of the matrix and creates waves/ rings of alternating negative and positive decreasing values around it
     */
    private void addWaveMatrix(){
        //adjustable values
        int lowerLimit = 0;
        int maxWaveCount = 3;
        int divisor = base;
        //variables
        int[][] waveMatrix = new int[matrix.length][matrix[0].length];
        ArrayList<Position> highValues = new ArrayList<>();
        Position currPos = new Position(0,0);
        double currValue;

        //goes through every position of the map
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {

                //set new position
                currPos.x = x;
                currPos.y = y;
                //get int at position
                currValue = matrix[y][x];

                if(currValue >= lowerLimit){
                    highValues.add(currPos.clone());
                }
            }
        }

        for (Position pos : highValues){
            createWave(waveMatrix, pos, maxWaveCount, divisor);
        }


        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                if (matrix[y][x] != Double.NEGATIVE_INFINITY) matrix[y][x] += waveMatrix[y][x];
                if (printOn) System.out.printf("%4d", waveMatrix[y][x]);
            }
            if (printOn) System.out.println();
        }
        if (printOn) System.out.println();

    }

    /**
     * creates the waves around one position
     * @param waveMatrix the matrix to create the waves in
     * @param pos the position around wich the waves are created
     */
    private void createWave(int[][] waveMatrix, Position pos, int maxWaveCount, int divisor){
        int x1, x2, y1, y2;
        x1 = pos.x;
        x2 = pos.x;
        y1 = pos.y;
        y2 = pos.y;
        int loopSign = -1;
        double value = matrix[pos.y][pos.x]/divisor; //sets value to value of field/divisor to start
        int waveCount = 1;

        while(value >= divisor && waveCount <= maxWaveCount) {
            x1-=1;
            x2+=1;
            y1-=1;
            y2+=1;

            //  *   *   *   * if
            //  *           * else
            //  *           * else
            //  *           * else
            //  *   *   *   * if
            for (int yi = y1; yi <= y2; yi++) {
                if (yi == y1 || yi == y2) { //first and last one is row
                    for (int xi = x1; xi <= x2; xi++) {
                        if(yi >= 0 && yi < waveMatrix.length && xi >= 0 && xi < waveMatrix[0].length)  waveMatrix[yi][xi] += loopSign * Math.round(value); //if catches indices that went over the edge
                    }
                }
                else { //column between the rows
                    for (int xi : new int[]{x1,x2}){
                        if(yi >= 0 && yi < waveMatrix.length && xi >= 0 && xi < waveMatrix[0].length) waveMatrix[yi][xi] += loopSign * Math.round(value); //if catches indices that went over the edge
                    }
                }
            }
            loopSign *= -1; //swap loop sign
            value /= divisor;
            waveCount++;
        }
    }
}
