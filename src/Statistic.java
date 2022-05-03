package src;

public class Statistic {
    public int leafNodes;
    public int interiorNodes;
    public int totalNodesSeen;
    private int timesNodesGotAdded;
    public long totalComputationTime;


    public Statistic(){
        leafNodes = 0;
        interiorNodes = 1;
        totalNodesSeen = 1;
        timesNodesGotAdded = 0;
        totalComputationTime = 0;
    }

    public double getAverageComputationTime() {
        return (double) totalComputationTime / (double) (leafNodes + interiorNodes);
    }

    public void addNodes(int countOfNodes, int depth){
        if (depth > 1) interiorNodes += countOfNodes;
        else leafNodes += countOfNodes;
        totalNodesSeen += countOfNodes;
        timesNodesGotAdded++;
    }

    public double branchFactor(){
        return (double)totalNodesSeen/timesNodesGotAdded;
    }

    public void reduceNodes(int countOfCutoffLeaves, int depth) {
        if (depth > 1) interiorNodes -= countOfCutoffLeaves;
        else leafNodes -= countOfCutoffLeaves;
    }


    @Override
    public String toString() {
        double averageComputationTimeMs = getAverageComputationTime() / 1_000_000;
        double totalComputationTimeMs = (double)totalComputationTime / 1_000_000_000;

        return "\nStatistical values:\n" +
                "total Nodes seen = " + totalNodesSeen + "\n" +
                "total Nodes visited = " + (leafNodes +interiorNodes) + "\n" +
                "leaf Nodes visited = " + leafNodes + "\n" +
                "interiorNodes visited= " + interiorNodes + "\n" +
                "average computation time = " + averageComputationTimeMs + "ms\n" +
                "total computation time = " + totalComputationTimeMs + "s\n" +
                "branching factor = " + branchFactor() + " = " + totalNodesSeen + "/" + timesNodesGotAdded + "\n";
    }


}
