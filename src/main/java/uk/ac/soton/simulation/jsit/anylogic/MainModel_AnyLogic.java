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

import org.slf4j.*;

import com.anylogic.engine.Agent;
import com.anylogic.engine.AgentList;
import com.anylogic.engine.Engine;
import com.anylogic.engine.Engine.State;

import java.text.DecimalFormat;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.math.BigDecimal;
import java.math.RoundingMode;

import uk.ac.soton.simulation.jsit.core.MainModel;
import uk.ac.soton.simulation.jsit.core.ModelInitialiser;

/**
 * Abstract superclass for all Agents representing the 'root' Agent for a runnable JSIT
 * AnyLogic model.
 * 
 * @author Stuart Rossiter
 * @since 0.1
 */
public abstract class MainModel_AnyLogic extends Agent implements MainModel {

    // ************************ Static Fields *****************************************

    /**
     * Enumeration of precision alternatives for sim time-of-day values.
     * 
     * @since 0.1
     */
    public static enum TimeOfDayPrecision {
        /** Minutes */
        MINS,
        /** Seconds */
        SECS,
        /** Milliseconds */
        MILLISECS
    };

    private static final long serialVersionUID = 1L;

    private static final Logger logger
                = LoggerFactory.getLogger(MainModel_AnyLogic.class);
    
    
    // ************************ Static Methods ***************************************
    
    /**
     * Get the (closest composing) MainModel_AnyLogic 'main' model
     * instance for the Agent in question. Useful for JSIT users to get access
     * to the MainModel_AnyLogic instance (and, from there, the model initialiser).
     * 
     * @since 0.2
     * 
     * @param agent
     * The Agent in question.
     * @return
     * The closest composing MainModel_AnyLogic instance (which could be itself).
     */
    public static MainModel_AnyLogic getMainFor(Agent agent) {
        
        Agent currAgent = agent;
        while (!(currAgent instanceof MainModel_AnyLogic)) {
            currAgent = currAgent.getOwner();
            if (currAgent == null) {
                throw new IllegalArgumentException("Given Agent of class "
                            + agent.getClass().getSimpleName()
                            + " has no containing MainModel_AnyLogic");
            } 
        }
              
        return (MainModel_AnyLogic) currAgent;
        
    }


    // ************************* Instance Fields ***************************************
    
    // The last runtime thread for this run known to be loggable-to (i.e., having had
    // MDC keys set up). Accessed by AnyLogicLogger
    
    Thread lastLoggableThread = null;


    // JSIT model initialiser which will run (and initialise the environment) as
    // part of its instantiation in this common superclass' constructor

    private final ModelInitialiser jsitInitialiser;
    private final DecimalFormat timeFormatter = new DecimalFormat("00");

    // Member class AnyLogic dynamic event (scheduled first at t=0) to ensure MDC
    // keys are set correctly at non-init model start (when the thread may have
    // switched to a non-child thread)

    private class InitialMDC_SetEvent extends com.anylogic.engine.DynamicEvent {     

        private static final long serialVersionUID = 1L;  // Since Serializable

        private InitialMDC_SetEvent() {
            super(MainModel_AnyLogic.this, 0.0d);
        }

        @Override
        public void execute() {
            super.execute();
            getModelInitialiser().possibleMDC_KeysLoss();
        }
    };
    
    
    // Map to associate Logback Loggers for this run (which are typically not
    // 1-1 with classes) with their AnyLogicLogger equivalents.

    private final HashMap<Logger, AnyLogicLogger> anyLogicLoggersMap
                                = new HashMap<Logger, AnyLogicLogger>();

    // List of logger accessors set up for this run (automatically added to via
    // AnyLogicLoggerAccessor's addLoggerForRun method) so that we can cleanly
    // remove them at end-of-run
    
    private final ArrayList<AnyLogicLoggerAccessor> loggerAccessorList
                                = new ArrayList<AnyLogicLoggerAccessor>();



    // ************************ Constructors *******************************************

