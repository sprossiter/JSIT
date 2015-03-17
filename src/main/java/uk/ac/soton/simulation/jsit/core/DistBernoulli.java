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

import com.thoughtworks.xstream.XStream;

//import org.slf4j.*;

/**
 * JSIT representation of a Bernoulli distribution.
 * 
 * @author Stuart Rossiter
 * @since 0.1
 */    
public class DistBernoulli
extends DistributionCategorical<Sampler.Binary> implements Serializable {

    // ************************** Static Fields ***************************************

    //private static final Logger logger = LoggerFactory.getLogger(
    //                    DistBernoulli.class);

    private static final long serialVersionUID = 1L;


    // ************************** Static Methods **************************************

    static void setupForInfoSerialisation(XStream xstream) {

        xstream.alias("distBernoulli", DistBernoulli.class);

    }


    // ************************ Instance Fields ***************************************

    private double p;


    // ************************** Constructors ****************************************

    /**
     * Create a Bernoulli distribution instance.
     * 
     * @since 0.1
     * 
     * @param p The p value (success probability) for the trial.
     */
    public DistBernoulli(double p) {

        super(Sampler.Binary.class);
        this.p = p;
        checkP();

    }


    // ************************* Public Methods *************************************

    /**
     * Change (set) the p value of the distribution.
     * 
     * @param p The new p value (probability of success).
     */
    public void setP(double p) {

        this.p = p;
        checkP();

    }

    /**
     * Get the distribution's p value (probability of success).
     * 
     * @return The p value.
     */
    public double getP() {

        return p;

    }

    /**
     * Custom toString implementation.
     * 
     * @since 0.1
     * 
     * @return A string representation of the distribution;
     * e.g., <code>"Bernoulli(0.8)"</code>.
     */
    @Override
    public String toString() {

        return "Bernoulli(" + p + ")";

    }

    /**
     * Create an unregistered copy of this distribution.
     * @since 0.2
     */
    @Override
    public AbstractStochasticItem createUnregisteredCopy() {
        
        return new DistBernoulli(p);
        
    }


    // ******************* Protected/Package-Access Methods *************************

    @Override
    protected int sampleOrdinalByMode() {

        Integer sample = null;
        Sampler.SampleMode mode = getAccessInfo().getSampleMode();

        if (mode == Sampler.SampleMode.NORMAL) {
            sample = getSampler().sampleBernoulli(p);
        }
        else if (mode == Sampler.SampleMode.COLLAPSE_MID) {           
            sample = (p < 0.5 ? 2 : 1);
        }

        return sample;

    }


    // ***************************** Private Methods ********************************

    private void checkP() {
        
        if (p < 0.0d || p > 1.0d) {
            throw new IllegalArgumentException(
                            "Binomial p value " + p + " is not in [0,1]");
        }
        
    }
    
}
