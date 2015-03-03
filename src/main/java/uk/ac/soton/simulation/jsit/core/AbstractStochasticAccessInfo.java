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
 * Abstract accessor for stochastic items. Concrete subclasses will include
 * methods to add, get and remove per-run stochastic items. (Variants are needed
 * because the default method---via MDC keys similarly to Logback---is
 * problematic for simulation toolkits like AnyLogic which don't keep runs
 * within the same thread (or sibling threads).
 * 
 * @author Stuart Rossiter
 * @since 0.2
 */
public abstract class AbstractStochasticAccessInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final Class<?> owner;
    private Sampler.SampleMode mode = null;
    
    public AbstractStochasticAccessInfo(Class<?> owner, String id) {
        
        this.owner = owner;
        this.id = id;
        
    }
    
    public String getFullID() {
        
        return owner.getSimpleName() + "." + id;
        
    }
    
    public String getOwnerName() {
        
        return owner.getSimpleName();
        
    }
    
    public abstract void removeMe(AbstractStochasticItem stochItem);

    /*
     * Will only be called by the model initialiser when checking the stochastic overrides. Needs
     * synchronization
     */
    synchronized void setSampleMode(Sampler.SampleMode mode) {

        this.mode = mode;

    }

    /*
     * Get the sample mode. Needs synchronization
     */
    synchronized Sampler.SampleMode getSampleMode() {

        if (mode == null) {            // Main 'route' by which non-registered stoch item gets caught
            throw new IllegalStateException("Stochastic item " + getFullID() + " not registered via an accessor");
        }
        return mode;

    }
    
}
