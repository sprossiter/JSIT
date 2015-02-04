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

import org.slf4j.*;

import com.thoughtworks.xstream.XStream;

import java.io.Serializable;
import java.util.*;

/**
 * JSIT abstract representation of all categorical distributions. Includes
 * nested Range class to represent the categories.
 * 
 * @author Stuart Rossiter
 * @since 0.1
 */	
public abstract class DistributionCategorical<C extends Enum<C>>
                extends DistributionDiscrete implements Serializable {

    // ************************ Static Fields *****************************************

    private static final Logger logger
            = LoggerFactory.getLogger(DistributionCategorical.class);

    private static final long serialVersionUID = 1L;

    public static class Range {
        public final int min;
        public final int max;
        public Range(int min, int max) {
            if (max < min) {
                throw new IllegalArgumentException("Range max is less than min");
            }
            this.min = min;
            this.max = max;
        }
        public int numEntries() {
            return max - min + 1;
        }
    };


    // ************************* Static Methods **************************************

    static void setupForInfoSerialisation(XStream xstream) {

        xstream.omitField(DistributionCategorical.class, "rangesEntries");
        xstream.alias("range", Range.class);
        xstream.addImplicitCollection(DistributionCategorical.class, "ranges");

    }

    // ************************* Instance Fields ***************************************

    private final Class<C> categoryEnumType;
    private int k;

    private int rangesEntries = 0;
    private LinkedList<Range> ranges = null;


    // ************************ Constructors *******************************************

    protected DistributionCategorical(Class<C> categoryEnumType) {

        super();
        this.categoryEnumType = categoryEnumType;
        this.k = categoryEnumType.getEnumConstants().length;
        if (k == 0) {
            throw new IllegalArgumentException(
                    "Wrong constructor for non-enum-category-returning distribution");
        }

    }

    protected DistributionCategorical(int k) {

        super();		
        this.categoryEnumType = null;
        this.k = k;

    }


    // ************************* Public Methods *************************************

    /*
     * Force concrete subclasses to override toString()
     */
    @Override
    public abstract String toString();

    /*
     * Sample a (discrete) integer. The 'raw' 1-K value is sampled
     * and then the ranges used to map this to an output integer
     */
    @Override
    protected int sampleIntByMode() {

        if (rangesEntries == 0) {			// No ranges; sample 'raw' 1-K int
            return sampleOrdinalByMode();
        }

        if (rangesEntries != getK()) {
            throw new UnsupportedOperationException(
                    "Need to set up ranges with K (" + getK()
                    + ") entries before sampling an integer");
        }

        Sampler.SampleMode mode = getAccessor().getSampleMode();
        int rangeIdx = sampleOrdinalByMode();		// One-based
        if (logger.isTraceEnabled()) {
            logger.trace(getAccessor().getFullID() + " (Mode " + mode + "): mapping raw ordinal " + rangeIdx
                    + " to ranges (K=" + getK() + ")");
        }
        Integer sample = null;
        for (Range r : ranges) {
            if (rangeIdx <= r.numEntries()) {
                sample = r.min + rangeIdx - 1;
                break;
            }
            rangeIdx -= r.numEntries();
        }
        assert sample != null;
        return sample;

    }

    public boolean returnsCategory() {

        return categoryEnumType != null;

    }

    public Class<C> getCategory() {

        return categoryEnumType;

    }

    public C sampleCategory() {

        AbstractStochasticAccessor<?> accessor = getAccessor();
        if (accessor == null) {
            throw new IllegalStateException("Stochastic item not added to (registered via) an accessor");
        }
        Sampler.SampleMode mode = accessor.getSampleMode();
        assert mode != null;
        if (!returnsCategory()) {
            throw new UnsupportedOperationException(
                    "This distribution not defined to return a category");
        }

        int enumIdx = sampleOrdinalByMode() - 1;		// Zero-based
        C sample = categoryEnumType.getEnumConstants()[enumIdx];		
        if (logger.isTraceEnabled()) {
            logger.trace(accessor.getFullID() + " (Mode " + mode + "): sampled " + sample + " from " + toString());
        }

        return sample;

    }

    /*
     * Add a range that the K outcomes will be mapped to. The
     * distribution will only be integer-sampleable when ranges
     * are added which together give a number of values equal to K.
     * Returns current number of entries in all ranges. If there are
     * no ranges added, the 'raw' 1-K integer is sampled
     */
    public int addRange(int min, int max) {

        Range newRange = new Range(min, max);
        rangesEntries += newRange.numEntries();
        if (rangesEntries > getK()) {
            throw new IllegalArgumentException(
                    "Range causes alternatives to exceed K");
        }
        if (ranges == null) {			// Lazy instantiation
            ranges = new LinkedList<Range>();
        }
        ranges.add(newRange);
        // No debug message since no access to ID if range being added during
        // instantiation (i.e., pre addition to a StochasticAccessor)

        return rangesEntries;

    }

    public Range getSubRange(int rangeIdx) {

        if (rangeIdx < 0 || ranges == null || rangeIdx >= ranges.size()) {
            return null;
        }
        return ranges.get(rangeIdx);

    }

    public void clearRanges() {

        if (ranges != null) {		// Nothing to do otherwise	
            ranges.clear();
            rangesEntries = 0;
        }

    }

    public void clearRanges(int revisedK) {

        if (revisedK <= 0) {
            throw new IllegalArgumentException("Number of alternatives K must be positive");
        }
        if (revisedK != k && returnsCategory()) {
            throw new UnsupportedOperationException("Can't change K when returning a category");
        }
        clearRanges();
        this.k = revisedK;

    }

    /*
     * Get the distribution's K value; i.e.,
     * the number of discrete alternatives (labellable 1-K)
     */
    public int getK() {

        return k;

    }


    // ******************* Protected/Package-Access Methods *************************

    protected abstract int sampleOrdinalByMode();

    protected void checkPMF(double[] pmfValues) {

        if (pmfValues.length != getK()) {
            throw new IllegalArgumentException("PMF needs to provide probs for the K ("
                    + getK() + ") alternatives");
        }

        double cumulativeProb = 0.0d;
        for (double d : pmfValues) {
            if (d < 0.0d || d > 1.0d) {
                throw new IllegalArgumentException("PMF entry " + d + " is not a [0,1] probability");
            }
            cumulativeProb += d;
        }
        if (cumulativeProb - 1.0d > DistributionDiscrete.CUMULATIVE_PROB_TOLERANCE) {
            throw new IllegalArgumentException("Sum of PMF probabilities is outside tolerance "
                    + CUMULATIVE_PROB_TOLERANCE + " from 1");
        }

    }

}
