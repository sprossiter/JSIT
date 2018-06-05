/*  
    Copyright 2018 University of Southampton, Stuart Rossiter
    
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

import ch.qos.logback.core.*;
import ch.qos.logback.classic.*;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.encoder.*;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.sift.*;

import org.slf4j.*;
import org.slf4j.Logger;        // To resolve ambiguity with Logback Logger

import java.text.SimpleDateFormat;
import java.text.DecimalFormat;
import java.io.*;
import java.net.URL;
import java.util.*;

import com.thoughtworks.xstream.XStream;

/**
 * Class which sets up the JSIT environment (logging, parameters serialisation
 * and other per-framework and/or per-model environment configuration).
 *
 * <p>We want to separate logs for each run in multi-run experiments and use the standard
 * way to do this in Logback: use sifting appenders which dynamically create per-run appenders
 * based on an SLF4J MDC key-value pair, where this pair is unique per thread (with the value
 * inherited by child threads). See:
 * 
 * <ul>
 * <li>http://www.slf4j.org/manual.html
 * <li>http://logback.qos.ch/manual/appenders.html
 * </ul>
 *
 * <p>Because visualised versions of the model may embed (or extend)
 * unvisualised versions (which can also run standalone), they share a ModelInitialiser 
 * instance (created by the parent or superclass MainModel). It is expected that both (as
 * MainModel instances) will want to get the shared instance of this class (see the static
 * factory method).
 *
 * <p>Typically, single model runs exist in a single thread, and batch runs
 * launch a thread per run therein. However, we have to allow for
 * <br><br>
 * (a) runs in a non-parallel multi-run experiment (or single-run ones which can be stopped/
 * re-run from scratch) being done in the same thread;
 * <br><br>
 * (b) models changing thread during their run.
 * 
 * (For example, AnyLogic does both of these: (b) when a model is paused and resumed.)
 * 
 * <p>Thus
 * 
 * <ul>
 * <li>MDC keys will exist if a parent initiated this run or (a) above applies.
 * In the first case that is OK; in the other they are not the correct key values for
 * this run.
 * 
 * <li>MDC keys will vanish in case (b) above.
 * </ul>
 * 
 * <p>We handle all this by storing the key values that we set (in the constructor) so that
 * we can compare to these and, in case (b), reapply the MDC keys from the stored values.
 * These keys include one (INIT_MAIN_MODEL_KEY) which is the class name of the MainModel
 * subclass which triggered the instantiation; if this exists and *isn't* the main model
 * class name calling the factory method, we know that a parent already instantiated it and
 * we return that (from our static Hashtable). In case (a) above, we would have the
 * class name of the main model attempting the instantiation.
 * 
 * <p>For case (b), this can only be detected by the (framework-specific) experiment running
 * the model. We provide a method to reapply the stored MDC key values to the new thread,
 * which code in the experiment needs to call at the appropriate time.
 * 
 * @author Stuart Rossiter
 * @since 0.1
 */
public abstract class ModelInitialiser {
    
    // ************************* Static Fields *****************************************

    private static final Logger logger
                = LoggerFactory.getLogger(ModelInitialiser.class);

    /**
     * Name for the model run settings file produced.
     * @since 0.1
     */
    public static final String SETTINGS_FILE = "settings.xml";
    
    /**
     * Name for the input stochasticity control file.
     * @since 0.1
     */
    public static final String STOCH_CONTROL_FILE = "stochControl.properties";

    /**
     * 'Domain events' (from event managers) go to this single uniquely-named logger.
     * @since 0.1
     */
    public static final String EVENT_LOGGER_NAME = "MODEL_DOMAIN_EVENTS";

    // The start time of the experiment, taken as the time this class was statically-initialised
    // (which should be just before the first instance is created)
    private static final String EXPERIMENT_START_TIME
    = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

    // Fixed MDC key names
    protected static final String RUN_ID_KEY = "runID";
    protected static final String RUN_NUM_KEY = "runNum";
    protected static final String RUN_OUTPUTS_PATH_KEY = "runOutPath";
    protected static final String INIT_MAIN_MODEL_KEY = "initMain";

    // Number for the next run (updated and read in a synchronised block)
    private static int nextRunNumber = 1;

    // Model initialisers per run ID (in thread-safe Hashtable)
    private static final Hashtable<String, ModelInitialiser> perRunInitialisers
    							= new Hashtable<String, ModelInitialiser>();

    // Whether first run in an experiment (and thus environment needs setting up)
    // Updated and read in a synchronised block
    private static boolean isFirstRun = true;

