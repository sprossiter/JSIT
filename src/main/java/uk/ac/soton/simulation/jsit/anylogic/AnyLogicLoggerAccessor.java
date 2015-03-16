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

import uk.ac.soton.simulation.jsit.core.ExceptionUtils;

import java.io.Serializable;
import java.util.*;

import org.slf4j.Logger;

import com.anylogic.engine.Agent;

/**
 * Logger accessor specifically for AnyLogic. This allows for 
 * the use of AnyLogicLogger Loggers (which work around AnyLogic
 * threading issues), where the logger is shared between instances of some class
 * (e.g., an AnyLogic Agent) <i>that are for the same model run</i>. (When using
 * parallel runs of multi-run experiments, you will have instances of model
 * classes for <i>all</i> parallel runs 'mixed together' in a single JVM.)
 * 
 * @author Stuart Rossiter
 * @since 0.2
 */    
public class AnyLogicLoggerAccessor implements Serializable {
    

    // ************************** Static Fields ****************************************
    
    private static final long serialVersionUID = 1L;
    

    // ************************* Static Methods ****************************************
    
    /**
     * Get an AnyLogicLogger without an associated AnyLogicLoggerAccessor. This
     * should <i>only</i> be used when you have singleton model classes (and
     * thus want to store the logger in the instance, not statically, and thus
     * don't need an accessor) or when you are <i>sure</i> that your model will
     * never be run in a multi-run AnyLogic experiment with parallel instances.
     * It is always safer to use a (static) AnyLogicLoggerAccessor instance
     * instead.
     * 
     * @since 0.2
     * 
     * @param owner
     *            The class which owns (is using) the logger.
     *            
     * @param agentInModel Any Agent instance that is part of the model. This
     * method will actually search for the closest 'parent' MainModel_AnyLogic
     * instance, so passing that (if available directly) will speed up the call.
     * 
     * @return The appropriate AnyLogicLogger for this class (which wraps the
     *         appropriate Logback Logger and provides the same interface, plus
     *         the ensureExternalLoggingForBlock method.
     */
    public static AnyLogicLogger getAccessorFreeAnyLogicLogger(Class<?> owner,
                                                               Agent agentInModel) {
        
        return MainModel_AnyLogic.getMainFor(agentInModel).getPerRunAnyLogicLogger(owner);
        
    }
    

    // ************************* Instance Fields ***************************************
    
    // We store the logger per run (keyed by the run ID) in a thread-safe Hashtable.
    // 'Optimise' for non-parallel experiments (where only 1 entry). Keying by run ID
    // (accessible from the ModelInitialiser shared by every MainModel_AnyLogic instance),
    // rather than MainModel_AnyLogic instance, because different model Agents can have
    // different 'parent' instances of the latter (e.g., for an Agent that is part of the
    // domain model vs. one used for visualisation) and we want to ensure that users can
    // use getLoggerForRun passing *any* Agent in the model
    private final Hashtable<String, AnyLogicLogger> itemsPerRun
                            = new Hashtable<String, AnyLogicLogger>(1);
    
    private final Class<?> owner;
    

    // ************************** Constructors *****************************************
    
    /**
     * Create an 'empty' accessor.
     * 
     * @since 0.2
     * 
     * @param owner The class which is owning (using) this accessor.
     */
    public AnyLogicLoggerAccessor(Class<?> owner) {
        
        this.owner = owner;
        
    }


    // *********************** Public Instance Methods *********************************
    

    
    /**
     * Thread-safe method to add a logger for the current run. This should only
     * ever be called once per run.
     * 
     * @since 0.2
     * 
     * @param agentInModel Any Agent instance that is part of the model. This
     * method will actually search for the closest 'parent' MainModel_AnyLogic
     * instance, so passing that (if available directly) will speed up the call.
     */
    public void addLoggerForRun(Agent agentInModel) {
        
        addLoggerForRun(MainModel_AnyLogic.getMainFor(agentInModel));
        
    }

    
    /**
     * Thread-safe method to retrieve the logger for the current run. This will
     * fail if one was not added.
     * 
     * @since 0.2
     * 
     * @param agentInModel Any Agent instance that is part of the model. This
     * method will actually search for the closest 'parent' MainModel_AnyLogic
     * instance, so passing that (if available directly) will speed up the call.
     */
    public Logger getLoggerForRun(Agent agentInModel) {
        
        return getLoggerForRun(MainModel_AnyLogic.getMainFor(agentInModel));
        
    }

    
    // ************ Protected / Package-Access Instance Methods ***************
    
    Class<?> getOwner() {
        
        return owner;
        
    }
    
    /*
     * Called at end of a run (automatically by JSIT so not public).
     */
    void removeForRun(MainModel_AnyLogic mainModel) {

        assert mainModel != null;
        Logger removedLogger = itemsPerRun.remove(mainModel);
        assert removedLogger != null;

    }
    
    
    // ******************** Private Instance Methods **************************
    
    /*
     * Add logger keyed by the runID
     */
    private void addLoggerForRun(MainModel_AnyLogic mainModel) {
        
        // Thread-safe via use of MDC and Hashtable
        
        if (mainModel == null) {
            throw new IllegalArgumentException(
              "Must add AnyLogic logger with non-null main model");
        }
        String runID = mainModel.getModelInitialiser().getRunID();
        if (itemsPerRun.containsKey(runID)) {
            throw new IllegalArgumentException(
                "Logger already added to " + owner.getName()
                + " accessor for run ID " + runID);
        }
        
        itemsPerRun.put(runID, mainModel.registerLoggerAccessor(this));
        
    }
    

    /*
     * Get run-ID-keyed logger instance
     */
    private Logger getLoggerForRun(MainModel_AnyLogic mainModel) {

        // Thread-safe via use of MDC and Hashtable
        
        if (mainModel == null) {
            throw new IllegalArgumentException(
                    "Need non-null main model to get per-run logger");
        }
        String runID = mainModel.getModelInitialiser().getRunID();
        AnyLogicLogger anyLogicLogger = itemsPerRun.get(runID);
        if (anyLogicLogger == null) {
            ExceptionUtils.throwWithThreadData(new IllegalStateException(
                    "Must add logger for run " + runID + " before retrieving it"));
        }
        return anyLogicLogger;

    }
    
}
