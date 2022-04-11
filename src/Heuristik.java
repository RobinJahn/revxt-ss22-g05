package src;

import java.util.ArrayList;

public class Heuristik {
    final boolean printOn;

    private Map map; //is automatically updated because of the reference here
    //the color of the player for wich the map is rated
    private final int myColorI;
    private final char myColorC;
    //the matrix that rates the different fields
    private double[][] matrix;
    //information that may be worth saving
    private ArrayList<Position> myFields = new ArrayList<>();
    private ArrayList<Position> bonusFields = new ArrayList<>();
    private ArrayList<Position> inversionFields = new ArrayList<>();
    private ArrayList<Position> choiceFields = new ArrayList<>();
    //relevant information
    private double stonePercentage = 0;
    private double movesPercentage = 0;
    private double sumOfMyFields = 0;
    //heuristic values
    private final int base = 3;


    /**
     * Constructor - initializes map and my color.
     * Calculates static infos about the map
     * @param map the map in any state. Only the static information are relevant
     * @param myColor the number(color) of the player for wich the map is rated - doesn't change for the client
     */
    public Heuristik(Map map, int myColor, boolean printOn){
        this.printOn = printOn;
        this.map = map;
        this.myColorI = myColor;
        this.myColorC = Integer.toString(myColor).charAt(0);
        matrix = new double[map.getHeight()][map.getWidth()];
        setStaticInfos();
        if (printOn) printMatrix();
        addWaveMatrix();
        if (printOn) System.out.println("Added Waves");
        if (printOn) printMatrix();
    }

    /**
     * Function to evaluate the map that was given the constructor (call by reference)
     * @return returns the value of the map
     */
    public double evaluate(){
        double result = 0;

        //update relevant infos
        setDynamicInfos();

        result += sumOfMyFields/ myFields.size(); //durchschnittswert meiner felder
        if (printOn) System.out.println("Sum of my field �: " + result);

        result += movesPercentage - 1;
        result += stonePercentage*100 - 1;
        //value
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
        int validFields = 0; //make sure it resets if that might be called more often

        for (int y = 0; y < map.getHeight(); y++){
            for (int x = 0; x < map.getWidth(); x++) {
                //set new position
                currPos.x = x;
                currPos.y = y;
                //get char at position
                currChar = map.getCharAt(x,y);

                //if it's a valid field
                if (currChar != '-' && currChar != 't'){
                    validFields++;
                    //calculate edges
                    matrix[y][x] += checkForEdges(currPos);
                }

                switch (currChar){
                    //mark invalid fields
                    case '-':
                    case 't':
                        matrix[y][x] = Double.NEGATIVE_INFINITY;
                        break;

                    //list bonus fields
                    case 'b':
                        bonusFields.add(new Position(x,y));
                        matrix[y][x] += 5;
                        break;

                    //list inversion fields
                    case 'i':
                        inversionFields.add(new Position(x,y));
                        matrix[y][x] += 5;
                        break;

                    //list choice fields
                    case 'c':
                        choiceFields.add(new Position(x,y));
                        matrix[y][x] += 5;
                        break;
                }
            }
        }
    }

