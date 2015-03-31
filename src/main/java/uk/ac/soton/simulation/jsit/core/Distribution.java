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
public abstract class Distribution extends AbstractStochasticItem
                                   implements Serializable {

    // ************************ Static Fields *****************************************

    private static final long serialVersionUID = 1L;


    // ************************ Constructors ********************************************

    protected Distribution() {

        super();

    }


    // ************************* Public Methods *************************************

    /**
     * Force concrete subclasses to override toString().
     */
    @Override
    public abstract String toString();
    
    /**
     * Create an unregistered copy of this distribution, which will then need
     * registering separately to be usable (and will be subject to any settings for
     * the owner and ID it is registered with).
     * 
     * @return The unregistered copy. (This will need casting as appropriate.)
     */
    public abstract Distribution createUnregisteredCopy();
    
    /**
     * Create a registered copy of this distribution; i.e., one which will be
     * subject to the same settings as the one it was copied from, and which has
     * no separate registered ID.
     * 
     * @return The registered copy. (This will need casting as appropriate.)
     */
    public Distribution createRegisteredCopy() {
        
        Distribution item = (Distribution) createUnregisteredCopy();
        item.registerSampler(getSampler());
        // Can copy the access info ref (which may be an accessor) OK since only
        // the original Distribution is actually held by the accessor (and listed in
        // the model initialiser) and thus only that is worked with at model destroy
        // time
        item.registerAccessInfo(getAccessInfo());
        return item;
        
    }

}
