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

import org.slf4j.Logger;
import org.slf4j.Marker;

/**
 * Helper class (an SLF4J Logger instance) to perform JSIT (Logback-backed)
 * logging in AnyLogic models, where model instances may change threads and
 * thus break the expectations of Logback as regards MDC key availability.
 * <p>
 * <b>AnyLogic models should always log via an instance of this class, not via
 * a standard Logback Logger instance.</b> They should get this model-specific
 * instance via the MainModel_AnyLogic getAnyLogicLogger method at ...
 * 
 * @author Stuart Rossiter
 * @since 0.2
 */
public class AnyLogicLogger implements Logger {
    

    // ************************* Instance Fields ***************************************
    
    private final MainModel_AnyLogic mainModel;
    private final Logger logbackLogger;
    

    // ************************** Constructors *****************************************
    
    /*
     * Only ever created internally via AnyLogicLoggerAccessor
     */
    AnyLogicLogger(MainModel_AnyLogic mainModel, Logger logbackLogger) {
        
        this.mainModel = mainModel;
        this.logbackLogger = logbackLogger;
        
    }


    // *********************** Public Instance Methods *********************************
    
    /**
     * Use to ensure that any logging <i>not</i> being done via AnyLogicLoggers
     * (e.g., logging from third-party libraries you are using, <i>including</i>
     * JSIT) will work OK for the current code block. You do <i>not</i> need to
     * explicitly call this for logging via AnyLogicLoggers, and it will
     * automatically be true if you have previously logged from an
     * AnyLogicLogger in this code block.
     * 
     * (Technically, it will work until the next point at which the execution
     * thread of the run could change; I think this amounts to the end of
     * whatever scheduling event the current code is running as part of. It is
     * easier and safer to assume it is required for any block of code (e.g.,
     * method body) where 'vanilla' logging may be used at a log level that has
     * been set as enabled.)
     * 
     * JSIT code itself uses 'vanilla' Logback Loggers, rather than
     * AnyLogicLogger-style ones (a) to minimise processing overhead; (b)
     * because only AnyLogic models are affected by the problem; (c) because
     * the outcome of logging 'wrongly' is relatively minor---the messages will
     * go to a log in a missingOutputsFolder directory instead of the correct
     * one for the run; (d) because most standard (INFO) level JSIT messages
     * occur at simulation initialisation or model-end (where the logging is
     * guaranteed to work anyway); and (e) because adding such a logging
     * capability would require all JSIT classes to have some reference to a
     * MainModel instance, which would mean the JSIT user having to supply one
     * (or a reference to itself so that JSIT could find it) at instantiation
     * time for certain classes (resulting in awkward and bloated user coding).
     */
    public void ensureExternalLoggingForBlock() {
        
        ensureMDC_KeysSetUp();
        
    }
    
    // All org.slf4j.Logger methods just defer to the encapsulated Logger instance
    // (checking that the MDC keys are set up before any actual logging operation).
    
    @Override
    public void debug(String arg0) {
        
        ensureMDC_KeysSetUp();
        logbackLogger.debug(arg0);

    }

    @Override
    public void debug(String arg0, Object arg1) {
        
        ensureMDC_KeysSetUp();
        logbackLogger.debug(arg0, arg1);

    }

    @Override
    public void debug(String arg0, Object... arg1) {
        
        ensureMDC_KeysSetUp();
        logbackLogger.debug(arg0, arg1);

    }

    @Override
    public void debug(String arg0, Throwable arg1) {
        // TODO Auto-generated method stub

    }

    @Override
    public void debug(Marker arg0, String arg1) {

        ensureMDC_KeysSetUp();
        logbackLogger.debug(arg0, arg1);

    }

    @Override
    public void debug(String arg0, Object arg1, Object arg2) {

        ensureMDC_KeysSetUp();
        logbackLogger.debug(arg0, arg1, arg2);

    }

    @Override
    public void debug(Marker arg0, String arg1, Object arg2) {

        ensureMDC_KeysSetUp();
        logbackLogger.debug(arg0, arg1, arg2);

    }

    @Override
    public void debug(Marker arg0, String arg1, Object... arg2) {

        ensureMDC_KeysSetUp();
        logbackLogger.debug(arg0, arg1, arg2);

    }

    @Override
    public void debug(Marker arg0, String arg1, Throwable arg2) {

        ensureMDC_KeysSetUp();
        logbackLogger.debug(arg0, arg1, arg2);

    }

