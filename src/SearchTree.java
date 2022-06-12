package src;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.TimeoutException;

public class SearchTree {

    //don't change
    private final boolean printOn;
    private final boolean extendedPrint;
    private final boolean serverLog;
    private final int myPlayerNr;
    private Heuristic heuristicForSimulation;

    final private boolean useAB;
    final private boolean useMS;
    final private boolean useBRS;
    final private boolean useKH;
    final private boolean useRM = true;
    final private boolean useZH = true;
    final private boolean useAW = true;

    //change
    private  boolean timed;
    private int depth;
    private double approximation = 1;
    int moveCounter;
    boolean cancelNextDepth = false;
    long upperTimeLimit;
    long timeForLastDepth1 = 0;

    ArrayList<KillerArray> killerArrays;

    int[] returnedMove;

    MctsNode currRoot = null;

    ZobristHashing ZH;
    TranspositionTable TT;
    AspirationWindow AW;

    double valueOfMove;

    int totalDepth = 0;

    public SearchTree(Map map, boolean printOn, boolean ServerLog, boolean extendedPrint, int myPlayerNr, boolean useAB, boolean useMS, boolean useBRS, boolean useKH, double[][] multiplier){
        this.printOn = printOn;
        this.serverLog = ServerLog;
        this.myPlayerNr = myPlayerNr;
        this.extendedPrint = extendedPrint;

        this.useAB = useAB;
        this.useMS = useMS;
        this.useBRS = useBRS;
        this.useKH = useKH;

        heuristicForSimulation = new Heuristic(map, myPlayerNr, false, false, multiplier);

        //Zobrist Hashing
        ZH = new ZobristHashing(map.getHeight(), map.getWidth(), myPlayerNr);
        TT = new TranspositionTable(64000);
        //Aspiration Window
        AW = new AspirationWindow(Double.MIN_VALUE,Double.MAX_VALUE,1000);
    }

    public int[] getMove(Map map, boolean timed, int depth, boolean phaseOne, ArrayList<int[]> validMoves, long upperTimeLimit, int moveCounter){

        this.timed = timed;
        this.depth = depth;
        this.moveCounter = moveCounter;
        this.upperTimeLimit = upperTimeLimit;

        int[] moveToMake = new int[]{-1, -1, -1};

        killerArrays = new ArrayList<>();

        Statistic statistic;

        if (timed){
            //this.upperTimeLimit = Long.MAX_VALUE;
            //moveToMake = getMoveWithMCTS(map, phaseOne, validMoves);

            moveToMake = getMoveByTime(map, phaseOne, validMoves);
        }
        else {
            statistic = new Statistic(depth);
            try {
                moveToMake = getMoveByDepth(map, phaseOne, validMoves, statistic);
                if (printOn) System.out.println(statistic);

            } catch (TimeoutException e) {
                System.err.println("Something went wrong - Time out Exception thrown but no time limit was set");
                e.printStackTrace();
            }
        }

        if(serverLog) {
            System.out.println("Search tree: For Move: " + moveCounter + ", Depth: " + (this.depth-1) + ", Move: " + Arrays.toString(moveToMake) + ",Value: " + valueOfMove);
            System.out.println();
            totalDepth+=(this.depth-1);
        }

        return moveToMake;
    }

    public int getTotalDepth()
    {
        return  totalDepth;
    }

