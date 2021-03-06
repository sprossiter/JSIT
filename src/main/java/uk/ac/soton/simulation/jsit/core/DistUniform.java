/*  
    Copyright 2018 University of Southampton, Stuart Rossiter
    
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
 * JSIT representation of the continuous uniform distribution.
 * 
 * @author Stuart Rossiter
 * @since 0.1
 */    
public class DistUniform extends DistributionContinuous implements Serializable {

    // ************************** Static Fields ***************************************

    //private static final Logger logger = LoggerFactory.getLogger(
    //                                      DistUniform.class);

    private static final long serialVersionUID = 1L;


    // ************************** Static Methods **************************************

    static void setupForInfoSerialisation(XStream xstream) {

        xstream.alias("distUniform", DistUniform.class);

    }


    // ************************* Instance Fields **************************************

    private double min;
    private double max;


    // ************************** Constructors ****************************************

    /**
     * Default constructor.
     * 
     * @since 0.1
     * 
     * @param min The minimum sampled value.
     * @param max The maximum sampled value.
     */
    public DistUniform(double min, double max) {

        super();
        setMin(min);
        setMax(max);

    }


    // ************************** Public Methods **************************************

    /**
     * Set (change) the distribution's minimum.
     * 
     * @since 0.1
     * 
     * @param min The minimum value to use.
     */
    public void setMin(double min) {

        this.min = min;

    }

    /**
     * Get the distribution's minimum.
     * 
     * @since 0.1
     * 
     * @return The minimum.
     */
    public double getMin() {

        return min;

    }

    /**
     * Set (change) the distribution's maximum.
     * 
     * @since 0.1
     * 
     * @param max The maximum value to use.
     */
    public void setMax(double max) {

        this.max = max;

    }

    /**
     * Get the distribution's maximum.
     * 
     * @since 0.1
     * 
     * @return The maximum.
     */
    public double getMax() {

        return max;

    }

    /**
     * Custom toString implementation.
     * 
     * @since 0.1
     * 
     * @return A string representation of the distribution;
     * e.g., <code>"U(3.0, 4.0)"</code>.
     */
    @Override
    public String toString() {

        return "U(" + min + "," + max + ")";

    }

    /**
     * Create an unregistered copy of this distribution.
     * 
     * @since 0.2
     * 
     * @return The copy of this distribution.
     */
    @Override
    public Distribution createUnregisteredCopy() {
        
        return new DistUniform(min, max);
        
    }


    // ******************* Protected/Package-Access Methods *************************

    @Override
    protected double sampleDoubleByMode() {

        Double sample = null;
        Sampler.SampleMode mode = getAccessInfo().getSampleMode();

        if (mode == Sampler.SampleMode.NORMAL) {
            sample = getSampler().sampleUniform(min, max);
        }
        else if (mode == Sampler.SampleMode.COLLAPSE_MID) {           
            sample = (min + max) / 2.0;
        }

        return sample;

    }


}
