package src;

public class Statistic {
    public int leafNodes;
    public int interiorNodes;
    public long totalComputationTime;
    private double breachingFactor;

    public Statistic(){
        leafNodes = 0;
        interiorNodes = 0;
        totalComputationTime = 0;
    }

    public double getAverageComputationTime() {
        return (double) totalComputationTime / (double) (leafNodes +interiorNodes);
    }

    public void addComputationTime(long totalCalculationTime) {
        this.totalComputationTime += totalCalculationTime;
    }

    @Override
    public String toString() {
        double averageComputationTimeMs = getAverageComputationTime() / 1_000_000;
        double totalComputationTimeMs = (double)totalComputationTime / 1_000_000_000;

        return "\nStatistical values:\n" +
                "total Nodes = " + (leafNodes +interiorNodes) + "\n" +
                "leaf Nodes = " + leafNodes + "\n" +
                "interiorNodes = " + interiorNodes + "\n" +
                "average computation time = " + averageComputationTimeMs + "ms\n" +
                "total computation time = " + totalComputationTimeMs + "s\n";
    }
}
