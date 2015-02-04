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
package uk.ac.soton.simulation.jsit.anylogic;

import java.io.Serializable;

import com.thoughtworks.xstream.XStream;
import com.anylogic.engine.HyperArray;
import com.anylogic.engine.Dimension;

import org.slf4j.*;

import uk.ac.soton.simulation.jsit.core.Sampler.*;
import uk.ac.soton.simulation.jsit.core.StochasticItem;

/**
 * An enum-or-numeric-dimensions-indexed lookup array
 * ('field') for categorical distributions using an AnyLogic HyperArray and related
 * Dimensions (which provides a nice mechanism to edit this 'custom distribution field'
 * and thus provide it as a model parameter), but using enums in the main model which
 * can map to AnyLogic Dimension indices (via the DimConvertable interface).
 * (We want to avoid using Dimensions directly in the model because---as static fields
 * in a generated HyperArrays class with a single scope for alternative names---they aren't
 * amenable to use as agent attributes.)
 * 
 * <p>Since Dimension values will typically be returned from simulation Agents (e.g.
 * querying death-in-year for an agent of particular gender and age-range), enums which
 * match the dimensions need to be used internally, with the (zero-based) ordinals of
 * given enum values used for querying this lookup.
 * 
 * <p>As examples,
 * <ul>
 * <li>probabilities (say of dieing per year) by age-range and gender &rarr;
 * Binary lookup by Gender and AgeRange Dimension values, backed by a
 * HyperArray of probabilities for Gender and AgeRange Dimensions;
 * 
 * <li>age range determined from empirical probabilities per gender
 * AgeRange lookup by Gender Dimension value, also backed by a
 * HyperArray of probabilities for Gender and AgeRange Dimensions, but
 * with the probabilities representing the chance of being in that AgeRange for
 * the given Gender.
 * </ul>
 */
