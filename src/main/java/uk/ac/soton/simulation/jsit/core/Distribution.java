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

/**
 * JSIT abstract superclass of all probability distributions.
 * 
 * @author Stuart Rossiter
 * @since 0.1
 */    
public abstract class Distribution extends StochasticItem
                                   implements Serializable {

    // ************************ Static Fields *****************************************

    private static final long serialVersionUID = 1L;


    // ************************ Constructors ********************************************

    protected Distribution() {

        super();

    }


    // ************************* Public Methods *************************************

    /*
     * Force concrete subclasses to override toString()
     */
    @Override
    public abstract String toString();

}
