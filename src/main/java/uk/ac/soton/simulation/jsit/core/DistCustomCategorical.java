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

import com.thoughtworks.xstream.XStream;

/**
 * JSIT representation of a custom categorical distribution.
 * 
 * @author Stuart Rossiter
 * @since 0.1
 */    
public class DistCustomCategorical<C extends Enum<C>>
        extends DistributionCategorical<C> implements Serializable {

    // ************************** Static Fields ***************************************

    private static final Logger logger = LoggerFactory.getLogger(
            DistCustomCategorical.class.getCanonicalName());

    private static final long serialVersionUID = 1L;


    // ************************** Static Methods **************************************

    static void setupForInfoSerialisation(XStream xstream) {

        xstream.alias("distCustomCategorical", DistCustomCategorical.class);

    }


    // ************************* Instance Fields **************************************    

    // PMF (Probability Mass Function) as an array of (double) probabilities
    private final double[] pmf;


    // ************************** Constructors ****************************************

    public DistCustomCategorical(Class<C> categoryEnumType, double[] pmf) {

        super(categoryEnumType);
        checkPMF(pmf);            // Superclass-provided check method       
        this.pmf = pmf;

    }


    // ************************* Public Methods *************************************

    public void setProbability(C categoryValue, double prob) {

        pmf[categoryValue.ordinal()] = prob;
        checkPMF(pmf);        // Recheck that probs sum to (near to) 1

    }

    @Override
    public String toString() {

        StringBuilder stringRep = new StringBuilder();
        stringRep.append("CustomPMF[");
        for (int i = 0; i < pmf.length; i++) {
            stringRep.append(pmf[i]);
            if (i != pmf.length - 1) {
                stringRep.append(",");
            }
        }
        stringRep.append("]");

        return stringRep.toString();

    }


    // ******************* Protected/Package-Access Methods *************************

    @Override
    protected int sampleOrdinalByMode() {

        Integer sample = null;
        Sampler.SampleMode mode = getAccessor().getSampleMode();

        double randomProb;        
        if (mode == Sampler.SampleMode.COLLAPSE_MID) {
            randomProb = 0.5d;
        }
        else {
            randomProb = getSampler().sampleUniform(0, 1);
        }

        if (logger.isTraceEnabled()) {
            logger.trace(getAccessor().getFullID() + " (Mode " + mode
                    + "): sampling for cumulative prob " + randomProb
                    + " from " + this.toString());
        }

        double cumulativeProb = 0.0d;
        for (int i = 0; i < pmf.length; i++) {
            cumulativeProb += pmf[i];
            if (randomProb <= cumulativeProb) {
                sample = i + 1;
                break;
            }
        }

        if (sample == null) {            // 'Overflow'; assume be due to rounding
            logger.warn("Overflowed lookup table matching random sample " + randomProb
                    + ": assuming rounding issues. Defaulting to last alternative");
            sample = pmf.length;
        }

        return sample;

    }


}
