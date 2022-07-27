package src;

/**
 * This method contains all the information of the transposition table of the zobrist hashing method.
 * Also, it provides the needed methods to set, update or get this information.
 */
public class TranspositionTable {

    private final int size;

    private Transposition[] TranspositionArray;

    private long TranspositionHits = 0;
    private long TranspositionMiss = 0;
    private long Replacements = 0;

    /**
     * Creates a transposition table of the given size.
     * @param size The size of the transposition table.
     */
    public TranspositionTable(int size)
    {
        this.size = size;
        TranspositionArray = new Transposition[size];
    }

    /**
     * Empties the transposition table.
     */
    public void empty()
    {
        TranspositionArray = new Transposition[size];
    }

    /**
     * Tries to find the given hash in the transposition table and returns the value of the map in case of a hit or in case of a miss a default value.
     * @param hash The hash value to look up.
     * @param depth The current depth.
     * @return Returns the value of the given map or the min value of a double + 1.
     */
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

    /**
     * Adds the value of a map represented by the hash value to the transposition table for a given depth.
     * @param hash The hash value of the map.
     * @param depth The current depth.
     * @param value The value of the map.
     */
    public void add(long hash, int depth, double value)
    {
        int index = (int)hash%size;
        if(index <0)
        {
            index *=-1;
        }

        Transposition transposition = new Transposition(hash, depth, value);

        if(TranspositionArray[index] != null)
        {
            Replacements++;
        }

        TranspositionArray[index] = transposition;
    }

    /**
     * Returns the number of transposition hits.
     * @return The number of transposition hits.
     */
    public long getTranspositionHits()
    {
        return TranspositionHits;
    }

    /**
     * Returns the number of transposition misses.
     * @return The number of transposition misses.
     */
    public long getTranspositionMiss() { return  TranspositionMiss;}

    /**
     * Returns the number of entries which have been replaced.
     * @return The number of replacements.
     */
    public long getReplacements() { return Replacements;}

    private static class Transposition
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
}
