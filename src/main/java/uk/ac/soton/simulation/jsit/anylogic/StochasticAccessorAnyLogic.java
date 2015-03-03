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

import uk.ac.soton.simulation.jsit.core.AbstractStochasticAccessInfo;
import uk.ac.soton.simulation.jsit.core.BasicStochasticAccessInfo;
import uk.ac.soton.simulation.jsit.core.AbstractStochasticItem;
import uk.ac.soton.simulation.jsit.core.Sampler;
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
public class StochasticAccessorAnyLogic<S extends AbstractStochasticItem>
                extends AbstractStochasticAccessInfo
                implements Serializable {

    
    // ************************* Static Fields *****************************************
    
    private static final long serialVersionUID = 1L;
    //private static final Logger logger = LoggerFactory.getLogger(
    //                                      StochasticAccessorAnyLogic.class);

    
    // ************************* Static Methods ****************************************
    
    /**
     * Register a stochastic item for use <i>without</i> an accessor for access.
     * This should only be done when using the item with singleton model classes
     * (where it does not have to be shared between instances), and holding it as
     * an instance field (i.e., not statically).
     * 
     * @since 0.2
     * 
     * @param owner
     *            The class owning (using) the stochastic item.
     * @param mainModel
     *            The closest enclosing MainModel_AnyLogic instance, which is
     *            used as a 'marker' object for this run.
     * @param id
     *            A user-provided ID to define this stochastic item (used in
     *            stochastic control configuration files together with the
     *            owning class name).
     * @param stochItem
     *            The stochastic item to register.
     */
    public static void registerAccessorFreeStochItem(Class<?> owner,
                                                     String id,
                                                     MainModel_AnyLogic mainModel,
                                                     AbstractStochasticItem stochItem) {
        
        BasicStochasticAccessInfo accessInfo = new BasicStochasticAccessInfo(owner, id);
        stochItem.registerAccessInfo(accessInfo);
        Sampler sampler = mainModel.getModelInitialiser().registerStochItem(stochItem);
        stochItem.registerSampler(sampler);                // Complete registration of stoch item       
        
    }

    
    // ************************* Instance Fields ***************************************
    
    // 'Optimise' for non-parallel experiments
    private final Hashtable<MainModel_AnyLogic, S> itemsPerRun
                            = new Hashtable<MainModel_AnyLogic, S>(1);        

    
    // ************************** Constructors *****************************************
    
    /**
     * Create an 'empty' accessor, normally held statically to access the per-run
     * stochastic item across all instances of the class (in a way that works around
     * AnyLogic threading issues).
     * 
     * @since 0.2
     * 
     * @param owner
     *            The class owning (using) the stochastic item.
     * @param id
     *            A user-provided ID to define this stochastic item (used in
     *            stochastic control configuration files together with the
     *            owning class name).
     */
    public StochasticAccessorAnyLogic(Class<?> owner, String id) {
        
        super(owner, id);
        
    }

    
    // *********************** Public Instance Methods *********************************
    
    /**
     * Thread-safe method to add a stochastic item for the current run. This
     * should only ever be called once per run.
     * 
     * @since 0.2
     * 
     * @param mainModel
     *            The closest enclosing MainModel_AnyLogic instance, which is
     *            used as a 'marker' object for this run.
     * @param stochItem
     *            The stochastic item being added.
     */
    public void addForRun(MainModel_AnyLogic mainModel, S stochItem) {
        
        // Thread-safe via use of MDC and Hashtable

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
        
        stochItem.registerAccessInfo(this);    // Needed before registering with initialiser
        Sampler sampler = mainModel.getModelInitialiser().registerStochItem(stochItem);
        stochItem.registerSampler(sampler);    // Complete registration of stoch item
        itemsPerRun.put(mainModel, stochItem);
        
    }

    /**
     * Thread-safe method to get the stochastic item for the current run.
     * 
     * @since 0.2
     * 
     * @param mainModel
     *            The closest enclosing MainModel_AnyLogic instance, which is
     *            used as a 'marker' object for this run.
     * @return The stochastic item for the run.
     */
    public S getForRun(MainModel_AnyLogic mainModel) {

        // Thread-safe via use of MDC and Hashtable
        
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
     * Exposed for technical reasons; not for JSIT user use.
     */
    @Override
    public void removeMe(AbstractStochasticItem stochItem) {
        
        // Done at end of a run. Thread-safe via use of Hashtable. Can't use S
        // as parameter type: causes problems since calling code doesn't always know the
        // concrete subclass

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
