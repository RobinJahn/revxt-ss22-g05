package src;

import java.util.Random;

public class ZobristHashing {

    final private int myStone = 1;
    final private int notMyStone = 2;

    final private int size;

    final private int width;
    final private int height;

    final private int me;

    long[][] zobristTable;

    final private long notMyTurn;

    public ZobristHashing(int height, int width, int me)
    {
        Random RandomGen = new Random();

        size = height*width;
        this.width = width;
        this.height = height;


        zobristTable = new long[size][2];
        for(int i= 0; i< size;i++)
        {
            for(int j = 0; j< 2; j++)
            {
                zobristTable[i][j] = RandomGen.nextLong();
            }
        }
        notMyTurn = RandomGen.nextLong();

        this.me = me;
    }

    public long hash(Map map)
    {
        long hashValue = 0;

        if(map.getCurrentlyPlayingI() == me)
        {
            hashValue ^= notMyTurn;
        }


        for(int i = 1;i<map.getAnzPlayers();i++)
        {
           for (Position p: map.getStonesOfPlayer(i))
            {
                int pos = p.y*width+p.x;
                if(i == me)
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
