package uk.ac.soton.simulation.jsit.core;

import java.io.Serializable;

import com.thoughtworks.xstream.XStream;

/**
 * JSIT representation of a (non-shifted) geometric distribution which models the
 * number of failures before a success in independent trials with a success
 * probability p.
 * 
 * @author Stuart Rossiter
 * @since 0.2
 */
public class DistGeometric extends DistributionDiscrete implements Serializable {
    
    // We could implement this as wrapping a NegBin distribution (since it is just a
    // special case of NegBin with n=1) but choose not to for now


    // ************************* Static Fields *****************************************
    
    private static final long serialVersionUID = 1L;
    

    // ************************* Static Methods ****************************************
    
    static void setupForInfoSerialisation(XStream xstream) {

        xstream.alias("distGeometric", DistGeometric.class);

    }


    // ************************* Instance Fields ***************************************
    
    private double p;

    
    // ************************** Constructors *****************************************
    
    /**
     * Create a geometric distribution instance.
     * 
     * @since 0.2
     * 
     * @param p The p parameter (probability of success) for the distribution.
     */
    public DistGeometric(double p) {
        
        super();
        this.p = p;
        checkParms();
        
    }

    // *********************** Public Instance Methods *********************************

    /**
     * Change (set) the p parameter of the distribution.
     * 
     * @param p The new p parameter.
     */
    public void setP(double p) {

        this.p = p;
        checkParms();

    }

    /**
     * Get the distribution's p parameter (probability of success).
     * 
     * @return The p parameter.
     */
    public double getP() {

        return p;

    }
    
    /**
     * Custom toString implementation.
     * 
     * @since 0.2
     * 
     * @return A string representation of the distribution;
     * e.g., <code>"Geometric(0.75)"</code>.
     */
    @Override
    public String toString() {
        
        return "Geometric(" + p + ")";
        
    }

    /**
     * Create an unregistered copy of this distribution.
     * @since 0.2
     */
    @Override
    public AbstractStochasticItem createUnregisteredCopy() {
        
        return new DistGeometric(p);
        
    }

    
    // **************** Protected/Package-Access Instance Methods **********************
    
    @Override
    protected int sampleIntByMode() {
        
        Sampler.SampleMode mode = getSampleMode();
        switch(getSampleMode()) {
            case NORMAL:
                return getSampler().sampleGeometric(p);
            case COLLAPSE_MID:
                return (int) Math.round((1.0d - p) / p);
            default:
                throw new InternalError("DistGeometric has omitted support for sample mode " + mode);
        }
        
    }

    
    // ************************* Private Instance Methods ******************************
    
    private void checkParms() {
        
        if (p <= 0.0d || p > 1.0d) {
            throw new IllegalArgumentException("p parameter " + p + " is not in (0,1]");
        }
    }

    
}
