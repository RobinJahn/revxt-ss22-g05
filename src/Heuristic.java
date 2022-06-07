package src;

import java.util.*;
import java.util.concurrent.TimeoutException;

public class Heuristic {
    private final boolean printOn;
    private final boolean extendedPrint;

    private Map map; //is automatically updated because of the reference here
    //the color of the player for which the map is rated
    private final int myColorI;
    private final char myColorC;
    //the matrix that rates the different fields
    private double[][] matrix;
    private double[][] fieldValueMatrix;
    private double[][] waveMatrix;
    private double[][] edgeMatrix;

    //heuristic values
    private final int base = 3;

    //booleans to enable or disable certain elements of the heuristic
    private boolean countStones;
    private boolean countMoves;
    private boolean useFieldValues;
    private boolean useEdges;
    private boolean useWaves;

    //multipliers
    public static final int countOfMultipliers = 5;
    public double[][] multiplier;

    private double stoneCountMultiplier;
    private double moveCountMultiplier;
    private double fieldValueMultiplier;
    private double edgeMultiplier;
    private double waveCount;

    //Staging
    private int stageNumber;


    /**
     * Constructor - initializes map and my color.
     * Calculates static infos about the map
     * @param map the map in any state. Only the static information are relevant
     * @param myColor the number(color) of the player for which the map is rated - doesn't change for the client
     * @param printOn boolean that defines if the heuristic should print relevant infos
     * @param extendedPrint prints more information. Like the Matrix
     * @param multiplier list of double values to define the multipliers for the different heuristically evaluations
     */
    public Heuristic(Map map, int myColor, boolean printOn, boolean extendedPrint, double[][] multiplier){
        this.printOn = printOn;
        this.extendedPrint = extendedPrint;
        this.map = map;
        this.myColorI = myColor;
        this.myColorC = (char)('0'+myColor);
        matrix = new double[map.getHeight()][map.getWidth()];

        this.multiplier = multiplier;
        stageNumber = getStage(true);
        setMultipliers(stageNumber);

        if (printOn){
            System.out.println("Multipliers per phase");
            System.out.println(Arrays.deepToString(this.multiplier).replace("],", "],\n"));
        }

        initMatrix();
        initEdgeMatrixAndFieldValueMatrix(); //needs multipliers

        setStaticInfos(); //needs initialized matrices
    }

    private void setMultipliers(int phase) {
        //set multiplier

        //default multipliers
        if (multiplier == null) {
            multiplier = new double[][]{
                    {1, 9, 4, 9, 5},
                    {5, 2, 0, 0, 7},
                    {2, 1, 8, 3, 2}
            };
        }

        //bomb phase multipliers
        if (phase == 4){
            stoneCountMultiplier = 1;
            moveCountMultiplier = 0;
            fieldValueMultiplier = 0;
            edgeMultiplier = 0;
            waveCount = 0;

            countStones = true;
            countMoves = false;
            useFieldValues = false;
            useEdges = false;
            useWaves = false;
            return;
        }

        //set given parameters
        for (int i = 0; i < multiplier[phase-1].length; i++) {
            switch (i) {
                case 0:
                    stoneCountMultiplier = multiplier[phase-1][i];
                    countStones = stoneCountMultiplier != 0;
                    break;
                case 1:
                    moveCountMultiplier = multiplier[phase-1][i];
                    countMoves = moveCountMultiplier != 0;
                    break;
                case 2:
                    fieldValueMultiplier = multiplier[phase-1][i];
                    useFieldValues = fieldValueMultiplier != 0;
                    break;
                case 3:
                    edgeMultiplier = multiplier[phase-1][i];
                    useEdges = edgeMultiplier != 0 ;
                    break;
                case 4:
                    waveCount = multiplier[phase-1][i];
                    useWaves = waveCount != 0;
                    break;
            }
        }
    }

    //SETTER -----------------------------------------------------------------------------------------------------------

    public void setStoneCountMultiplier(double stoneCountMultiplier) {
        this.stoneCountMultiplier = stoneCountMultiplier;
    }

    public void setMoveCountMultiplier(double moveCountMultiplier) {
        this.moveCountMultiplier = moveCountMultiplier;
    }

    public void setFieldValueMultiplier(double fieldValueMultiplier) {
        this.fieldValueMultiplier = fieldValueMultiplier;
    }

    public void setEdgeMultiplier(double edgeMultiplier) {
        this.edgeMultiplier = edgeMultiplier;
    }

