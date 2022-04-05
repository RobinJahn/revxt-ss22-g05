package src;

import java.util.ArrayList;

public class Heuristik {

    private Map map; //is automaticly updated because of the reference here
    //the color of the player for wich the map is rated
    private int myColorI;
    private final char myColorC;
    //the matrix that rates the different fields
    private double[][] matrix;
    //information that may be worth saving
    private ArrayList<Position> myFileds = new ArrayList<>();
    private ArrayList<Position> bonusFields = new ArrayList<>();
    private ArrayList<Position> inversionFields = new ArrayList<>();
    private ArrayList<Position> coiseFields = new ArrayList<>();
    private int validFields = 0;
    //relevant information
    private double stonePercentage = 0;
    private double movesPercentage = 0;
    private double sumOfMyFields = 0;
    //heuristic values
    private final int base = 3;


    /**
     * Constructor - initializes map and my color.
     * Calculates static infos about the map
     * @param map the map in any state. Only the ststic information are relevant
     * @param myColor the number(color) of the player for wich the map is rated - doesn't change for the client
     */
    public Heuristik(Map map, int myColor){
        this.map = map;
        this.myColorI = myColor;
        this.myColorC = Integer.toString(myColor).charAt(0);
        matrix = new double[map.getHeight()][map.getWidth()];
        setStaticRelevantInfos();
        printMatrix();
        addWaveMatrix();
        printMatrix();
    }

    /**
     * Function to evaluate the map that was given the constructor (call by reference)
     * @return
     */
    public double evaluate(){
        double result = 0;

        //update relevant infos
        setDynamicRelevantInfos();

        result += sumOfMyFields/myFileds.size(); //durchschnittswert meiner felder
        System.out.println("Sum of my field Ø: " + result);
        //result += get%myKeystones(result)*ModifierKeytones(Gamelength,TurnNumber,Playercount,Tiefe)
        result += movesPercentage - 1;
        result += stonePercentage - 1;
        //value
        return result;
    }

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
     * Current heuristik implimentations:
     *      check for capturability
     *      check for outgoings
     *
     * Information that are currently saved:
     *      bonus
     *      inversion
     *      choise
     */
    private void setStaticRelevantInfos(){
        char currChar;
        Position currPos = new Position(0,0); //position that goes throu whole map
        validFields = 0; //make shure it resets if that might be called more often

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

                    //list coise fields
                    case 'c':
                        coiseFields.add(new Position(x,y));
                        matrix[y][x] += 5;
                        break;
                }
            }
        }
    }

    /**
     * updates the values that are relevant for the heuristical evaluation
     */
    private void setDynamicRelevantInfos(){
        sumOfMyFields = 0;
        myFileds.clear();
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
        Position currPos = new Position(0,0); //position that goes throu whole map

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
                    myFileds.add(new Position(x, y));
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
        //set posible moves percentage
        if (enemyMovesPercentage != 0) movesPercentage = (double)myPossibleMoves/ enemyMovesPercentage;
        else movesPercentage = 10;

        System.out.println("countOfOwnStones: " + countOfOwnStones + " countOfEnemyStones Ø: " + enemyStonesPercentage  + " (countOfEnemyStones: " + countOfEnemyStones + ")");
        System.out.println("myPossibleMoves: " + myPossibleMoves +  " possibleMovesOfEnemys Ø: " +  enemyMovesPercentage  + " (possibleMovesOfEnemys: " + possibleMovesOfEnemys + ")");
    }

    /**
     * checks if the current position is an edge position and wich kind
     * for the different kinds it returns a different value
     * @param pos position to check
     * @return
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

    //create waves
    private void addWaveMatrix(){
        int[][] waveMatrix = new int[matrix.length][matrix[0].length];
        ArrayList<Position> heighValues = new ArrayList<>();
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
                    heighValues.add(currPos.clone());
                }
            }
        }

        for (Position pos : heighValues){
            createWave(waveMatrix, pos);
        }

        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                if (matrix[y][x] != Double.NEGATIVE_INFINITY) matrix[y][x] += waveMatrix[y][x];
                System.out.printf("%4d", waveMatrix[y][x]);
            }
            System.out.println();
        }
        System.out.println();
    }

    private void createWave(int[][] waveMatrix, Position pos){
        int x1, x2, y1, y2;
        x1 = pos.x;
        x2 = pos.x;
        y1 = pos.y;
        y2 = pos.y;
        int loopSign = -1;
        int threshhold = base;
        double value = matrix[pos.y][pos.x]/base; //sets value to value of field/2

        while(value >= threshhold) {
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
