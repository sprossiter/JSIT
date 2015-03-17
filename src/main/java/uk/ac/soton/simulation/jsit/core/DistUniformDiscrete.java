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
 * JSIT representation of the discrete uniform distribution.
 * 
 * @author Stuart Rossiter
 * @since 0.1
 */    
public class DistUniformDiscrete<C extends Enum<C>>
                extends DistributionCategorical<C> implements Serializable {

    // ************************** Static Fields ***************************************

    //private static final Logger logger = LoggerFactory.getLogger(
    //                        DistUniformDiscrete.class);

    private static final long serialVersionUID = 1L;


    // ************************** Static Methods **************************************

    static void setupForInfoSerialisation(XStream xstream) {

        xstream.alias("distUniformDiscrete", DistUniformDiscrete.class);

    }


    // ************************** Constructors ****************************************

    /**
     * Create an instance where sampling returns a Java enum category.
     * 
     * @since 0.1
     * 
     * @param categoryEnumType The type (Class instance) of the category enum.
     */
    public DistUniformDiscrete(Class<C> categoryEnumType) {

        super(categoryEnumType);

    }

    /**
     * Create an instance returning an integer value from a single range.
     * 
     * @since 0.1
     * 
     * @param min The range minimum.
     * @param max The range maximum.
     */
    public DistUniformDiscrete(int min, int max) {

        super(max - min + 1, true);     // Keep locked for correctness-sake
        setRange(min, max);             // This unlocks once finished

    }


    // ************************* Public Methods *************************************

    /**
     * Change or add a range for the distribution. Will error if it
     * is also set to return an enum category with a different number of categories.
     * 
     * @since 0.1
     * 
     * @param min The revised range minimum.
     * @param max The revised range maximum.
     */
    public void setRange(int min, int max) {

        clearRanges(max - min + 1, true);  // Lock after clear for correctness-sake
        if (min != 1) {                // No range needed if min == 1; returns 'raw' 1-max         
            addRange(min, max);            // Add range to superclass
        }
        unlock();

    }
    
    /**
     * Get the Range (DistributionCategorical.Range) for this distribution.
     * 
     * @since 0.2
     * 
     * @return The uniform discrete range as a Range object.
     */
    public Range getRange() {
        
        if (usesMappedRanges()) {
            return getSubRange(0);
        }
        else {
            return null;
        }
        
    }

    /**
     * Custom toString implementation.
     * 
     * @since 0.1
     * 
     * @return A string representation of the distribution;
     * e.g., <code>"DiscreteUniform(1,10)"</code>.
     */
    @Override
    public String toString() {

        String stringRep;

        if (usesEnumCategory()) {
            stringRep = "DiscreteUniform(" + getCategory().getSimpleName() + ")";
        }
        else {
            Range r = getSubRange(0);    // Always only 1 range
            if (r == null) {
                stringRep = "DiscreteUniform(1," + getK() + ")";
            }
            else {
                stringRep = "DiscreteUniform(" + r.min + "," + r.max + ")";
            }
        }

        return stringRep;

    }

    /**
     * Create an unregistered copy of this distribution.
     * @since 0.2
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public AbstractStochasticItem createUnregisteredCopy() {
        
        DistUniformDiscrete copy;
        Range currRange;
        
        if (usesEnumCategory()) {
            copy = new DistUniformDiscrete(getCategory());
            if (usesMappedRanges()) {
                currRange = getRange();
                copy.addRange(currRange.min, currRange.max);
            }
        }
        else {
            currRange = getRange();
            copy = new DistUniformDiscrete(currRange.min, currRange.max);
        }
        return copy;
        
    }


    // ******************* Protected/Package-Access Methods *************************

    @Override
    protected int sampleOrdinalByMode() {

        Integer sample = null;
        Sampler.SampleMode mode = getAccessInfo().getSampleMode();

        if (mode == Sampler.SampleMode.NORMAL) {
            sample = getSampler().sampleUniformDiscrete(getK());
        }
        else if (mode == Sampler.SampleMode.COLLAPSE_MID) {           
            sample = (1 + getK()) / 2;            // Truncate if odd
        }

        return sample;

    }

}
