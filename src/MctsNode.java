package src;

import java.util.ArrayList;
import java.util.HashMap;

public class MctsNode {
    private Map map;
    private ArrayList<MctsNode> childs = new ArrayList<>();
    private MctsNode parent;
    private int[] actionLeadingToThis;
    private int countOfVisits;
    private double reward;
    private ArrayList<int[]> possibleMoves = null;
    private boolean phaseOne;
    private boolean terminal;

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

    public boolean isTerminal(Heuristic heuristic){
        return terminal;
    }

    public boolean isFullyExpanded(){
        return possibleMoves.isEmpty();
    }

    public void addChild(MctsNode newChild){
        childs.add(newChild);
    }

    public ArrayList<int[]> getUntriedActions(){
        return possibleMoves;
    }

    public int[] getUntriedActionAndRemoveIt(){
        int index = (int)Math.round(Math.random()*(possibleMoves.size()-1));
        int[] result = possibleMoves.get( index );
        possibleMoves.remove(index);
        return result;
    }


    public double getTotalReward(){
        return reward;
    }

    public int getCountOfVisits(){
        return countOfVisits;
    }

    public ArrayList<MctsNode> getChilds() {
        return childs;
    }

    public Map getMap(){
        return map;
    }

    public void increaseVisits(){
        countOfVisits++;
    }

    public void updateReward(double delta){
        reward+=delta;
    }

    public MctsNode getParent(){
        return parent;
    }

    public int[] getActionLeadingToThis(){
        return actionLeadingToThis;
    }

    public boolean isPhaseOne(){
        return phaseOne;
    }

}
