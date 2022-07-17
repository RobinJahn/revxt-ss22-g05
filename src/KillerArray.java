package src;

public class KillerArray {

    private final KillerLine[] killerLineArray;
    private int length = 0;
    private final int maxLength = 10;

    /**
     * This Constructor creates a new KillerArray of a given Size of 10.
     */
    public KillerArray()
    {
        this.killerLineArray = new KillerLine[maxLength];
    }

    /**
     * This Function returns the Position and Additional Information of a Move in the KillerArray
     * @param index The Index of the Position to take
     * @return Returns the Position and Information of an entry in the KillerArray.
     */
    public int[] getPositionAndInfo(int index)
    {
        if(index<= length) {
            return killerLineArray[index].getPositionAndInfo().toIntArray();
        }
        return null;
    }

    /**
     * This Function returns the Length of the KillerArray.
     * @return Returns the Length of the KillerArray.
     */
    public int getLength()
    {
        return length;
    }

    /**
     * This Function Adds a new Move to the KillerArray, if there is Space available or the move cuts more moves than the currently worst move saved.
     * @param PositionAndInfo The New Move to Check
     * @param NumberOfCutLeaves The Number of Leaves the Move cut.
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
