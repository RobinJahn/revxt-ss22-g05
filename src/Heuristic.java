package src;

import java.util.*;
import java.util.concurrent.TimeoutException;

public class Heuristic {
    private final boolean printOn;

    private Map map; //is automatically updated because of the reference here
    //the color of the player for which the map is being evaluated
    private final int myColorI;
    private final char myColorC;

    private final StaticHeuristicPerPhase staticHeuristicPerPhase;

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
    public Heuristic(Map map, int myColor, boolean printOn, boolean extendedPrint, double[][] multiplier, StaticHeuristicPerPhase shpp){
        this.printOn = printOn;
        this.map = map;
        this.myColorI = myColor;
        this.myColorC = (char)('0'+myColor);

        this.multiplier = multiplier;
        stageNumber = getStage(true);
        setMultipliers(stageNumber);

        if (printOn){
            System.out.println("Multipliers per phase");
            System.out.println(Arrays.deepToString(this.multiplier).replace("],", "],\n"));
        }

        staticHeuristicPerPhase = shpp;
    }

    private void setMultipliers(int phase) {
        //set multiplier

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

        //check if useWaves isn't disabled by edges and fieldValues
        if (!useEdges && !useFieldValues) {
            waveCount = 0;
            useWaves = false;
        }

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
        double overwriteStonesAndBombsEval = 0;
        double result = 0;

        updateHeuristicMultipliers(phaseOne);

        //out of time ?
        if(timed && (UpperTimeLimit - System.nanoTime()<0)) {
            if (printOn||ServerLog) System.out.println("Out of time (Heuristic.evaluate - After update of multipliers)");
            throw new TimeoutException();
        }

        //count stones
        if (countStones) {
            if (phaseOne) countOfStonesEvaluation = countStones();
            else countOfStonesEvaluation = countStonesBombPhase();
        }

        //out of time ?
        if(timed && (UpperTimeLimit - System.nanoTime()<0)) {
            if (printOn||ServerLog) System.out.println("Out of time (Heuristic.evaluate - After count stones)");
            throw new TimeoutException();
        }

        //count moves
        if (countMoves) countOfMovesEvaluation = countMoves(timed,ServerLog,UpperTimeLimit);

        //out of time ?
        if(timed && (UpperTimeLimit - System.nanoTime()<0)) {
            if (printOn||ServerLog) System.out.println("Out of time (Heuristic.evaluate - After count Moves)");
            throw new TimeoutException();
        }

        //add values of won fields
        if (useFieldValues || useEdges) averageFieldValue = getFieldValues(); //If useWave is enabled alone there are no waves

        //out of time ?
        if(timed && (UpperTimeLimit - System.nanoTime()<0)) {
            if (printOn||ServerLog) System.out.println("Out of time (Heuristic.evaluate - After field value)");
            throw new TimeoutException();
        }

        overwriteStonesAndBombsEval = getOverwriteAndBombCount();


        //calculate result
        if (countStones) result += countOfStonesEvaluation * stoneCountMultiplier;
        if (countMoves) result += countOfMovesEvaluation * moveCountMultiplier;
        if (useFieldValues || useEdges) result += averageFieldValue;
        result += overwriteStonesAndBombsEval;

        //prints
        if (printOn) {
            System.out.println("Value for stone count = " + countOfStonesEvaluation * stoneCountMultiplier);
            System.out.println("Value for move count = " + countOfMovesEvaluation * moveCountMultiplier);
            System.out.println("Value for field Values = " + averageFieldValue);
            System.out.println("Value for Overwrite Stones and Bombs = " + overwriteStonesAndBombsEval);
        }

        //value
        return result;
    }

    public static double fastEvaluate(Map map, int myPlayerNr){

        int myStoneCount = map.getCountOfStonesOfPlayer(myPlayerNr);
        int enemyStoneCount = 0;
        double result;

        for (int playerNr = 1; playerNr <= map.getAnzPlayers(); playerNr++){
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

    public boolean evaluateOverwriteMove(Position pos) {
        return false;
    }

    public int selectBombOrOverwrite(){
        if (map.getExplosionRadius() > 4){
            return 20;
        }
        else {
            return 21;
        }
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
        for (int playerNr = 1; playerNr <= map.getAnzPlayers(); playerNr++) {

            //out of time ?
            if(timed && (UpperTimeLimit - System.nanoTime()<0)) {
                if (printOn||ServerLog) System.out.println("Out of time (Heuristic.evaluate - After get FieldValue)");
                throw new TimeoutException();
            }

            if (myColorI == map.getCurrentlyPlayingI()) {
                myPossibleMoves = Map.getValidMoves(map,timed,printOn,ServerLog,UpperTimeLimit, this).size();
            } else {
                possibleMovesOfEnemies += Map.getValidMoves(map,timed,printOn,ServerLog,UpperTimeLimit, this).size(); //TODO: Make valid Moves contain all moves from beginning on
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

    private double getFieldValues() {
        double averageFieldValue = 0;

        if (map.getCountOfStonesOfPlayer(myColorI) == 0) return averageFieldValue;

        for (Position pos :  map.getStonesOfPlayer(myColorI)) {
            averageFieldValue += staticHeuristicPerPhase.getValueFromMatrix(pos.x, pos.y, stageNumber);
            averageFieldValue += staticHeuristicPerPhase.getWaveValueForPos(pos, map, stageNumber);
        }

        return averageFieldValue/map.getCountOfStonesOfPlayer(myColorI); //calculates the average stone value
    }

    private double getOverwriteAndBombCount(){
        double result = 0;
        result += map.getOverwriteStonesForPlayer(myColorI) * 100;
        result += map.getBombsForPlayer(myColorI) * 100;
        return result;
    }

}
