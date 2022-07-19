package src;

public class PositionAndValue implements Comparable<src.PositionAndValue>{

    private int x;
    private int y;
    private double value;

    /**
     * Returns the Value
     * @return Returns the Value
     */
    public double getValue()
    {
        return value;
    }

    /**
     * Returns the X-Position
     * @return Returns the X-Position
     */
    public int getX() {
        return x;
    }

    /**
     * Returns the Y-Position
     * @return Returns the Y-Position
     */
    public int getY() {
        return y;
    }

    /**
     * Creates a new PositionAndValue Object with the given Parameters
     * @param x X-Position
     * @param y Y-Position
     * @param value Value for this Position
     */
    public PositionAndValue(int x, int y, double value) {
        this.x = x;
        this.y = y;
        this.value = value;
    }

    /**
     * Creates a new PositionAndValue Object given a PositionAndInfo Object
     * @param posAndInfo A PositionAndInfo Object
     */
    public PositionAndValue(int[] posAndInfo) {
        if (posAndInfo.length != 3) return;
        this.x = posAndInfo[0];
        this.y = posAndInfo[1];
        this.value = posAndInfo[2];
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
     * Evaluates if 2 PositionAndValues are equal based on their Position alone
     * @param obj Another PositionAndValue Object
     * @return Returns true if the 2 have the same Position
     */
    @Override
    public boolean equals(Object obj) {
        src.PositionAndValue p = (src.PositionAndValue) obj;
        return this.x == p.x && this.y == p.y;
    }

    /**
     * Generates a String with all the necessary Information off the Object
     * @return Returns a String following this Pattern (x,y,value)
     */
    @Override
    public String toString() {
        return "(" + x + "," + y + "," + value + ")";
    }

    /**
     * This Function clones the current Object by returning a new Object with the same Values.
     * @return Returns the copied Object.
     */
    @Override
    protected src.PositionAndValue clone() {
        return new src.PositionAndValue(x,y, value);
    }

    /**
     * This Function compares two given PositionAndValue Objects
     * @param o the object to be compared.
     * @return Returns true if the two Objects have the same x,y and Value. False Otherwise.
     */
    @Override
    public int compareTo(src.PositionAndValue o) {
        return Double.compare(x * 10_000 + y * 100 + value, o.x * 10_000 + o.y * 100 + o.value);
    }
}