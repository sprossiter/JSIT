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

/**
 * Interface that top-level JSIT-using model classes should implement
 * to configure JSIT usage. Helper libraries will typically provide
 * default implementations for these which the user should override
 * if they need to.
 * 
 * @author Stuart Rossiter
 * @since 0.1
 */
public interface MainModel {

    /**
     * Provide the relative path (from the runtime working directory)
     * to where experiment inputs are kept.
     * 
     * @return The relative path to the inputs folder (as a String).
     * 
     * @since 0.1
     */
    public String getInputsBasePath();
    
    /**
     * Provide the relative path (from the runtime working directory)
     * to where experiment outputs are kept (in subfolders per run).
     * 
     * @return The relative path to the outputs folder (as a String).
     * 
     * @since 0.1
     */
    public String getOutputsBasePath();
    
    /**
     * Return the current simulation time formatted as desired for
     * diagnostic log entries.
     * 
     * @return The formatted simulation time (as a String).
     * 
     * @since 0.1
     */
    public String getDiagnosticLogFormattedSimTime();
    
    /**
     * Return the current simulation time formatted as desired for
     * events log entries.
     * 
     * @return The formatted simulation time (as a String).
     * 
     * @since 0.1
     */
    public String getEventsLogFormattedSimTime();
    
    /**
     * Any run- or model-specific logic to perform at JSIT initialisation
     * time (after all other JSIT initialisation has completed).
     * 
     * @since 0.1
     */
    public void runSpecificEnvironmentSetup();
    
    /**
     * Called once during JSIT initialisation for the user to register
     * any statically-held StochasticItem instances (i.e., ones not tied
     * to specific instances of model components). The user should
     * override this method and perform the registrations as desired (e.g.,
     * 'globally' accessing all relevant classes or chaining down through
     * the hierarchy of model components).
     * 
     * @since 0.1
     */
    public void doAllStaticStochRegistration();

}
