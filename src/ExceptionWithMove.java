package src;

import java.util.concurrent.TimeoutException;

public class ExceptionWithMove extends TimeoutException {
    public int[] PosAndInfo;

    public ExceptionWithMove(int[] posAndInfo){
        this.PosAndInfo = posAndInfo;
    }
}
