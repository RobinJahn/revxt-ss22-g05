package src;

import java.util.HashMap;

public class Transitions {
    public static char saveInChar(int x, int y, int r){
        char mem = (char) (1000 * (short)x + 10 * (short)y + (short)r); //TODO: handle error
        return mem;
    }

    public static int getX(char mem){
         return mem / 1000;
    }

    public static int getY(char mem) {
        return (mem%1000)/10;
    }

    public static int getR(char mem) {
        return mem%10;
    }

    private static String pairToString(Character char1 , Character char2) {
        int x1 = Transitions.getX(char1);
        int y1 = Transitions.getY(char1);
        int r1 = Transitions.getR(char1);

        int x2 = Transitions.getX(char2);
        int y2 = Transitions.getY(char2);
        int r2 = Transitions.getR(char2);

        return x1 + " " + y1 + " " + r1 + " <-> " + x2 + " " + y2 + " " + r2 + '\n';
    }
    
    public static String AllToString(HashMap<Character,Character> Hmap)
    {
    	String ergebnis ="";
    	
    	 char[] tr = new char [Hmap.size()];
         int num = 0;
         for (Character kombi : Hmap.keySet())
         {
         	tr[num]=kombi;
         	num++;
         }
         for(int i = 0;i<tr.length;i++)
         {
         	if(tr[i]!= 0)
         	{
         		char char2 = Hmap.get(tr[i]);
         		ergebnis += Transitions.pairToString(tr[i], char2);
         		for(int j = 0;j<tr.length;j++)
         		{
         			if(char2 == tr[j])
         			{
         				tr[j]=0;
         				break;
         			}
         		}
         	}
         }
    	return ergebnis;
    }

}
