package src;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

public class SearchTree {

    //don't change
    private final boolean printOn;
    private final boolean extendedPrint;
    private final boolean ServerLog;
    private final int myPlayerNr;
    private Heuristic heuristicForSimulation;

    final private boolean useAB;
    final private boolean useMS;
    final private boolean useBRS;
    final private boolean useKH;

    //change
    private  boolean timed;
    private int depth;
    private double approximation;
    private int[] pos;
    int moveCounter;
    boolean cancelNextDepth = false;
    long upperTimeLimit;

    KillerArray killerArray = new KillerArray();


    public SearchTree(Map map, boolean printOn, boolean ServerLog, boolean extendedPrint, int myPlayerNr, boolean useAB, boolean useMS, boolean useBRS, boolean useKH, double[] multiplier){
        this.printOn = printOn;
        this.ServerLog = ServerLog;
        this.myPlayerNr = myPlayerNr;
        this.extendedPrint = extendedPrint;

        this.useAB = useAB;
        this.useMS = useMS;
        this.useBRS = useBRS;
        this.useKH = useKH;

        heuristicForSimulation = new Heuristic(map, myPlayerNr, false, multiplier);
    }

    public int[] getMove(Map map, boolean timed, int depth, boolean phaseOne, ArrayList<int[]> validMoves, long time, int moveCounter){

        this.timed = timed;
        this.depth = depth;
        this.moveCounter = moveCounter;

        int[] moveToMake = new int[]{-1, -1, -1};

        Statistic statistic = new Statistic();

        if (timed){
            moveToMake = getMoveByTime(map, phaseOne, validMoves, time);
        }
        else {
            try {
                moveToMake = getMoveByDepth(map, phaseOne, validMoves, statistic);
                if (printOn) System.out.println(statistic);

            } catch (TimeoutException e) {
                System.err.println("Something went wrong - Time out Exception thrown but no time limit was set");
                e.printStackTrace();
            }
        }

        if(ServerLog) {
            System.out.println("Search tree: For Move: " + moveCounter + ", Depth: " + depth + ", Move: " + Arrays.toString(moveToMake));
        }

        return moveToMake;
    }

    private int[] getMoveByDepth(Map map, boolean phaseOne, ArrayList<int[]> validMoves, Statistic statistic) throws TimeoutException{
        int[] posAndInfo;
        ArrayList<Integer> indexList = new ArrayList<>(validMoves.size());
        int currIndex;
        double currBestValue = Double.NEGATIVE_INFINITY;
        double evaluation;
        int indexOfBest = 0;
        double[] alphaAndBeta = new double[]{Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY};
        boolean cuttof;

        //fill index List
        for (int i = 0; i < validMoves.size(); i++){
            indexList.add(i);
        }

        //sort index list
        if (useMS) sortMove();
        if (useBRS) useBRS();
        if (useKH) useKillerheuristic();

        //prints
        if (printOn) {
            System.out.println("Calculating values for " + validMoves.size() + " moves with depth " + depth);
            System.out.println("Currently at: ");
        }

        //go over every move
        for (int i = 0; i < validMoves.size(); i++) {
            //prints
            if (printOn){
                if (!extendedPrint) System.out.print((i+1) + ", ");
                else System.out.println((i+1) + ", ");
            }


            //get values and reset values
            currIndex = indexList.get(i);
            posAndInfo = validMoves.get(currIndex);
            evaluation = Double.NEGATIVE_INFINITY;

            //get evaluation
            try {
                evaluation = getValueForNode(map, posAndInfo, phaseOne, 1, statistic, alphaAndBeta.clone());
            }
            catch (TimeoutException e) {
                e.printStackTrace();
            }

            //print infos
            if (extendedPrint){
                System.out.print("NodeForDepth("+ depth +"): ");
                if (phaseOne) System.out.printf("[(%2d,%2d,%2d)= %.2f], ",posAndInfo[0],posAndInfo[1],posAndInfo[2],evaluation);
                else System.out.printf("[(%2d,%2d)= %.2f], ",posAndInfo[0],posAndInfo[1],evaluation);
                System.out.println();
            }

            //get highest value
            if (evaluation > currBestValue) {
                currBestValue = evaluation;
                indexOfBest = currIndex;
            }

            cuttof = useAlphaBetaPruning(alphaAndBeta, true, currBestValue, statistic, validMoves, i);
            if (cuttof){
                //Killer Heuristic
                if (useKH) {
                    killerArray.add(new PositionAndInfo( validMoves.get(currIndex) ), validMoves.size()-i);
                }
                break;
            }
        }

        //prints
        if (printOn) System.out.println();

        //prints
        if (extendedPrint) {
            System.out.print("NodeForDepth(" + depth + "): ");
            System.out.println("returning: " + currBestValue);
            System.out.println();
        }

        return validMoves.get(indexOfBest);
    }

