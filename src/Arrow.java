package src;

import java.util.ArrayList;

/**
 * This class is used to save possible moves. It saves all the position from the start to the field it points to.
 * Also is saves if the arrow creates a valid move.
 */
public class Arrow {
    //Variables - public because of the better performance - acceptable because it's only used by the map class
    public ArrayList<int[]> positionsWithDirection = new ArrayList<>(); //inclusive start and pointing to //int[]{x,y,r}
    public boolean createsValidMove = false; //boolean if the Arrow creates a valid move

    /**
     * Copy method. With exclusion of the positions itself this method will create a deep copy of the arrow.
     * @return Returns a new object of type arrow.
     */
    @Override
    protected Arrow clone(){
        Arrow arrow = new Arrow();
        arrow.positionsWithDirection.addAll(positionsWithDirection);
        arrow.createsValidMove = createsValidMove;
        return arrow;
    }
}
