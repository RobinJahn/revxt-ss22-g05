package src;

/**
 * This class is used to store the x and y coordinates of a position and also a double that can be used to save further information.
 * Also, it provides functionality to compare two positions or to use the positions for a hash value.
 */
public class PositionAndValue implements Comparable<src.PositionAndValue>{

    private int x;
    private int y;
    private double value;

    /**
     * Returns the value.
     * @return Returns the value.
     */
    public double getValue()
    {
        return value;
    }

    /**
     * Returns the x position.
     * @return Returns the x position.
     */
    public int getX() {
        return x;
    }

    /**
     * Returns the y position.
     * @return Returns the y position.
     */
    public int getY() {
        return y;
    }

    /**
     * Creates a new PositionAndValue object with the given parameters.
     * @param x The x position.
     * @param y The y position.
     * @param value The value for this position.
     */
    public PositionAndValue(int x, int y, double value) {
        this.x = x;
        this.y = y;
        this.value = value;
    }

    /**
     * This is a copy constructor. It creates a new PositionAndValue object given a PositionAndInfo object.
     * @param posAndInfo The PositionAndInfo object to clone.
     */
    public PositionAndValue(int[] posAndInfo) {
        if (posAndInfo.length != 3) return;
        this.x = posAndInfo[0];
        this.y = posAndInfo[1];
        this.value = posAndInfo[2];
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
     * Evaluates if 2 PositionAndValues objects are equal based on their position alone
     * @param obj The second PositionAndValue object.
     * @return Returns true if the 2 have the same position.
     */
    @Override
    public boolean equals(Object obj) {
        src.PositionAndValue p = (src.PositionAndValue) obj;
        return this.x == p.x && this.y == p.y;
    }

    /**
     * Generates a string with all the necessary information off the object.
     * @return Returns a string following this pattern (x,y,value).
     */
    @Override
    public String toString() {
        return "(" + x + "," + y + "," + value + ")";
    }

    /**
     * This function clones the current object by returning a new object with the same values.
     * @return Returns the copied object.
     */
    @Override
    protected src.PositionAndValue clone() {
        return new src.PositionAndValue(x,y, value);
    }

    /**
     * This function compares two given PositionAndValue objects.
     * @param o The second PositionAndInfo object.
     * @return Returns true if the two objects have the same x,y and value and false otherwise.
     */
    @Override
    public int compareTo(src.PositionAndValue o) {
        return Double.compare(x * 10_000 + y * 100 + value, o.x * 10_000 + o.y * 100 + o.value);
    }
}