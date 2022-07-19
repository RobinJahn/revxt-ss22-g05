package src;

public class Position {
    private final int x;
    private final int y;

    /**
     * This Function creates a Position Object given X and Y
     * @param x X-Value
     * @param y Y-Value
     */
    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * This Function creates a Position Object given an Int Array of Length two
     * @param pos An Int Array where the pos[0] = X and pos[1] = Y
     */
    public Position(int[] pos) {
        this.x = pos[0];
        this.y = pos[1];
    }

    /**
     * This Function returns the X-Value of the Position-Object
     * @return The X-Value
     */
    public int getX() {
        return x;
    }

    /**
     * This Function returns the Y-Value of the Position-Object
     * @return The Y-Value
     */
    public int getY() {
        return y;
    }

    /**
     * Generates a hash value for the given Object
     * @return Returns the hash value.
     */
    @Override
    public int hashCode() {
        return 100 * y + x;
    }

    /**
     * This Function evaluates if 2 Positions are the Same.
     * @param obj Another Positions
     * @return Returns true if the X & Y Value Pairs are identically.
     */
    @Override
    public boolean equals(Object obj) {
        Position p = (Position) obj;
        return this.x == p.x && this.y == p.y;
    }

    /**
     * Generates a String with all the necessary Information of the Object
     * @return Returns a String following this Pattern (x,y)
     */
    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }

    /**
     * Returns a new Position Object with the Updated X & Y Values after going 1 Step in the provided Direction.
     * @param pos The Current Position
     * @param r The Current Direction (0 stands for North, 1 stands for NE, ...)
     * @return A new Position Object with the updated Position
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
     * This Function clones the current Object by returning a new Object with the same Values.
     * @return Returns the copied Object.
     */
    @Override
    protected Position clone() {
        return new Position(x,y);
    }
}
