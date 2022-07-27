package src;

/**
 * This class contains all the statistical information for a calculation in a certain depth. Also, it provides methods to update and get the information.
 */
public class Statistic {
    private int leafNodes;
    private int interiorNodes;
    private int totalNodesSeen;
    private int timesNodesGotAdded;
    private long totalComputationTime;
    private final int depth;

    /**
     * Creates a new Statistic object for a given depth.
     * @param depth The depth given for the Statistic object.
     */
    public Statistic(int depth){
        leafNodes = 0;
        interiorNodes = 1;
        totalNodesSeen = 1;
        timesNodesGotAdded = 0;
        totalComputationTime = 0;
        this.depth = depth;
    }

    /**
     * This function calculates the average computation time for each node.
     * @return Returns the average computation time per node.
     */
    public double getAverageComputationTime() {
        return (double) totalComputationTime / (double) (leafNodes + interiorNodes);
    }

    /**
     * This function adds the given count of nodes either to interior or leaf nodes depending on the depth given.
     * @param countOfNodes Count of nodes to be added.
     * @param depth Depth of the counted nodes.
     */
    public void addNodes(int countOfNodes, int depth){
        if (depth > 1) interiorNodes += countOfNodes;
        else leafNodes += countOfNodes;
        totalNodesSeen += countOfNodes;
        timesNodesGotAdded++;
    }

    /**
     * Calculates the branching factor per node.
     * @return Returns the branching factor.
     */
    public double branchFactor(){
        return (double)(totalNodesSeen-1)/timesNodesGotAdded;
    }

    /**
     * This function reduces the nodes in the statistic by a given amount in either the interior nodes or the leaf nodes depending on the depth value.
     * @param countOfCutoffLeaves The number off cut leaves.
     * @param currDepth The current depth.
     */
    public void reduceNodes(int countOfCutoffLeaves, int currDepth) {
        if (depth == currDepth) interiorNodes -= countOfCutoffLeaves;
        else leafNodes -= countOfCutoffLeaves;
    }

    /**
     * This function returns the count of total nodes seen.
     * @return Count of total nodes seen.
     */
    public int getTotalNodesSeen()
    {
        return totalNodesSeen;
    }

    /**
     * This function returns the total computation time.
     * @return Total computation time.
     */
    public long getTotalComputationTime()
    {
        return totalComputationTime;
    }

    /**
     * This function adds time to the total computation time.
     * @param timeValue The time value which is added to the total computation time.
     */
    public void addTotalComputationTime(long timeValue)
    {
        totalComputationTime += timeValue;
    }

    /**
     * This function overrides the standard toSting() function in order to provide a better representation of the statistic.
     * @return A formatted string with the total nodes seen, total nodes visited, leaf nodes visited, interior nodes visited, average computation time, total computation time and branching factor.
     */
    @Override
    public String toString() {
        double averageComputationTimeMs = getAverageComputationTime() / 1_000_000;
        double totalComputationTimeMs = (double)totalComputationTime / 1_000_000;

        return "\nStatistical values:\n" +
                "total Nodes seen = " + totalNodesSeen + "\n" +
                "total Nodes visited = " + (leafNodes +interiorNodes) + "\n" +
                "leaf Nodes visited = " + leafNodes + "\n" +
                "interiorNodes visited= " + interiorNodes + "\n" +
                "average computation time = " + averageComputationTimeMs + "ms\n" +
                "total computation time = " + totalComputationTimeMs + "ms\n" +
                "branching factor = " + branchFactor() + " = " + (totalNodesSeen-1) + "/" + timesNodesGotAdded + "\n";
    }
}
