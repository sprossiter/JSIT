package uk.ac.soton.simulation.jsit.anylogic;

/**
 * Interface for random number generators (RNGs) which make the seed accessible.
 * 
 * @author Stuart Rossiter
 * @since 0.2
 *
 */
public interface SeedAccessibleRNG {

    /**
     * Get the seed value for this RNG.
     * 
     * @return The seed value.
     */
    long getSeed();
    
}
