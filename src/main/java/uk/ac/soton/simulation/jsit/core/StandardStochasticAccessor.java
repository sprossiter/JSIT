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
import java.util.*;

import org.slf4j.*;

/**
 * 'Default' stochastic accessor using MDC keys---as Logback does---
 * to track stochastic items per run.
 * 
 * @author Stuart Rossiter
 * @since 0.2
 */    
public class StandardStochasticAccessor<S extends AbstractStochasticItem>
                    extends AbstractStochasticAccessInfo
                    implements Serializable {

    
    // ************************* Static Fields *****************************************
    
    private static final long serialVersionUID = 1L;

    
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
     * @param id
     *            A user-provided ID to define this stochastic item (used in
     *            stochastic control configuration files together with the
     *            owning class name).
     * @param stochItem
     *            The stochastic item to register.
     */
    public static void registerAccessorFreeStochItem(Class<?> owner,
                                                     String id,
                                                     AbstractStochasticItem stochItem) {
        
        BasicStochasticAccessInfo accessInfo = new BasicStochasticAccessInfo(owner, id);
        stochItem.registerAccessInfo(accessInfo);
        Sampler sampler = ModelInitialiser.getInitialiserForRunViaMDC().registerStochItem(stochItem);
        stochItem.registerSampler(sampler);                // Complete registration of stoch item       
        
    }
    
    // ************************* Instance Fields ***************************************
    
    private final Hashtable<String, S> itemsPerRun
            = new Hashtable<String, S>(1);  // 'Optimise' for non-parallel experiments

 
    // ************************** Constructors *****************************************
    
    /**
     * Create an 'empty' accessor, normally held statically to access the per-run
     * stochastic item across all instances of the class.
     * 
     * @since 0.1
     * 
     * @param owner
     *            The class owning (using) the stochastic item.
     * @param id
     *            A user-provided ID to define this stochastic item (used in
     *            stochastic control configuration files together with the
     *            owning class name).
     */
    public StandardStochasticAccessor(Class<?> owner, String id) {

        super(owner, id);

    }
    
    
    // *********************** Public Instance Methods *********************************
    
    /*
     * Thread-safe via use of MDC and Hashtable
     */
    public void addForRun(S stochItem) {

        String runID = MDC.get(ModelInitialiser.RUN_ID_KEY);
        if (runID == null) {
            ExceptionUtils.throwWithThreadData(new AssertionError("Null run ID"));
        }
        if (itemsPerRun.containsKey(runID)) {
            throw new IllegalArgumentException("Stochastic item already added to " + getFullID()
                    + " accessor for run ID " + runID);
        }

        stochItem.registerAccessInfo(this);                // Needed before registering with initialiser
        Sampler sampler = ModelInitialiser.getInitialiserForRunViaMDC().registerStochItem(stochItem);
        stochItem.registerSampler(sampler);                // Complete registration of stoch item
        itemsPerRun.put(runID, stochItem);

    }

    /*
     * Thread-safe via use of MDC and Hashtable
     */
    public S getForRun() {

        String runID = MDC.get(ModelInitialiser.RUN_ID_KEY);
        if (runID == null) {
            ExceptionUtils.throwWithThreadData(new AssertionError("Null run ID"));
        }
        S stochItem = itemsPerRun.get(runID);
        if (stochItem == null) {
            ExceptionUtils.throwWithThreadData(new IllegalStateException(
                    "Must add distribution for run before retrieving it"));
        }
        return stochItem;

    }

    /**
     * Exposed for technical reasons; not for JSIT user use.    
     */
    @Override
    public void removeMe(AbstractStochasticItem stochItem) {
        
        // Done at end of a run. Thread-safe via use of MDC and Hashtable. Can't use S
        // as parameter type: causes problems since calling code doesn't always know the
        // concrete subclass

        String runID = MDC.get(ModelInitialiser.RUN_ID_KEY);
        assert runID != null;
        S removedItem = itemsPerRun.remove(runID);
        assert removedItem != null;
        assert removedItem == stochItem;

    }

}
