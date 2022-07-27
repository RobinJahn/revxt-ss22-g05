package src;

/**
 * This class is used for the killer heuristic. It stores all the necessary information for it and provides the functionality to modify it.
 */
public class KillerArray {

    private final KillerLine[] killerLineArray;
    private int length = 0;
    private final int maxLength = 10;

    /**
     * This constructor creates a new KillerArray of a given size of 10.
     */
    public KillerArray()
    {
        this.killerLineArray = new KillerLine[maxLength];
    }

    /**
     * This function returns the position and additional information of a move in the KillerArray.
     * @param index The index of the position to take.
     * @return Returns the position and information of an entry in the KillerArray.
     */
    public int[] getPositionAndInfo(int index)
    {
        if(index<= length) {
            return killerLineArray[index].getPositionAndInfo().toIntArray();
        }
        return null;
    }

    /**
     * This function returns the length of the KillerArray.
     * @return Returns the length of the KillerArray.
     */
    public int getLength()
    {
        return length;
    }

    /**
     * This function adds a new move to the KillerArray, if there is space available or the move cuts more moves than the currently worst move saved.
     * @param PositionAndInfo The new move to check
     * @param NumberOfCutLeaves The number of leaves the move cut.
     */
    public void add(PositionAndInfo PositionAndInfo, int NumberOfCutLeaves)
    {
        //If Space is available
        if(length < maxLength)
        {   killerLineArray[length] = new KillerLine(PositionAndInfo, NumberOfCutLeaves);
            length++;
        }
        //If No Space left
        else
        {
            // If last Space is worse, then current Cut Swap in
            if(killerLineArray[maxLength-1].getNumberOfCutLeaves()<NumberOfCutLeaves)
            {
                killerLineArray[maxLength-1].setPositionAndInfo(PositionAndInfo);
                killerLineArray[maxLength-1].setNumberOfCutLeaves(NumberOfCutLeaves);
            }
        }
        sort();
    }

    //Long live the BubbleSort
    private void sort(){
        for(int i = 0;i<length-1;i++)
        {
            for(int j = i+1;j<length;j++)
            {
                if(killerLineArray[i].getNumberOfCutLeaves()< killerLineArray[j].getNumberOfCutLeaves())
                {
                    KillerLine temp = killerLineArray[i];
                    killerLineArray[i] = killerLineArray[j];
                    killerLineArray[j] = temp;
                }
            }
        }
    }

    private static class KillerLine {
        private PositionAndInfo Pos;
        private int NumberOfCutLeaves;

        private KillerLine(PositionAndInfo pos, int NumberOfCutLeaves)
        {
            this.Pos = pos.clone();
            this.NumberOfCutLeaves = NumberOfCutLeaves;
        }

        private PositionAndInfo getPositionAndInfo() {
            return Pos;
        }

        private int getNumberOfCutLeaves() {
            return NumberOfCutLeaves;
        }

        private void setPositionAndInfo(PositionAndInfo pos) {
            Pos = pos;
        }

        private void setNumberOfCutLeaves(int numberOfCutLeaves) {
            NumberOfCutLeaves = numberOfCutLeaves;
        }
    }
}
