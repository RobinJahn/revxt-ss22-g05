package src;

import java.util.ArrayList;

public class Arrow {
    //Varables - public because of the better performance - acceptable because it's only used by the map class
    public ArrayList<int[]> positionsWithDirection = new ArrayList<>(); //inclusive start and pointing to //int[]{x,y,r}
    public boolean createsValidMove = false; //boolean if the Arrow creates a valid move

    /**
     * Copy method. With exclusion of the Positions itself this method will create a deep copy of the Arrow.
     * @return new Object of type arrow
     */
    @Override
    protected Arrow clone(){
        Arrow arrow = new Arrow();
        arrow.positionsWithDirection.addAll(positionsWithDirection);
        arrow.createsValidMove = createsValidMove;
        return arrow;
    }
}
