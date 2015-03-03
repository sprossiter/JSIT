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

import ch.qos.logback.core.*;
import ch.qos.logback.classic.*;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.*;
import ch.qos.logback.core.sift.*;

import org.slf4j.*;
import org.slf4j.Logger;        // To resolve ambiguity with Logback Logger

import java.text.SimpleDateFormat;
import java.text.DecimalFormat;
import java.io.*;
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

    public static final String SETTINGS_FILE = "settings.xml";
    public static final String STOCH_CONTROL_FILE = "stochControl.properties";

    // 'Domain events' (from event managers) go to this single uniquely-named logger
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

    // Debug messages for the thread we're in and the stored MDC value

    public static void debugThreadInfo() {

        logger.debug("In thread " + Thread.currentThread().getName()
                + " (" + Thread.currentThread().getId() + "), runID from MDC "
                + MDC.get((ModelInitialiser.RUN_ID_KEY)));

    }

    /**
     * Get the initialiser for the model run (thread) called from. Useful if Agents
     * have asked the initialiser to store their stochasticity handlers for them
     */
    public static ModelInitialiser getInitialiserForRunViaMDC() {

        String runID = MDC.get(ModelInitialiser.RUN_ID_KEY);
        if (runID == null) {
            throw new IllegalStateException("Model initialiser not yet set up");
        }
        return perRunInitialisers.get(runID);

    }

    /*
     * 
     * Returns null if this run has not previously been initialised (by an embedded or
     * subclass Agent, which will have stored a main model class name which is different
     * to the one currently trying to initialise the model)
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

    /*
     * Private constructor called by the static factory method as required. Check
     * input parameters and then initialise the environment
     */
    public ModelInitialiser(String experimentName,
                            MainModel modelMain) {
        
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
        
        initialiseEnvironment();   // Initialise logging
        loadStochSettings();       // Load settings from stoch control file (if exists)
        
        // Store this initialiser for access by embedding/superclasses       
        ModelInitialiser.perRunInitialisers.put(runID, this);
        
    }

    
    // ************************* Public Instance Methods *******************************
    
    /*
     * Used both internally and externally by experiments to reset the MDC keys (the latter
     * for when AnyLogic switches threads for a single run)
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

    /*
     * Determine whether a main model is the model initialiser or not. Only called
     * after instantiation so can (now) use existence in static list to determine
     */
    public boolean isModelInitiator(MainModel modelMain) {
        
        assert modelInitiator != null;
        return modelMain.getClass().getSimpleName().equals(modelInitiator);
        
    }
      
    /*
     * Get elapsed model time
     */
    public String getElapsedTimeSecs() {

        double rawElapsedTimeSecs = (System.currentTimeMillis() - modelStartTime) / 1000.0;
        DecimalFormat formatter = new DecimalFormat("0.00");        // Format as 2d.p.
        formatter.setRoundingMode(java.math.RoundingMode.HALF_UP);    // 'School' rounding mode (default is half-even)
        return formatter.format(rawElapsedTimeSecs);

    }

    /*
     * Get calculated run ID. May be useful to pass to embedded Agents
     */
    public String getRunID() {

        return runID;

    }

    /*
     * Get the base path for this run's output files
     */
    public String getOutputFilesBasePath() {

        return modelMain.getOutputsBasePath() + "/" + runID;

    }

    /*
     * Save model settings to a file in the output folder. Called as part of initialisation
     * by MainModel subclasses (at a point when the parameters are available) 
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
    
    
    /*
     * User can call when sure that no further stochastic items will be registered to clean up /
     * save JSIT resources
     */
    public void finaliseStochRegistrations() {
        
        if (settingsWriter == null) {
            logger.warn("Stochastic registration finalisation prior to model startup completion ignored");
            return;     // Assume called at inappropriate time by user (will still get done at model end)
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
    
    /*
     * Call when the MainModel is destroyed to clean up (static) storage for the run
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

    
    // ******************** Protected/Package-Access Instance Methods ******************
 
    /**
     * Method exposed for technical reasons. Will hopefully be removed/made non-public
     * in future releases.
     * 
     * Register a dist or lookup, which involves setting its sample mode and checking for
     * double registrations. Returns the sampler so that the caller (a stoch accessor)
     * can register itself and the sampler in the stoch item.
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
    private void initialiseEnvironment() {

        synchronized(ModelInitialiser.class) {            // Ensure parallel runs do this sequentially
            this.runID = EXPERIMENT_START_TIME + "-" + experimentName + "-" + ModelInitialiser.nextRunNumber;
            this.runNum = Integer.toString(nextRunNumber);
            this.modelInitiator = modelMain.getClass().getSimpleName();

            ModelInitialiser.nextRunNumber++;
            setMDC_Keys();                    // Set the MDC keys from the attributes
            setUpLogbackLogging(runID);        // Replace encoders for our per-run appenders

            logger.info("******** Per-Run Model Setup ***********");

            if (logger.isDebugEnabled()) {
                debugThreadInfo();
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
    private void setUpLogbackLogging(String runID) {

        ch.qos.logback.classic.Logger currLogger;
        LoggerContext context;
        OutputStreamAppender<ILoggingEvent> modelAppender;

        // Initially use the root logger for the AMD code (i.e., the one for the package name
        // of this class). Explicit package usage in cast to get the Logback Logger instead of the
        // imported SLF4J one

        currLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(
                                                    "uk.ac.soton.simulation.jsit");
        context = currLogger.getLoggerContext();

        modelAppender = (OutputStreamAppender<ILoggingEvent>) (currLogger.getAppender("CONSOLE"));

        // If the first experiment run, replace the default encoder for the single console appender.
        // Otherwise, just update the LogLayout's reference to the main model

        if (isFirstRun) {                  
            modelAppender.stop();
            modelAppender.setEncoder(createEncoder(context, LogFormatType.CONSOLE));
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
        modelAppender.setEncoder(createEncoder(context, LogFormatType.DIAGNOSTICS));
        modelAppender.start();

        if (logger.isTraceEnabled()) {
            traceLoggerInfo(currLogger);
        }

        // Switch to events logger. Explicit package usage to get the Logback Logger instead
        // of the imported SLF4J one

        currLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(EVENT_LOGGER_NAME);
        context = currLogger.getLoggerContext();

        // Trigger events log appender addition and replace the encoder. The appender
        // in question has an ID equal to the per-run output folder path

        currLogger.error("Dummy error msg to set up event logging (PLEASE IGNORE)");         

        modelAppender = (OutputStreamAppender<ILoggingEvent>)
                ((SiftingAppenderBase<ILoggingEvent>) currLogger.getAppender("EVENTS_RUN_SIFTER"))
                .getAppenderTracker().getOrCreate(getOutputFilesBasePath(), System.currentTimeMillis());
        modelAppender.stop();
        modelAppender.setEncoder(createEncoder(context, LogFormatType.EVENTS));
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
