package src;

import java.util.ArrayList;
import java.util.Arrays;

public class StaticHeuristicPerPhase {

    ArrayList<StaticHeurisic> staticHeuristicPerPhase;

    int height;
    int width;

    int stonesIndex = 0;
    int movesIndex = 1;
    int fieldValueIndex = 2;
    int edgesIndex = 3;
    int wavesIndex = 4;

    boolean extendedPrint;

    public double[][] staticMatrix;
    public double[][] fieldValueMatrix;
    public boolean[][] edgeMatrix;


    private ArrayList<Position> specialFields = new ArrayList<>(); //TODO: fill

    double[][] multiplier; //[] phases, [] stones, moves, Field Values, Edges, Waves
    boolean[][] enables;


    public StaticHeuristicPerPhase(Map map, double[][] multiplier, boolean extendedPrint) {
        height = map.getHeight();
        width = map.getWidth();
        this.extendedPrint = extendedPrint;

        //handle multiplier
        this.multiplier = multiplier;
        enables = new boolean[multiplier.length][multiplier[0].length];

        for (int phase = 0; phase < multiplier.length; phase++){
            for (int indexOfMultiplier = 0; indexOfMultiplier < multiplier[0].length; indexOfMultiplier++){
                if (indexOfMultiplier == wavesIndex) { //waves also get disables if Field Values and Edges are disabled
                    enables[phase][indexOfMultiplier] = multiplier[phase][indexOfMultiplier] != 0 && (multiplier[phase][fieldValueIndex] != 0 || multiplier[phase][edgesIndex] != 0);
                    continue;
                }
                enables[phase][indexOfMultiplier] = multiplier[phase][indexOfMultiplier] != 0;
            }
        }

        //handle matrices
        fieldValueMatrix = new double[height][width];
        edgeMatrix = new boolean[height][width];

        staticHeuristicPerPhase = new ArrayList<>(multiplier.length);

        for (int phase = 0; phase < multiplier.length; phase++){
            staticHeuristicPerPhase.add(new StaticHeurisic(map, specialFields, (int)multiplier[phase][wavesIndex]));
        }

        setStaticInfos(map);
    }

    public double getValueFromMatrix(int x, int y, int phase){
        return staticHeuristicPerPhase.get(phase-1).matrix[y][x];
    }

    //initialize Static parts

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
                edgeMatrix[y][x] = isCapturable(outgoingDirections);


                //field value matrix
                sumOfReachableFields = 0;
                reachableFields = checkReachableFields(new Position(x, y), map);
                for (int a : reachableFields) sumOfReachableFields += a;
                fieldValueMatrix[y][x] = (double) sumOfReachableFields / map.getCountOfReachableFields();
            }
        }


    }

    private void initStaticMatrix(Map map){
        char currChar;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                //valid Position ?
                currChar = map.getCharAt(x,y);
                if (currChar == '-' || currChar == 't') {
                    staticMatrix[y][x] = Double.NEGATIVE_INFINITY;
                }

            }
        }
    }

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

    private boolean isCapturable(boolean[] outgoingDirections){

        int blockedAxisCount = 0;

        for (int r = 0; r < 4; r++){
            if (!outgoingDirections[r] || !outgoingDirections[(r+4)%8]) blockedAxisCount++;
        }

        if (blockedAxisCount ==4) return false;
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

    //initialize Matrices per Phase

    private void setStaticInfos(Map map) {
        StaticHeurisic sh;

        initStaticMatrix(map);
        initEdgeMatrixAndFieldValueMatrix(map);

        if (extendedPrint) {
            System.out.println("Field value Matrix:");
            printMatrix(fieldValueMatrix);

            System.out.println("Edge matrix");
            printMatrix(edgeMatrix);
        }


        //add matrices
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {

                if (staticMatrix[y][x] == Double.NEGATIVE_INFINITY) continue;

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
        for (StaticHeurisic staticHeurisic : staticHeuristicPerPhase) {
            staticHeurisic.setFieldsWithHighValues();
            staticHeurisic.createWaves();
            //staticHeurisic.setFieldsForOverwriteMoves();
        }
    }

    //helper
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
