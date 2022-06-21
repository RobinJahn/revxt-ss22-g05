package src;

import java.util.ArrayList;

public class StaticHeuristicPerPhase {

    private final ArrayList<StaticHeurisic> staticHeuristicPerPhase;

    private final int height;
    private final int width;

    private final int stonesIndex = 0;
    private final int movesIndex = 1;
    private final int fieldValueIndex = 2;
    private final int edgesIndex = 3;
    private final int wavesIndex = 4;

    private final boolean extendedPrint;

    private final  double[][] staticMatrix;
    private final  double[][] fieldValueMatrix;
    private final  boolean[][] edgeMatrix;


    private final ArrayList<Position> specialFields = new ArrayList<>();

    private final double[][] multiplier; //[] phases, [] stones, moves, Field Values, Edges, Waves
    private final boolean[][] enables;


    public StaticHeuristicPerPhase(Map map, double[][] multiplier, boolean extendedPrint) {
        height = map.getHeight();
        width = map.getWidth();
        this.extendedPrint = extendedPrint;

        //handle multiplier
        this.multiplier = multiplier;
        enables = new boolean[multiplier.length][multiplier[0].length];

        //fill enables
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

        //create static Heurisics
        for (int phase = 0; phase < multiplier.length; phase++){
            staticHeuristicPerPhase.add(new StaticHeurisic(map, specialFields, (int)multiplier[phase][wavesIndex]));
        }

        //set infos in static Heurisics
        setStaticInfos();
    }

    public double getValueFromMatrix(int x, int y, int phase){
        if (phase-1 >= staticHeuristicPerPhase.size()){
            System.err.println("Phase it too high - getValueFromMatrix");
        }
        return staticHeuristicPerPhase.get(phase-1).matrix[y][x];
    }

    public double getWaveValueForPos(Position pos, Map map, int phase) {
        if (phase-1 >= staticHeuristicPerPhase.size()){
            System.err.println("Phase it too high - getWaveValueForPos");
        }
        return staticHeuristicPerPhase.get(phase-1).getWaveValueForPos(pos, map);
    }

    //initialize Matrices per Phase

    private void setStaticInfos() {
        StaticHeurisic sh;

        //add matrices
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                if (staticMatrix[y][x] == Double.NEGATIVE_INFINITY) {
                    for (StaticHeurisic staticHeurisic : staticHeuristicPerPhase) {
                        staticHeurisic.matrix[y][x] = Double.NEGATIVE_INFINITY;
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
        for (StaticHeurisic staticHeurisic : staticHeuristicPerPhase) {
            staticHeurisic.setFieldsWithHighValues();
            staticHeurisic.createWaves();
            //staticHeurisic.setFieldsForOverwriteMoves();
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
                edgeMatrix[y][x] = !isCapturable(outgoingDirections);


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