    @Override
    public void debug(Marker arg0, String arg1, Object arg2, Object arg3) {

        ensureMDC_KeysSetUp();
        logbackLogger.debug(arg0, arg1, arg2, arg3);

    }

    @Override
    public void error(String arg0) {

        ensureMDC_KeysSetUp();
        logbackLogger.error(arg0);

    }

    @Override
    public void error(String arg0, Object arg1) {

        ensureMDC_KeysSetUp();
        logbackLogger.error(arg0, arg1);

    }

    @Override
    public void error(String arg0, Object... arg1) {

        ensureMDC_KeysSetUp();
        logbackLogger.error(arg0, arg1);

    }

    @Override
    public void error(String arg0, Throwable arg1) {

        ensureMDC_KeysSetUp();
        logbackLogger.error(arg0, arg1);

    }

    @Override
    public void error(Marker arg0, String arg1) {

        ensureMDC_KeysSetUp();
        logbackLogger.error(arg0, arg1);

    }

    @Override
    public void error(String arg0, Object arg1, Object arg2) {

        ensureMDC_KeysSetUp();
        logbackLogger.error(arg0, arg1, arg2);

    }

    @Override
    public void error(Marker arg0, String arg1, Object arg2) {

        ensureMDC_KeysSetUp();
        logbackLogger.error(arg0, arg1, arg2);

    }

    @Override
    public void error(Marker arg0, String arg1, Object... arg2) {

        ensureMDC_KeysSetUp();
        logbackLogger.error(arg0, arg1, arg2);

    }

    @Override
    public void error(Marker arg0, String arg1, Throwable arg2) {

        ensureMDC_KeysSetUp();
        logbackLogger.error(arg0, arg1, arg2);

    }

    @Override
    public void error(Marker arg0, String arg1, Object arg2, Object arg3) {

        ensureMDC_KeysSetUp();
        logbackLogger.error(arg0, arg1, arg2, arg3);

    }

    @Override
    public String getName() {
        
        return logbackLogger.getName();
        
    }

    @Override
    public void info(String arg0) {

        ensureMDC_KeysSetUp();
        logbackLogger.info(arg0);

    }

    @Override
    public void info(String arg0, Object arg1) {

        ensureMDC_KeysSetUp();
        logbackLogger.info(arg0, arg1);

    }

    @Override
    public void info(String arg0, Object... arg1) {

        ensureMDC_KeysSetUp();
        logbackLogger.info(arg0, arg1);

    }

    @Override
    public void info(String arg0, Throwable arg1) {

        ensureMDC_KeysSetUp();
        logbackLogger.info(arg0, arg1);

    }

    @Override
    public void info(Marker arg0, String arg1) {

        ensureMDC_KeysSetUp();
        logbackLogger.info(arg0, arg1);

    }

    @Override
    public void info(String arg0, Object arg1, Object arg2) {

        ensureMDC_KeysSetUp();
        logbackLogger.info(arg0, arg1, arg2);

    }

    @Override
    public void info(Marker arg0, String arg1, Object arg2) {

        ensureMDC_KeysSetUp();
        logbackLogger.info(arg0, arg1, arg2);

    }

    @Override
    public void info(Marker arg0, String arg1, Object... arg2) {

        ensureMDC_KeysSetUp();
        logbackLogger.info(arg0, arg1, arg2);

    }

    @Override
    public void info(Marker arg0, String arg1, Throwable arg2) {

        ensureMDC_KeysSetUp();
        logbackLogger.info(arg0, arg1, arg2);

    }

    @Override
    public void info(Marker arg0, String arg1, Object arg2, Object arg3) {

        ensureMDC_KeysSetUp();
        logbackLogger.info(arg0, arg1, arg2, arg3);

    }

    @Override
    public boolean isDebugEnabled() {
        
        return logbackLogger.isDebugEnabled();
        
    }

    @Override
    public boolean isDebugEnabled(Marker arg0) {
        
        return logbackLogger.isDebugEnabled(arg0);
        
    }

    @Override
    public boolean isErrorEnabled() {
        
        return logbackLogger.isErrorEnabled();
        
    }

    @Override
    public boolean isErrorEnabled(Marker arg0) {
        
        return logbackLogger.isErrorEnabled(arg0);
        
    }

    @Override
    public boolean isInfoEnabled() {
        
        return logbackLogger.isInfoEnabled();
        
    }

    @Override
    public boolean isInfoEnabled(Marker arg0) {
        
        return logbackLogger.isInfoEnabled(arg0);
        
    }

