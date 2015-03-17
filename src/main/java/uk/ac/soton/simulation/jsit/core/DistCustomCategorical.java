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
import java.util.Arrays;
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

    /**
     * Create an instance where outcomes map to an Java enum category.
     * 
     * @since 0.1
     * 
     * @param categoryEnumType The enum which defines your categories.
     * @param pmf The probability mass function (PMF) as an array of probabilities.
     */
    public DistCustomCategorical(Class<C> categoryEnumType, double[] pmf) {

        super(categoryEnumType);
        checkPMF(pmf);            // Superclass-provided check method       
        this.pmf = pmf;

    }
    
    /**
     * Create an instance where outcomes map to integers 1-K. You may wish to
     * add ranges so that one of a particular set of integers is returned, in
     * which case you should keep the distribution locked until all ranges
     * have been added (and then unlock it).
     * 
     * @since 0.2
     * 
     * @param pmf The probability mass function (PMF) as an array of probabilities.
     * @param keepLocked Whether to keep the distribution locked (see above).
     */
    public DistCustomCategorical(double[] pmf, boolean keepLocked) {

        super(pmf.length, keepLocked);
        checkPMF(pmf);            // Superclass-provided check method       
        this.pmf = pmf;

    }


    // ************************* Public Methods *************************************
    
    /**
     * Unlock the distribution (allowing it to be sampled) after changes to probabilities
     * are completed. The probabilities will need to sum to 1 at this point.
     * 
     * @since 0.2
     */
    @Override
    public void unlock() {
        
        checkPMF(pmf);
        super.unlock();
        
    }
    
    /**
     * Set (change) a probability value linked to a categorical outcome.
     * The distribution will be locked automatically when you change the first
     * probability; you should unlock it after the last change is done.
     * 
     * @since 0.1
     * 
     * @param categoryValue The enum value (category) being changed.
     * @param prob The new probability.
     */
    public void setProbability(C categoryValue, double prob) {

        pmf[categoryValue.ordinal()] = prob;
        if (!isLocked()) {      // Lock on the first prob change
            lock();
        }

    }
    
    /**
     * Set (change) a probability value linked to a numeric (integer) outcome.
     * The distribution will be locked automatically when you change the first
     * probability; you should unlock it after the last change is done.
     * 
     * @since 0.2
     * 
     * @param outcomeNum
     *            The number ('position') of the outcome to change (1-K).
     * @param prob
     *            The new probability.
     */
    public void setProbability(int outcomeNum, double prob) {
        
        if (outcomeNum < 1 || outcomeNum > pmf.length) {
            throw new IllegalArgumentException("Outcome number " + outcomeNum
                    + " is outside the expected range [1," + getK() + "]");
        }
        pmf[outcomeNum - 1] = prob;
        if (!isLocked()) {      // Lock on the first prob change
            lock();
        }
        
    }

    /**
     * Custom toString implementation.
     * 
     * @since 0.1
     * 
     * @return A string representation of the distribution;
     * e.g., <code>"CustomPMF[0.1,0.2,0.7]"</code>.
     */
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

    /**
     * Create an unregistered copy of this distribution.
     * @since 0.2
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public AbstractStochasticItem createUnregisteredCopy() {
        
        DistCustomCategorical copy;
        Range currRange;
        double[] pmfCopy = Arrays.copyOf(pmf, pmf.length);
        
        if (usesEnumCategory()) {
            copy = new DistCustomCategorical(getCategory(), pmfCopy);          
        }
        else {
            copy = new DistCustomCategorical(pmfCopy, false);
        }
        
        if (usesMappedRanges()) {
            copy.lock();
            for (int i = 0; i < getNumSubRanges(); i++) {
                currRange = getSubRange(i);
                assert currRange != null;
                copy.addRange(currRange.min, currRange.max);
            }
            copy.unlock();
        }        
        
        return copy;
        
    }


    // ******************* Protected/Package-Access Methods *************************

    @Override
    protected int sampleOrdinalByMode() {

        Integer sample = null;
        Sampler.SampleMode mode = getAccessInfo().getSampleMode();

        double randomProb;        
        if (mode == Sampler.SampleMode.COLLAPSE_MID) {
            randomProb = 0.5d;
        }
        else {
            randomProb = getSampler().sampleUniform(0, 1);
        }

        if (logger.isTraceEnabled()) {
            logger.trace(getAccessInfo().getFullID() + " (Mode " + mode
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
