package src;

/**
 * This class is used to store all the information needed for the aspiration window technique.
 * Also, it provides the methods to modify the values.
 */
public class AspirationWindow {

    private double alpha;
    private double beta;
    private int windowSize;

    /**
     * This class contains the Information to use the Aspiration Window Technique.
     * @param alpha the alpha from alpha beta pruning.
     * @param beta the beta from alpha beta pruning.
     * @param windowSize the desired window size
     */
    public AspirationWindow(double alpha,double beta, int windowSize)
    {
        this.alpha = alpha;
        this.beta = beta;
        this.windowSize = windowSize;
    }

    /**
     * This method returns the window size.
     * @return Returns a double array of size 2 where the first element is the adjusted alpha, and the second element the adjusted beta.
     */
    public double[] getWindow()
    {
        double[] window = new double[2];
        window[0] = alpha - windowSize;
        window[1] = beta + windowSize;
        return window;
    }

    /**
     * This method updates alpha and beta if needed.
     * @param alpha the alpha value
     * @param beta the beta value
     */
    public void setAlphaAndBeta(double alpha,double beta)
    {
        if(this.alpha<alpha)
        {
            this.alpha = alpha;
        }
        if(this.beta > beta)
        {
            this.beta = beta;
        }
    }

    /**
     * This method checks weather the given value is inside the window.
     * @param value the value to check
     * @return Returns true when inside the window and false otherwise
     */
    public boolean insideWindow(double value)
    {
        if(value> alpha-windowSize && value < beta+windowSize)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * This Method resets the window-size to it's maximum
     */
    public void resetWindow()
    {
        alpha = Double.MIN_VALUE;
        beta = Double.MAX_VALUE;
    }

    /**
     * This method sets the window size to the given value
     * @param windowSize the value it gets set to.
     */
    public void setWindowSize(int windowSize)
    {
        this.windowSize = windowSize;
    }

    /**
     * This method prints the current alpha and beta to the output.
     */
    public void printAlphaAndBeta()
    {
        System.out.println("Alpha: " + (alpha-windowSize) + "\nBeta: " + (beta+windowSize));
    }
}
