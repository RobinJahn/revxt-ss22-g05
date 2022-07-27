package src;

/**
 * This class is used to store the x and y coordinates of a position and also an integer that can be used to save further information.
 * Also, it provides functionality to compare two positions or to use the positions for a hash value
 */
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
     * This function returns the information.
     * @return Returns the information.
     */
    public int getInfo()
    {
        return info;
    }

    /**
     * This function sets the information.
     * @param info Sets the information to the given one.
     */
    public void setInfo(int info) {
        this.info = info;
    }

    /**
     * This function returns the value of x.
     * @return Returns the value of x.
     */
    public int getX()
    {
        return x;
    }

    /**
     * This function returns the value of y.
     * @return Returns the value of y.
     */
    public int getY()
    {
        return y;
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
     * Evaluates if 2 PositionAndInfo objects are equal based on their position alone.
     * @param obj The second PositionAndInfo object.
     * @return Returns true if the 2 have the same position.
     */
    @Override
    public boolean equals(Object obj) {
        PositionAndInfo p = (PositionAndInfo) obj;
        return this.x == p.x && this.y == p.y && this.info == p.info;
    }

    /**
     * Generates a string with all the necessary information of the object.
     * @return Returns a string following this pattern: (x,y,info).
     */
    @Override
    public String toString() {
        return "(" + x + "," + y + "," + info + ")";
    }

    /**
     * This function clones the current object by returning a new object with the same values.
     * @return Returns the copied object.
     */
    @Override
    protected PositionAndInfo clone() {
        return new PositionAndInfo(x,y,info);
    }

    /**
     * This function returns the values of the object in an int array.
     * @return Returns an int array following this pattern: {x,y,info}.
     */
    public int[] toIntArray(){
        return new int[]{x,y,info};
    }

    /**
     * This function compares two given PositionAndInfo objects
     * @param o The second PositionAndInfo object.
     * @return Returns true if the two objects have the same x,y and info and false otherwise.
     */
    @Override
    public int compareTo(PositionAndInfo o) {
        return Integer.compare(x*10_000 + y*100 + info,o.x*10_000 + o.y*100 + o.info);
    }
}