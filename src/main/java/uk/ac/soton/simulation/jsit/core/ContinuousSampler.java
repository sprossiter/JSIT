package uk.ac.soton.simulation.jsit.core;

/**
 * Interface to abstract sampling from a 'continuous distribution'. This
 * allows discrete distributions to be sampled as though continuous; i.e.,
 * just return a floating-point (double) value that is always an integer.
 * 
 * @author Stuart Rossiter
 * @since 0.2
 */
public interface ContinuousSampler {
    
    /**
     * Sample from the distribution.
     * @return The double sampled.
     * @since 0.2
     */
    double sampleDouble();

}