    /**
     * Constructor with required signature for an Agent (undocumented by AnyLogic). Not
     * intended for user usage.
     * 
     * @since 0.1
     * 
     * @param engine As required by AnyLogic.
     * @param owner As required by AnyLogic.
     * @param collection As required by AnyLogic.
     */
    public MainModel_AnyLogic(Engine engine,
            Agent owner,
            AgentList<? extends MainModel_AnyLogic> collection) {
        
        /*
         * We get the JSIT initialiser for this run (which creates it if we're
         * the initialising MainModel instance) and set up a
         * guaranteed-to-run-first dynamic event to check if the MDC per-run
         * keys need to change. (This is needed to allow for use of Run Until
         * and Run For, because these do *not* trigger the run(), pause() or
         * step() methods of the Experiment and do the non-init processing of
         * the model in a separate (non-child) thread to that which it (and
         * JSIT) was initialised in. This non-init processing only begins when
         * the event schedule starts to be worked through, and so the logic has
         * to be done within a guaranteed-to-be-scheduled-first event.)
         */

        super(engine, owner, collection);

        jsitInitialiser = ModelInitialiser_AnyLogic.getInitialiserForRun(
                getEngine().getExperiment(),
                this);

        // Require FIFO to ensure events scheduled here are run first and, in any
        // case, this is really the most sensible option to avoid alternating
        // invocations of simultaneous events with LIFO

        getEngine().setSimultaneousEventsSelectionMode(Engine.EVENT_SELECTION_FIFO);
        new InitialMDC_SetEvent();            // Schedules itself on instantiation

    }

    /**
     * Simple constructor as now included in all AnyLogic 7 generated Agent code. This
     * is never actually invoked.
     * 
     * @since 0.1
     */
    public MainModel_AnyLogic() {

        throw new IllegalStateException("Not expecting simple constructor to be used!");

    }


    // ********************** Public Instance Methods ******************************

    // Overridden Agent Methods

    /**
     * AnyLogic hook point (prior to startup logic). Generic point at which to save
     * model settings and finalise stochasticity controls since this is after parameters have
     * been given their values, but before normal user-facing startup logic.
     * 
     * @since 0.1
     */
    @Override
    public void onCreate() {

        // Superclass logic first (which will chain down through all embedded Agents)
        super.onCreate();        

        if (jsitInitialiser.isModelInitiator(this)) {        // Only do if we're the initialiser
            logger.debug("JSIT MainModel_AnyLogic onCreate() logic");
            try {
                jsitInitialiser.saveModelSettings();
            }
            catch (IOException e) {
                // Have to convert since no access to visually-designed Agent constructors
                throw new RuntimeException("Can't create model settings file!", e);
            }

            doAllStaticPerRunLoggerSetup();
            doAllStaticStochRegistration();     // Hook for modeller to do any static reg

        }

    }

    /**
     * AnyLogic hook point at model destruction. Clears up the model initialiser.
     * 
     * @since 0.1
     */
    @Override
    public void onDestroy() {

        if (jsitInitialiser.isModelInitiator(this)) {        // Only do if we're the initialiser            
            // For single-run experiments, AnyLogic destroys in a special Model Exec Control
            // Handler thread which will lose MDC keys if the model wasn't paused earlier
            // (since pauses and destruction trigger the creation of this thread). We could
            // check the thread name to only rewrite the keys for single-run experiments, but
            // no harm in doing it anyway and less dependent on AnyLogic internals. This ensures
            // that our non-accessor-wrapped Logger instance is OK for logging from
            jsitInitialiser.possibleMDC_KeysLoss();
            
            // Remove all logger accessor entries for this run
            for (AnyLogicLoggerAccessor a : loggerAccessorList) {
                a.removeForRun(this);
                logger.info("Cleaned up logger for class " + a.getOwner().getSimpleName()
                            + ", run ID " + getModelInitialiser().getRunID());
            }
            // Clean up initialiser. (May already have been done by embedded class or subclass.)
            jsitInitialiser.onMainModelDestroy();
        }

        super.onDestroy();            // As per pattern in AnyLogic-generated code

    }