    public void setWaveCount(int waveCount) {
        this.waveCount = waveCount;
    }



    //UPDATES ----------------------------------------------------------------------------------------------------------

    /**
     * Function updates the Heuristic Multipliers according to the current GameStage
     */
    public void updateHeuristicMultipliers(boolean phaseOne)
    {
        int stageNumber = getStage(phaseOne);

        //if stage number changed
        if (this.stageNumber != stageNumber)
        {
            this.stageNumber = stageNumber;

            setMultipliers(stageNumber);

            setStaticInfos();
        }
    }

    /**
     * Updates the Map object (if it was cloned for example).
     * Also updates th dynamic infos.
     * The general Map needs to be the same as the one the heuristic got initialized with
     * @param mapToUpdateWith The new Map object to update the infos with
     */
    public void updateMap(Map mapToUpdateWith){
        this.map = mapToUpdateWith;
    }

    //GETTER -----------------------------------------------------------------------------------------------------------

    /**
     * Function to evaluate the map that was given the constructor (call by reference)
     * @return returns the value of the map
     */
    public double evaluate(boolean phaseOne,boolean timed, boolean ServerLog, long UpperTimeLimit) throws TimeoutException{
        double countOfStonesEvaluation = 0;
        double countOfMovesEvaluation = 0;
        double averageFieldValue = 0;
        double result = 0;

        updateHeuristicMultipliers(phaseOne);

        //count stones
        if (countStones) {
            if (phaseOne) countOfStonesEvaluation = countStones();
            else countOfStonesEvaluation = countStonesBombPhase();
        }

        //out of time ?
        if(timed && (UpperTimeLimit - System.nanoTime()<0)) {
            if (printOn||ServerLog) System.out.println("Out of time (Heuristic.evaluate - After get countStones)");
            throw new TimeoutException();
        }

        //count moves
        if (countMoves) countOfMovesEvaluation = countMoves(timed,ServerLog,UpperTimeLimit);

        //out of time ?
        if(timed && (UpperTimeLimit - System.nanoTime()<0)) {
            if (printOn||ServerLog) System.out.println("Out of time (Heuristic.evaluate - After get count Moves)");
            throw new TimeoutException();
        }

        //add values of won fields
        if (useFieldValues || useEdges) averageFieldValue = getFieldValues(fieldValueMultiplier); //If use wave is enabled alone there are no waves

        //out of time ?
        if(timed && (UpperTimeLimit - System.nanoTime()<0)) {
            if (printOn||ServerLog) System.out.println("Out of time (Heuristic.evaluate - After get FieldValue)");
            throw new TimeoutException();
        }

        //calculate result
        if (countStones) result += countOfStonesEvaluation * stoneCountMultiplier;
        if (countMoves) result += countOfMovesEvaluation * moveCountMultiplier;
        if (useFieldValues || useEdges) result += averageFieldValue;

        //prints
        if (printOn) {
            System.out.println("Value for stone count = " + countOfStonesEvaluation * stoneCountMultiplier);
            System.out.println("Value for move count = " + countOfMovesEvaluation * moveCountMultiplier);
            System.out.println("Value for field Values = " + averageFieldValue);
        }

        //value
        return result;
    }

