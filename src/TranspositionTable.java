package src;

/**
 * This method contains all the information of the Transposition-table of the Zobrist-Hashing-Method.
 * Also, it provides the needed methods to set, update or get this information.
 */
public class TranspositionTable {

    private final int size;

    private Transposition[] TranspositionArray;

    private long TranspositionHits = 0;
    private long TranspositionMiss = 0;
    private long Replacements = 0;

    /**
     * Creates a Transposition Table of the given Size.
     * @param size The Size of the TranspositionTable
     */
    public TranspositionTable(int size)
    {
        this.size = size;
        TranspositionArray = new Transposition[size];
    }

    /**
     * Empties the Transposition Table
     */
    public void empty()
    {
        TranspositionArray = new Transposition[size];
    }

    /**
     * Tries to find the given hash in the Transposition Table and returns the Value of the Map in case of a Hit or in Case of a Miss a Default Value.
     * @param hash The Hash Value to look up
     * @param depth The Current Depth
     * @return Returns the Value of the given Map or the Min Value of a Double + 1
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
     * Adds the Value of a Map represented by the hash Value to the Transposition Table for a given depth.
     * @param hash The Hash Value of the Map
     * @param depth The Current Depth
     * @param value The Value of the Map
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
     * Returns the Number of Transposition Hits.
     * @return The Number of Transposition Hits
     */
    public long getTranspositionHits()
    {
        return TranspositionHits;
    }

    /**
     * Returns the Number of Transposition Misses.
     * @return The Number of Transposition Misses
     */
    public long getTranspositionMiss() { return  TranspositionMiss;}

    /**
     * Returns the Number of Entries which have been replaced.
     * @return The Number of Replacements
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
