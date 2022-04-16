package src;

public class PositionAndInfo implements Comparable<PositionAndInfo> {
    public int x;
    public int y;
    public int info;

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

    @Override
    public int hashCode() {
        return 100 * y + x;
    }

    @Override
    public boolean equals(Object obj) {
        PositionAndInfo p = (PositionAndInfo) obj;
        if (this.x == p.x && this.y == p.y && this.info == p.info) return true;
        return false;
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + "," + info + ")";
    }

    @Override
    protected PositionAndInfo clone() {
        return new PositionAndInfo(x,y,info);
    }

    public int[] toIntArray(){
        return new int[]{x,y,info};
    }

    @Override
    public int compareTo(PositionAndInfo o) {
        return Integer.compare(x*10_000 + y*100 + info,o.x*10_000 + o.y*100 + o.info);
    }
}
