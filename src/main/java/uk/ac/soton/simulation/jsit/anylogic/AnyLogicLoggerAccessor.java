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

/**
 * Logger accessor specifically for AnyLogic. This allows for 
 * the use of AnyLogicLogger Loggers (which work around AnyLogic
 * threading issues), where one is needed per class <b>and per
 * model instance</b> (unlike normal Loggers where one per class
 * is neeed).
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
     * @param owner
     *            The class which owns (is using) the logger.
     * @param mainModel
     *            The closest enclosing MainModel_AnyLogic instance, which is
     *            used as a 'marker' object for this run.
     * @return The appropriate AnyLogicLogger for this class (which wraps the
     *         appropriate Logback Logger and provides the same interface, plus
     *         the ensureExternalLoggingForBlock method.
     */
    public static AnyLogicLogger getAccessorFreeAnyLogicLogger(Class<?> owner,
                                                               MainModel_AnyLogic mainModel) {
        
        return mainModel.getPerRunAnyLogicLogger(owner);
        
    }
    

    // ************************* Instance Fields ***************************************
    
    // 'Optimise' for non-parallel experiments
    private final Hashtable<MainModel_AnyLogic, AnyLogicLogger> itemsPerRun
                            = new Hashtable<MainModel_AnyLogic, AnyLogicLogger>(1);
    
    private final Class<?> owner;
    

    // ************************** Constructors *****************************************
    
    /**
     * Create an 'empty' accessor.
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
     * @param mainModel The closest enclosing MainModel_AnyLogic instance which
     * is used as a 'marker' object for this run.
     */
    public void addLoggerForRun(MainModel_AnyLogic mainModel) {
        
        // Thread-safe via use of MDC and Hashtable
        
        if (mainModel == null) {
            throw new IllegalArgumentException(
              "Must add AnyLogic logger with non-null main model");
        }
        if (itemsPerRun.containsKey(mainModel)) {
            throw new IllegalArgumentException(
                "Logger already added to " + owner.getName()
                + " accessor for run ID "
                + mainModel.getModelInitialiser().getRunID());
        }
        
        itemsPerRun.put(mainModel, mainModel.registerLoggerAccessor(this));
        
    }

    /**
     * Thread-safe method to retrieve the logger for the current run. This will
     * fail if one was not added.
     * 
     * @param mainModel The closest enclosing MainModel_AnyLogic instance which
     * is used as a 'marker' object for this run.
     * @return The appropriate Logger instance.
     */
    public Logger getLoggerForRun(MainModel_AnyLogic mainModel) {

        // Thread-safe via use of MDC and Hashtable
        
        if (mainModel == null) {
            throw new IllegalArgumentException(
                    "Need non-null main model to get per-run logger");
        }
        AnyLogicLogger anyLogicLogger = itemsPerRun.get(mainModel);
        if (anyLogicLogger == null) {
            ExceptionUtils.throwWithThreadData(new IllegalStateException(
                    "Must add logger for run "
                    + mainModel.getModelInitialiser().getRunID()
                    + " before retrieving it"));
        }
        return anyLogicLogger;

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
    
}