    public static double fastEvaluate(Map map, int myPlayerNr){

        int myStoneCount = map.getCountOfStonesOfPlayer(myPlayerNr);
        int enemyStoneCount = 0;
        double result;

        for (int playerNr = 1; playerNr < map.getAnzPlayers(); playerNr++){
            if (playerNr == myPlayerNr) continue;
            enemyStoneCount += map.getCountOfStonesOfPlayer(playerNr);
        }

        result = (double)myStoneCount / ((double)enemyStoneCount/ (map.getAnzPlayers() - 1));
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

    private int getStage(boolean phaseOne){

        int stageNumber = 1;
        double fillPercentage;

        if (!phaseOne) {
            stageNumber = 4;
            return stageNumber;
        }

        fillPercentage = map.getFillPercentage();

        if(fillPercentage > 0.5 && fillPercentage < 0.8)
        {
            stageNumber = 2;
        }
        else if(fillPercentage> 0.8)
        {
            stageNumber = 3;
        }

        return stageNumber;
    }

    //PRINT ------------------------------------------------------------------------------------------------------------

    /**
     * prints out the evaluation matrix
     */
    public void printMatrix(double[][] matrix){
        for (int y = 0; y < matrix.length; y++){
            for (int x = 0; x < matrix[y].length; x++){
                if (matrix[y][x] != Double.NEGATIVE_INFINITY && matrix[y][x] != Double.POSITIVE_INFINITY) System.out.printf("%8s", String.format("%4.2f", matrix[y][x]) );
                if (matrix[y][x] == Double.NEGATIVE_INFINITY) System.out.printf("%8s", "-");
                if (matrix[y][x] == Double.POSITIVE_INFINITY) System.out.printf("%8s", "+");
            }
            System.out.println();
        }
        System.out.println();
    }


    //HELPER -----------------------------------------------------------------------------------------------------------

    //  getter of dynamic evaluate call

    private double countMoves(boolean timed,boolean ServerLog, long UpperTimeLimit)throws TimeoutException {
        //Variables
        int myPossibleMoves = 0;
        int possibleMovesOfEnemies = 0;
        double countOfMovesEvaluation;

        //gets possible moves of all players and adds them to the corresponding move counter
        for (int i = 1; i <= map.getAnzPlayers(); i++) {

            //out of time ?
            if(timed && (UpperTimeLimit - System.nanoTime()<0)) {
                if (printOn||ServerLog) System.out.println("Out of time (Heuristic.evaluate - After get FieldValue)");
                throw new TimeoutException();
            }

            if (myColorI == map.getCurrentlyPlayingI()) {
                myPossibleMoves = Map.getValidMoves(map,timed,printOn,ServerLog,UpperTimeLimit).size();
            } else {
                possibleMovesOfEnemies += Map.getValidMoves(map,timed,printOn,ServerLog,UpperTimeLimit).size(); //TODO: Make valid Moves contain all moves from beginning on
            }
            map.nextPlayer();
        } //resets to currently playing


        countOfMovesEvaluation = (double)(myPossibleMoves * map.getAnzPlayers()) / (myPossibleMoves + possibleMovesOfEnemies); //actual formula: #myMoves in % / average moves per player in % -> (#myMoves/#allMoves ) / (1/#player)

        return countOfMovesEvaluation;
    }

    private double countStones() {
        //Variables
        int countOfOwnStones;
        int countOfEnemyStones = 0;
        double countOfStonesEvaluation;

        //gets count of own stones
        countOfOwnStones = map.getCountOfStonesOfPlayer(myColorI);

        //gets count of enemy stones
        for (int playerNr = 1; playerNr <= map.getAnzPlayers(); playerNr++) {
            if (playerNr == myColorI) continue;
            countOfEnemyStones += map.getCountOfStonesOfPlayer(playerNr);
        }

        countOfStonesEvaluation = (double)(countOfOwnStones * map.getAnzPlayers()) / (countOfEnemyStones + countOfOwnStones);  //actual formula: #myStones in % / average colored stones per player in % -> (#myStones/#coloredStones ) / (1/#player)

        return countOfStonesEvaluation;
    }

    private double countStonesBombPhase() {
        //Variables
        int ownStoneCount = map.getStonesOfPlayer(myColorI).size();

        int nearestEnemiesLower = 0;
        int nearestEnemiesUpper = 2500;

        int ownBombCount = map.getBombsForPlayer(myColorI);
        int enemyBombCount = 0;
        int enemiesWithLessStones = 0;

        int erg;
        //gets count of enemy stones
        if(printOn)
        {
            for (int playerNr = 1; playerNr <= map.getAnzPlayers(); playerNr++) {
                System.out.println("Player: "+ playerNr +" has " +map.getStonesOfPlayer(playerNr).size());
            }
        }
        for (int playerNr = 1; playerNr <= map.getAnzPlayers(); playerNr++) {

            if(myColorI!= playerNr && !map.getDisqualifiedPlayer(playerNr))
            {
                int countOfEnemy = map.getStonesOfPlayer(playerNr).size();
                if(countOfEnemy >= ownStoneCount && countOfEnemy < nearestEnemiesUpper)
                {
                    nearestEnemiesUpper = countOfEnemy;
                }
                else if(countOfEnemy<= ownStoneCount && countOfEnemy > nearestEnemiesLower)
                {
                    nearestEnemiesLower = countOfEnemy;
                }
                if(countOfEnemy< ownStoneCount)
                {
                    enemiesWithLessStones++;
                }
                enemyBombCount += map.getBombsForPlayer(playerNr);

            }
        }
        //We have the most stones => target the person with the second most stones
        if(nearestEnemiesUpper == 2500)
        {
            erg = 2500+(ownStoneCount - nearestEnemiesLower) +enemiesWithLessStones * 10000;
        }
        //We have the lowest amount of stones => target the second to last player
        else if( nearestEnemiesLower == 0 )
        {
            erg = ownStoneCount - nearestEnemiesUpper + enemiesWithLessStones * 10000;
        }
        //We have more or equal bombs than all the other players combined => Aggressive Bombing for better Position
        //=> Minimize Distance
        // IF we can overtake someone we will do so
        else if(ownBombCount>=enemyBombCount)
        {
            erg = -3*(nearestEnemiesUpper - ownStoneCount) + enemiesWithLessStones * 10000;
        }
        //We are somewhere between => minimize distance to Upper and Maximize Distance to Lower
        // Here we could add Parameters if we want to encourage a behavior
        // Playing it Safe => Maximize Distance *3
        // IF we can overtake someone we will do so
        else
        {
            erg = -(nearestEnemiesUpper - ownStoneCount) + 3*(ownStoneCount - nearestEnemiesLower) + enemiesWithLessStones * 10000;
        }
        if(printOn) System.out.println("ERG: " + erg);

        return erg;
    }

    private double getFieldValues(double fieldValueMultiplier) {
        double averageFieldValue = 0;

        if (map.getCountOfStonesOfPlayer(myColorI) == 0) return averageFieldValue;

        for (Position pos :  map.getStonesOfPlayer(myColorI)) {
            averageFieldValue += matrix[pos.y][pos.x];
        }

        return averageFieldValue/map.getCountOfStonesOfPlayer(myColorI); //calculates the average stone value
    }


    //  getter of static infos

    /**
     * calculates and sets static information about the map.
     * initializes the matrix of values for the map
     *
     * Current heuristic implementations:
     *      check for backed up outgoings
     *
     * Information that are currently saved:
     *      bonus
     *      inversion
     *      choice
     */
    private void setStaticInfos(){

        //add matrices
        if (useEdges || useFieldValues) { //only gets updated when used because when it's not used the matrix don't need to be updated
            for (int y = 1; y < map.getHeight() - 1; y++) {
                for (int x = 1; x < map.getWidth() - 1; x++) {

                    if (matrix[y][x] == Double.NEGATIVE_INFINITY) continue;

                    if (useFieldValues) matrix[y][x] = fieldValueMatrix[y][x] * fieldValueMultiplier;

                    if (useEdges) matrix[y][x] *= (edgeMatrix[y][x] == 0) ? 1 : edgeMultiplier;
                }
            }
        }


        if (extendedPrint) {
            if (useFieldValues) {
                System.out.println("Field value Matrix:");
                printMatrix(fieldValueMatrix);
            }

            if (useEdges) {
                System.out.println("Edge matrix");
                printMatrix(edgeMatrix);
            }

            System.out.println("Combined matrix");
            printMatrix(matrix);
        }

        //evaluate every position by its neighbours
        if (useWaves && (useEdges || useFieldValues)) addWaveMatrix();

        if (extendedPrint && useWaves && (useEdges || useFieldValues)) {
            System.out.println("Added Waves");
            printMatrix(matrix);
        }
    }

    private void initEdgeMatrixAndFieldValueMatrix(){
        //edge Matrix
        boolean[] outgoingDirections;
        edgeMatrix = new double[map.getHeight()][map.getWidth()];


        //field Value Matrix
        int[] reachableFields;
        int sumOfReachableFields;
        fieldValueMatrix = new double[map.getHeight()][map.getWidth()];


        for (int y = 1; y < map.getHeight() - 1; y++) {
            for (int x = 1; x < map.getWidth() - 1; x++) {

                if (matrix[y][x] == Double.NEGATIVE_INFINITY) continue;

                //edge Matrix
                outgoingDirections = getOutgoingDirections(new Position(x, y));
                edgeMatrix[y][x] = isCapturable(outgoingDirections) ? 0 : 1;


                //field value matrix
                sumOfReachableFields = 0;
                reachableFields = checkReachableFields(new Position(x, y));
                for (int a : reachableFields) sumOfReachableFields += a;
                fieldValueMatrix[y][x] = (double) sumOfReachableFields / map.getCountOfReachableFields();
            }
        }


    }

    private void initMatrix(){
        char currChar;

        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {

                //valid Position ?
                currChar = map.getCharAt(x,y);
                if (currChar == '-' || currChar == 't') {
                    matrix[y][x] = Double.NEGATIVE_INFINITY;
                }

            }
        }
    }

