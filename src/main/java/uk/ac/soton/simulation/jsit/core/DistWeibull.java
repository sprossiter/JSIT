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
import org.apache.commons.math3.special.Gamma;

//import org.slf4j.*;

/**
 * JSIT representation of a Weibull distribution.
 * 
 * @author Stuart Rossiter
 * @since 0.2
 */    
public class DistWeibull extends DistributionContinuous implements Serializable {

    // ************************** Static Fields ***************************************

    //private static final Logger logger = LoggerFactory.getLogger(
    //                DistWeibull.class);

    private static final long serialVersionUID = 1L;


    // ************************** Static Methods **************************************

    static void setupForInfoSerialisation(XStream xstream) {

        xstream.alias("distWeibull", DistWeibull.class);

    }


    // ************************* Instance Fields **************************************

    private double shape;
    private double scale;
    
    private Double mean = null;		// Expensive-calculation mean lazy instantiated if needed


    // ************************** Constructors ****************************************

    public DistWeibull(double shape, double scale) {
        super();
        setShape(shape);
        setScale(scale);
    }


    // ************************** Public Methods **************************************

    public void setShape(double shape) {

        this.shape = shape;

    }

    public double getShape() {

        return shape;

    }

    public void setScale(double scale) {

        this.scale = scale;

    }

    public double getScale() {

        return scale;

    }

    @Override
    public String toString() {

        return "Weibull(" + shape + "," + scale + ")";

    }

    /**
     * Create an unregistered copy of this distribution.
     * @since 0.2
     */
    @Override
    public Distribution createUnregisteredCopy() {
        
        return new DistWeibull(shape, scale);
        
    }


    // ******************* Protected/Package-Access Methods *************************

    @Override
    protected double sampleDoubleByMode() {

        Double sample = null;
        Sampler.SampleMode mode = getAccessInfo().getSampleMode();

        if (mode == Sampler.SampleMode.NORMAL) {
            sample = getSampler().sampleWeibull(shape, scale);
        }
        else if (mode == Sampler.SampleMode.COLLAPSE_MID) {
        	// Lazy instantiation of mean if needed (expensive gamma function calculation)
            if (mean == null) {
            	mean = scale * Gamma.gamma(1.0d + 1.0d / shape);
            }
        	sample = mean;
        }

        return sample;

    }


}
