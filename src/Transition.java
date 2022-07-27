package src;

import java.util.HashMap;

/**
 * This class provides usability functions, so that a transition end can easily be saved in a specific formatted char.
 */
public class Transition {
    /**
     * Saves a given transition end in a char and returns it.
     * @param x The x position.
     * @param y The y position.
     * @param r The direction.
     * @return Returns the transition end in a char.
     */
    public static char saveInChar(int x, int y, int r){
        char mem = (char) (1000 * (short)x + 10 * (short)y + (short)r);
        return mem;
    }

    /**
     * Calculates the x position of a given transition end.
     * @param mem The transition.
     * @return Returns the x position.
     */
    public static int getX(char mem){
         return mem / 1000;
    }

    /**
     * Calculates the y position of a given transition end.
     * @param mem The transition.
     * @return Returns the y position.
     */
    public static int getY(char mem) {
        return (mem%1000)/10;
    }

    /**
     * Calculates the direction of a given transition end.
     * @param mem The transition.
     * @return Returns the direction in which the transition faces.
     */
    public static int getR(char mem) {
        return mem%10;
    }

    private static String pairToString(Character char1 , Character char2) {
        int x1 = Transition.getX(char1);
        int y1 = Transition.getY(char1);
        int r1 = Transition.getR(char1);

        int x2 = Transition.getX(char2);
        int y2 = Transition.getY(char2);
        int r2 = Transition.getR(char2);

        return x1 + " " + y1 + " " + r1 + " <-> " + x2 + " " + y2 + " " + r2 + '\n';
    }

    /**
     * Returns a formatted string containing all transitions of the map.
     * @param hashMap All transitions in a hash map.
     * @return Returns a formatted sting of all transitions.
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
         		result += Transition.pairToString(tr[i], char2);
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
        int x1 = Transition.getX(char1)-1;
        int y1 = Transition.getY(char1)-1;
        int r1 = Transition.getR(char1);

        int x2 = Transition.getX(char2)-1;
        int y2 = Transition.getY(char2)-1;
        int r2 = Transition.getR(char2);

        return x1 + " " + y1 + " " + r1 + " <-> " + x2 + " " + y2 + " " + r2 + '\n';
    }

    /**
     * Returns a formatted string containing all transitions of the map without our internal index shift.
     * @param HMap All transitions in a hash map.
     * @return Returns a formatted sting off all transitions.
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
                result += Transition.pairToStringWithIndexShift(tr[i], char2);
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
