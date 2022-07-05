package src;

import java.util.Random;

public class ZobristHashing {

    final private int size;

    final private int width;

    final private int playerNumber;

    final private long[][] zobristTable;

    final private long notMyTurn;

    /**
     * Instantiates a Zobrist Table, of the size of height*width and depth 2, and fills it with random long Values. Also generates a random long Value for it being not my Turn.
     * @param height The height of the Map for which to create a Zobrist Table for.
     * @param width The width of the Map for which to create a Zobrist Table for.
     * @param playerNumber The Player Number of the Client for this map.
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
     * Given a map this function returns the hash Value for it by using the xor function, Zobrist Table and Map
     * @param map Is the Map for which a hash is to generate.
     * @return Returns the Hashed Value of a given Map.
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
     * Prints the Values of the Zobrist Table in two Rows and the Value for being not my turn.
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
