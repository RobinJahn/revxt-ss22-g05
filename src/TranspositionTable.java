package src;

public class TranspositionTable {

    private final int size;

    private Transposition[] TranspositionArray;

    private long TranspositionHits = 0;
    private long TranspositionMiss = 0;
    private long Replacements = 0;

    public TranspositionTable(int size)
    {
        this.size = size;
        TranspositionArray = new Transposition[size];
    }


    public void empty()
    {
        TranspositionArray = new Transposition[size];
    }

    public double lookUp(long hash, int depth)
    {
        int index = (int)hash%size;
        if(index <0)
        {
            index *=-1;
        }
        Transposition transposition = TranspositionArray[index];
        if(transposition != null)
        {
            if(transposition.depth >= depth && TranspositionArray[index].hash == hash)
            {
                TranspositionHits++;
                return transposition.value;
            }
        }
        TranspositionMiss++;
        return Double.MIN_VALUE+1;
    }

    public void add(long hash, int depth, double value)
    {
        int index = (int)hash%size;
        if(index <0)
        {
            index *=-1;
        }

        Transposition transposition = new Transposition(hash,depth,value);

        TranspositionArray[index] = transposition;
    }

    public long getTranspositionHits()
    {
        return TranspositionHits;
    }
    public long getTranspositionMiss() { return  TranspositionMiss;}
    public long getReplacements() { return Replacements;}
}

 class Transposition
{
    long hash;
    double value;
    int depth;

    public Transposition(long hash, int depth, double value)
    {
        this.hash = hash;
        this.depth = depth;
        this.value = value;
    }
}