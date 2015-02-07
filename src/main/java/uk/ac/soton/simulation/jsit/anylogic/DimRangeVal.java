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

import com.anylogic.engine.Dimension;

/**
 * Represents an AnyLogic Dimension value.
 * 
 * @author Stuart Rossiter
 * @since 0.1
 */    
public class DimRangeVal implements DimConvertable, Serializable {

    // ************************ Static Fields *****************************************

    private static final long serialVersionUID = 1L;


    // ************************ Instance Fields ***************************************

    private int intValue;


    // ************************* Constructors *****************************************

    public DimRangeVal(Dimension mappedDim, int intValue) {

        checkValue(mappedDim, intValue);
        this.intValue = intValue;

    }

    // ******************* Public Instance Methods ************************************

    // DimConvertable interface methods

    @Override
    public DimType getDimType() {

        return DimType.INT_RANGE_VALUE;

    }

    @Override
    public int asAnyLogicDimIndex() {

        return intValue;

    }

    private void checkValue(Dimension mappedDim, int intValue) {

        // Suppress this check due to bug in AnyLogic where Dimension is not thread-safe
        //if (mappedDim.getIndexPositionByName(Integer.toString(intValue)) == -1) {
        //    throw new IllegalArgumentException("Value " + intValue + " is not in the Dimension's range");
        //}

    }

}
