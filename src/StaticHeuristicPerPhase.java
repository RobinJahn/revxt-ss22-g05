package src;

import java.util.ArrayList;

/**
 * This class is used to store information for the heuristic.
 * It contains the information for the field value and wave value for all phases of the game
 */
public class StaticHeuristicPerPhase {

    private final ArrayList<StaticHeuristic> staticHeuristicPerPhase;
    private final int height;
    private final int width;
    private final int fieldValueIndex = 2;
    private final int edgesIndex = 3;
    private final  double[][] staticMatrix;
    private final  double[][] fieldValueMatrix;
    private final  boolean[][] edgeMatrix;
    private final ArrayList<Position> specialFields = new ArrayList<>();
    private final double[][] multiplier; //[] phases, [] stones, moves, Field Values, Edges, Waves
    private final boolean[][] enables;

    /**
     * Constructor of the static heuristic for each game phase.
     * @param map The map.
     * @param multiplier Multipliers for each game phase.
     * @param extendedPrint If true, also prints the created value maps.
     */
    public StaticHeuristicPerPhase(Map map, double[][] multiplier, boolean extendedPrint) {
        height = map.getHeight();
        width = map.getWidth();

        //handle multiplier
        this.multiplier = multiplier;
        enables = new boolean[multiplier.length][multiplier[0].length];

        //fill enables
        int wavesIndex = 4;
        for (int phase = 0; phase < multiplier.length; phase++){
            for (int indexOfMultiplier = 0; indexOfMultiplier < multiplier[0].length; indexOfMultiplier++){
                if (indexOfMultiplier == wavesIndex) { //waves also get disables if Field Values and Edges are disabled
                    enables[phase][indexOfMultiplier] = multiplier[phase][indexOfMultiplier] != 0 && (multiplier[phase][fieldValueIndex] != 0 || multiplier[phase][edgesIndex] != 0);
                    continue;
                }
                enables[phase][indexOfMultiplier] = multiplier[phase][indexOfMultiplier] != 0;
            }
        }

        //inits
        fieldValueMatrix = new double[height][width];
        edgeMatrix = new boolean[height][width];
        staticMatrix = new double[height][width];

        staticHeuristicPerPhase = new ArrayList<>(multiplier.length);

        //fill Matrices
        initStaticMatrixAndSetSpecialFields(map);
        initEdgeMatrixAndFieldValueMatrix(map);
        //  Print them
        if (extendedPrint) {
            System.out.println("Field value Matrix:");
            printMatrix(fieldValueMatrix);

            System.out.println("Edge matrix");
            printMatrix(edgeMatrix);
        }

        //create static Heuristics
        for (int phase = 0; phase < multiplier.length; phase++){
            staticHeuristicPerPhase.add(new StaticHeuristic(map, specialFields, (int)multiplier[phase][wavesIndex]));
        }

        //set infos in static Heuristics
        setStaticInfos();
    }

    /**
     * Returns the value of a given position according to the given game phase.
     * @param x The x coordinate of the position.
     * @param y The y coordinate of the position.
     * @param phase The current game phase.
     * @return The value of the given position in the given game phase.
     */
    public double getValueFromMatrix(int x, int y, int phase){
        if (phase-1 >= staticHeuristicPerPhase.size()){
            System.err.println("Phase it too high - getValueFromMatrix");
        }
        return staticHeuristicPerPhase.get(phase-1).matrix[y][x];
    }

    /**
     * Returns the wave value of a given position according to the given game phase.
     * @param pos Current position.
     * @param map The current map.
     * @param phase Current game phase.
     * @return The wave value of the given position in the given game phase.
     */
    public double getWaveValueForPos(Position pos, Map map, int phase) {
        if (phase-1 >= staticHeuristicPerPhase.size()){
            System.err.println("Phase it too high - getWaveValueForPos");
        }
        return staticHeuristicPerPhase.get(phase-1).getWaveValueForPos(pos, map);
    }

    //initialize Matrices per Phase

