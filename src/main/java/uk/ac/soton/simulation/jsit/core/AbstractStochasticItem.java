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
 * Generic abstract class for all JSIT stochastic items.
 * 
 * We choose to store a reference to the (framework-specific) sampler per stochastic
 * item, rather than incurring the lookup overhead of querying it each time.
 * 
 * @author Stuart Rossiter
 * @since 0.1
 */    
public abstract class AbstractStochasticItem implements Serializable {

    // ************************ Static Fields *****************************************

    private static final long serialVersionUID = 1L;


    // ************************ Instance Fields ***************************************

    private AbstractStochasticAccessInfo accessInfo = null;
    private Sampler sampler = null;            // Framework-specific sampler


    // ************************** Constructors ****************************************

    protected AbstractStochasticItem() {

        // Nothing to do

    }


    // ********************* Public Instance Methods **********************************

    /**
     * Internal method exposed for technical reasons. Called only by accessor
     * when adding the item to itself.
     * 
     * @param accessor
     * The accessor that is being registered.
     */
    public void registerAccessInfo(AbstractStochasticAccessInfo accessor) {

        this.accessInfo = accessor;

    }

    /*
     * Called only by accessor when adding the item to itself
     */
    public void registerSampler(Sampler sampler) {

        this.sampler = sampler;

    }
    
    /*
     * Create an unregistered copy of this stochastic item.
     * TODO: Implement this (instead of only for Distribution subclasses), though
     * considerable work to duplicate a MultiDimEnumMap
     * 
     * @return The unregistered copy. (This will need casting as appropriate.)
     */
    //public abstract AbstractStochasticItem createUnregisteredCopy();


    
    // ************ Protected / Package-Access Instance Methods ***********************

    /*
     * Called only model initialiser when cleaning up at end of run
     */
    void deregisterItem() {

        getAccessInfo().removeMe(this);

    }
    
    protected AbstractStochasticAccessInfo getAccessInfo() {

        if (accessInfo == null) {
            throw new IllegalStateException("Stochastic item not added to (registered via) an accessor");
        }
        return accessInfo;

    }

    protected Sampler.SampleMode getSampleMode() {

        return getAccessInfo().getSampleMode();

    }

    protected Sampler getSampler() {

        if (sampler == null) {
            throw new IllegalStateException("Stochastic item not added to (registered via) an accessor");
        }
        return sampler;

    }


}
