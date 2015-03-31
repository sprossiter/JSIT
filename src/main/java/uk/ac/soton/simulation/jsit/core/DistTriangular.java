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
 * JSIT representation of a triangular distribution.
 * 
 * @author Stuart Rossiter
 * @since 0.2
 */    
public class DistTriangular extends DistributionContinuous
                                 implements Serializable {

    // ************************** Static Fields ***************************************

    private static final long serialVersionUID = 1L;


    // ************************** Static Methods **************************************

    static void setupForInfoSerialisation(XStream xstream) {

        xstream.alias("distTriangular", DistTriangular.class);

    }


    // ************************* Instance Fields **************************************

    private double min;
    private double mode;
    private double max;


    // ************************** Constructors ****************************************

    /**
     * Create a triangular distribution instance.
     * 
     * @since 0.2
     * 
     * @param min The minimum parameter.
     * @param mode The mode parameter.
     * @param max The maximum parameter.
     */
    public DistTriangular(double min, double mode, double max) {
        
        super();
        this.min = min;
        this.mode = mode;
        this.max = max;
        checkParms();
        
    }


    // ************************** Public Methods **************************************
    
    /**
     * Change (set) the min parameter of the distribution.
     * 
     * @param min The new min parameter (minimum outcome).
     */
    public void setMin(double min) {

        this.min = min;
        checkParms();

    }

    /**
     * Get the distribution's min parameter (minimum outcome).
     * 
     * @return The min parameter.
     */
    public double getMin() {

        return min;

    }

    /**
     * Change (set) the mode parameter of the distribution.
     * 
     * @param mode The new mode parameter (modal outcome).
     */
    public void setMode(double mode) {

        this.mode = mode;
        checkParms();

    }

    /**
     * Get the distribution's mode parameter (modal outcome).
     * 
     * @return The mode parameter.
     */
    public double getMode() {

        return mode;

    }
    
    /**
     * Change (set) the max parameter of the distribution.
     * 
     * @param max The new max parameter (maximum outcome).
     */
    public void setMax(double max) {

        this.max = max;
        checkParms();

    }

    /**
     * Get the distribution's max parameter (maximum outcome).
     * 
     * @return The max parameter.
     */
    public double getMax() {

        return max;

    }
    
    /**
     * Custom toString implementation.
     * 
     * @since 0.2
     * 
     * @return A string representation of the distribution;
     * e.g., <code>"Triangular(3.0, 4.0, 5.0)"</code>.
     */
    @Override
    public String toString() {

        return "Triangular(" + min + ", " + mode + ", " + max + ")";

    }

    /**
     * Create an unregistered copy of this distribution.
     * @since 0.2
     */
    @Override
    public Distribution createUnregisteredCopy() {
        
        return new DistTriangular(min, mode, max);
        
    }


    // ******************* Protected/Package-Access Methods *************************

    @Override
    protected double sampleDoubleByMode() {

        Sampler.SampleMode mode = getSampleMode();
        switch(getSampleMode()) {
            case NORMAL:
                return getSampler().sampleTriangular(min, this.mode, max);
            case COLLAPSE_MID:
                return (int) Math.round((min + this.mode + max) / 3.0d);
            default:
                throw new InternalError("DistPoisson has omitted support for sample mode " + mode);
        }

    }

    
    // ************************* Private Instance Methods ******************************
    
    private void checkParms() {
        
        if (max <= min || min > mode || mode > max) {
            throw new IllegalArgumentException("Parameters (" + min + ", " + mode + ", " + max
                                               + " are not consistent");
        }
        
    }
    
}
