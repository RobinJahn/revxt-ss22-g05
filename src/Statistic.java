package src;

public class Statistic {
    public int leaveNodes;
    public int interiorNodes;
    private long totalComputationTime;
    private int countOfCalcValues;

    public Statistic(){
        leaveNodes = 0;
        interiorNodes = 0;
        totalComputationTime = 0;
        countOfCalcValues = 0;
    }

    public double getAverageComputationTime() {
        return (double) totalComputationTime / (double)countOfCalcValues;
    }

    public void addComputationTime(long totalCalculationTime) {
        this.totalComputationTime += totalCalculationTime;
        this.countOfCalcValues++;
    }

    @Override
    public String toString() {
        double averageComputationTimeMs = getAverageComputationTime() / 1_000_000;
        double totalComputationTimeMs = (double)totalComputationTime / 1_000_000_000;

        return "\nStatistical values:\n" +
                "total Nodes = " + (leaveNodes+interiorNodes) + "\n" +
                "leave Nodes = " + leaveNodes + "\n" +
                "interiorNodes = " + interiorNodes + "\n" +
                "average computation time = " + averageComputationTimeMs + "ms\n" +
                "total computation time = " + totalComputationTimeMs + "s\n";
    }
}