    private static enum LogFormatType { CONSOLE, DIAGNOSTICS, EVENTS; };


    // ************************* Static Methods ****************************************

    /**
     * Using the Logger provided, produce a debug message showing the current thread
     * and the run ID stored via MDCs. Intended primarily for JSIT debugging.
     * 
     * @since 0.1
     * 
     * @param logger The Logger to use to log the message. (This must be set to log
     * DEBUG level messages for the message actually be produced.)
     */
    public static void debugThreadInfo(Logger logger) {

        logger.debug("In thread " + Thread.currentThread().getName()
                + " (" + Thread.currentThread().getId() + "), runID from MDC "
                + MDC.get((ModelInitialiser.RUN_ID_KEY)));

    }

    /**
     * Exposed for technical reasons; not intended for JSIT user use.
     * 
     * @since 0.1
     * 
     * @param modelMain The MainModel instance that is checking for an existing initialiser.
     * @return The existing ModelInitialiser (produced by another MainModel) or null if
     * there is no existing one.
     */
    public static ModelInitialiser getExistingInitialiser(MainModel modelMain) {

        ModelInitialiser initialiser = null;
        String loggedThreadRunID = MDC.get(ModelInitialiser.RUN_ID_KEY);
        String loggedThreadMainModel = MDC.get(ModelInitialiser.INIT_MAIN_MODEL_KEY);
        if (loggedThreadRunID != null) {
            assert loggedThreadMainModel != null;
            if (!loggedThreadMainModel.equals(modelMain.getClass().getSimpleName())) {
                initialiser = perRunInitialisers.get(loggedThreadRunID);
                assert initialiser != null;
            }
        }

        return initialiser;

    }
    
    /*
     * Get the initialiser for the model run (thread) called from. Keep non-public
     * primarily to avoid confusion with use in AnyLogic models (where this won't work)
     */
    static ModelInitialiser getInitialiserForRunViaMDC() {

        String runID = MDC.get(ModelInitialiser.RUN_ID_KEY);
        if (runID == null) {
            throw new IllegalStateException("Model initialiser not yet set up");
        }
        return perRunInitialisers.get(runID);

    }
    
    /**
     * Utility method to get the current JRE major version number.
     * 
     * @since 0.2
     * 
     * @return The major version number.
     */
    public static int getJRE_MajorVersionNumber() {
    	
    	String verString = System.getProperty("java.version");
    	int majorVerNumber = 9;		// Assume Java 9 for now
    	
    	try {
    		majorVerNumber = Integer.valueOf(verString.split("\\.")[0]);  // Escape the dot in the regexp
    	} catch (NumberFormatException e) {
    		logger.warn("Unexpected format Java version string " + verString
    					+ "; arbitrarily assuming this is Java 9");
    		// Swallow the exception
    	}
    	
    	return majorVerNumber;
    		
    }

    // ********************** Static Member Classes ************************************

    /*
     * Class to do the log message layout, which will call-back into the main model
     * instance to get the required sim-time formatting. This main model instance needs
     * resetting if a LogsLayout instance is reused for multiple runs
     */
    private static class LogsLayout extends LayoutBase<ILoggingEvent> {
        private final String lineEnd = System.getProperty("line.separator");
        private MainModel timeAccessor;
        private final LogFormatType formatType;

        public LogsLayout(MainModel timeAccessor, LogFormatType formatType) {
            this.timeAccessor = timeAccessor;
            this.formatType = formatType;
        }

        public String doLayout(ILoggingEvent event) {
            StringBuffer msgBuffer = new StringBuffer();
            if (formatType != LogFormatType.EVENTS) {
                msgBuffer.append("THREAD ");
                msgBuffer.append(Thread.currentThread().getId());
                msgBuffer.append(" ");
            }
            if (formatType == LogFormatType.CONSOLE) {                
                msgBuffer.append("RUN ");
                msgBuffer.append(MDC.get(ModelInitialiser.RUN_NUM_KEY));
                msgBuffer.append(" ");
            }
            if (formatType == LogFormatType.DIAGNOSTICS) {            
                msgBuffer.append(timeAccessor.getDiagnosticLogFormattedSimTime());
                msgBuffer.append(" ");
                String[] nameParts = event.getLoggerName().split("\\.");
                msgBuffer.append(nameParts[nameParts.length - 1]);        // Last simple name
            }
            else {
                msgBuffer.append(timeAccessor.getEventsLogFormattedSimTime());
            }        
            if (formatType != LogFormatType.EVENTS) {
                msgBuffer.append(" ");
                msgBuffer.append(event.getLevel().toString());
            }

            msgBuffer.append("  ");
            msgBuffer.append(event.getFormattedMessage());
            msgBuffer.append(lineEnd);

            return msgBuffer.toString();
        }
    }; 


