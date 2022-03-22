package src;

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

    public static String pairToString(char[] pair) {
        int x1 = Transitions.getX(pair[0]);
        int y1 = Transitions.getY(pair[0]);
        int r1 = Transitions.getR(pair[0]);

        int x2 = Transitions.getX(pair[1]);
        int y2 = Transitions.getY(pair[1]);
        int r2 = Transitions.getR(pair[1]);

        return x1 + " " + y1 + " " + r1 + " <-> " + x2 + " " + y2 + " " + r2 + '\n';
    }


}
