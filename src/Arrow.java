package src;

import java.util.ArrayList;

public class Arrow {
    ArrayList<int[]> positionsWithDirection = new ArrayList<>(); //inclusive start and pointing to //int[]{x,y,r}
    boolean createsValidMove = false;

    @Override
    protected Arrow clone(){
        Arrow arrow = new Arrow();
        arrow.positionsWithDirection.addAll(positionsWithDirection);
        arrow.createsValidMove = createsValidMove;
        return arrow;
    }
}
