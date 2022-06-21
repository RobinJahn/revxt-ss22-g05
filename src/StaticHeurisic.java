package src;

import java.util.*;

public class StaticHeurisic {
    public double[][] matrix;

    public HashMap<Position, LinkedList<PositionAndValue>> wavesAndFields;
    public ArrayList<Position> fieldsWithHighValues = new ArrayList<>();

    private final int waveCount;

    ArrayList<Position> specialFields;
    Map map;

    public StaticHeurisic(Map map, ArrayList<Position> specialFields, int waveCount){
        matrix = new double[map.getHeight()][map.getWidth()];
        wavesAndFields = new HashMap<>();

        this.specialFields = specialFields;
        this.map = map;
        this.waveCount = waveCount;
    }

    //  waves

    public void setFieldsWithHighValues(){
        double percentageOfValues = 0.05;
        int i = 0;
        double lastValue;
        int currIdnex;
        Position pos;

        fieldsWithHighValues = new ArrayList<>();

        ArrayList<Position> posList = new ArrayList<>();
        ArrayList<Double> valueList = new ArrayList<>();
        ArrayList<Integer> indexList = new ArrayList<>();

        for (int y = 0; y < matrix.length; y++) {
            for (int x = 0; x < matrix[0].length; x++) {

                //add high rated fields of matrix
                posList.add(new Position(x, y));
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

            fieldsWithHighValues.add(pos);
        }

    }


    public double getWaveValueForPos(Position currPos, Map map){
        double result = 0;
        LinkedList<PositionAndValue> list;
        char charAtPos;

        //get List of values for the position
        list = wavesAndFields.get(currPos);
        if (list != null) {
            //go throu every position that creats a wave that's affacting this position
            for (PositionAndValue pav : list) {
                //get char at creating position
                charAtPos = map.getCharAt(pav.x, pav.y);
                //only use the value when the field is free
                if (charAtPos == '0' || !Character.isDigit(charAtPos)) {
                    result += pav.value;
                }
            }
        }

        return result;
    }

    public void createWaves(){
        wavesAndFields = new HashMap<>();

        for (Position pos : specialFields){
            createWaveForPos(pos);
        }
        for (Position pos : fieldsWithHighValues) {
            if (specialFields.contains(pos)) continue;
            createWaveForPos(pos);
        }
    }

    private void createWaveForPos(Position pos){
        int divisor = 5;
        double originValue = 0;

        //for breadth-first search
        Queue<PositionAndInfo> posQ = new LinkedList<>();
        LinkedList<PositionAndValue> currList;
        Position currPos;
        Position posAfterStep;
        PositionAndInfo currPosAndDist;
        PositionAndValue posAndVal;
        int distanceFromOriginForNewField = 0;
        Integer newR;

        int index;
        PositionAndValue posAndValInList;

        if (specialFields.contains(pos)) originValue += 100;
        if (fieldsWithHighValues.contains(pos)) originValue += matrix[pos.y][pos.x];

        posQ.add(new PositionAndInfo(pos.x,pos.y,distanceFromOriginForNewField));

        while (!posQ.isEmpty()){
            currPosAndDist = posQ.poll();
            currPos = new Position(currPosAndDist.x, currPosAndDist.y);
            distanceFromOriginForNewField = currPosAndDist.info + 1;


            //go in every possible direction
            for (int r = 0; r <= 7; r++) {
                posAfterStep = currPos.clone();
                //get position it will move to
                newR = map.doAStep(posAfterStep, r);
                if (newR == null || specialFields.contains(posAfterStep) || fieldsWithHighValues.contains(posAfterStep)) continue;

                //create position and value for this position
                posAndVal = new PositionAndValue(pos.x, pos.y, originValue/Math.pow(-divisor, distanceFromOriginForNewField) );

                //check what's there
                currList = wavesAndFields.get(posAfterStep);

                //if the field wasn't visited yet
                if (currList == null) {
                    //create List
                    wavesAndFields.put( posAfterStep, new LinkedList<>());
                    //add current positionAndValue to this field
                    wavesAndFields.get(posAfterStep).add(posAndVal);
                    if (distanceFromOriginForNewField < waveCount) posQ.add(new PositionAndInfo(posAfterStep.x, posAfterStep.y, distanceFromOriginForNewField));
                }
                //if the field was already visited but not from us (if we visited it already that route was faster and so would create a |hieger| value)
                else if (!currList.contains(posAndVal)) {
                    //add current positionAndValue to this field
                    currList.add(posAndVal);
                    if (distanceFromOriginForNewField < waveCount) posQ.add(new PositionAndInfo(posAfterStep.x, posAfterStep.y, distanceFromOriginForNewField));
                }
            }

            /*
            System.out.println("Wave Matrix: For field " + pos + ", and processing " + currPos);
            double val;
            Position posInLoop;
            String buffer;
            for(int y = 0; y < matrix.length; y++){
                for (int x = 0; x < matrix[0].length; x++){
                    posInLoop = new Position(x,y);
                    val = getWaveValueForPos(posInLoop);
                    buffer = "";
                    if (posInLoop.equals(currPos)) buffer += "(";
                    if (posInLoop.equals(pos)) buffer += "[";

                    if (val == 0){
                        buffer += "-";
                    }
                    else {
                         buffer += String.format("%4.2f", val);
                    }

                    if (posInLoop.equals(pos)) buffer += "]";
                    if (posInLoop.equals(currPos)) buffer += ")";

                    System.out.printf("%10s", buffer);
                }
                System.out.println();
            }
            System.out.println();
             */

        }
    }

}
