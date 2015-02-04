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

import uk.ac.soton.simulation.jsit.core.AbstractStochasticAccessor;
import uk.ac.soton.simulation.jsit.core.StochasticItem;
import uk.ac.soton.simulation.jsit.core.Sampler;
import uk.ac.soton.simulation.jsit.core.ModelInitialiser;
import uk.ac.soton.simulation.jsit.core.ExceptionUtils;

import java.io.Serializable;
import java.util.*;

/**
 * Stochastic item accessor specifically for AnyLogic. This allows for 
 * the vagaries of AnyLogic model threading, where a model run may
 * switch between multiple unrelated (parent-child or sibling) threads.
 * 
 * @author Stuart Rossiter
 * @since 0.2
 */	
public class StochasticAccessorAnyLogic<S extends StochasticItem>
                extends AbstractStochasticAccessor<S>
                implements Serializable {

    private static final long serialVersionUID = 1L;
    //private static final Logger logger = LoggerFactory.getLogger(
    //                                      StochasticAccessorAnyLogic.class);

    // 'Optimise' for non-parallel experiments
    private final Hashtable<MainModel_AnyLogic, S> itemsPerRun
                            = new Hashtable<MainModel_AnyLogic, S>(1);		
   
	
    public StochasticAccessorAnyLogic(Class<?> owner, String id) {
    	
        super(owner, id);
    	
    }
    
    /*
     * Thread-safe via use of MDC and Hashtable
     */
    public void addForRun(MainModel_AnyLogic mainModel, S stochItem) {
    	
        if (mainModel == null || stochItem == null) {
            throw new IllegalArgumentException(
              "Must add stochastic item with non-null main model and item");
        }
    	if (itemsPerRun.containsKey(mainModel)) {
    		throw new IllegalArgumentException(
    		        "Stochastic item already added to " + getFullID()
    			+ " accessor for run ID "
    			+ mainModel.getModelInitialiser().getRunID());
    	}
    	
    	stochItem.registerAccessor(this);	// Needed before registering with initialiser
    	Sampler sampler = ModelInitialiser.getInitialiserForRunViaMDC().registerStochItem(stochItem);
    	stochItem.registerSampler(sampler);	// Complete registration of stoch item
    	itemsPerRun.put(mainModel, stochItem);
    	
    }

    /*
     * Thread-safe via use of MDC and Hashtable
     */
    public S getForRun(MainModel_AnyLogic mainModel) {

        if (mainModel == null) {
            throw new IllegalArgumentException(
                    "Need non-null main model to get stochastic item");
        }
        S stochItem = itemsPerRun.get(mainModel);
        if (stochItem == null) {
            ExceptionUtils.throwWithThreadData(new IllegalStateException(
                    "Must add distribution for run "
                    + mainModel.getModelInitialiser().getRunID()
                    + " before retrieving it"));
        }
        return stochItem;

    }
	
    /**
     * Done at end of a run. Thread-safe via use of Hashtable. Can't use S
     * as parameter type: causes problems since calling code doesn't always know the
     * concrete subclass
     */
    @Override
    public void removeMe(StochasticItem stochItem) {

        if (stochItem == null) {
            throw new IllegalArgumentException("Need non-null stoch item to remove");
        }
        boolean foundVal = false;
        Set<MainModel_AnyLogic> keys = itemsPerRun.keySet();
        for (MainModel_AnyLogic m : keys) {
            if (stochItem == itemsPerRun.get(m)) {
                foundVal = true;
                keys.remove(m);     // Safe via the key set
                break;
            }
        }
        
        if (!foundVal) {
            ExceptionUtils.throwWithThreadData(
                    new IllegalArgumentException(
                            "Can't find stochastic item for removal"));
        }

    }
	
}