    // MainModel Interface Methods

    /**
     * Default inputs base path; user should override in their subclass to change this.
     * AnyLogic sets the working directory as the directory of the .alp file that the
     * Experiment is in.
     * 
     * @since 0.1
     */
    @Override
    public String getInputsBasePath() {

        return ".." + File.separator + "Inputs";

    }    

    /**
     * Default outputs base path; user should override in their subclass to change this.
     * AnyLogic sets the working directory as the directory of the .alp file that the
     * Experiment is in.
     * 
     * @since 0.1
     */
    @Override
    public String getOutputsBasePath() {

        return ".." + File.separator + "Outputs";

    }

    /**
     * Default diagnostic log sim time formatting (as raw time to 0 d.p. + day + HH:MM).
     * The modeller should override this method (using the helper methods to compose
     * constituent parts) for their own formatting. This default implementation is as below:
     * <p>
     * <pre>
     * if (modelIsInitialising()) {
     *     return "MODEL INIT";
     * }
     * else {
     *     return getSimRawTime(0) + " " + getSimDay() + " "
     *              + getSimTimeOfDay(TimeOfDayPrecision.MINS);
     * }
     * </pre>
     * 
     * @since 0.1
     */
    @Override
    public String getDiagnosticLogFormattedSimTime() {

        if (modelIsInitialising()) {
            return "MODEL INIT";
        }
        else {
            return getSimRawTime(0) + " " + getSimDay() + " "
                    + getSimTimeOfDay(TimeOfDayPrecision.MINS);
        }

    }

    /**
     * Default events log sim time formatting (as day + HH:MM).
     * The modeller should override this method (using the helper methods to compose
     * constituent parts) for their own formatting. This default implementation is as below:
     * <p>
     * <pre>
     * if (modelIsInitialising()) {
     *     return "MODEL INIT";
     * }
     * else {
     *     return getSimDay() + " " + getSimTimeOfDay(TimeOfDayPrecision.MINS);
     * }
     * </pre>
     * 
     * @since 0.1
     */
    @Override
    public String getEventsLogFormattedSimTime() {

        if (modelIsInitialising()) {
            return "MODEL INIT";
        }
        else {
            return getSimDay() + " " + getSimTimeOfDay(TimeOfDayPrecision.MINS);
        }

    }

    /**
     * Default run-specific environment setup to do nothing. Override this in
     * your user-written subclass to perform any environment setup that you want
     * to be performed at this very early JSIT-initialisation stage (after all JSIT
     * initialisation is complete).
     * 
     * @since 0.1
     */
    @Override
    public void runSpecificEnvironmentSetup() {
        
        // Do nothing
        
    }

    /**
     * Any static-level stochastic item registration needs to be specified by the user
     * subclass. (We could provide a default 'do nothing' implementation here so that
     * AnyLogic modellers need only code an overriding method (AnyLogic function) if
     * they are using stochastic control and need to do static registration. However,
     * this is a common need and it is easy for modellers to forget to include this
     * function, so we force it to be needed by making it abstract.)
     * 
     * @since 0.1
     */
    @Override
    public abstract void doAllStaticStochRegistration();
    
    // Other Public Instance Methods
    
    /**
     * Any static-level AnyLogic logger setup needs specifying in the user subclass.
     * 
     * @since 0.2
     */
    public abstract void doAllStaticPerRunLoggerSetup();
    
    /**
     * Determine if model is initialising or not.
     * 
     * @since 0.1
     * 
     * @return True if model is running.
     */
    public boolean modelIsInitialising() {

        return  (time() == 0.0d && getEngine().getState() != State.RUNNING);

    }

    /**
     * Method so that model objects can get the singleton model initialiser (especially
     * so as to obtain the run ID).
     * 
     * @since 0.1
     * 
     * @return The existing ModelInitialiser.
     */   
    public ModelInitialiser getModelInitialiser() {

        return jsitInitialiser;

    }