    /**
     * updates the values that are relevant for the heuristic evaluation for the current state the map is in
     */
    private void setDynamicInfos(){
        sumOfMyFields = 0;
        myFields.clear();
        //get my stone positions in %
        int countOfOwnStones = 0;
        int countOfEnemyStones = 0;
        //get enemy stone percentage
        double enemyStonesPercentage;
        double enemyMovesPercentage;
        //get possible moves in %
        int myPossibleMoves = 0;
        int possibleMovesOfEnemys = 0;


        char currChar;
        Position currPos = new Position(0,0); //position that goes through whole map

        //goes through every position of the map
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                //set new position
                currPos.x = x;
                currPos.y = y;
                //get char at position
                currChar = map.getCharAt(x,y);

                //saves field and counts stones that are of own color
                if (currChar == myColorC) {
                    myFields.add(new Position(x, y));
                    countOfOwnStones++;
                    sumOfMyFields += matrix[y][x];
                }
                //if an enemy stone is there
                else if (Character.isDigit(currChar) && currChar != '0'){
                    countOfEnemyStones++;
                }
            }
        }

        //gets possible moves of all players and adds them to the corresponding move counter
        for (int i = 1; i <= map.getAnzPlayers(); i++){
            if (myColorI == map.getCurrentlyPlayingI()){
                myPossibleMoves = Client.getValidMoves(map).size();
            }
            else {
                possibleMovesOfEnemys += Client.getValidMoves(map).size();
            }
            map.nextPlayer();
        } //reset to currently playing

        //get percentages out of the values
        enemyStonesPercentage = (double)countOfEnemyStones/((double)map.getAnzPlayers()-1);
        enemyMovesPercentage = (double)possibleMovesOfEnemys/((double)map.getAnzPlayers()-1);

        //set relevant infos

        //set stone percentage
        if (enemyStonesPercentage != 0) stonePercentage = (double)countOfOwnStones/ enemyStonesPercentage; //  myStones/ average stones of enemy
        else stonePercentage = 10; //if enemy has no stones you have 10.000 % of his stones
        //set possible moves percentage
        if (enemyMovesPercentage != 0) movesPercentage = (double)myPossibleMoves/ enemyMovesPercentage;
        else movesPercentage = 10;

        if (printOn) System.out.println("countOfOwnStones: " + countOfOwnStones + " countOfEnemyStones �: " + enemyStonesPercentage  + " (countOfEnemyStones: " + countOfEnemyStones + ")");
        if (printOn) System.out.println("myPossibleMoves: " + myPossibleMoves +  " possibleMovesOfEnemys �: " +  enemyMovesPercentage  + " (possibleMovesOfEnemys: " + possibleMovesOfEnemys + ")");
    }

    /**
     * checks if the current position is an edge position and wich kind
     * for the different kinds it returns a different value
     * @param pos position to check
     * @return returns value of the field
     */
    private double checkForEdges(Position pos){
        int backedUpOutgoings = 0; //count's the axes across wich the stone could be captured
        int oppositeDirection;
        boolean firstDirectionIsWall;
        boolean secondDirectionIsWall;
        char charAtPos;
        Position savedPos = pos;

        for (int r = 0; r <= 3; r++) { //checks the 4 axes
            firstDirectionIsWall = false;
            secondDirectionIsWall = false;
            //check first direction
            pos = Position.goInR(pos,r);
            charAtPos = map.getCharAt(pos);
            //check if there's a wall
            if(charAtPos == '-') {
                firstDirectionIsWall = true;
            }
            //check if there's a transition and if it's relevant
            if(charAtPos == 't'){
                if (map.transitionen.get(Transitions.saveInChar(savedPos.x,savedPos.y,r)) == null) firstDirectionIsWall = true;
            }
            //reset position
            pos = savedPos.clone();

            //check second direction
            oppositeDirection = (r+4)%8;
            pos = Position.goInR(pos,oppositeDirection);
            charAtPos = map.getCharAt(pos);
            //check if there's a wall
            if(charAtPos == '-') {
                secondDirectionIsWall = true;
            }
            //check if there's a transition and if it's relevant
            if(charAtPos == 't'){
                if (map.transitionen.get(Transitions.saveInChar(savedPos.x,savedPos.y,oppositeDirection)) == null) secondDirectionIsWall = true;
            }
            //reset position
            pos = savedPos.clone();

            //increase axis count
            if (firstDirectionIsWall ^ secondDirectionIsWall) backedUpOutgoings++; //xor - if one of them is a wall and the other isn't

        }
        //heuristik
        return Math.pow(base,backedUpOutgoings); //backed up outgoings can have values from 0 to 4
    }

    /**
     * takes the greater values of the matrix and creates waves/ rings of alternating negative and positive decreasing values around it
     */
    private void addWaveMatrix(){
        int[][] waveMatrix = new int[matrix.length][matrix[0].length];
        ArrayList<Position> highValues = new ArrayList<>();
        int lowerLimit = 4;
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
            createWave(waveMatrix, pos);
        }

        if (printOn) {
            for (int y = 0; y < map.getHeight(); y++) {
                for (int x = 0; x < map.getWidth(); x++) {
                    if (matrix[y][x] != Double.NEGATIVE_INFINITY) matrix[y][x] += waveMatrix[y][x];
                    System.out.printf("%4d", waveMatrix[y][x]);
                }
                System.out.println();
            }
            System.out.println();
        }
    }

    /**
     * creates the waves around one position
     * @param waveMatrix the matrix to create the waves in
     * @param pos the position around wich the waves are created
     */
    private void createWave(int[][] waveMatrix, Position pos){
        int x1, x2, y1, y2;
        x1 = pos.x;
        x2 = pos.x;
        y1 = pos.y;
        y2 = pos.y;
        int loopSign = -1;
        double value = matrix[pos.y][pos.x]/base; //sets value to value of field/2

        while(value >= base) {
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
                        if(yi >= 0 && yi < waveMatrix.length && xi >= 0 && xi < waveMatrix[0].length)  waveMatrix[yi][xi] += loopSign * value; //if catches indices that went over the edge
                    }
                }
                else { //column between the rows
                    for (int xi : new int[]{x1,x2}){
                        if(yi >= 0 && yi < waveMatrix.length && xi >= 0 && xi < waveMatrix[0].length) waveMatrix[yi][xi] += loopSign * value; //if catches indices that went over the edge
                    }
                }
            }
            loopSign *= -1; //swap loop vorzeichen
            value /= base;
        }
    }
}