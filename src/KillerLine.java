package src;

public class KillerLine {

    PositionAndInfo Pos;

    int NumberOfCutLeaves;

    public KillerLine(PositionAndInfo pos, int NumberOfCutLeaves)
    {
        this.Pos = pos.clone();
        this.NumberOfCutLeaves = NumberOfCutLeaves;
    }

    public PositionAndInfo getPositionAndInfo() {
        return Pos;
    }

    public int getNumberOfCutLeaves() {
        return NumberOfCutLeaves;
    }

    public void setPositionAndInfo(PositionAndInfo pos) {
        Pos = pos;
    }

    public void setNumberOfCutLeaves(int numberOfCutLeaves) {
        NumberOfCutLeaves = numberOfCutLeaves;
    }
}
