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
 * JSIT abstract representation of all categorical distributions. The categories
 * can either be a Java enum and/or numbered outcomes 1-K which can be mapped to a
 * sequence of integer ranges [a,b] (where there should be K entries across all
 * the ranges). The nested Range class represents those ranges.
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

    /**
     * Nested class representing an integer (inclusive) range [min, max].
     * 
     * @author Stuart Rossiter
     * @since 0.1
     */
    public static class Range {
        /**
         * The range minimum.
         */
        public final int min;
        /**
         * The range maximum.
         */
        public final int max;
        
        /**
         * Create a range.
         * @param min The minimum.
         * @param max The maximum.
         */
        public Range(int min, int max) {
            if (max < min) {
                throw new IllegalArgumentException("Range max is less than min");
            }
            this.min = min;
            this.max = max;
        }
        
        /**
         * Get the number of entries for the range.
         * 
         * @return The number of entries.
         */
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
    
    // Dist is 'locked' when the probs are being changed but the changes have not yet
    // been completed
    private boolean isLocked = false;

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

    protected DistributionCategorical(int k, boolean keepLocked) {

        super();        
        this.categoryEnumType = null;
        this.k = k;
        if (keepLocked) {
            lock();
        }

    }


    // ************************* Public Methods *************************************

    /**
     * Lock the distribution (which will cause an error on sampling) whilst changes
     * are made to it (e.g., ranges are added to map to numeric outcomes).
     * 
     * @since 0.2
     */
    public void lock() {
        
        if (isLocked) {
            throw new IllegalStateException("Distribution is already locked");
        }
        isLocked = true;
        
    }
    
    /**
     * Unlock the distribution (allowing it to be sampled) after changes to it
     * are completed. Any checks as to the consistency of the distribution will be
     * (re)made at this point.
     * 
     * @since 0.2
     */
    public void unlock() {
        
        if (!isLocked) {
            throw new IllegalStateException("Distribution must be locked to unlock it");
        }
        if (ranges != null && rangesEntries != getK()) {
            throw new UnsupportedOperationException(
                    "Need to set up ranges with K (" + getK() + ") entries before unlocking");
        }
        isLocked = false;
        
    }
    
    /**
     * Determine if the distribution is locked.
     * 
     * @since 0.2
     * 
     * @return A boolean true/false value.
     */
    public boolean isLocked() {
        
        return isLocked;
        
    }
    
    /**
     * Require concrete subclasses to implement toString.
     * 
     * @since 0.1
     */
    @Override
    public abstract String toString();

    /**
     * Whether this distribution returns an enum category on sampling or not. (If not,
     * it returns an integer.)
     * 
     * @since 0.1
     * 
     * @return A boolean true/false.
     */
    public boolean usesEnumCategory() {

        return categoryEnumType != null;

    }
    
    /**
     * Whether this distribution uses mapped ranges or not. (If not,
     * it returns a 1-K integer when sampling an integer.)
     * 
     * @since 0.2
     * 
     * @return A boolean true/false.
     */
    public boolean usesMappedRanges() {

        return ranges != null;

    }

    /**
     * Sample a Java enum category value from the distribution. Will error if the
     * distribution is locked.
     * 
     * @since 0.1
     * 
     * @return The enum value sampled.
     */
    public C sampleCategory() {

        if (isLocked) {
            throw new UnsupportedOperationException("Distribution is locked");
        }
        AbstractStochasticAccessInfo accessor = getAccessInfo();
        if (accessor == null) {
            throw new IllegalStateException("Stochastic item not added to (registered via) an accessor");
        }
        Sampler.SampleMode mode = accessor.getSampleMode();
        assert mode != null;
        if (!usesEnumCategory()) {
            throw new UnsupportedOperationException(
                    "This distribution not defined to return a category");
        }

        int enumIdx = sampleOrdinalByMode() - 1;        // Zero-based
        C sample = categoryEnumType.getEnumConstants()[enumIdx];        
        if (logger.isTraceEnabled()) {
            logger.trace(accessor.getFullID() + " (Mode " + mode + "): sampled " + sample + " from " + toString());
        }

        return sample;

    }

    /**
     * Add a range to be used as part of mapping sampled outcomes to an integer. Ranges
     * are applied in the order added; e.g., adding [1,2] and [5,6] ranges means that
     * the 4 possible outcomes map to 1,2,5 and 6 respectively.
     * 
     * The distribution should be locked prior to adding the first such range, and unlocked
     * after the last is added (at which point it will be checked that the sequence of ranges
     * includes K elements).
     * 
     * @since 0.1
     * 
     * @param min The range minimum.
     * @param max The range maximum.
     * @return The number of entries across all ranges added so far. (This should equal K
     * when the last range is added.)
     */
    public int addRange(int min, int max) {

        Range newRange = new Range(min, max);
        rangesEntries += newRange.numEntries();
        if (rangesEntries > getK()) {
            throw new IllegalArgumentException(
                    "Range causes alternatives to exceed K");
        }
        if (ranges == null) {            // Lazy instantiation
            ranges = new LinkedList<Range>();
        }
        ranges.add(newRange);
        // No debug message since no access to ID if range being added during
        // instantiation (i.e., pre addition to a StochasticAccessor)

        return rangesEntries;

    }

    /**
     * Clear all ranges, which will revert any integer sampling to return a
     * 'raw' 1-K value.
     * 
     * @since 0.1
     */
    public void clearRanges() {

        if (ranges != null) {        // Nothing to do otherwise    
            ranges.clear();
            rangesEntries = 0;
        }

    }

    /**
     * Get the distribution's K value; i.e.,
     * the number of discrete alternatives (labellable 1-K)
     * 
     * @since 0.1
     * 
     * @return The K value.
     */
    public int getK() {

        return k;

    }


    // ******************* Protected/Package-Access Methods *************************

    protected abstract int sampleOrdinalByMode();
    
    /*
     * Sample a (discrete) integer. The 'raw' 1-K value is sampled
     * and then the ranges used to map this to an output integer
     */
    @Override
    protected int sampleIntByMode() {

        if (isLocked) {
            throw new UnsupportedOperationException("Distribution is locked");
        }
        if (rangesEntries == 0) {            // No ranges; sample 'raw' 1-K int
            return sampleOrdinalByMode();
        }
        

        Sampler.SampleMode mode = getAccessInfo().getSampleMode();
        int rangeIdx = sampleOrdinalByMode();        // One-based
        if (logger.isTraceEnabled()) {
            logger.trace(getAccessInfo().getFullID() + " (Mode " + mode
                    + "): mapping raw ordinal " + rangeIdx
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
    
    /*
     * Get the Java enum used as the category. Will return null if none has been set up.
     */
    protected Class<C> getCategory() {

        return categoryEnumType;

    }
    
    /*
     * Get a particular sub-range.
     */
    protected Range getSubRange(int rangeIdx) {

        if (rangeIdx < 0 || ranges == null || rangeIdx >= ranges.size()) {
            return null;
        }
        return ranges.get(rangeIdx);

    }
    
    /*
     * Clear ranges and change the K value. Only works if an enum category has not been set
     * (unless the revised K is actually the same as before).
     */
    protected void clearRanges(int revisedK, boolean keepLocked) {

        if (revisedK <= 0) {
            throw new IllegalArgumentException("Number of alternatives K must be positive");
        }
        if (revisedK != k && usesEnumCategory()) {
            throw new UnsupportedOperationException("Can't change K when returning a category");
        }
        clearRanges();
        this.k = revisedK;
        if (keepLocked) {
            lock();
        }

    }

}