    private int[] getMoveByTime(Map map, boolean phaseOne, ArrayList<int[]> validMoves, long time){
        //declarations
        Statistic statistic = new Statistic();
        int[] currMove;
        int[] validPosition = validMoves.get(0);
        //Timing
        long startTime = System.nanoTime();
        long timeOffset = 80_000_000; //ns -> xx ms
        long timeNextDepth = 0;
        upperTimeLimit = startTime + time * 1_000_000 - timeOffset;
        double leavesNextDepth;
        double totalNodesToGoOver;

        //iterative deepening
        for (depth = 1; (upperTimeLimit - System.nanoTime() - timeNextDepth > 0); depth++) {

            //reset statistic
            statistic = new Statistic();

            //print
            if (printOn) System.out.println("DEPTH: " + depth);

            //get the best move for this depth
            try {
                currMove = getMoveByDepth(map, phaseOne, validMoves, statistic);
            }
            //if it noticed we have no more time
            catch (TimeoutException te){
                if (printOn || ServerLog) {
                    System.out.println("For Move: " + moveCounter + ", Depth: " + depth + ", Move: " + Arrays.toString(validPosition));
                    System.out.println("Time out Exception thrown");
                    System.out.println("Time Remaining: " + (double)(upperTimeLimit - System.nanoTime()) / 1_000_000 + "ms");
                    te.printStackTrace();
                }
                return validPosition;
            }

            //if we got a valid Position without running out of time - update it
            validPosition = currMove;

            //calculate time needed for next depth
            leavesNextDepth = statistic.leafNodes * statistic.branchFactor();
            totalNodesToGoOver = statistic.totalNodesSeen + leavesNextDepth;

            //time comparison prints
            if (printOn){
                System.out.println("Expected Time needed for this depth: " + timeNextDepth/ 1_000_000 + "ms");
                System.out.println("Actual time needed: " + (double)statistic.totalComputationTime/ 1_000_000 + "ms");
                System.out.println("Approximation: " + approximation);
            }

            //If we know we won or lost -> no need to check deeper
            if (cancelNextDepth){ //TODO set this
                return validPosition;
            }

            //calculate time needed for the next depth
            if (timeNextDepth == 0) {
                timeNextDepth = Math.round(totalNodesToGoOver * statistic.getAverageComputationTime());
            }
            else {
                approximation = (approximation + ((double)statistic.totalComputationTime /timeNextDepth) ) / 2;
                timeNextDepth = Math.round(totalNodesToGoOver * statistic.getAverageComputationTime() * approximation);
            }

            //prints after one depth
            if (printOn) {
                //print recommendet move
                if (phaseOne) System.out.println("Recommended Move: (" + validPosition[0] + "," + validPosition[1] + "," + validPosition[2] + ")");
                else System.out.println("Recommended Move: (" + validPosition[0] + "," + validPosition[1] + ")");

                //print statisic
                System.out.println(statistic);

                //print timing information
                System.out.println("Expected time needed for next depth: " + (double)timeNextDepth/ 1_000_000 + "ms");
                System.out.println("Time Remaining: " + (double)(upperTimeLimit - System.nanoTime()) / 1_000_000 + "ms");
                System.out.println("Expected remaining time after calculating next depth: " + (double)(upperTimeLimit - System.nanoTime() - timeNextDepth)/ 1_000_000 + "ms");
                System.out.println();
            }
        }

        return validPosition;
    }