    /**
     * Get the 'raw' AnyLogic simulation time, rounded <b>down</b> to a given number
     * of decimal places (so that it always represents the 'time slot' of the logged
     * event; e.g., something at sim time 100.89 is operating in '0 decimal places
     * time slot' 100.
     * 
     * @since 0.1
     * 
     * @param rawTimeDecimalPlaces
     * The number of decimal places to round down to
     * 
     * @return A String representation of the rounded raw time
     */
    public String getSimRawTime(int rawTimeDecimalPlaces) {

        StringBuffer msgBuffer = new StringBuffer();
        BigDecimal rawSimTime = new BigDecimal(time());
        rawSimTime = rawSimTime.setScale(rawTimeDecimalPlaces,
                                         RoundingMode.FLOOR);
        msgBuffer.append("SIM-TIME ");
        msgBuffer.append(rawTimeDecimalPlaces == 0 ? rawSimTime.intValue()
                                                   : rawSimTime.doubleValue());

        return msgBuffer.toString();

    }

    /**
     * Default current sim day AnyLogic implementation, calculating using time()
     * and day(). User can override if they have a more efficient implementation
     * (e.g., they were already storing a current day integer via a daily event).
     * 
     * @since 0.1
     *     
     * @return The current simulation day (starting at day 1; should never be called
     * when model is initialising)
     */
    public String getSimDay() {

        StringBuffer msgBuffer = new StringBuffer();

        msgBuffer.append("DAY ");
        BigDecimal daysElapsed = new BigDecimal(time() / day());
        daysElapsed = daysElapsed.setScale(0, RoundingMode.FLOOR);
        msgBuffer.append(daysElapsed.intValue() + 1);

        return msgBuffer.toString();

    }

    /**
     * Get the simulation current time of day to a given precision. This should
     * not be called during model initialisation (use modelIsInitialising() to check.)
     * 
     * @since 0.1
     * 
     * @param precision
     * The enumeration value for the required precision
     * 
     * @return A String-formatted time-of-day representation
     */  
    public String getSimTimeOfDay(TimeOfDayPrecision precision) {

        StringBuffer msgBuffer = new StringBuffer();

        msgBuffer.append(timeFormatter.format(getHourOfDay()));
        msgBuffer.append(":");
        msgBuffer.append(timeFormatter.format(getMinute()));
        if (precision == TimeOfDayPrecision.SECS) {
            msgBuffer.append(":");
            msgBuffer.append(timeFormatter.format(getSecond()));
        }
        else if (precision == TimeOfDayPrecision.MILLISECS) {
            msgBuffer.append(":");
            msgBuffer.append(timeFormatter.format(getMillisecond()));            
        }

        return msgBuffer.toString();

    }

    
    // ************ Protected / Package-Access Instance Methods ***************
    
    /*
     * Return the appropriate AnyLogicLogger instance (in 1-1 mapping to a
     * Logback logger) for this run; Logback Loggers will be shared across multiple
     * runs.
     */
    AnyLogicLogger getPerRunAnyLogicLogger(Class<?> owner) {

        assert owner != null;
        Logger logbackLogger = LoggerFactory.getLogger(owner);
        AnyLogicLogger anyLogicLogger = anyLogicLoggersMap.get(logbackLogger);
        if (anyLogicLogger == null) {
            anyLogicLogger = new AnyLogicLogger(this, logbackLogger);
            anyLogicLoggersMap.put(logbackLogger, anyLogicLogger);
        }
        logger.info("AnyLogic Logger set up for class "
                + owner.getSimpleName() + ", run ID "
                + getModelInitialiser().getRunID());
        
        return anyLogicLogger;

    }
    
    /*
     * Register an accessor (for clean-up at run-end) and return the appropriate
     * AnyLogicLogger.
     */
    AnyLogicLogger registerLoggerAccessor(AnyLogicLoggerAccessor accessor) {

        assert accessor != null;
        loggerAccessorList.add(accessor);       // So can clean up at run-end
        return getPerRunAnyLogicLogger(accessor.getOwner());

    }

}
