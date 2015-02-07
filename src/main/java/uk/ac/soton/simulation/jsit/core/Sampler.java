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

//import org.slf4j.*;

/**
 * Abstract class representing a JSIT stochasticity sampler.
 * 
 * @author Stuart Rossiter
 * @since 0.1
 */    
public abstract class Sampler implements Serializable {

    // ************************ Static Fields *****************************************

    //private static final Logger logger
    //                = LoggerFactory.getLogger(JSIT_Sampler.class);

    private static final long serialVersionUID = 1L;

    /**
     * Fixed enum for binary categorical outcomes (as in the Bernoulli distribution)
     * Standard order of failure/success (as in 0/1 outcome).
     */
    public static enum Binary {

        FAILURE, SUCCESS;

    }

    /**
     * Fixed zero-alternatives enum to instantiate integer-only-outcomes categorical
     * distributions.
     */
    public static enum NumericCategory {

        // No alternatives 'marker enum'

    }

    /**
     * Sample-mode alternatives for stochastic items.
     */
    public static enum SampleMode {

        NORMAL, COLLAPSE_MID;

    };


    // *************************** Constructors *****************************************

    /**
     * Default constructor.
     */
    public Sampler() {

        // Nothing to do

    }


    // ******************* Protected/Package-Access Methods ****************************

    /*
     * Whether distribution is supported (a framework-specific subclass
     * decision)
     */
    protected abstract boolean distIsSupported(Distribution dist);

    /*
     * Standard mean, sd ordering
     */
    protected abstract double sampleNormal(double mean, double sd);

    /*
     * Mean is 1/lambda
     */
    protected abstract double sampleExponential(double mean);

    /*
     * Continuous uniform dist
     */
    protected abstract double sampleUniform(double min, double max);

    /*
     * Continuous uniform dist. Only [1, max] version since use ranges to
     * transform to others
     */
    protected abstract int sampleUniformDiscrete(int max);

    /*
     * Bernoulli (probabilistic trial). To fit 1-K raw
     * scheme, this should return 1 (failure) or 2 (success)
     */
    protected abstract int sampleBernoulli(double p);


    // ************************** Private Methods **************************************


}
