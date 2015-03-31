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
 * JSIT representation of a 'dummy' continuous distribution to return a fixed value.
 * (A DistUniformDiscrete(0,0) can be used for the discrete equivalent.)
 * 
 * @author Stuart Rossiter
 * @since 0.1
 */    
public class DistFixedContinuous extends DistributionContinuous
                                 implements Serializable {

    // ************************** Static Fields ***************************************

    //private static final Logger logger = LoggerFactory.getLogger(
    //                DistFixedContinuous.class);

    private static final long serialVersionUID = 1L;


    // ************************** Static Methods **************************************

    static void setupForInfoSerialisation(XStream xstream) {

        xstream.alias("distFixedContinuous", DistFixedContinuous.class);

    }


    // ************************* Instance Fields **************************************

    double fixedVal;


    // ************************** Constructors ****************************************

    public DistFixedContinuous(double fixedVal) {
        super();
        this.fixedVal = fixedVal;
    }


    // ************************** Public Methods **************************************

    @Override
    public String toString() {

        return "FixedContinuous(" + fixedVal + ")";

    }

    /**
     * Create an unregistered copy of this distribution.
     * @since 0.2
     */
    @Override
    public Distribution createUnregisteredCopy() {
        
        return new DistFixedContinuous(fixedVal);
        
    }


    // ******************* Protected/Package-Access Methods *************************

    @Override
    protected double sampleDoubleByMode() {

        return fixedVal;

    }

}
