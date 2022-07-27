package src;

/**
 * This class is used to store the x and y coordinates of a position. Also, it provides functionality to compare two positions or to use the positions for a hash value.
 */
public class Position {
    int x;
    int y;

    /**
     * This function creates a position object given x and y.
     * @param x The x value.
     * @param y The y value.
     */
    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * This function creates a position object given an int array of length two.
     * @param pos An int array where the pos[0] = x and pos[1] = y.
     */
    public Position(int[] pos) {
        this.x = pos[0];
        this.y = pos[1];
    }

    /**
     * Generates a hash value for the given object.
     * @return Returns the hash value.
     */
    @Override
    public int hashCode() {
        return 100 * y + x;
    }

    /**
     * This function evaluates if 2 positions are the same.
     * @param obj The second positions.
     * @return Returns true if the x and y value pairs are identically.
     */
    @Override
    public boolean equals(Object obj) {
        Position p = (Position) obj;
        return this.x == p.x && this.y == p.y;
    }

    /**
     * Generates a string with all the necessary information of the object.
     * @return Returns a string following this pattern: (x,y).
     */
    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }

    /**
     * Returns a new position object with the updated x and y values after going 1 step in the provided direction.
     * @param pos The current position.
     * @param r The current direction (0 is up, 1 is up and right, ...).
     * @return Returns a new position object with the updated position.
     */
    public static Position goInR(Position pos, int r) {
        int x = pos.x;
        int y = pos.y;
        switch (r) {
            case 0:
                y = y - 1;
                break;
            case 1:
                x = x + 1;
                y =y - 1;
                break;
            case 2:
                x = x + 1;
                break;
            case 3:
                x= x + 1;
                y = y + 1;
                break;
            case 4:
                y = y + 1;
                break;
            case 5:
                x = x - 1;
                y = y + 1;
                break;
            case 6:
                x = x - 1;
                break;
            case 7:
                x = x - 1;
                y = y - 1;
                break;
        }
        return new Position(x,y);
    }

    /**
     * This function clones the current object by returning a new object with the same values.
     * @return Returns the copied object.
     */
    @Override
    protected Position clone() {
        return new Position(x,y);
    }
}