    //recursive
    private double getValueForNode(Map map, int[] posAndInfo, boolean phaseOne, int depth, Statistic statistic, double[] alphaAndBeta) throws TimeoutException {
        Double winOrLossReturn;
        ArrayList<int[]> validMoves = new ArrayList<>();
        ArrayList<Integer> indexList;
        int currIndex;
        double currBestValue;
        double evaluation = 0;
        int indexOfBest = 0;
        boolean isMax;
        boolean cuttof;

        //Out of Time ?
        if(timed && (upperTimeLimit - System.nanoTime()<0)) {
            if (printOn||ServerLog) System.out.println("Out of Time (get Value For Node - start of Method)");
            throw new TimeoutException();
        }

        //FIRST PART: simulate move and get the moves for the next layer

        //SIMULATE MOVE
        map = simulateMove(map, posAndInfo, phaseOne);

        //HANDLE WIN OR LOSS
        winOrLossReturn = checkForWinOrLoss(map);
        if (winOrLossReturn != null) return winOrLossReturn;

        //IF WE'RE A LEAF RETURN EVALUATION
        if (depth >= this.depth){
            heuristicForSimulation.updateMap(map);
            evaluation = heuristicForSimulation.evaluate(phaseOne,timed,ServerLog,upperTimeLimit); //computing-intensive // Here TIME LEAK !!!!!!!
            return evaluation;
        }

        //GET MOVES for the next player
        //  getMovesForNextPlayer fills validMoves array
        phaseOne = getMovesForNextPlayer(map, validMoves, phaseOne, timed, printOn, ServerLog);
        indexList = new ArrayList<>(validMoves.size());

        //  check if it reached the end of the game
        if (validMoves.isEmpty()) {
            heuristicForSimulation.updateMap(map); //computing-intensive
            return heuristicForSimulation.evaluate(phaseOne, timed, ServerLog, upperTimeLimit); //computing-intensive
        }

        //add values to statistic
        statistic.addNodes(validMoves.size(), depth);


        //SECOND PART: use algorithms for move sorting, recursive call and get the best value + alpha beta pruning


        //SET MAXIMIZER OR MINIMIZER
        isMax = isMax(map);

        //  set evaluation
        if (isMax) {
            currBestValue = Double.NEGATIVE_INFINITY;
            //TODO: BRS+
            //BRS+ Algorithm
            //brsCount = 0;
        }
        else {
            currBestValue = Double.POSITIVE_INFINITY;
            //TODO: BRS+
            //BRS+ Algorithm
            /*PhiZugIndex Random Choice Hier merken wir uns den Index unseres PhiZuges
            PhiZugIndex = (int)(Math.random()*(everyPossibleMove.size()-1));

            if(brsCount == 2 && useBRS)
            {
                int[] PhiZug = everyPossibleMove.get(PhiZugIndex);
                everyPossibleMove = new ArrayList<int[]>();
                everyPossibleMove.add(PhiZug);

            }
            */
        }

        //fill index List
        for (int i = 0; i < validMoves.size(); i++){
            indexList.add(i);
        }

        //SORT MOVES
        if (useMS) sortMove();
        if (useBRS) useBRS();
        if (useKH) useKillerheuristic();

        //Out of Time ?
        if(timed && (upperTimeLimit - System.nanoTime()<0)) {
            if (printOn||ServerLog) System.out.println("Out of Time (get Value For Node - after move sorting)");
            throw new TimeoutException();
        }

        //RECURSIVE CALL FOR EVERY MOVE
        for (int i = 0; i < validMoves.size(); i++) {

            //Out of Time ?
            if(timed && (upperTimeLimit - System.nanoTime()<0)) {
                if (printOn||ServerLog) System.out.println("Out of Time (get Value For Node - start of for every move)");
                throw new TimeoutException();
            }

            //set and reset variables
            currIndex = indexList.get(i);
            posAndInfo = validMoves.get(currIndex);

            //recursive call
            try {
                evaluation = getValueForNode(map, posAndInfo, phaseOne, depth+1, statistic, alphaAndBeta.clone());
            } catch (TimeoutException e) {
                e.printStackTrace();
            }

            //Out of Time ?
            if(timed && (upperTimeLimit - System.nanoTime()<0)) {
                if (printOn||ServerLog) System.out.println("Out of Time (After recursive call or evaluation of map)");
                throw new TimeoutException();
            }

            //print infos
            if (extendedPrint){
                System.out.print("NodeForDepth("+ depth +"): ");
                if (phaseOne) System.out.printf("[(%2d,%2d,%2d)= %.2f], ",posAndInfo[0],posAndInfo[1],posAndInfo[2],evaluation);
                else System.out.printf("[(%2d,%2d)= %.2f], ",posAndInfo[0],posAndInfo[1],evaluation);
                System.out.println();
            }

            //Get highest or lowest value
            if (isMax) {
                //get highest value
                if (evaluation > currBestValue) {
                    currBestValue = evaluation;
                    indexOfBest = currIndex;
                }
            }
            else {
                //get lowest value
                if (evaluation < currBestValue) {
                    currBestValue = evaluation;
                    indexOfBest = currIndex;
                }
            }

            cuttof = useAlphaBetaPruning(alphaAndBeta, isMax, currBestValue, statistic, validMoves, i);

            if (cuttof) {
                //Killer Heuristic
                if (useKH) {
                    killerArray.add(new PositionAndInfo( validMoves.get(currIndex) ), validMoves.size()-i);
                }
                break;
            }
        }

        //prints
        if (extendedPrint) {
            System.out.print("NodeForDepth(" + depth + "): ");
            System.out.println("returning: " + currBestValue);
            System.out.println();
        }

        return currBestValue;
    }


