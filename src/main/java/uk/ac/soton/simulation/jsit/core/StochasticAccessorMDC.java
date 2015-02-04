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
public class StochasticAccessorMDC<S extends StochasticItem>
                    extends AbstractStochasticAccessor<S>
                    implements Serializable {

    private static final long serialVersionUID = 1L;
    //private static final Logger logger = LoggerFactory.getLogger(
    //                                          StochasticAccessor.class);

    private final Hashtable<String, S> itemsPerRun
            = new Hashtable<String, S>(1);  // 'Optimise' for non-parallel experiments


    public StochasticAccessorMDC(Class<?> owner, String id) {

        super(owner, id);

    }

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

        stochItem.registerAccessor(this);				// Needed before registering with initialiser
        Sampler sampler = ModelInitialiser.getInitialiserForRunViaMDC().registerStochItem(stochItem);
        stochItem.registerSampler(sampler);				// Complete registration of stoch item
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
     * Done at end of a run. Thread-safe via use of MDC and Hashtable. Can't use S
     * as parameter type: causes problems since calling code doesn't always know the
     * concrete subclass
     */
    @Override
    public void removeMe(StochasticItem stochItem) {

        String runID = MDC.get(ModelInitialiser.RUN_ID_KEY);
        assert runID != null;
        S removedItem = itemsPerRun.remove(runID);
        assert removedItem != null;
        assert removedItem == stochItem;

    }

}