    @Override
    public boolean isTraceEnabled() {
        
        return logbackLogger.isTraceEnabled();
        
    }

    @Override
    public boolean isTraceEnabled(Marker arg0) {
        
        return logbackLogger.isTraceEnabled(arg0);
        
    }

    @Override
    public boolean isWarnEnabled() {
        
        return logbackLogger.isWarnEnabled();
        
    }

    @Override
    public boolean isWarnEnabled(Marker arg0) {
        
        return logbackLogger.isWarnEnabled(arg0);
        
    }

    @Override
    public void trace(String arg0) {

        ensureMDC_KeysSetUp();
        logbackLogger.trace(arg0);

    }

    @Override
    public void trace(String arg0, Object arg1) {

        ensureMDC_KeysSetUp();
        logbackLogger.trace(arg0, arg1);

    }

    @Override
    public void trace(String arg0, Object... arg1) {

        ensureMDC_KeysSetUp();
        logbackLogger.trace(arg0, arg1);

    }

    @Override
    public void trace(String arg0, Throwable arg1) {

        ensureMDC_KeysSetUp();
        logbackLogger.trace(arg0, arg1);

    }

    @Override
    public void trace(Marker arg0, String arg1) {

        ensureMDC_KeysSetUp();
        logbackLogger.trace(arg0, arg1);

    }

    @Override
    public void trace(String arg0, Object arg1, Object arg2) {

        ensureMDC_KeysSetUp();
        logbackLogger.trace(arg0, arg1, arg2);

    }

    @Override
    public void trace(Marker arg0, String arg1, Object arg2) {

        ensureMDC_KeysSetUp();
        logbackLogger.trace(arg0, arg1, arg2);

    }

    @Override
    public void trace(Marker arg0, String arg1, Object... arg2) {

        ensureMDC_KeysSetUp();
        logbackLogger.trace(arg0, arg1, arg2);

    }

    @Override
    public void trace(Marker arg0, String arg1, Throwable arg2) {

        ensureMDC_KeysSetUp();
        logbackLogger.trace(arg0, arg1, arg2);

    }

    @Override
    public void trace(Marker arg0, String arg1, Object arg2, Object arg3) {

        ensureMDC_KeysSetUp();
        logbackLogger.trace(arg0, arg1, arg2, arg3);

    }

    @Override
    public void warn(String arg0) {

        ensureMDC_KeysSetUp();
        logbackLogger.warn(arg0);

    }

    @Override
    public void warn(String arg0, Object arg1) {

        ensureMDC_KeysSetUp();
        logbackLogger.warn(arg0, arg1);

    }

    @Override
    public void warn(String arg0, Object... arg1) {

        ensureMDC_KeysSetUp();
        logbackLogger.warn(arg0, arg1);

    }

    @Override
    public void warn(String arg0, Throwable arg1) {

        ensureMDC_KeysSetUp();
        logbackLogger.warn(arg0, arg1);

    }

    @Override
    public void warn(Marker arg0, String arg1) {

        ensureMDC_KeysSetUp();
        logbackLogger.warn(arg0, arg1);

    }

    @Override
    public void warn(String arg0, Object arg1, Object arg2) {

        ensureMDC_KeysSetUp();
        logbackLogger.warn(arg0, arg1, arg2);

    }

    @Override
    public void warn(Marker arg0, String arg1, Object arg2) {

        ensureMDC_KeysSetUp();
        logbackLogger.warn(arg0, arg1, arg2);

    }

    @Override
    public void warn(Marker arg0, String arg1, Object... arg2) {

        ensureMDC_KeysSetUp();
        logbackLogger.warn(arg0, arg1, arg2);

    }

    @Override
    public void warn(Marker arg0, String arg1, Throwable arg2) {

        ensureMDC_KeysSetUp();
        logbackLogger.warn(arg0, arg1, arg2);

    }

    @Override
    public void warn(Marker arg0, String arg1, Object arg2, Object arg3) {

        ensureMDC_KeysSetUp();
        logbackLogger.warn(arg0, arg1, arg2, arg3);

    }

    
    // *********************** Private Instance Methods ********************************
    
    /*
     * If our working thread has changed, ensure that we reset MDC keys to be sure that
     * logging will work properly.
     */
    private void ensureMDC_KeysSetUp() {
        
        Thread currThread = Thread.currentThread();
        if (currThread != mainModel.lastLoggableThread) {
            mainModel.getModelInitialiser().possibleMDC_KeysLoss();
            mainModel.lastLoggableThread = currThread;
        }
        
    }
   
}
