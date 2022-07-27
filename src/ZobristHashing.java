package src;

import java.util.Random;

/**
 * This class provides the functionality needed to use the zobrist hashing method.
 */
public class ZobristHashing {

    final private int size;

    final private int width;

    final private int playerNumber;

    final private long[][] zobristTable;

    final private long notMyTurn;

    /**
     * Instantiates a zobrist table, of the size of height * width and depth 2, and fills it with random long values. Also generates a random long value for it being not the own turn.
     * @param height The height of the map for which to create a zobrist table for.
     * @param width The width of the map for which to create a zobrist table for.
     * @param playerNumber The player number of the client for this map.
     */
    public ZobristHashing(int height, int width, int playerNumber)
    {
        Random RandomGen = new Random();

        size = height*width;
        this.width = width;
        this.playerNumber = playerNumber;

        zobristTable = new long[size][2];
        for(int i= 0; i< size;i++)
        {
            for(int j = 0; j< 2; j++)
            {
                zobristTable[i][j] = RandomGen.nextLong();
            }
        }
        notMyTurn = RandomGen.nextLong();
    }

    /**
     * Given a map this function returns the hash value for it by using the xor function, zobrist table and map.
     * @param map Is the map for which a hash is to generate.
     * @return Returns the hashed value of a given map.
     */
    public long hash(Map map)
    {
        long hashValue = 0;

        if(map.getCurrentlyPlayingI() == playerNumber)
        {
            hashValue ^= notMyTurn;
        }

        for(int i = 1;i<map.getAnzPlayers();i++)
        {
           for (Position p: map.getStonesOfPlayer(i))
            {
                int pos = p.y*width+p.x;
                if(i == playerNumber)
                {
                    hashValue ^= zobristTable[pos][0];
                }
                else {
                    hashValue ^= zobristTable[pos][1];
                }
            }
        }

        return hashValue;
    }

    /**
     * Prints the values of the zobrist table in two rows and the value for being not my turn.
     */
    public void printZobristTable()
    {
        System.out.println("Zobrist Table: ");
        for(int j = 0; j < 2; j++)
        {
            for(int i = 0; i < size;i++)
            {
                System.out.print(zobristTable[i][j] + ",");
            }
            System.out.println();
        }
        System.out.println(notMyTurn);
    }
}
