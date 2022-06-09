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

    public MctsNode(Map map, MctsNode parent, int[] posAndInfoLeadingToThis){
        this.map = map;
        this.parent = parent;
        actionLeadingToThis = posAndInfoLeadingToThis;
        countOfVisits = 0;
        reward = 0;
        try { //TODO: maybe only get them when needed
            possibleMoves = Map.getValidMoves(map, false, false, false, Long.MAX_VALUE, null); //TODO: use time
        } catch (ExceptionWithMove e) {
            System.out.println("Something went wrong - getValidMoves trew exception even if there was no time limit");
            e.printStackTrace();
        }
    }

    public boolean isTerminal(){
        return map.isTerminal();
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

}
