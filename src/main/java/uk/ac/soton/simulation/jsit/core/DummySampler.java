package uk.ac.soton.simulation.jsit.core;

/**
 * Dummy Sampler which supports no distributions. This is intended for use by models
 * not using a helper library which therefore need to implement their own Sampler
 * subclass and may need to only support a small number of distributions (or none),
 * and thus can use DummySampler as their Sampler implementation or a base for their
 * own subclass which only overrides methods as needed.
 * <p>
 * distIsSupported always returns false and all the sample* methods throw an
 * UnsupportedOperationException.
 * 
 * @author Stuart Rossiter
 * @since 0.2
 *
 */
public class DummySampler extends Sampler {

    // ************************* Static Fields *****************************************

    private static final long serialVersionUID = 1L;
    private static final String UNSUPPORTED_MSG = "Dummy sampler supports no distributions";


    // ************************** Constructors *****************************************
    
    public DummySampler() {
        
        // Nothing to do
        super();
        
    }


    // **************** Protected/Package-Access Instance Methods **********************

    @Override
    protected boolean distIsSupported(Distribution dist) {
        
        return false;
        
    }

    @Override
    protected double sampleNormal(double mean, double sd) {
        
        throw new UnsupportedOperationException(DummySampler.UNSUPPORTED_MSG);
        
    }

    @Override
    protected double sampleExponential(double mean) {
        
        throw new UnsupportedOperationException(DummySampler.UNSUPPORTED_MSG);
        
    }

    @Override
    protected double sampleUniform(double min, double max) {
        
        throw new UnsupportedOperationException(DummySampler.UNSUPPORTED_MSG);
        
    }

    @Override
    protected int sampleUniformDiscrete(int max) {
        
        throw new UnsupportedOperationException(DummySampler.UNSUPPORTED_MSG);
        
    }

    @Override
    protected int sampleBernoulli(double p) {
        
        throw new UnsupportedOperationException(DummySampler.UNSUPPORTED_MSG);
        
    }

    @Override
    protected int samplePoisson(double lambda) {
        
        throw new UnsupportedOperationException(DummySampler.UNSUPPORTED_MSG);
        
    }


    @Override
    protected int sampleNegBin(int r, double p) {
        
        throw new UnsupportedOperationException(DummySampler.UNSUPPORTED_MSG);
        
    }


    @Override
    protected int sampleGeometric(double p) {
        
        throw new UnsupportedOperationException(DummySampler.UNSUPPORTED_MSG);
        
    }


    @Override
    protected double sampleTriangular(double min, double mode, double max) {
        
        throw new UnsupportedOperationException(DummySampler.UNSUPPORTED_MSG);
        
    }

 
}
