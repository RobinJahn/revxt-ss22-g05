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


}