public class HyperArrayLookup<C extends Enum<C>> extends StochasticItem
                                                 implements Serializable  {

    /*
     * TODO: Move sample mode and isBernoulliLookup to abstract JSIT (non-AL-specific)
     * superclass and code in registration of these fields (and stoch settings applying at
     * the field level). Fields included in stoch handler and thus sampler moved out as well
     */ 
    
    // ************************ Static Fields *****************************************

    private static final Logger logger
                = LoggerFactory.getLogger(HyperArrayLookup.class);

    private static final long serialVersionUID = 1L;


    // ************************** Static Methods **************************************

    static void setupForInfoSerialisation(XStream xstream) {

        xstream.alias("hyperArrayLookup", HyperArrayLookup.class);
        xstream.omitField(HyperArrayLookup.class, "hyperArrayDims");

    }


    // ************************ Instance Fields ***************************************

    // The category (enum class) which this lookup samples
    protected final Class<C> categoryEnumType;

    // The underlying HyperArray and convenience copy of the dimensions
    private final HyperArray hyperArray;
    private final Dimension[] hyperArrayDims;


    // ************************* Constructors *****************************************

    public HyperArrayLookup(Class<C> categoryEnumType,
                            HyperArray hyperArray) {

        this.categoryEnumType = categoryEnumType;

        // Check that entries are probabilities
        // TODO: may need to use a tolerance to avoid false exceptions due to rounding
        // TODO: Can also check that probs sum to (near to) 1 (Bernoulli lookup) or sum to 1 per
        // lookup dimension combination (non-Bernoulli lookup)

        for (double prob : hyperArray.getData()) {
            if (prob < 0.0 || prob > 1.0) {
                throw new IllegalArgumentException(
                        "HyperArray entry " + prob + " is not in [0,1]");
            }
        }   	

        this.hyperArray = hyperArray;
        this.hyperArrayDims = hyperArray.getDimensions();

    }


    // ************************* Public Instance Methods ******************************

    /*
     * TODO
     */
    public C sampleCategory(DimConvertable... dimIndexSources) {

        if (isBernoulliLookup()) {
            throw new UnsupportedOperationException("Can only sample trials on a Bernoulli lookup");
        }
        if (dimIndexSources.length != hyperArrayDims.length - 1) {
            throw new IllegalArgumentException("Should supply first " + (hyperArrayDims.length - 1)
                    + " HyperArray dims for lookup category sample");
        }

        C sample = null;
        SampleMode mode = getSampleMode();

        int[] accessIndices = new int[hyperArrayDims.length];
        int i = 0;
        while (i < dimIndexSources.length) {
            accessIndices[i] = dimIndexSources[i].asAnyLogicDimIndex();
            i++;
        }

        // i now indexes the last dimension (which we're going to loop through)

        double randomProb;    	
        if (mode == SampleMode.COLLAPSE_MID) {
            randomProb = 0.5d;
        }
        else {
            randomProb = ((Sampler_AnyLogic) getSampler()).sampleUniform(0, 1);
        }

        if (logger.isTraceEnabled()) {
            StringBuilder printRow = new StringBuilder();
            printRow.append("[");
            for (int j = 1; j <= hyperArrayDims[i].size(); j++) {
                accessIndices[i] = j;
                printRow.append(hyperArray.get(accessIndices));
                printRow.append(",");
            }
            printRow.deleteCharAt(printRow.length() - 1);
            printRow.append("]");

            logger.trace(getAccessor().getFullID() + " (Mode " + mode
                    + "): sampling for cumulative prob " + randomProb
                    + " from " + printRow.toString());
        }

        double cumulativeProb = 0.0d;
        for (int j = 1; j <= hyperArrayDims[i].size(); j++) {
            accessIndices[i] = j;
            cumulativeProb += hyperArray.get(accessIndices);
            if (randomProb <= cumulativeProb) {
                sample = categoryEnumType.getEnumConstants()[j - 1];
                break;
            }
        }

        if (sample == null) {			// 'Overflow'; assume be due to rounding
            logger.warn("Overflowed lookup table matching random sample " + randomProb
                    + ": assuming rounding issues. Defaulting to last alternative");
            C[] alts = categoryEnumType.getEnumConstants();
            sample = alts[alts.length - 1];		// Default to last alternative
        }

        if (logger.isTraceEnabled()) {
            logger.trace(getAccessor().getFullID() + " (Mode " + mode
                    + "): sampled categorical outcome " + sample);
        }

        return sample;

    }

    public Binary sampleTrialOutcome(DimConvertable ...dimIndexSources) {

        if (!isBernoulliLookup()) {
            throw new UnsupportedOperationException("Can only sample trials on a Bernoulli lookup");
        }
        if (dimIndexSources.length != hyperArrayDims.length) {
            throw new IllegalArgumentException("Should supply all " + hyperArrayDims.length
                    + " HyperArray dims for lookup binary sample");
        }

        int[] accessIndices = new int[hyperArrayDims.length];
        for (int i = 0; i < accessIndices.length; i++) {
            accessIndices[i] = dimIndexSources[i].asAnyLogicDimIndex();
        }

        double prob = hyperArray.get(accessIndices);
        Binary sample;
        SampleMode mode = getSampleMode();

        if (mode == SampleMode.COLLAPSE_MID) {
            sample = (prob < 0.5d ? Binary.FAILURE : Binary.SUCCESS);
        }
        else {
            // This 'internal' Bernoulli dist never needs exposing to the user. Any stochastic
            // overrides apply to the lookup. We could have an explicit internal DistBernoulli,
            // but we'd have to change its p value every sample
            int alSample = ((Sampler_AnyLogic) getSampler()).sampleBernoulli(prob);
            assert alSample == 1 || alSample == 2;			// 1-K scheme; see Sampler comments
            sample = (alSample == 1 ? Binary.FAILURE : Binary.SUCCESS);
        }

        if (logger.isTraceEnabled()) {
            logger.trace(getAccessor().getFullID() + " (Mode " + mode
                    + "): sampled trial outcome " + sample + " for p=" + prob);
        }
        return sample;

    }


    // **************** Protected / Package-Access Instance Methods ********************

    protected boolean isBernoulliLookup() {

        return categoryEnumType.getCanonicalName().equals(
                "uk.ac.soton.simulation.jsit.core.Sampler.Binary");

    }

}
