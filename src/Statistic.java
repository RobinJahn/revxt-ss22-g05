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
     * Creates a new Statistic Object for a given depth
     * @param depth depth given for the Statistic Object
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
     * This Function calculates the Average Computation Time for each Node
     * @return Returns the Average Computation Time per Node
     */
    public double getAverageComputationTime() {
        return (double) totalComputationTime / (double) (leafNodes + interiorNodes);
    }

    /**
     * This Function Adds the given countOfNodes either to interior or Leaf Nodes depending on the depth given
     * @param countOfNodes Count of Nodes to be added
     * @param depth depth of the Counted Nodes
     */
    public void addNodes(int countOfNodes, int depth){
        if (depth > 1) interiorNodes += countOfNodes;
        else leafNodes += countOfNodes;
        totalNodesSeen += countOfNodes;
        timesNodesGotAdded++;
    }

    /**
     * Calculates the branching Factor per Node
     * @return Returns the Branching Factor
     */
    public double branchFactor(){
        return (double)(totalNodesSeen-1)/timesNodesGotAdded;
    }

    /**
     * This Function reduces the Nodes in the Statistic by a given amount in either the InteriorNodes or the Leaf Nodes depending on the depth value.
     * @param countOfCutoffLeaves Number off Cut Leaves
     * @param currDepth The Current Depth
     */
    public void reduceNodes(int countOfCutoffLeaves, int currDepth) {
        if (depth == currDepth) interiorNodes -= countOfCutoffLeaves;
        else leafNodes -= countOfCutoffLeaves;
    }

    /**
     * This Function returns the Count of Total Nodes seen.
     * @return Count of Total Nodes seen
     */
    public int getTotalNodesSeen()
    {
        return totalNodesSeen;
    }

    /**
     * This Function returns the total Computation Time.
     * @return total Computation Time
     */
    public long getTotalComputationTime()
    {
        return totalComputationTime;
    }

    /**
     * This Function adds Time to the Total Computation Time.
     * @param timeValue The Time Value which is added to the total Computation Time
     */
    public void addTotalComputationTime(long timeValue)
    {
        totalComputationTime += timeValue;
    }

    /**
     * This Function overrides the standard toSting function in order to provide a better Representation of the Statistic.
     * @return A formatted String with the Total Nodes Seen, Total Nodes visited, Leaf Nodes visited, Interior Nodes visited, Average Computation Time, Total Computation Time and Branching Factor
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