    private void setStaticInfos() {
        StaticHeuristic sh;

        //add matrices
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (staticMatrix[y][x] == Double.NEGATIVE_INFINITY) {
                    for (StaticHeuristic staticHeuristic : staticHeuristicPerPhase) {
                        staticHeuristic.matrix[y][x] = Double.NEGATIVE_INFINITY;
                    }
                    continue;
                }

                for (int phase = 0; phase < staticHeuristicPerPhase.size(); phase++) {

                    sh = staticHeuristicPerPhase.get(phase);

                    //field Values
                    if (enables[phase][fieldValueIndex])
                        sh.matrix[y][x] = fieldValueMatrix[y][x] * multiplier[phase][fieldValueIndex];

                    //edges
                    if (enables[phase][edgesIndex]) {
                        if (enables[phase][fieldValueIndex])
                            sh.matrix[y][x] *= (edgeMatrix[y][x]) ? multiplier[phase][edgesIndex] : 1;
                        else sh.matrix[y][x] = (edgeMatrix[y][x]) ? multiplier[phase][edgesIndex] : 1;
                    }
                }
            }
        }


        //evaluate every position by its neighbours
        for (StaticHeuristic staticHeuristic : staticHeuristicPerPhase) {
            //TODO: only do this when multiplier says so
            staticHeuristic.setFieldsWithHighValues();
            staticHeuristic.createWaves();
            //staticHeuristic.setFieldsForOverwriteMoves();
        }
    }

    //initialize Static parts

    private void initStaticMatrixAndSetSpecialFields(Map map){
        char currChar;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                //valid Position ?
                currChar = map.getCharAt(x,y);

                switch (currChar){
                    case '-':
                    case 't':
                        staticMatrix[y][x] = Double.NEGATIVE_INFINITY;
                        break;
                    case 'b':
                        specialFields.add(new Position(x,y));
                }
            }
        }
    }

    private void initEdgeMatrixAndFieldValueMatrix(Map map){
        //edge Matrix
        boolean[] outgoingDirections;

        //field Value Matrix
        int[] reachableFields;
        int sumOfReachableFields;

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {

                if (staticMatrix[y][x] == Double.NEGATIVE_INFINITY) continue;

                //edge Matrix
                outgoingDirections = getOutgoingDirections(new Position(x, y), map);
                edgeMatrix[y][x] = !isCaptureAble(outgoingDirections);


                //field value matrix
                sumOfReachableFields = 0;
                reachableFields = checkReachableFields(new Position(x, y), map);
                for (int a : reachableFields) sumOfReachableFields += a;
                fieldValueMatrix[y][x] = (double) sumOfReachableFields / map.getCountOfReachableFields();
            }
        }
    }

    //helpers for initEdgeMatrixAndFieldValueMatrix

    private boolean[] getOutgoingDirections(Position savedPos, Map map){
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

    private boolean isCaptureAble(boolean[] outgoingDirections){

        int blockedAxisCount = 0;

        for (int r = 0; r < 4; r++){
            if (!outgoingDirections[r] || !outgoingDirections[(r+4)%8]) blockedAxisCount++;
        }

        if (blockedAxisCount == 4) return false;
        else return true;
    }

    private int[] checkReachableFields(Position savedPos, Map map) {
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
                if (reachableFields[r] > 8 * (width-2) * (height-2) ) { // 8 because of the 8 directions. reachable fields = maximum stones a field in any direction could reach.
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

    //helper

    /**
     * This method prints out the given matrix in a nicely readable way. Every value is formatted with %4.2f.
     * For negative and positive infinity a "+" or "-" gets printed.
     * @param matrix The double matrix to print.
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
    /**
     * This method prints out the given matrix in a nicely readable way.
     * For "true" it prints a "+" and for "false" a "-".
     * @param matrix The boolean matrix to print.
     */
    public void printMatrix(boolean[][] matrix){
        char currChar;
        for (int y = 0; y < matrix.length; y++){
            for (int x = 0; x < matrix[y].length; x++){
                currChar = (edgeMatrix[y][x])? '+' : '-';
                System.out.printf("%3s", currChar);
            }
            System.out.println();
        }
        System.out.println();
    }
}
