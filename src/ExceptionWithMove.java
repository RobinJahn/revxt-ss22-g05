package src;

import java.util.concurrent.TimeoutException;

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