    // ************************* Instance Fields ***************************************

    private final long modelStartTime;
    private String runID;
    private String runNum;
    private String modelInitiator;

    private final String experimentName;
    protected final MainModel modelMain;
    
    // Logback context (never changes for a given model instance)
    private LoggerContext logbackContext;

    // Per-run framework-specific sampler created by subclass
    private Sampler sampler;

    // Registered stoch items for this run and flag for whether registrations finalised

    private ArrayList<AbstractStochasticItem> registeredStochItems
                            = new ArrayList<AbstractStochasticItem>();
    private boolean registrationsFinalised = false;

    // Properties object loaded from stochastic control settings file

    private Properties stochSettings = null;

    // Writer for settings file and related XStream instance

    private BufferedWriter settingsWriter = null;
    private XStream xstream = null;


    // ************************ Constructors *******************************************

    /**
     * Constructor.
     * 
     * @since 0.1
     * 
     * @param experimentName
     *            A string defining the experiment (set of related runs) this
     *            model instance is part of. (Used to derive the JSIT run ID.)
     * @param modelMain
     *            The MainModel instance that is the 'root' object for the
     *            model.
     * @param slf4jBoundToLogback
     * 			  Whether SLF4J is bound to Logback or not. (If not, we need to
     * 			  explicitly initialise Logback ourselves; see later comments.)
     */
    public ModelInitialiser(String experimentName,
                            MainModel modelMain,
                            boolean slf4jBoundToLogback) {
        
        this.modelStartTime = System.currentTimeMillis();
        if (modelMain == null || experimentName == null || experimentName.trim().equals("")) {
            throw new IllegalArgumentException(
                        "Must supply experiment name and main model instance");
        }
        
        this.experimentName = experimentName;
        this.modelMain = modelMain;    
        
        String currPath = modelMain.getOutputsBasePath();
        if (currPath == null || currPath.trim().equals("")) {
            throw new IllegalArgumentException("Must supply outputs base path");
        }       
        File basePath = new File(currPath);
        if (!basePath.exists() || !basePath.canWrite()) {
            throw new IllegalArgumentException("Outputs base path " + currPath
                               + " must exist and be writeable");
        }
        
        currPath = modelMain.getInputsBasePath();
        if (currPath == null || currPath.trim().equals("")) {
            throw new IllegalArgumentException("Must supply inputs base paths");
        }
        basePath = new File(currPath);
        if (!basePath.exists() || !basePath.canRead()) {
            throw new IllegalArgumentException("Inputs base path " + currPath
                               + " must exist and be readable");
        }
        
        initialiseEnvironment(slf4jBoundToLogback);   // Initialise logging
        loadStochSettings();       // Load settings from stoch control file (if exists)
        
        // Store this initialiser for access by embedding/superclasses       
        ModelInitialiser.perRunInitialisers.put(runID, this);
        
    }

    
    // ************************* Public Instance Methods *******************************
    
    /**
     * Primarily used internally (and exposed for technical reasons) to restore MDC
     * key values used to associate an active thread with a model run (needed for AnyLogic
     * models due to threading issues).
     * <p>
     * JSIT users may want to call this themselves if there are instances where their
     * model can switch threads for a run (to one that is not a child of the previous one).
     * 
     * @since 0.2
     */
    public void possibleMDC_KeysLoss() {

        assert (runID != null && runNum != null && modelInitiator != null);
        String currThreadRunID = MDC.get(RUN_ID_KEY);
        if (!runID.equals(currThreadRunID)) {        // LHS always non-null
            setMDC_Keys();
            if (logger.isDebugEnabled()) {
                logger.debug("Thread switch to {}. Replaced logging key {} with stored key {}",
                        Thread.currentThread().getName() + " ("
                                + Thread.currentThread().getId() + ")",
                                currThreadRunID,
                                runID);
            }
        }

    }

    /**
     * Determine whether a main model is the model initialiser or not (since a model
     * may have multiple MainModel instances and only one initiates the JSIT environment,
     * typically the first instantiated).
     * <p>
     * This is primarily used internally (and exposed for technical reasons).
     * 
     * @since 0.1
     * 
     * @param modelMain The MainModel interface to check.
     * @return Whether the MainModel provided is the model initiator.
     */
    public boolean isModelInitiator(MainModel modelMain) {
        
        assert modelInitiator != null;
        return modelMain.getClass().getSimpleName().equals(modelInitiator);
        
    }
      