    private double calculateValueOfPosition(Position pos, char charAtPos){
        double result = 0;
        boolean[] outgoingDirections;
        boolean isCapturable;
        int[] reachableFields;
        int sumOfReachableFields = 0;
        int backedUpOutgoings;

        //set values
        outgoingDirections = getOutgoingDirections(pos);
        isCapturable = isCapturable(outgoingDirections);
        reachableFields = checkReachableFields(pos);
        for (int a : reachableFields) sumOfReachableFields += a;
        backedUpOutgoings = getBackedUpOutgoings(outgoingDirections);

        //get evaluation
        if (useFieldValues) {
            result = (double)sumOfReachableFields / map.getCountOfReachableFields();
        }

        // or: result = Math.pow(base, backedUpOutgoings);
        if (!isCapturable){
            if (useFieldValues) result *= edgeMultiplier;
            else result += edgeMultiplier;
        }
        result += bonusFieldValue(charAtPos);

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

    private int[] checkReachableFields(Position savedPos) {
        Integer newR;
        int[] reachableFields = new int[8];
        Position pos;

        for (int r = 0; r <= 7; r++){
            pos = savedPos.clone();
            newR = r;
            while (true) {
                newR = map.doAStep(pos, newR);

                if (newR == null || ( pos.equals(savedPos) && newR == r )){ //was pos.equals(savedPos)
                    break;
                }
                reachableFields[r]++;

                //safety to not run infinitely
                if (reachableFields[r] > 8 * (map.getWidth()-2) * (map.getHeight()-2) ) { // 8 because of the 8 directions. reachable fields = maximum stones a field in any direction could reach.
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


    //  waves

    /**
     * takes the greater values of the matrix and creates waves/ rings of alternating negative and positive decreasing values around it
     */
    private void addWaveMatrix(){
        //adjustable values
        double percentageOfValues = 0.1; //percentage of the highest rated positions to create waves
        int maxWaveCount = 1;
        int divisor = base;
        //variables
        waveMatrix = new double[map.getHeight()][map.getWidth()];
        int i = 0;
        Position pos;
        double lastValue;
        int currIdnex;

        ArrayList<Position> posList = new ArrayList<>();
        ArrayList<Double> valueList = new ArrayList<>();
        ArrayList<Integer> indexList = new ArrayList<>();
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                posList.add(new Position(x,y));
                valueList.add(matrix[y][x]);
                indexList.add(i);
                i++;
            }
        }

        indexList.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                Double d1 = valueList.get(o1);
                Double d2 = valueList.get(o2);
                return Double.compare(d2,d1);
            }
        });

        lastValue = valueList.get( indexList.get( (int)(indexList.size() * percentageOfValues) ) ); //gets the value of the last element defined by percentage
        for (i = 0; i < indexList.size(); i++) {
            currIdnex = indexList.get(i);

            if (valueList.get(currIdnex) < lastValue) break; //goes until the value is smaller than the lastValue

            pos = posList.get(currIdnex);

            createWave(pos, maxWaveCount, divisor); //changes wave matrix
        }

        //print of waves
        if (extendedPrint) {
            System.out.println("Wave matrix");
            printMatrix(waveMatrix);
        }

        for (int y = 1; y < map.getHeight()-1; y++) {
            for (int x = 1; x < map.getWidth()-1; x++) {
                matrix[y][x] += waveMatrix[y][x];
            }
        }
    }

    /**
     * creates the waves around one position
     * @param pos the position around which the waves are created
     */
    private void createWave(Position pos, int maxWaveCount, int divisor){
        int x1, x2, y1, y2;
        x1 = pos.x;
        x2 = pos.x;
        y1 = pos.y;
        y2 = pos.y;
        int loopSign = -1;
        double value = matrix[pos.y][pos.x]/divisor; //sets value to value of field/divisor to start
        int waveCount = 1;

        while(waveCount <= maxWaveCount) {
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
                        if(yi >= 0 && yi < waveMatrix.length && xi >= 0 && xi < waveMatrix[0].length)  { //if catches indices that went over the edge
                            waveMatrix[yi][xi] += loopSign * value;
                        }
                    }
                }
                else { //column between the rows
                    for (int xi : new int[]{x1,x2}){
                        if(yi >= 0 && yi < waveMatrix.length && xi >= 0 && xi < waveMatrix[0].length) { //if catches indices that went over the edge
                            waveMatrix[yi][xi] += loopSign * value;
                        }
                    }
                }
            }
            loopSign *= -1; //swap loop sign
            value /= divisor;
            waveCount++;
        }
    }
}
