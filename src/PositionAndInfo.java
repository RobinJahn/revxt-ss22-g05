package src;

public class PositionAndInfo implements Comparable<PositionAndInfo> {
    private int x;
    private int y;
    private int info;

    public PositionAndInfo(int x, int y, int info) {
        this.x = x;
        this.y = y;
        this.info = info;
    }

    public PositionAndInfo(int[] posAndInfo) {
        if (posAndInfo.length != 3) return;
        this.x = posAndInfo[0];
        this.y = posAndInfo[1];
        this.info = posAndInfo[2];
    }

    /**
     * This Function returns the Information
     * @return Returns the Information
     */
    public int getInfo()
    {
        return info;
    }

    /**
     * This Function sets the Information
     * @param info Sets the Information to the given one.
     */
    public void setInfo(int info) {
        this.info = info;
    }

    /**
     * This Function returns the Value of X
     * @return Returns the Value of X
     */
    public int getX()
    {
        return x;
    }

    /**
     * This Function returns the Value of Y
     * @return Returns the Value of Y
     */
    public int getY()
    {
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
     * Evaluates if 2 PositionAndInfo are equal based on their Position alone
     * @param obj Another PositionAndInfo Object
     * @return Returns true if the 2 have the same Position
     */
    @Override
    public boolean equals(Object obj) {
        PositionAndInfo p = (PositionAndInfo) obj;
        return this.x == p.x && this.y == p.y && this.info == p.info;
    }

    /**
     * Generates a String with all the necessary Information off the Object
     * @return Returns a String following this Pattern (x,y,info)
     */
    @Override
    public String toString() {
        return "(" + x + "," + y + "," + info + ")";
    }

    /**
     * This Function clones the current Object by returning a new Object with the same Values.
     * @return Returns the copied Object.
     */
    @Override
    protected PositionAndInfo clone() {
        return new PositionAndInfo(x,y,info);
    }

    /**
     * This Function returns the Values of the Object in an Int Array
     * @return Returns an Int Array following this Pattern {x,y,info}
     */
    public int[] toIntArray(){
        return new int[]{x,y,info};
    }

    /**
     * This Function compares two given PositionAndInfo Objects
     * @param o the object to be compared.
     * @return Returns true if the two Objects have the same x,y and Info. False Otherwise.
     */
    @Override
    public int compareTo(PositionAndInfo o) {
        return Integer.compare(x*10_000 + y*100 + info,o.x*10_000 + o.y*100 + o.info);
    }
}