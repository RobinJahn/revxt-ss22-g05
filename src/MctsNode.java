package src;

import java.util.ArrayList;

/**
 * This class is used to store all the information for the Monte-Carlo-Tree-Search-Algorithm. Also, it provides the methods to change or get this information.
 */
public class MctsNode {

    //final
    private final Map map;
    private final ArrayList<MctsNode> childNodes = new ArrayList<>();
    private final MctsNode parent;
    private final int[] actionLeadingToThis;
    private final ArrayList<int[]> possibleMoves;
    private final boolean phaseOne;
    private final boolean terminal;

    //not final
    private int countOfVisits;
    private double reward;

    /**
     * This Constructor creates an MCTS-Node. This is used for the Monte-Carlo-Tree-Search algorithm.
     *
     * @param map The map the game state has.
     * @param parent The parent MCTS-Node. If it has no parent, it can be set to null.
     * @param posAndInfoLeadingToThis This int array stores a move that lead to this game state. (x, y, info)
     * @param possibleMoves This represents a list of Moves that could follow this game state. The Move is stored in an int array of the form (x, y, info)
     * @param phaseOne This boolean contains the current phase of the game state.
     */
    public MctsNode(Map map, MctsNode parent, int[] posAndInfoLeadingToThis, ArrayList<int[]> possibleMoves, boolean phaseOne){
        this.map = map;
        this.parent = parent;
        actionLeadingToThis = posAndInfoLeadingToThis;
        countOfVisits = 0;
        reward = 0;
        this.possibleMoves = possibleMoves;
        this.phaseOne = phaseOne;
        if (possibleMoves.isEmpty()) terminal = true;
        else terminal = false;
    }

    /**
     * This method returns a boolean that represents if the game state is terminal.
     * @return returns true if the game state is terminal and false otherwise.
     */
    public boolean isTerminal(){
        return terminal;
    }

    /**
     * This method returns a boolean that represents if there are moves left, that weren't already checked.
     * @return returns true if the node is fully extended.
     */
    public boolean isFullyExpanded(){
        return possibleMoves.isEmpty();
    }

    /**
     * This method adds a child node to the node it is called on.
     * @param newChild The child that should be added.
     */
    public void addChild(MctsNode newChild){
        childNodes.add(newChild);
    }

    /**
     * This method returns a random move that wasn't checked already. Also, it marks the move it returns as used.
     * Before calling this method it needs to be checked if the node is fully expanded.
     * @return Move that wasn't checked already.
     */
    public int[] getUntriedActionAndRemoveIt(){
        int index = (int)Math.round(Math.random()*(possibleMoves.size()-1));
        int[] result = possibleMoves.get( index );
        possibleMoves.remove(index);
        return result;
    }

    /**
     * @return Returns a boolean that represents the reward that this node has.
     */
    public double getTotalReward(){
        return reward;
    }

    /**
     * This method returns the amount of visits this node had.
     * @return number of visits.
     */
    public int getCountOfVisits(){
        return countOfVisits;
    }

    /**
     * This Method returns a list of all children the node has.
     * @return List of children.
     */
    public ArrayList<MctsNode> getChildNodes() {
        return childNodes;
    }

    /**
     * This Method returns the Map of the game state the node represents.
     * @return the Map object
     */
    public Map getMap(){
        return map;
    }

    /**
     * This method increases the visits the node had.
     */
    public void increaseVisits(){
        countOfVisits++;
    }

    /**
     * This method adds delta to the reward of the node
     * @param delta the value that gets added to the reward.
     */
    public void updateReward(double delta){
        reward+=delta;
    }

    /**
     * This method returns the parent of the node
     * @return the parent MCTS-Node
     */
    public MctsNode getParent(){
        return parent;
    }

    /**
     * This method returns the move that lead to this game state.
     * @return int array that represents the move (x, y, info)
     */
    public int[] getActionLeadingToThis(){
        return actionLeadingToThis;
    }

    /**
     * This method returns a boolean that represents if the game state of this node is in the first, or the second phase.
     * @return Returns true if the game state is in the first phase and false otherwise.
     */
    public boolean isPhaseOne(){
        return phaseOne;
    }

}
