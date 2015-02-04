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

/**
 * Interface for enums to indicate that they are 'mapped' to an AnyLogic Dimension
 * and can be converted to a Dimension index (using AnyLogic Dimension terminology).
 * 
 * @author Stuart Rossiter
 * @since 0.1
 */
public interface DimConvertable {

    public static enum DimType { ENUM, INT_RANGE_VALUE };

    DimType getDimType();
    int asAnyLogicDimIndex();

}