    /**
     * Get elapsed model processing time in seconds.
     * 
     * @since 0.1
     * 
     * @return Elapsed processing time rounded to 2 d.p. (as a String).
     */
    public String getElapsedTimeSecs() {

        double rawElapsedTimeSecs = (System.currentTimeMillis() - modelStartTime) / 1000.0;
        DecimalFormat formatter = new DecimalFormat("0.00");        // Format as 2d.p.
        formatter.setRoundingMode(java.math.RoundingMode.HALF_UP);    // 'School' rounding mode (default is half-even)
        return formatter.format(rawElapsedTimeSecs);

    }

    /**
     * Get the JSIT-derived run ID (as used in output folder names).
     * 
     * @since 0.1
     * 
     * @return The unique run ID.
     */
    public String getRunID() {

        return runID;

    }

    /**
     * Get the base path for this run's output files. User subclasses (when not using a helper
     * library) may want to override this.
     * 
     * @since 0.1
     * 
     * @return The output files base path (as a String).
     */
    public String getOutputFilesBasePath() {

        return modelMain.getOutputsBasePath() + "/" + runID;

    }
    
    /**
     * Get the Logback context for this model.
     * 
     * @since 0.2
     * 
     * @return The context
     */
    public LoggerContext getLogbackContext() {
    	
    	return logbackContext;
    	
    }

