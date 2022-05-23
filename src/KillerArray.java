package src;

public class KillerArray {

    private KillerLine[] killerLineArray;

    private int length = 0;

    private int maxLength = 10;

    public KillerArray(int size)
    {
        this.killerLineArray = new KillerLine[size];
        this.maxLength = size;
    }

    public KillerArray()
    {
        this.killerLineArray = new KillerLine[10];
    }

    public int[] getPositionAndInfo(int index)
    {
        if(index<= length) {
            return killerLineArray[index].getPositionAndInfo().toIntArray();
        }
        return null;
    }

    public int getLength()
    {
        return length;
    }

    public void add(PositionAndInfo PositionAndInfo, int NumberOfCutLeaves)
    {
        //If Space is available
        if(length < maxLength)
        {   killerLineArray[length] = new KillerLine(PositionAndInfo,NumberOfCutLeaves);
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
}
