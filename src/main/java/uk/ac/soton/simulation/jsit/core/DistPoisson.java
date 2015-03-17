package uk.ac.soton.simulation.jsit.core;

import java.io.Serializable;

import com.thoughtworks.xstream.XStream;

/**
 * JSIT representation of a Poisson distribution.
 * 
 * @author Stuart Rossiter
 * @since 0.2
 */
public class DistPoisson extends DistributionDiscrete implements Serializable {


    // ************************* Static Fields *****************************************
    
    private static final long serialVersionUID = 1L;
    

    // ************************* Static Methods ****************************************
    
    static void setupForInfoSerialisation(XStream xstream) {

        xstream.alias("distPoisson", DistPoisson.class);

    }


    // ************************* Instance Fields ***************************************
    
    private double lambda;

    
    // ************************** Constructors *****************************************
    
    /**
     * Create a Poisson distribution instance.
     * 
     * @since 0.2
     * 
     * @param lambda The lambda value (mean) for the distribution.
     */
    public DistPoisson(double lambda) {
        
        super();
        this.lambda = lambda;
        checkParms();
        
    }

    // *********************** Public Instance Methods *********************************

    /**
     * Change (set) the lambda value of the distribution.
     * 
     * @param lambda The new lambda value (distribution mean).
     */
    public void setLambda(double lambda) {

        this.lambda = lambda;
        checkParms();

    }

    /**
     * Get the distribution's lambda value (mean).
     * 
     * @return The lambda value.
     */
    public double getLambda() {

        return lambda;

    }
    
    /**
     * Custom toString implementation.
     * 
     * @since 0.2
     * 
     * @return A string representation of the distribution;
     * e.g., <code>"Poisson(1.0)"</code>.
     */
    @Override
    public String toString() {
        
        return "Poisson(" + lambda + ")";
        
    }

    /**
     * Create an unregistered copy of this distribution.
     * @since 0.2
     */
    @Override
    public AbstractStochasticItem createUnregisteredCopy() {
        
        return new DistPoisson(lambda);
        
    }

    
    // **************** Protected/Package-Access Instance Methods **********************
    
    @Override
    protected int sampleIntByMode() {
        
        Sampler.SampleMode mode = getSampleMode();
        switch(getSampleMode()) {
            case NORMAL:
                return getSampler().samplePoisson(lambda);
            case COLLAPSE_MID:
                return (int) Math.round(lambda);
            default:
                throw new InternalError("DistPoisson has omitted support for sample mode " + mode);
        }
        
    }

    
    // ************************* Private Instance Methods ******************************
    
    private void checkParms() {
        
        if (lambda < 0.0d) {
            throw new IllegalArgumentException("Lambda value " + lambda + " is < 0");
        }
    }

}