    /**
     * Save model settings to a file in the output folder. This sets up the XStream
     * object and calls writeModelSettings (implemented by the subclass) to do the
     * actual writing.
     * <p>
     * JSIT users will only need to call this manually if not using a helper library.
     * 
     * @since 0.1
     * 
     * @throws IOException if there are problems creating/writing the output file.
     */
    public void saveModelSettings() throws IOException {

        // Set up file and XStream instance. Ignore fields for stochastic items which relate
        // to their registration which hasn't happened yet and aren't related to the 'core'
        // object. (Stochastic items can be serialised at this point because they may be model
        // parameters.)
        xstream = new XStream();
        xstream.omitField(AbstractStochasticItem.class, "accessor");
        xstream.omitField(AbstractStochasticItem.class, "sampler");
        xstream.alias("environmentSettings", RunEnvironmentSettings.class);
        xstream.alias("libraryDetail", ModelVersioningAssistant.LibraryDetail.class);
        // TODO: Do this generically via reflection or some static list
        DistributionCategorical.setupForInfoSerialisation(xstream);
        DistBernoulli.setupForInfoSerialisation(xstream);
        DistCustomCategorical.setupForInfoSerialisation(xstream);
        DistExponential.setupForInfoSerialisation(xstream);
        DistFixedContinuous.setupForInfoSerialisation(xstream);
        DistNormal.setupForInfoSerialisation(xstream);
        DistUniform.setupForInfoSerialisation(xstream);
        DistUniformDiscrete.setupForInfoSerialisation(xstream);
        DistLookupByEnums.setupForInfoSerialisation(xstream);
        DistGeometric.setupForInfoSerialisation(xstream);
        DistNegativeBinomial.setupForInfoSerialisation(xstream);
        DistPoisson.setupForInfoSerialisation(xstream);
        DistTriangular.setupForInfoSerialisation(xstream);
        setupForInfoSerialisation(xstream);      // Allow subclass to setup as well      
        
        String settingsFilePath = getOutputFilesBasePath() + "/" + SETTINGS_FILE;
        settingsWriter = new BufferedWriter(new FileWriter(new File(settingsFilePath)));
        boolean writtenOK = false;

        try {
            writeModelSettings(xstream, settingsWriter);
            logger.info("Written model settings to " + settingsFilePath);
            writtenOK = true;
        }
        catch (IOException e) {
            throw new IOException("I/O error writing " + settingsFilePath, e);
        }
        finally {
            if (!writtenOK) {         // Otherwise keep it open for stochastic item info. writing
                settingsWriter.close();
            }
        }

    }
    
    
    /**
     * Call this when sure that no further stochastic items will be registered, which then
     * allows the per-run settings file to be produced.
     * <p>
     * This will be called as part of the main model destroy processing (onMainModelDestroy
     * method) if the user does not call it earlier.
     * 
     * @since 0.1
     */
    public void finaliseStochRegistrations() {
        
        if (settingsWriter == null) {   // Already done, not writing settings or called too early in model init
            return;     // Will still get done at model end if not done previously
        }
        
        // Write XML for registered stochastic items to settings file
        // TODO: Use proper object serialisation for the ID and sample mode bits (complicated by the
        // 'circularity' of stoch item and accessor references, and the fact that we omitted fields earlier
        // for XStream serialisation of stochastic items which were model parameters). Best to do this in
        // conjunction with changes to allow accessor-free stoch items where parallel runs are not an issue.
        //
        // We consciously use XML attributes (rather than nested elements) for the bits we create
        // 'manually' rather than from XStream serialisation to distinguish them.
         
        assert xstream != null;
        StringBuilder xmlString;
        int i = 0;
               
        try {
            for (AbstractStochasticItem stochItem : registeredStochItems) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Writing stochastic item " + stochItem.getAccessInfo().getFullID()
                                 + " to settings file");
                }
                xmlString = new StringBuilder();
                if (i == 0) {           // First registration
                    xmlString.append("\n<stochasticItems>\n");
                }           
                xmlString.append("  <item id=\"" + stochItem.getAccessInfo().getFullID() + "\" sampleMode=\""
                                 + stochItem.getAccessInfo().getSampleMode() + "\">\n");
                String[] itemLines = xstream.toXML(stochItem).split("\n");
                for (String line : itemLines) {
                    xmlString.append("    ");
                    xmlString.append(line);
                    xmlString.append("\n");
                }
                xmlString.append("  </item>\n");
                settingsWriter.write(xmlString.toString());
                i++;
            }
            if (i != 0) {
                settingsWriter.write("</stochasticItems>\n");
            }
        }
        catch (IOException e) {
            throw new RuntimeException("Problem writing stochastic item info to settings file", e);
        }
        finally {
            try {
                settingsWriter.close();
                settingsWriter = null;        // Clean up file
                registrationsFinalised = true;
            }
            catch (IOException e) {
                // Swallow it
            }
        }
                
        // Keep registeredStochItems so that can remove from static accessor maps at model end
        
    }
    
    /**
     * Call this when the MainModel is destroyed to clean up statically-held storage for the run.
     * 
     * @since 0.1
     */
    public void onMainModelDestroy() {
    
        logger.debug("Main model destruction processing for run ID " + runID);
        // Deregister (remove) any stoch items for the run
        for (AbstractStochasticItem stochItem : registeredStochItems) {
            if (logger.isDebugEnabled()) {
                logger.debug("Deregistering stochastic item " + stochItem.getAccessInfo().getFullID());
            }
            stochItem.deregisterItem();
        }
        
        ModelInitialiser removedInitialiser = perRunInitialisers.remove(runID);
        if (removedInitialiser == null) {       // Already gone
            logger.debug("Model initialiser already cleared by embedded MainModel or subclass for run ID "
                         + runID);
        }
        else {
            assert removedInitialiser == this;
            logger.debug("Cleared up JSIT model initialiser for run ID " + runID);
        }
        
        if (!registrationsFinalised) {       // If not done via user code call
            finaliseStochRegistrations();
        }
        
        registeredStochItems = null;        // For completeness
        
    }

    /**
     * Method exposed for technical reasons; not intended for JSIT user use.
     * <p>
     * Register a dist or lookup, which involves setting its sample mode and checking for
     * double registrations. Returns the sampler so that the caller (a stoch accessor)
     * can register itself and the sampler in the stoch item.
     * 
     * @since 0.2
     * 
     * @param stochItem The stochastic item to register.
     * @return The relevant Sampler for that stochastic item to use.
     */
    public Sampler registerStochItem(AbstractStochasticItem stochItem) {

        if (registrationsFinalised) {
            throw new IllegalStateException("Stochastic registrations already finalised by user");
        }

        if (sampler == null) {        // Lazy instantiation (can't do in constructor because subclass not ready)
            sampler = createFrameworkSpecificSampler();
            assert sampler != null;
        }

        String qualifiedID = stochItem.getAccessInfo().getFullID();    
        String classAllID = stochItem.getAccessInfo().getOwnerName() + ".ALL";            
        if (registeredStochItems.contains(stochItem)) {
            throw new IllegalArgumentException("Stochastic item " + qualifiedID
                    + " has already been registered, possibly by another class");
        }

        Sampler.SampleMode mode = Sampler.SampleMode.NORMAL;

        if (stochSettings != null) {    // Calc mode using less to more specific filters
            if (stochSettings.containsKey("ALL")) {
                mode = Sampler.SampleMode.valueOf(stochSettings.getProperty("ALL"));
            }
            if (stochSettings.containsKey(classAllID)) {
                mode = Sampler.SampleMode.valueOf(stochSettings.getProperty(classAllID));
            }
            if (stochSettings.containsKey(qualifiedID)) {
                mode = Sampler.SampleMode.valueOf(stochSettings.getProperty(qualifiedID));
            }
        }

        stochItem.getAccessInfo().setSampleMode(mode);
        registeredStochItems.add(stochItem);

        String logMsg = qualifiedID + " stochastic item " + stochItem.toString()
                        + " set up for run ID " + runID;
        if (mode == Sampler.SampleMode.NORMAL) {
            logger.info(logMsg);
        }
        else {
            logger.info(logMsg + " --> overridden sample mode " + mode);
        }

        return sampler;

    }
    
    /**
     * Disable stochastic overrides (as provided by the stochasticity control file).
     * Must be called before registering any stochastic items to be effective.
     * 
     * This allows JSIT users to set up a stochasticity control file (used, for 
     * example, during testing) but not actually have it used in a 'normal' run.
     * 
     * @since 0.2
     */
    public void disableStochOverrides() {
    	
    	stochSettings = null;
    	logger.info("Disabling any previously set up stochastic overrides by user request");
    	
    }

    
    // ******************** Protected/Package-Access Instance Methods ******************
      
    protected abstract void writeModelSettings(XStream xstream, BufferedWriter writer) throws IOException;
    protected abstract void setupForInfoSerialisation(XStream xstream);
    protected abstract Sampler createFrameworkSpecificSampler();
        
      
    // *************************** Private Instance Methods ****************************
    
    private void setMDC_Keys() {

        MDC.put(ModelInitialiser.RUN_ID_KEY, runID);
        MDC.put(ModelInitialiser.RUN_NUM_KEY, runNum);
        MDC.put(ModelInitialiser.RUN_OUTPUTS_PATH_KEY, getOutputFilesBasePath());
        MDC.put(ModelInitialiser.INIT_MAIN_MODEL_KEY, modelInitiator);

    }
    
    /*
     * New model run: recalculate all keys, set up logging for this run and, if first run of
     * an experiment, set up the AnyLogic environment
     */
    private void initialiseEnvironment(boolean slf4jBoundToLogback) {

        synchronized(ModelInitialiser.class) {            // Ensure parallel runs do this sequentially
            this.runID = EXPERIMENT_START_TIME + "-" + experimentName + "-" + ModelInitialiser.nextRunNumber;
            this.runNum = Integer.toString(nextRunNumber);
            this.modelInitiator = modelMain.getClass().getSimpleName();

            ModelInitialiser.nextRunNumber++;
            setMDC_Keys();                    // Set the MDC keys from the attributes
            setUpLogbackLogging(runID, slf4jBoundToLogback);  // Replace encoders for our per-run appenders

            logger.info("******** Per-Run Model Setup ***********");

            if (logger.isDebugEnabled()) {
                debugThreadInfo(logger);
            }
            logger.info("Run ID {} calculated by {} and used as outputs folder name",
                    runID, modelInitiator);

            modelMain.runSpecificEnvironmentSetup();        // Any model specific setup at this stage        
        }

    }

    /*
     * Set up the Logback logging environment. The Logback configuration file
     * sets up the correct MDC-differentiated log files, but we need to
     * programmatically replace the standard encoders used with our own which
     * include the simulation time in the message.
     * New appenders are only created via sifting appender on the first
     * log message below (not when we put the MDC value): see my mailing list
     * query at
     * http://logback.10977.n7.nabble.com/Timing-of-sifting-appender-dynamic-appender-instantiation-tp11687.html
     * This needs to be a level that actually gets logged.
     *
     * TODO: This needs refactoring to use a custom converter instead. See
     * http://logback.10977.n7.nabble.com/Why-are-encoder-less-appenders-not-consistently-allowed-tp11688.html
     * 
     * NB: Changes to this code may cause untrapped errors which are silently 
     * swallowed by Logback. Turn on the status listener in the Logback conf file to get this information
     */
    private void setUpLogbackLogging(String runID, boolean slf4jBoundToLogback) {

        ch.qos.logback.classic.Logger currLogger = null;
        OutputStreamAppender<ILoggingEvent> modelAppender;

        // Initially use the root logger for JSIT (i.e., the one for the package name
        // of this class).
        //
        // If SLF4J is not bound to Logback, we have to explicitly initialise Logback
        // using its own classes and get Logger instances via the Logback context. This does
        // mean that the loggers used by JSIT code (which use LoggerFactory to get loggers) will
        // be bound to whatever SLF4J is bound to (and not necessarily Logback).
        //
        // However, in virtually all cases the user has control over the classpath and so can
        // ensure that SLF4J is bound to Logback. (One notable exception is in AnyLogic 8 models
        // where SLF4J is pre-bound to log4j and, when running via the AnyLogic client, the user isn't
        // able to override the classpath in the way they need to so as to bind it to Logback.)
        
        if (slf4jBoundToLogback) {
        	currLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(
                    						"uk.ac.soton.simulation.jsit");
        	logbackContext = currLogger.getLoggerContext();	
        } else {
        	if (ModelInitialiser.isFirstRun) {
        		logbackContext = new LoggerContext();
	        	ContextInitializer contextInitializer = new ContextInitializer(logbackContext);
        	    URL configurationUrl = Thread.currentThread().getContextClassLoader().getResource("logback.xml");
        	    if (configurationUrl == null) {
        	        throw new IllegalStateException("Unable to find logback configuration file");
        	    }
	        	try {
	        	    contextInitializer.configureByResource(configurationUrl);       	    
	        	} catch (JoranException e) {
	        	    throw new RuntimeException("Unable to explicitly configure Logback", e);
	        	}
        	}
        	
        	currLogger = logbackContext.getLogger("uk.ac.soton.simulation.jsit");
        }

        modelAppender = (OutputStreamAppender<ILoggingEvent>) (currLogger.getAppender("CONSOLE"));

        // If the first experiment run, replace the default encoder for the single console appender.
        // Otherwise, just update the LogLayout's reference to the main model

        if (isFirstRun) {                  
            modelAppender.stop();
            modelAppender.setEncoder(createEncoder(logbackContext, LogFormatType.CONSOLE));
            modelAppender.start();
        }
        else {
            LogsLayout customLayout = (LogsLayout)
                    ((LayoutWrappingEncoder<ILoggingEvent>) modelAppender.getEncoder()).getLayout();
            customLayout.timeAccessor = modelMain;
        }

        // Trigger diagnostic log appender addition and then replace the encoder. The appender
        // in question has an ID equal to the per-run output folder path

        currLogger.error("Dummy error msg to set up diagnostic logging (PLEASE IGNORE)");

        modelAppender = (OutputStreamAppender<ILoggingEvent>)
                ((SiftingAppenderBase<ILoggingEvent>) currLogger.getAppender("MSGS_RUN_SIFTER"))
                .getAppenderTracker().getOrCreate(getOutputFilesBasePath(), System.currentTimeMillis());
        modelAppender.stop();
        modelAppender.setEncoder(createEncoder(logbackContext, LogFormatType.DIAGNOSTICS));
        modelAppender.start();

        if (logger.isTraceEnabled()) {
            traceLoggerInfo(currLogger);
        }

        // Switch to events logger. Explicit package usage to get the Logback Logger instead
        // of the imported SLF4J one

        if (slf4jBoundToLogback) {
        	currLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(EVENT_LOGGER_NAME);
        } else {
        	currLogger = logbackContext.getLogger(EVENT_LOGGER_NAME);
        }

        // Trigger events log appender addition and replace the encoder. The appender
        // in question has an ID equal to the per-run output folder path

        currLogger.error("Dummy error msg to set up event logging (PLEASE IGNORE)");         

        modelAppender = (OutputStreamAppender<ILoggingEvent>)
                ((SiftingAppenderBase<ILoggingEvent>) currLogger.getAppender("EVENTS_RUN_SIFTER"))
                .getAppenderTracker().getOrCreate(getOutputFilesBasePath(), System.currentTimeMillis());
        modelAppender.stop();
        modelAppender.setEncoder(createEncoder(logbackContext, LogFormatType.EVENTS));
        modelAppender.start();

        if (logger.isTraceEnabled()) {
            logger.trace("Logging set up for MDC keys (run num, run ID, outputs path, initiator):\n"
                         + MDC.get(ModelInitialiser.RUN_NUM_KEY) + ", "
                         + MDC.get(ModelInitialiser.RUN_ID_KEY) + ", "
                         + MDC.get(ModelInitialiser.RUN_OUTPUTS_PATH_KEY) + ", "
                         + MDC.get(ModelInitialiser.INIT_MAIN_MODEL_KEY));
            traceLoggerInfo(currLogger);
        }

    }

    /*
     * Utility method to create our custom encoder
     */
    private Encoder<ILoggingEvent> createEncoder(LoggerContext context,
                                                 LogFormatType formatType) {

        LayoutBase<ILoggingEvent> modelLayout = new LogsLayout(modelMain, formatType);
        modelLayout.setContext(context);
        LayoutWrappingEncoder<ILoggingEvent> modelEncoder = new LayoutWrappingEncoder<ILoggingEvent>();
        modelEncoder.setContext(context);
        modelEncoder.setLayout(modelLayout);
        return modelEncoder;

    }

    /*
     * Utility methods to trace the structure of a logger for debugging the logging setup
     */
    private void traceLoggerInfo(ch.qos.logback.classic.Logger l) {

        logger.trace("Logger configuration info for " + l.getName());

        Appender<ILoggingEvent> a;
        Appender<ILoggingEvent> subA;
        AppenderTracker<ILoggingEvent> tracker;
        Set<String> subAppKeys;

        for (Iterator<Appender<ILoggingEvent>> i = l.iteratorForAppenders(); i.hasNext();) {
            a = i.next();
            logger.trace("\t" + a.getName() + " (appender type " + a.getClass().getName() + ")");
            if (a instanceof ch.qos.logback.classic.sift.SiftingAppender) {
                tracker = ((SiftingAppenderBase<ILoggingEvent>) a).getAppenderTracker();
                subAppKeys = tracker.allKeys();
                if (subAppKeys.size() == 0) {
                    logger.trace("\t\tNo dynamic appenders");
                }
                for (String s : subAppKeys) {
                    logger.trace("\t\t" + s + " (sub-appender key)");
                    subA = tracker.getOrCreate(s, 0);        // Timestamp 0 seems to work!
                    logger.trace("\t\t\t" + subA.getName()
                            + " (sub-appender type " + subA.getClass().getName() + ")");
                    if (subA instanceof OutputStreamAppender) {
                        traceOutputStreamAppender((OutputStreamAppender<ILoggingEvent>) subA, 4);
                    }
                }
            }
            else if (a instanceof OutputStreamAppender) {
                traceOutputStreamAppender((OutputStreamAppender<ILoggingEvent>) a, 2);
            }
        }

    }

    private void traceOutputStreamAppender(OutputStreamAppender<ILoggingEvent> a, int nestingLevel) {

        StringBuilder s = new StringBuilder();
        for (int i = 0; i < nestingLevel; i++) {
            s.append("\t");
        }

        Encoder<ILoggingEvent> e = ((OutputStreamAppender<ILoggingEvent>) a).getEncoder();
        if (e == null) {
            s.append("No encoder");
            logger.trace(s.toString());
        }
        else {
            s.append(e.getClass().getName());
            s.append(" (encoder)");
            logger.trace(s.toString());
            if (e instanceof LayoutWrappingEncoder) {
                nestingLevel++;
                StringBuilder s2 = new StringBuilder();
                for (int i = 0; i < nestingLevel; i++) {
                    s2.append("\t");
                }
                Layout<ILoggingEvent> l = ((LayoutWrappingEncoder<ILoggingEvent>) e).getLayout();
                if (l == null) {
                    s2.append("No layout");
                }
                else {
                    s2.append(l.getClass().getName());
                    s2.append(" (layout)");
                }
                logger.trace(s2.toString());
            }
        }

    }

    /*
     * Load up any override properties
     */
    private void loadStochSettings() {

        boolean stochOverrides = true;        // Assume overrides exist
        String fullStochControlPath = modelMain.getInputsBasePath() + File.separator + STOCH_CONTROL_FILE;
        File stochControlFile = new File(fullStochControlPath);
        if (stochControlFile.exists()) {
            stochSettings = new Properties();
            try {            
                stochSettings.load(new FileReader(stochControlFile));

            }
            catch (IOException e) {
                throw new IllegalArgumentException(
                        "Error reading stochasticity control overrides file "
                                + fullStochControlPath);
            }
            if (stochSettings.size() == 0) {        // Treat empty file as all normal settings
                logger.warn("Empty stochasticity control file; treating as not present");
                stochOverrides = false;
            }
            else if (stochSettings.size() == 1 && stochSettings.containsKey("ALL")) {
                Sampler.SampleMode allMode = Sampler.SampleMode.valueOf(stochSettings.getProperty("ALL"));
                if (allMode == Sampler.SampleMode.NORMAL) {
                    stochOverrides = false;            // Overrides file just has ALL = NORMAL
                }
            }
        }
        else {
            stochOverrides = false;            
        }

        if (stochOverrides) {
            logger.info("Using stochasticity control overrides from " + fullStochControlPath);

        }
        else {
            logger.info("Using normal stochasticity (no overrides)");
        }

    }
      
}