    //Helper functions for getValueForNode()

    private Double checkForWinOrLoss(Map map) {
        int enemyStoneAndOverwriteCount = 0;

        //If we have no stones and no overwrite stones -> LOSS
        if (map.getCountOfStonesOfPlayer(myPlayerNr) == 0 && map.getOverwriteStonesForPlayer(myPlayerNr) == 0){
            if (extendedPrint) System.out.println("getValueForNode found situation where we got eliminated");
            return Double.NEGATIVE_INFINITY;
        }

        //If we have all stones and no enemy has an overwrite stone -> WINN
        for (int playerNr = 1; playerNr <= map.getAnzPlayers(); playerNr++){
            if (playerNr == myPlayerNr) continue;
            enemyStoneAndOverwriteCount += map.getCountOfStonesOfPlayer(playerNr);
            enemyStoneAndOverwriteCount += map.getOverwriteStonesForPlayer(playerNr);
        }
        if (enemyStoneAndOverwriteCount == 0) {
            if (extendedPrint) System.out.println("DFSVisit found situation where we WON");
            return Double.POSITIVE_INFINITY;
        }

        return null;
    }

    private Map simulateMove(Map map, int[] posAndInfo, boolean phaseOne) throws TimeoutException {
        Map nextMap;
        // clones Map
        nextMap = new Map(map, phaseOne);

        // Out of Time ?
        if(timed && (upperTimeLimit - System.nanoTime() < 0)) {
            if (printOn||ServerLog) System.out.println("Out of time - in simulate move after clone");
            throw new TimeoutException();
        }

        // if it's the first phase
        if (phaseOne) {
            Map.updateMapWithMove(new Position(posAndInfo[0], posAndInfo[1]), posAndInfo[2], nextMap.getCurrentlyPlayingI(), nextMap, false);
        }
        //if it's the bomb phase
        else {
            Map.updateMapAfterBombingBFS(posAndInfo[0], posAndInfo[1], nextMap.getCurrentlyPlayingI(), nextMap);
        }

        return nextMap;
    }

