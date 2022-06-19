package src;

public class PositionAndValue implements Comparable<src.PositionAndValue>{

    public int x;
    public int y;
    public double value;

    public PositionAndValue(int x, int y, double value) {
        this.x = x;
        this.y = y;
        this.value = value;
    }

    public PositionAndValue(int[] posAndInfo) {
        if (posAndInfo.length != 3) return;
        this.x = posAndInfo[0];
        this.y = posAndInfo[1];
        this.value = posAndInfo[2];
    }

    @Override
    public int hashCode() {
        return 100 * y + x;
    }

    @Override
    public boolean equals(Object obj) {
        src.PositionAndValue p = (src.PositionAndValue) obj;
        if (this.x == p.x && this.y == p.y) return true;
        return false;
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + "," + value + ")";
    }

    @Override
    protected src.PositionAndValue clone() {
        return new src.PositionAndValue(x,y, value);
    }

    @Override
    public int compareTo(src.PositionAndValue o) {
        return Double.compare(x * 10_000 + y * 100 + value, o.x * 10_000 + o.y * 100 + o.value);
    }

}
