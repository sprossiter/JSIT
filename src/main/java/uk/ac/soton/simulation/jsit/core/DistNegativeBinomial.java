package uk.ac.soton.simulation.jsit.core;

import java.io.Serializable;

import com.thoughtworks.xstream.XStream;

/**
 * JSIT representation of a negative binomial distribution. This uses the formulation
 * as in most statistical definitions (e.g., Wolfram Math World, atRisk) where it
 * gives the number of failures before n successes in a sequence of independent trials,
 * each with probability of success p.
 * <p>
 * Compare this to Wikipedia's definition, where it is the number of successes before
 * r failures.
 * 
 * @author Stuart Rossiter
 * @since 0.2
 */
public class DistNegativeBinomial extends DistributionDiscrete implements Serializable {


    // ************************* Static Fields *****************************************
    
    private static final long serialVersionUID = 1L;
    

    // ************************* Static Methods ****************************************
    
    static void setupForInfoSerialisation(XStream xstream) {

        xstream.alias("distNegativeBinomial", DistNegativeBinomial.class);

    }


    // ************************* Instance Fields ***************************************
    
    private int n;
    private double p;

    
    // ************************** Constructors *****************************************
    
    /**
     * Create a negative binomial distribution instance.
     * 
     * @since 0.2
     * 
     * @param n The n parameter (number of successes).
     * @param p The p parameter (probability of success per trial).
     */
    public DistNegativeBinomial(int n, double p) {
        
        super();
        this.n = n;
        this.p = p;
        checkParms();
        
    }

    // *********************** Public Instance Methods *********************************

    /**
     * Change (set) the p parameter of the distribution.
     * 
     * @param p The new p parameter (probability of success).
     */
    public void setP(double p) {

        this.p = p;
        checkParms();

    }

    /**
     * Get the distribution's p parameter (probability of success).
     * 
     * @return The p value.
     */
    public double getP() {

        return p;

    }

    /**
     * Change (set) the n parameter of the distribution.
     * 
     * @param n The new n parameter (number of successes).
     */
    public void setN(int n) {

        this.n = n;
        checkParms();

    }

    /**
     * Get the distribution's n parameter (number of successes).
     * 
     * @return The n parameter.
     */
    public int getN() {

        return n;

    }
    
    /**
     * Custom toString implementation.
     * 
     * @since 0.2
     * 
     * @return A string representation of the distribution;
     * e.g., <code>"NegBin(8, 0.5)"</code>.
     */
    @Override
    public String toString() {
        
        return "NegBin(" + n + ", " + p + ")";
        
    }

    /**
     * Create an unregistered copy of this distribution.
     * @since 0.2
     */
    @Override
    public AbstractStochasticItem createUnregisteredCopy() {
        
        return new DistNegativeBinomial(n, p);
        
    }

    
    // **************** Protected/Package-Access Instance Methods **********************
    
    @Override
    protected int sampleIntByMode() {
        
        Sampler.SampleMode mode = getSampleMode();
        switch(getSampleMode()) {
            case NORMAL:
                return getSampler().sampleNegBin(n, p);
            case COLLAPSE_MID:          // Mean is pr / 1 - p
                return (int) Math.round(n * (1.0d - p) / p);
            default:
                throw new InternalError(
                        "DistNegativeBinomial has omitted support for sample mode " + mode);
        }
        
    }
    
    
    // ************************* Private Instance Methods ******************************
    
    private void checkParms() {
        
        if (p <= 0.0d || p >= 1.0d) {
            throw new IllegalArgumentException("Given p " + p + " is not in (0,1)");
        }
        if (n <= 0) {
            throw new IllegalArgumentException("Given n " + n + " is <= 0");
        }
        
    }

}