    private boolean getMovesForNextPlayer(Map map, ArrayList<int[]> movesToReturn, boolean phaseOne,boolean timed,boolean printOn,boolean ServerLog) throws TimeoutException{
        ArrayList<int[]> everyPossibleMove;
        int skippedPlayers = 0;

        //checks if players can make a move
        while (true) {
            //get valid moves depending on stage of game
            if (phaseOne) { //phase one
                everyPossibleMove = Map.getValidMoves(map,timed,printOn,ServerLog, upperTimeLimit);
            }
            else { //bomb phase
                //if we have bombs
                if (map.getBombsForPlayer(map.getCurrentlyPlayingI()) > 0) everyPossibleMove = Client.getPositionsToSetABomb(map);
                    //if not
                else everyPossibleMove = new ArrayList<>(); //empty list
            }

            //if there are possible moves
            if (!everyPossibleMove.isEmpty()) {
                break;
            }

            //if there are no possible moves

            //chek if the next player can make a move
            map.nextPlayer();
            skippedPlayers++;

            //if no player can make a move
            if (skippedPlayers >= map.getAnzPlayers()-1){ //shouldn't be greater - just for safety
                //if no player can make a move in phase 1 switch to phase 2
                if (phaseOne) {
                    phaseOne = false; //end of phase 1
                    skippedPlayers = 0;
                    //continues while but in phase 2 -> checks if players can place bombs
                }
                //if no player can make a move in phase 2 the game ends
                else {
                    everyPossibleMove = new ArrayList<>(); //empty list
                    break;
                }
            }
        }

        //returning all moves by call by reference
        movesToReturn.addAll(everyPossibleMove);

        //returns change of phase
        return phaseOne;
    }


    //Move Sorting
    private void sortMove(){

    }

    //BRS+
    private void useBRS(){

    }

    //Killer Heurisitc
    private void useKillerheuristic(){

    }

    private boolean isMax(Map map){
        //	Maximizer
        if (map.getCurrentlyPlayingI() == myPlayerNr) {
            return true;
        }
        //	Minimizer
        else {
            return false;
        }
    }

    private boolean useAlphaBetaPruning(double[] alphaAndBeta, boolean isMax, double currBestValue, Statistic statistic, ArrayList<int[]> validMoves, int currentIndex){
        double currAlpha = alphaAndBeta[0];
        double currBeta = alphaAndBeta[1];
        boolean cuttof = false;

        //Maximizer
        if (isMax) {
            //update Alpha ?
            if (currBestValue > currAlpha) {
                currAlpha = currBestValue;
                if (extendedPrint) System.out.println("Alpha Updated: [" + currAlpha + ", " + currBeta + "]");
            }

            //Cuttoff ?
            if (currBestValue >= currBeta) {
                int countOfCutoffLeaves = validMoves.size() - currentIndex;
                //delete nodes out of statistic
                statistic.reduceNodes(countOfCutoffLeaves, depth);

                //Print before return
                if (extendedPrint) {
                    System.out.println("Cutoff: Current highest value (" + currBestValue + ") >= current Beta (" + currBeta + ") - " + countOfCutoffLeaves + " values skipped");
                }
                cuttof = true;
            }
        }
        //Minimizer
        else {
            //update Beta ?
            if (currBestValue < currBeta) {
                currBeta = currBestValue;
                if (extendedPrint) System.out.println("Beta Updated: [" + currAlpha + ", " + currBeta + "]");
            }

            //Cuttoff ?
            if (currBestValue <= currAlpha) {
                int countOfCutoffLeaves = validMoves.size()- currentIndex;
                //delete nodes out of statistic
                statistic.reduceNodes(countOfCutoffLeaves, depth);

                //Print before return
                if (extendedPrint) {
                    System.out.println("Cutoff: Current lowest value (" + currBestValue + ") <= current Alpha (" + currAlpha + ") - " + countOfCutoffLeaves + " values skipped");
                }
                cuttof = true;
            }
        }

        //update alpha and beta
        alphaAndBeta[0] = currAlpha;
        alphaAndBeta[1] = currBeta;

        return cuttof;
    }


}
