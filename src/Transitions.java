package src;

import java.util.HashMap;

public class Transitions {
    /**
     * Calculates the Transition given its Parameters
     * @param x X-Position
     * @param y Y-Position
     * @param r Direction
     * @return The TransitionValue
     */
    public static char saveInChar(int x, int y, int r){
        char mem = (char) (1000 * (short)x + 10 * (short)y + (short)r);
        return mem;
    }

    /**
     * Calculates the X-Position of a given Transition Point
     * @param mem The Transition
     * @return Returns X-Position
     */
    public static int getX(char mem){
         return mem / 1000;
    }

    /**
     * Calculates the Y-Position of a given Transition Point
     * @param mem The Transition
     * @return Returns Y-Position
     */
    public static int getY(char mem) {
        return (mem%1000)/10;
    }

    /**
     * Calculates the Direction of a given Transition Point
     * @param mem The Transition
     * @return Returns the Direction
     */
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

    /**
     * Returns a formatted String containing all Transitions of the Map.
     * @param hashMap all Transitions in a Hash Map.
     * @return Returns a formatted Sting off all Transitions
     */
    public static String AllToString(HashMap<Character,Character> hashMap)
    {
    	String result = "";
    	
    	 char[] tr = new char [hashMap.size()];
         int num = 0;
         for (Character combination : hashMap.keySet())
         {
         	tr[num]=combination;
         	num++;
         }
         for(int i = 0;i<tr.length;i++)
         {
         	if(tr[i]!= 0)
         	{
         		char char2 = hashMap.get(tr[i]);
         		result += Transitions.pairToString(tr[i], char2);
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
    	return result;
    }

    private static String pairToStringWithIndexShift(Character char1 , Character char2) {
        int x1 = Transitions.getX(char1)-1;
        int y1 = Transitions.getY(char1)-1;
        int r1 = Transitions.getR(char1);

        int x2 = Transitions.getX(char2)-1;
        int y2 = Transitions.getY(char2)-1;
        int r2 = Transitions.getR(char2);

        return x1 + " " + y1 + " " + r1 + " <-> " + x2 + " " + y2 + " " + r2 + '\n';
    }

    /**
     * Returns a formatted String containing all Transitions of the Map without our internal IndexShift.
     * @param HMap all Transitions in a Hash Map.
     * @return Returns a formatted Sting off all Transitions
     */
    public static String AllToStringWithIndexShift(HashMap<Character,Character> HMap)
    {
        String result ="";

        char[] tr = new char [HMap.size()];
        int num = 0;
        for (Character comb : HMap.keySet())
        {
            tr[num]=comb;
            num++;
        }
        for(int i = 0;i<tr.length;i++)
        {
            if(tr[i]!= 0)
            {
                char char2 = HMap.get(tr[i]);
                result += Transitions.pairToStringWithIndexShift(tr[i], char2);
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
        return result;
    }

}
