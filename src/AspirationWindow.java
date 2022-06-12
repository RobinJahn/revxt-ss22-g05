package src;

public class AspirationWindow {

    private double alpha;
    private double beta;
    private int windowSize;

    public AspirationWindow(double alpha,double beta, int windowSize)
    {
        this.alpha = alpha;
        this.beta = beta;
        this.windowSize = windowSize;
    }

    public double[] getWindow()
    {
        double[] window = new double[2];
        window[0] = alpha - windowSize;
        window[1] = beta + windowSize;
        return window;
    }

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

    public void resetWindow()
    {
        alpha = Double.MIN_VALUE;
        beta = Double.MAX_VALUE;
    }

    public void setWindowSize(int windowSize)
    {
        this.windowSize = windowSize;
    }

    public void printAlphaAndBeta()
    {
        System.out.println("Alpha: " + (alpha-windowSize) + "\nBeta: " + (beta+windowSize));
    }
}
