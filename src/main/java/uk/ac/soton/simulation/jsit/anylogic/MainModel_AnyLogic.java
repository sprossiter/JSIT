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
import java.io.IOException;
import java.util.HashMap;
import java.math.BigDecimal;
import java.math.RoundingMode;

import uk.ac.soton.simulation.jsit.core.MainModel;
import uk.ac.soton.simulation.jsit.core.ModelInitialiser;

/**
 * Abstract superclass for all Agents representing a runnable AMD model.
 * 
 * @author Stuart Rossiter
 * @since 0.1
 */
public abstract class MainModel_AnyLogic extends Agent implements MainModel {

    // ************************ Static Fields *****************************************

    /**
     * Enumeration of precision alternatives for sim time-of-day values
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
     * instance for the Agent in question. Needed to be able to use
     * this as a 'run token' for StochasticAccessorAnyLogic and for
     * AnyLogic-supporting logging (though the latter normally uses the
     * convenience methods getAgentLogger and getNonAgentLogger).
     * 
     * If the Agent is already a MainModel_AnyLogic instance this should
     * not be called; use the non-static getLogger instead.
     * 
     * @param agent
     * The Agent in question.
     * @return
     * The closest composing MainModel_AnyLogic instance.
     */
    public static MainModel_AnyLogic getMainFor(Agent agent) {
        
        Agent currAgent = agent;
        do {            // Should not be calling if agent is already a main
            currAgent = currAgent.getOwner();
            if (currAgent == null) {
                throw new IllegalArgumentException("Given Agent of class "
                            + agent.getClass().getSimpleName()
                            + " has no containing MainModel_AnyLogic");
            } 
        } while (!(currAgent instanceof MainModel_AnyLogic));
              
        return (MainModel_AnyLogic) currAgent;
        
    }
    
    /**
     * Static convenience method to get the appropriate Logger for a
     * given Agent. This is equivalent to
     * getMainFor(agent).getLogger(agent).
     * 
     * @param agent
     * The Agent in question.
     * 
     * @return
     * The appropriate Logger instance.
     */
    public static Logger getAgentLogger(Agent agent) {
        
        return getMainFor(agent).getLogger(agent);
        
    }
    
    /**
     * Static convenience method to get the appropriate Logger for a
     * given non-Agent. This is equivalent to
     * getMainFor(anyModelAgent).getLogger(obj).
     * 
     * @param obj
     * The Object to get the Logger for.
     * @param anyModelAgent
     * Any Agent that the caller (typically obj) has reference to for the simulation
     * (used to get the MainModel_AnyLogic instance).
     * 
     * @return
     * The appropriate Logger instance.
     */
    public static Logger getNonAgentLogger(Object obj, Agent anyModelAgent) {
        
        return getMainFor(anyModelAgent).getLogger(obj);
        
    }


    // ************************* Instance Fields ***************************************

    // JSIT model initialiser which will run (and initialise the environment) as
    // part of its instantiation in this common superclass' constructor

    private final ModelInitialiser jsitInitialiser;
    private final DecimalFormat timeFormatter = new DecimalFormat("00");

    private Thread lastLoggableThread = null;
    private final HashMap<Class<?>, Logger> loggersMap
                                = new HashMap<Class<?>, Logger>();

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

    // ************************ Constructors *******************************************

    /**
     * Constructor with required signature for an Agent (undocumented by AnyLogic!). We get
     * the JSIT initialiser for this run and set up a guaranteed-to-run-first
     * dynamic event to check if the MDC per-run keys need to change. (This is
     * needed to allow for use of Run Until and Run For, because these do *not*
     * trigger the run(), pause() or step() methods of the Experiment and do
     * the non-init processing of the model in a separate (non-child) thread to that
     * which it (and JSIT) was initialised in. This non-init processing only begins when
     * the event schedule starts to be worked through, and so the logic has to be
     * done within a guaranteed-to-be-scheduled-first event.)
     */
    public MainModel_AnyLogic(Engine engine,
            Agent owner,
            AgentList<? extends MainModel_AnyLogic> collection) {

        super(engine, owner, collection);

        // Get or create (if I'm the initialising instance) the JSIT model initialiser

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
     * seems never to be invoked, so we assert that we don't think it should.
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

            doAllStaticStochRegistration();                        // Hook for modeller to do any static reg

        }

    }

    /**
     * AnyLogic hook point at model destruction. Clears up the model initialiser.
     */
    @Override
    public void onDestroy() {

        if (jsitInitialiser.isModelInitiator(this)) {        // Only do if we're the initialiser
            // For single-run experiments, AnyLogic destroys in a special Model Exec Control
            // Handler thread which will lose MDC keys if the model wasn't paused earlier
            // (since pauses and destruction trigger the creation of this thread). We could
            // check the thread name to only rewrite the keys for single-run experiments, but
            // no harm in doing it anyway and less dependent on AnyLogic internals
            jsitInitialiser.possibleMDC_KeysLoss();       
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
     */
    @Override
    public String getInputsBasePath() {

        return "Inputs";

    }    

    /**
     * Default outputs base path; user should override in their subclass to change this.
     * AnyLogic sets the working directory as the directory of the .alp file that the
     * Experiment is in.
     */
    @Override
    public String getOutputsBasePath() {

        return "Outputs";

    }

    /**
     * Default diagnostic log sim time formatting (as raw time to 0 d.p. + day + HH:MM).
     * The modeller should override this method (using the helper methods to compose
     * constituent parts) for their own formatting. This default implementation is as below:
     * 
     * if (modelIsInitialising()) {
     *     return "MODEL INIT";
     * }
     * else {
     *     return getSimRawTime(0) + " " + getSimDay() + " "
     *              + getSimTimeOfDay(TimeOfDayPrecision.MINS);
     * }
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
     * 
     * if (modelIsInitialising()) {
     *     return "MODEL INIT";
     * }
     * else {
     *     return getSimDay() + " " + getSimTimeOfDay(TimeOfDayPrecision.MINS);
     * }
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
     */
    @Override
    public void runSpecificEnvironmentSetup() {
        
        // Do nothing
        
    }

    /**
     * Default to no static stochastic registration so that AnyLogic modellers need
     * only code an overriding method (AnyLogic function) if they are using stochastic
     * control and need to do static registration.
     */
    @Override
    public void doAllStaticStochRegistration() {
        
        // Do nothing
        
    }
    
    // Other Public Instance Methods
    
    /**
     * Get the appropriate Logger for a simulation Object (Agent or
     * non-Agent). Modellers must use this for AnyLogic-compliant logging
     * (rather than the normal private static Logger instance) to
     * work round AnyLogic's model threading behaviour.
     * 
     * @param o
     * The Object to get the Logger for.
     *
     * @return
     * The appropriate Logger instance.
     */
    public Logger getLogger(Object o) {

        Class<?> loggingClass = o.getClass();
        Logger logger = loggersMap.get(loggingClass);

        if (logger == null) {           // Lazy map it
            logger = LoggerFactory.getLogger(loggingClass);
            loggersMap.put(loggingClass, logger);
        }
        assert logger != null;
        Thread currThread = Thread.currentThread();
        if (currThread != lastLoggableThread) {
            getModelInitialiser().possibleMDC_KeysLoss();
            lastLoggableThread = currThread;
        }
        return logger;

    }

    /**
     * Determine if model is initialising or not.
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

}
