/*  
    Copyright 2015 University of Southampton
    
    This file is part of JSIT.

    JSIT is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    JSIT is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with JSIT.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.soton.simulation.jsit.core;

import java.io.Serializable;

import org.slf4j.*;

/**
 * JSIT abstract representation of all discrete probability distributions. Also
 * implements ContinuousSampler so that it can be sampled 'as' a continuous
 * distribution if required.
 * 
 * @author Stuart Rossiter
 * @since 0.1
 */    
public abstract class DistributionDiscrete
                extends Distribution implements ContinuousSampler, Serializable {

    // ************************ Static Fields *****************************************

    public static final double CUMULATIVE_PROB_TOLERANCE = 0.001;

    private static final Logger logger
                = LoggerFactory.getLogger(DistributionDiscrete.class);

    private static final long serialVersionUID = 1L;


    // ************************ Constructors ********************************************

    DistributionDiscrete() {

        super();

    }


    // ************************* Public Methods *************************************

    /*
     * Sample an int; distribution must have been registered (sample mode set)
     * first
     */
    public int sampleInt() {

        AbstractStochasticAccessInfo accessor = getAccessInfo();
        if (accessor == null) {
            throw new IllegalStateException("Stochastic item not added to (registered via) an accessor");
        }
        Sampler.SampleMode mode = accessor.getSampleMode();
        if (mode == null) {
            throw new IllegalStateException("Distribution not registered");
        }

        int sample = sampleIntByMode();        
        if (logger.isTraceEnabled()) {
            logger.trace(accessor.getFullID() + " (Mode " + mode + "): sampled "
                    + sample + " from " + toString());
        }

        return sample;

    }
    
    /**
     * Sample as though a continuous distribution (just returns the sampled
     * integer as a double).
     */
    @Override
    public double sampleDouble() {
        
        return (double) sampleInt();
        
    }

    /*
     * Force concrete subclasses to override toString()
     */
    @Override
    public abstract String toString();


    // ******************* Protected/Package-Access Methods *************************

    /*
     * 'Internal' abstract method to sample an int using the sample mode
     */
    protected abstract int sampleIntByMode();

}
