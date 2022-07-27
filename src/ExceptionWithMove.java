package src;

import java.util.concurrent.TimeoutException;

/**
 * This exception is a TimeoutException which contains an int array where information can be handed over.
 * The information mainly is used to hand over a possible move if the time is up.
 */
public class ExceptionWithMove extends TimeoutException {
    public int[] PosAndInfo;

    /**
     * This Constructor creates an exception that contains a Position.
     * For example this can be used when the time is up when searching for all possible moves.
     * The Position then could represent a possible move the player could make.
     * @param posAndInfo int array that specifies the move. (x, y, info)
     */
    public ExceptionWithMove(int[] posAndInfo){
        this.PosAndInfo = posAndInfo;
    }
}