    private int[] getMoveByDepth(Map map, boolean phaseOne, ArrayList<int[]> validMoves, Statistic statistic) throws TimeoutException{
        int[] posAndInfo;
        ArrayList<Integer> indexList = new ArrayList<>(validMoves.size());
        int currIndex;
        double currBestValue = Double.NEGATIVE_INFINITY;
        double evaluation;
        int indexOfBest = 0;
        double[] alphaAndBeta;
        boolean cutoff;
        int depth = 0;

        //for brs+ which we don't use here - yes
        Map nextMap;
        ArrayList<int[]> validMovesForNext;
        boolean phaseOneAfterNextMove;
        int brsCount = 0;

        statistic.addNodes(validMoves.size(), depth);

        if (useAW) {
            alphaAndBeta = AW.getWindow();
        }
        else {
            alphaAndBeta = new double[]{Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY};
        }

        //fill index List
        for (int i = 0; i < validMoves.size(); i++){
            indexList.add(i);
        }

        //sort index list
        if (useMS) sortMove(indexList, validMoves, true, map, phaseOne);
        if (useKH) useKillerheuristic(indexList, validMoves, depth);
        if (useRM) useReturnedMove(indexList, validMoves, returnedMove);

        // Out of Time ?
        if(timed && (upperTimeLimit - System.nanoTime() < 0)) {
            if (printOn || serverLog) System.out.println("Out of time - in getMoveByDepth after move sorting");
            throw new TimeoutException();
        }

        //prints
        if (printOn) {
            System.out.println("Calculating values for " + validMoves.size() + " moves with depth " + this.depth);
            System.out.println("Currently at: ");
        }


        //go over every move
        for (int i = 0; i < validMoves.size(); i++) {
            //prints
            if (printOn){
                if (!extendedPrint) System.out.print((i+1) + ", ");
                else System.out.println((i+1) + ", ");
                if (i % map.getWidth() == 0) System.out.println();
            }

            //get values and reset values
            currIndex = indexList.get(i);
            posAndInfo = validMoves.get(currIndex);
            evaluation = Double.NEGATIVE_INFINITY;

            // Out of Time ?
            if(timed && (upperTimeLimit - System.nanoTime() < 0)) {
                if (printOn || serverLog) System.out.println("Out of time - in getMoveByDepth at beginnin of for");
                throw new TimeoutException();
            }


            //SIMULATE MOVE and get the moves for the next layer

            //simulate move
            nextMap = simulateMove(map, posAndInfo, phaseOne);

            // Out of Time ?
            if(timed && (upperTimeLimit - System.nanoTime() < 0)) {
                if (printOn || serverLog) System.out.println("Out of time - in getMoveByDepth after nextMap got build");
                throw new TimeoutException();
            }

            //get moves for the next player
            //  getMovesForNextPlayer fills validMoves array
            validMovesForNext = new ArrayList<>();
            //  get next phase
            phaseOneAfterNextMove = getMovesForNextPlayer(nextMap, validMovesForNext, phaseOne, timed, printOn, serverLog);


            //  add values to statistic
            statistic.addNodes(validMovesForNext.size(), depth+1);

            // Out of Time ?
            if(timed && (upperTimeLimit - System.nanoTime() < 0)) {
                if (printOn || serverLog) System.out.println("Out of time - in getMoveByDepth after valid Moves for next player were fetched");
                throw new TimeoutException();
            }

            //GET EVALUATION - based on state of game, leaf node or inner node

            //handle Win or Loss
            if (validMovesForNext.isEmpty()){
                evaluation = returnValueForWinOrLoss(nextMap);
            }
            else {

                //if the next node would be a leaf
                if (depth >= this.depth - 1) {
                    heuristicForSimulation.updateMap(nextMap);
                    if(useZH)
                    {
                        evaluation = Zobrist(nextMap);
                    }
                    else {
                        evaluation = heuristicForSimulation.evaluate(phaseOneAfterNextMove, timed, serverLog, upperTimeLimit);
                    }
                }
                else {
                    //recursive call
                    if(useZH)
                    {
                        evaluation = TT.lookUp(ZH.hash(nextMap),depth);
                        if(evaluation == Double.MIN_VALUE+1)
                        {
                            long hash = ZH.hash(nextMap);
                            evaluation = getValueForNode(nextMap, validMovesForNext, posAndInfo, phaseOneAfterNextMove, depth+1, statistic, alphaAndBeta.clone(), brsCount + 1);
                            TT.add(hash,depth,evaluation);
                        }
                    }
                    else
                    {
                        evaluation = getValueForNode(nextMap, validMovesForNext, posAndInfo, phaseOneAfterNextMove, depth+1, statistic, alphaAndBeta.clone(), brsCount + 1);
                    }
                }
            }

            // Out of Time ?
            if(timed && (upperTimeLimit - System.nanoTime() < 0)) {
                if (printOn || serverLog) System.out.println("Out of time - in getMoveByDepth after evaluate was set");
                throw new TimeoutException();
            }

            //print infos
            if (extendedPrint){
                System.out.print("NodeForDepth("+ depth +"): ");
                if (phaseOneAfterNextMove) System.out.printf("[(%2d,%2d,%2d)= %.2f], ",posAndInfo[0],posAndInfo[1],posAndInfo[2],evaluation);
                else System.out.printf("[(%2d,%2d)= %.2f], ",posAndInfo[0],posAndInfo[1],evaluation);
                System.out.println();
            }

            //get highest value
            if (evaluation > currBestValue) {
                currBestValue = evaluation;
                indexOfBest = currIndex;

                valueOfMove = currBestValue;
            }

            cutoff = useAlphaBetaPruning(alphaAndBeta, true, currBestValue, statistic, validMoves, i);
            if (cutoff){
                //Killer Heuristic
                if (useKH) {
                    if (killerArrays.size()-1 < depth) killerArrays.add(new KillerArray());
                    killerArrays.get(depth).add(new PositionAndInfo( validMoves.get(currIndex) ), validMoves.size()-i);
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

        if (currBestValue == Double.POSITIVE_INFINITY || currBestValue == Double.NEGATIVE_INFINITY){
            cancelNextDepth = true;
        }

        return validMoves.get(indexOfBest);
    }

    private int[] getMoveByTime(Map map, boolean phaseOne, ArrayList<int[]> validMoves){
        //declarations
        Statistic statistic = null;
        int[] currMove;
        int[] validPosition = validMoves.get(0);
        //Timing
        long startTime = System.nanoTime();
        long totalTime;
        long timeOffsetCatch = 50_000_000;
        long timeNextDepth = timeForLastDepth1;

        double totalNodesToGoOver;

        //Resets the returned Move for the next Move
        returnedMove = null;
        //iterative deepening
        for (depth = 1; (upperTimeLimit - System.nanoTime() - timeNextDepth > 0); depth++) {

            //reset statistic
            statistic = new Statistic(depth);

            //reset Transposition Table
            TT.empty();

            //print
            if (printOn || serverLog) System.out.println("DEPTH: " + depth);

            //get the best move for this depth
            try {
                //start timing
                startTime = System.nanoTime();
                //call
                currMove = getMoveByDepth(map, phaseOne, validMoves, statistic);
                //end of timing
                totalTime = System.nanoTime() - startTime;
                statistic.totalComputationTime += totalTime;
            }
            //if it noticed we have no more time
            catch (TimeoutException te){
                //end of timing
                totalTime = System.nanoTime() - startTime;
                if (depth == 1) {
                    timeForLastDepth1 = totalTime + timeOffsetCatch;

                }

                if (printOn || serverLog) {
                    System.out.println("Time out Exception thrown");
                    System.out.println("Time Remaining (excluding offset): " + (double)(upperTimeLimit - System.nanoTime()) / 1_000_000 + "ms");
                }
                if (printOn){
                    System.out.println(Arrays.toString(te.getStackTrace()).replace(", ","\n"));
                    if(map.getCurrentlyPlayingI() == myPlayerNr) AW.printAlphaAndBeta();
                }
                return validPosition;
            }

            //if we got a valid Position without running out of time - update it
            validPosition = currMove;

            //Store the Move
            if(useRM){ returnedMove = validPosition;}

            //time comparison prints
            if (printOn) System.out.println("Expected Time needed for this depth: " + timeNextDepth/ 1_000_000 + "ms");
            if (printOn || serverLog){
                System.out.println("Actual time needed: " + (double)statistic.totalComputationTime/ 1_000_000 + "ms");
                System.out.println("Approximation: " + approximation);
            }

            //If we know we won or lost -> no need to check deeper
            if (cancelNextDepth){
                return validPosition;
            }

            //calculate time needed for the next depth //TODO: refine so that only the first ever move don't get approximated
            totalNodesToGoOver = statistic.totalNodesSeen * statistic.branchFactor();

            if (timeNextDepth == 0) {
                timeNextDepth = Math.round(totalNodesToGoOver * statistic.getAverageComputationTime());
            }
            else {
                approximation = (approximation + ((double)statistic.totalComputationTime /timeNextDepth) ) / 2;
                timeNextDepth = Math.round(totalNodesToGoOver * statistic.getAverageComputationTime() * approximation);
            }

            if (depth == 1) timeForLastDepth1 = statistic.totalComputationTime;

            //prints after one depth
            if (printOn) {
                //print recommended move
                if (phaseOne)
                    System.out.println("Recommended Move: (" + validPosition[0] + "," + validPosition[1] + "," + validPosition[2] + ")");
                else System.out.println("Recommended Move: (" + validPosition[0] + "," + validPosition[1] + ")");

                //print statistic
                System.out.println(statistic);
            }
            if (printOn || serverLog) {
                //print timing information
                System.out.println("Expected time needed for next depth: " + (double)timeNextDepth/ 1_000_000 + "ms");
                System.out.println("Time Remaining: " + (double)(upperTimeLimit - System.nanoTime()) / 1_000_000 + "ms");
                System.out.println("Expected remaining time after calculating next depth: " + (double)(upperTimeLimit - System.nanoTime() - timeNextDepth)/ 1_000_000 + "ms");
                System.out.println();
            }
        }
        if(printOn) AW.printAlphaAndBeta();
        return validPosition;
    }


    //MONTE CARLO TREE SEARCH

    private int[] getMoveWithMCTS(Map map, boolean phaseOne, ArrayList<int[]> validMoves){
        MctsNode rootNode;
        rootNode = new MctsNode(map, null, null, validMoves, phaseOne);
        //TODO: get child of root Node in regards which path it took
        MctsNode currV;
        double delta;
        int loopCount = 0;

        while (upperTimeLimit - System.nanoTime() > 0 || loopCount > 1000) {
            try {
                currV = treePolicy(rootNode);
                delta = defaultPolicy( currV.getMap(), phaseOne);
                backup(currV, delta);
            }
            catch (TimeoutException e) {
                e.printStackTrace();
                break;
            }
            loopCount++;
        }

        currV = bestChild(rootNode, 0);
        currRoot = currV;

        return currV.getActionLeadingToThis();
    }

    private MctsNode treePolicy(MctsNode v) throws TimeoutException {

        double c = 1;

        while (!v.isTerminal(heuristicForSimulation)) {
            if (!v.isFullyExpanded()) {
                return expand(v);
            }
            else {
                v = bestChild(v, c);
            }
        }
        return v;
    }

    private MctsNode expand(MctsNode v) throws TimeoutException {
        ArrayList<int[]> validMovesForNext = new ArrayList<>();
        boolean phaseOneAfterNextMove;
        int[] a = v.getUntriedActionAndRemoveIt(); //call by Reference !
        boolean phaseOne = v.isPhaseOne();

        //simulate move
        //TODO: can it be that expand gets called on a node where the player cant play because he has no bombs
        Map nextMap = simulateMove(v.getMap(), a, phaseOne);
        phaseOneAfterNextMove = getMovesForNextPlayer(nextMap, validMovesForNext, phaseOne, timed, printOn, serverLog);

        //create Child
        MctsNode newChild = new MctsNode(nextMap, v, a, validMovesForNext, phaseOneAfterNextMove);

        //add child
        v.addChild(newChild);

        return newChild;
    }

    private MctsNode bestChild(MctsNode v, double c){
        double maxValue = Double.NEGATIVE_INFINITY;
        double currValue;
        MctsNode bestChild = null;

        for (MctsNode vChild : v.getChilds()){
            currValue = (vChild.getTotalReward() / vChild.getCountOfVisits()) + c * Math.sqrt( (2* Math.log(v.getCountOfVisits())) / vChild.getCountOfVisits() );
            if (currValue > maxValue){
                maxValue = currValue;
                bestChild = vChild;
            }
        }

        return bestChild;
    }

    private double defaultPolicy(Map map, boolean phaseOne) throws TimeoutException {
        int[] a;
        int placement;

        //first phase
        while (!map.isTerminal(heuristicForSimulation)){
            a = map.getRandomMove();
            map = simulateMove(map, a, phaseOne);
        }
        //second phase
        phaseOne = false;
        while (!map.isTerminalSecondPhase()){
            a = map.getRandomBombMove();
            map = simulateMove(map, a, phaseOne);
        }

        placement = map.getPlacement(myPlayerNr);
        return 1 - ((double)(placement - 1) / (map.getAnzPlayers() - 1)); //norms place to a value between 1 best and 0 worst
    }

    private void backup(MctsNode v, double delta) {
        while (v != null){
            v.increaseVisits();
            v.updateReward(delta); //p shouldn't be necessary because node has Map.currentlyPlaying
            v = v.getParent();
        }
    }

    //END OF MONTE CARLO TREE SEARCH


    //recursive
    private double getValueForNode(Map map, ArrayList<int[]> validMoves, int[] posAndInfo, boolean phaseOne, int depth, Statistic statistic, double[] alphaAndBeta, int brsCount) throws TimeoutException {
        ArrayList<Integer> indexList;

        int currIndex;
        double currBestValue;
        double evaluation;
        boolean isMax;
        boolean cutoff;

        Map nextMap;
        ArrayList<int[]> validMovesForNext;
        boolean phaseOneAfterNextMove;
        int nextPlayer;
        int nextBrsCount;
        boolean isPhiMove = false;
        boolean madePhiMove = false;

        ArrayList<Map> mapList = null;

        //Out of Time ?
        if(timed && (upperTimeLimit - System.nanoTime()<0)) {
            if (printOn || serverLog) System.out.println("Out of Time - in getValueForNode - start of Method");
            throw new TimeoutException();
        }


        //FIRST PART: use algorithms for move sorting, recursive call and get the best value + alpha beta pruning

        //FILL INDEX LIST
        indexList = new ArrayList<>(validMoves.size());
        for (int i = 0; i < validMoves.size(); i++) indexList.add(i);

        //SET MAXIMIZER OR MINIMIZER
        isMax = isMax(map);

        //  set currBestValue
        if (isMax) {
            currBestValue = Double.NEGATIVE_INFINITY;
        }
        else {
            currBestValue = Double.POSITIVE_INFINITY;
        }

        //SORT MOVES
        if (useMS) mapList = sortMove(indexList, validMoves, isMax, map, phaseOne); //changes index List
        if (useKH) useKillerheuristic(indexList, validMoves, depth);
        if (useBRS) {
            indexList = useBRS1(indexList, isMax, brsCount);
            if (indexList.size() == 1) isPhiMove = true;
        }

        //Out of Time ?
        if(timed && (upperTimeLimit - System.nanoTime()<0)) {
            if (printOn || serverLog) System.out.println("Out of Time - in getValueForNode after move sorting");
            throw new TimeoutException();
        }


        //RECURSIVE CALL FOR EVERY MOVE
        for (int i = 0; i < indexList.size(); i++) {

            //set and reset variables
            currIndex = indexList.get(i);
            posAndInfo = validMoves.get(currIndex);

            //Out of Time ?
            if(timed && (upperTimeLimit - System.nanoTime()<0)) {
                if (printOn || serverLog) System.out.println("Out of Time - in getValueForNode start of for");
                throw new TimeoutException();
            }

            //SIMULATE MOVE and get the moves for the next layer
            if (mapList == null){
                //simulate move
                nextMap = simulateMove(map, posAndInfo, phaseOne);

                //Out of Time ?
                if(timed && (upperTimeLimit - System.nanoTime()<0)) {
                    if (printOn || serverLog) System.out.println("Out of Time - in getValueForNode after next map got build");
                    throw new TimeoutException();
                }
            }
            else {
                nextMap = mapList.get(currIndex);
            }

            //get moves for the next player
            //  getMovesForNextPlayer fills validMoves array
            validMovesForNext = new ArrayList<>();
            //  get next phase
            phaseOneAfterNextMove = getMovesForNextPlayer(nextMap, validMovesForNext, phaseOne, timed, printOn, serverLog);
            //  get next player
            nextPlayer = nextMap.getCurrentlyPlayingI();

            //  add values to statistic
            statistic.addNodes(validMovesForNext.size(), depth+1);

            //Out of Time ?
            if(timed && (upperTimeLimit - System.nanoTime()<0)) {
                if (printOn || serverLog) System.out.println("Out of Time - in getValueForNode after moves for next player got fetched");
                throw new TimeoutException();
            }


            if (useBRS) {
                if (!madePhiMove && !isPhiMove && useBRS2(isMax, brsCount, nextPlayer)) {
                    isPhiMove = true;
                    madePhiMove = true;
                }
            }


            //GET EVALUATION - based on state of game, leaf node or inner node

            //handle Win or Loss
            if (validMovesForNext.isEmpty()){
                //cancelNextDepth = true; TODO: not right, here
                evaluation = returnValueForWinOrLoss(nextMap);
            }
            else {

                //if the next node would be a leaf
                if (depth >= this.depth - 1) {
                    if(useZH)
                    {
                        evaluation = Zobrist(nextMap);
                    }
                    else
                    {
                        heuristicForSimulation.updateMap(nextMap);
                        evaluation = heuristicForSimulation.evaluate(phaseOneAfterNextMove, timed, serverLog, upperTimeLimit); //computing-intensive
                    }
                }
                else {
                    if (isPhiMove) nextBrsCount = brsCount;
                    else nextBrsCount = brsCount + 1;

                    //recursive call
                    if(useZH)
                    {
                        evaluation = TT.lookUp(ZH.hash(nextMap),depth);
                        if(evaluation == Double.MIN_VALUE+1)
                        {
                            long hash = ZH.hash(nextMap);
                            evaluation = getValueForNode(nextMap, validMovesForNext, posAndInfo, phaseOneAfterNextMove, depth + 1, statistic, alphaAndBeta.clone(), nextBrsCount);
                            TT.add(hash,depth,evaluation);
                        }
                    }
                    else
                    {
                        evaluation = getValueForNode(nextMap, validMovesForNext, posAndInfo, phaseOneAfterNextMove, depth + 1, statistic, alphaAndBeta.clone(), nextBrsCount);
                    }
                }
            }

            //Out of Time ?
            if(timed && (upperTimeLimit - System.nanoTime()<0)) {
                if (printOn || serverLog) System.out.println("Out of Time - in getValueForNode after evaluation was set");
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
                    if(useAW) {
                        AW.setAlphaAndBeta(alphaAndBeta[0],alphaAndBeta[1]);
                        if(!AW.insideWindow(evaluation)) AW.resetWindow();
                    }
                }
            }
            else {
                //get lowest value
                if (evaluation < currBestValue) {
                    currBestValue = evaluation;
                    if(useAW){
                        AW.setAlphaAndBeta(alphaAndBeta[0],alphaAndBeta[1]);
                        if(!AW.insideWindow(evaluation)) AW.resetWindow();
                    }
                }
            }

            cutoff = useAlphaBetaPruning(alphaAndBeta, isMax, currBestValue, statistic, validMoves, i); //parameter i here because it needs to calculate how many branches were cut

            if (cutoff) {
                //Killer Heuristic
                if (useKH) {
                    while (killerArrays.size()-1 < depth) {
                        killerArrays.add(new KillerArray());
                    }
                    killerArrays.get(depth).add(new PositionAndInfo( validMoves.get(currIndex) ), validMoves.size()-i);
                }
                break;
            }

            if (useBRS) isPhiMove = false;
        }

        //prints
        if (extendedPrint) {
            System.out.print("NodeForDepth(" + depth + "): ");
            System.out.println("returning: " + currBestValue);
            System.out.println();
        }

        return currBestValue;
    }

    private double Zobrist(Map map) throws TimeoutException{

        long hash = ZH.hash(map);
        double value = TT.lookUp(hash,depth);
        if(value != Double.MIN_VALUE+1)
        {
            return value;
        }
        else
        {
            heuristicForSimulation.updateMap(map);
            value = heuristicForSimulation.evaluate(true, timed, serverLog, upperTimeLimit); //computing-intensive
            TT.add(hash,depth,value);
        }
        return value;
    }

    //Helper functions for getValueForNode()

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

    //TODO: refine to return value according to placement
    private Double returnValueForWinOrLoss(Map map) {
        int myStoneCount = map.getCountOfStonesOfPlayer(myPlayerNr);

        for (int playerNr = 1; playerNr <= map.getAnzPlayers(); playerNr++){
            if (playerNr == myPlayerNr) continue;
            if (myStoneCount < map.getCountOfStonesOfPlayer(playerNr)) return Double.NEGATIVE_INFINITY;
        }

        return Double.POSITIVE_INFINITY;
    }

    private Map simulateMove(Map map, int[] posAndInfo, boolean phaseOne) throws TimeoutException {
        Map nextMap;
        // clones Map
        nextMap = new Map(map, phaseOne);

        // Out of Time ?
        if(timed && (upperTimeLimit - System.nanoTime() < 0)) {
            if (printOn|| serverLog) System.out.println("Out of time - in simulate move after clone");
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

    private boolean getMovesForNextPlayer(Map map, ArrayList<int[]> movesToReturn, boolean phaseOne,boolean timed,boolean printOn,boolean ServerLog) throws TimeoutException {
        ArrayList<int[]> everyPossibleMove;
        int skippedPlayers = 0;

        //checks if players can make a move
        while (true) {
            //get valid moves depending on stage of game
            if (phaseOne) { //phase one
                everyPossibleMove = Map.getValidMoves(map,timed,printOn,ServerLog, upperTimeLimit, heuristicForSimulation);
            }
            else { //bomb phase
                //if we have bombs
                if (map.getBombsForPlayer(map.getCurrentlyPlayingI()) > 0) everyPossibleMove = Map.getPositionsToSetABomb(map);
                //if not
                else everyPossibleMove = new ArrayList<>(); //empty list
            }

            //if there are possible moves
            if (!everyPossibleMove.isEmpty()) {
                break;
            }

            //if there are no possible moves

            //check if the next player can make a move
            skippedPlayers += map.nextPlayer();
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


    //Alpha Beta Pruning
    private boolean useAlphaBetaPruning(double[] alphaAndBeta, boolean isMax, double currBestValue, Statistic statistic, ArrayList<int[]> validMoves, int currentIndex){
        double currAlpha = alphaAndBeta[0];
        double currBeta = alphaAndBeta[1];
        boolean cutoff = false;

        //Maximizer
        if (isMax) {
            //update Alpha ?
            if (currBestValue > currAlpha) {
                currAlpha = currBestValue;
                if (extendedPrint) System.out.println("Alpha Updated: [" + currAlpha + ", " + currBeta + "]");
            }

            //Cutoff ?
            if (currBestValue >= currBeta) {
                int countOfCutoffLeaves = validMoves.size() - currentIndex;
                //delete nodes out of statistic
                //statistic.reduceNodes(countOfCutoffLeaves, depth);

                //Print before return
                if (extendedPrint) {
                    System.out.println("Cutoff: Current highest value (" + currBestValue + ") >= current Beta (" + currBeta + ") - " + countOfCutoffLeaves + " values skipped");
                }
                cutoff = true;
            }
        }
        //Minimizer
        else {
            //update Beta ?
            if (currBestValue < currBeta) {
                currBeta = currBestValue;
                if (extendedPrint) System.out.println("Beta Updated: [" + currAlpha + ", " + currBeta + "]");
            }

            //Cutoff ?
            if (currBestValue <= currAlpha) {
                int countOfCutoffLeaves = validMoves.size()- currentIndex;
                //delete nodes out of statistic
                //statistic.reduceNodes(countOfCutoffLeaves, depth);

                //Print before return
                if (extendedPrint) {
                    System.out.println("Cutoff: Current lowest value (" + currBestValue + ") <= current Alpha (" + currAlpha + ") - " + countOfCutoffLeaves + " values skipped");
                }
                cutoff = true;
            }
        }

        //update alpha and beta
        alphaAndBeta[0] = currAlpha;
        alphaAndBeta[1] = currBeta;

        return cutoff;
    }

    //Move Sorting
    private ArrayList<Map> sortMove(ArrayList<Integer> indexList, ArrayList<int[]> validMoves, boolean isMax, Map map, boolean phaseOne) throws TimeoutException {
        ArrayList<Map> mapList = new ArrayList<>();
        Map nextMap;

        if (Map.useArrows && phaseOne) {
            indexList.sort(new Comparator<Integer>() {
                @Override
                public int compare(Integer i1, Integer i2) {
                    int[] positionAndInfo1 = validMoves.get(i1);
                    int[] positionAndInfo2 = validMoves.get(i2);
                    double valueM1 = Map.getStoneCountAfterMove(map, myPlayerNr, positionAndInfo1);
                    double valueM2 = Map.getStoneCountAfterMove(map, myPlayerNr, positionAndInfo2);
                    if (isMax) return Double.compare(valueM2, valueM1);
                    else return Double.compare(valueM1, valueM2);
                }
            });
            return null;
        }
        else {
            //go over every move
            for (int[] positionAndInfo : validMoves) {
                //Out of Time ?
                if(timed && (upperTimeLimit - System.nanoTime() < 0)) {
                    if (printOn|| serverLog) System.out.println("Out of time (getBestValueAndIndexFromMoves - In Move Sorting - start of for)");
                    throw new TimeoutException();
                }

                //clones Map
                nextMap = new Map(map, phaseOne);

                //Out of Time ?
                if(timed && (upperTimeLimit - System.nanoTime() < 0)) {
                    if (printOn|| serverLog) System.out.println("Out of time (getBestValueAndIndexFromMoves - In Move Sorting - after clone)");
                    throw new TimeoutException();
                }

                //if it's the first phase
                if (phaseOne) {
                    Map.updateMapWithMove(new Position(positionAndInfo[0], positionAndInfo[1]), positionAndInfo[2], nextMap.getCurrentlyPlayingI(), nextMap, false);
                }
                //if it's the bomb phase
                else {
                    Map.updateMapAfterBombingBFS(positionAndInfo[0], positionAndInfo[1], nextMap.getCurrentlyPlayingI(), nextMap);
                }

                mapList.add(nextMap);
            }

            indexList.sort(new Comparator<Integer>() {
                @Override
                public int compare(Integer i1, Integer i2) {
					/*
					if(i1>= mapList.size()|| i2>= mapList.size())
					{
						System.err.printf("IndexListSize: " + indexList.size() + "\nMapListSize: " + mapList.size());
						System.err.println("\nI1: " + i1 +" \nI2" + i2);
						return 0;
					}
					*/
                    Map m1 = mapList.get(i1);
                    Map m2 = mapList.get(i2);
                    double valueM1 = Heuristic.fastEvaluate(m1, myPlayerNr);
                    double valueM2 = Heuristic.fastEvaluate(m2, myPlayerNr);
                    if (isMax) return Double.compare(valueM2, valueM1);
                    else return Double.compare(valueM1, valueM2);
                }
            });

            return mapList;
        }
    }

    //BRS+
    private ArrayList<Integer> useBRS1(ArrayList<Integer> indexList, boolean isMax, int brsCount){
        if (indexList.isEmpty()) return indexList;

        ArrayList<Integer> newIndexList = indexList;
        if (!isMax && brsCount == 2){
            newIndexList = new ArrayList<>();
            newIndexList.add( indexList.get(0) ); //TODO: here we can select what kind of move the phi move should be
        }
        return newIndexList;
    }

    private boolean useBRS2(boolean isMax, int brsCount, int nextPlayer){
        //TODO: either this could make the first move to a phi move or it could save all / keep the best or worst the possible phi moves and then add one of them to the end of the index list
        if (!isMax && nextPlayer != myPlayerNr){
            return true;
        }
        //if is Max it's our move
        //if nextPlayer == myPlayerNr we're next
        return false;
    }

    //Killer Heuristic
    private void useKillerheuristic(ArrayList<Integer> indexList, ArrayList<int[]> validMoves, int depth) throws TimeoutException{

        if (killerArrays.size()-1 < depth) return;

        for(int i = killerArrays.get(depth).getLength()-1; i >= 0; i--)
        {
            //Out of Time ?
            if(timed && (upperTimeLimit - System.nanoTime() < 0)) {
                if (printOn|| serverLog) System.out.println("Out of time (getBestValueAndIndexFromMoves - In Killer Heuristic)");
                throw new TimeoutException();
            }

            for (int j = 0; j < validMoves.size(); j++)
            {
                int[] positionAndInfo = validMoves.get(j);

                //If We found a Move which cuts off we place it in front
                if(Arrays.equals(killerArrays.get(depth).getPositionAndInfo(i), positionAndInfo))
                {
                    indexList.remove((Integer) j); //the cast to Integer need to be there because otherwise it would remove the element at the index j
                    indexList.add(0, j);
                }
            }
        }
    }

    private void useReturnedMove(ArrayList<Integer> indexList, ArrayList<int[]> validMoves, int[] returnedMove)
    {
        if(returnedMove != null)
        {
            indexList.remove((Integer) validMoves.indexOf(returnedMove));
            indexList.add(0,validMoves.indexOf(returnedMove));
        }
    }
}
