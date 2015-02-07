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

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.*;
import com.thoughtworks.xstream.io.*;
import com.anylogic.engine.Dimension;
import com.anylogic.engine.Experiment;
import com.anylogic.engine.HyperArray;

import java.lang.reflect.*;
import java.io.*;

import org.slf4j.*;

import uk.ac.soton.simulation.jsit.core.ModelInitialiser;
import uk.ac.soton.simulation.jsit.core.Sampler;

/**
 * AnyLogic helper library ModelInitialiser concrete subclass.
 * 
 * @author Stuart Rossiter
 * @since 0.1
 */    
public class ModelInitialiser_AnyLogic extends ModelInitialiser implements Serializable {
    
    // ***************************** Static Fields *****************************************
    
    private static final Logger logger = LoggerFactory.getLogger(
                    ModelInitialiser_AnyLogic.class.getCanonicalName());

    private static final long serialVersionUID = 1L;
    
    private static class AnyLogicHyperArrayConverter implements Converter {

        public boolean canConvert(@SuppressWarnings("rawtypes") Class clazz) {
            return HyperArray.class == clazz;
        }

        public void marshal(Object value,
                            HierarchicalStreamWriter writer,
                            MarshallingContext context) {
            assert (value instanceof HyperArray);
            HyperArray h = (HyperArray) value;
            Dimension[] dims = h.getDimensions();
            for (Dimension d : dims) {
                writer.startNode("dimension");
                writer.setValue(d.getName());
                writer.endNode();
            }
            writer.startNode("values");
            // Converting the array (rather than its double elements) allows
            // XStream to use a set of <double> tags. Converting an element just
            // gives back the raw value as a string (with no tags)
            context.convertAnother(h.getData());
            writer.endNode();
        }

        public Object unmarshal(HierarchicalStreamReader reader,
                UnmarshallingContext context) {
            // TODO: Add unmarshal logic
            throw new UnsupportedOperationException();
        }

    };
    
    
    // ***************************** Static Methods ****************************************
    
    /*
     * Static factory method that ensures an existing initialiser is returned if an embedded class or subclass
     * already initialised the model
     */
    static ModelInitialiser_AnyLogic getInitialiserForRun(
            Experiment<?> experiment,
            MainModel_AnyLogic modelMain) {
        
        ModelInitialiser initialiser = ModelInitialiser.getExistingInitialiser(modelMain);
        if (initialiser == null) {            // Not already initialised by embedded/subclass
            initialiser = new ModelInitialiser_AnyLogic(experiment, modelMain);
            logger.debug("Completed JSIT model initialisation for run ID " + initialiser.getRunID());
        }
        else {
            logger.debug("Reusing JSIT model initialiser from embedded class or subclass for run ID "
                         + initialiser.getRunID());
        }
        
        return (ModelInitialiser_AnyLogic) initialiser;
        
    }
    
    
    // ************************** Instance Fields *************************************
    
    private RunEnvironmentSettingsAnyLogic envSettings;    

    private class AnyLogicMainConverter implements Converter {
            
        // Convert any MainModel_AnyLogic subclass
        
        public boolean canConvert(@SuppressWarnings("rawtypes") Class clazz) {
            return MainModel_AnyLogic.class.isAssignableFrom(clazz);
        }

        public void marshal(Object value,
                            HierarchicalStreamWriter writer,
                            MarshallingContext context) {
            assert (value instanceof MainModel_AnyLogic);
                        
            // Write model parameters using Java reflection. (AnyLogic includes set_<parmname> methods to
            // set each parameter which we can use to identify them all.)
            
            Class<?> c = value.getClass();
            Method[] methods = c.getMethods();
            String currParmName = null;
            Object currParmVal = null;
            writer.startNode("parameters");
            for (Method m : methods) {
                String methodName = m.getName();
                if (methodName.startsWith("set_")) {
                    currParmName = methodName.substring(4);
                    logger.debug("Processing parm name <" + currParmName + ">");
                    writer.startNode(currParmName);
                    try {
                        currParmVal = c.getField(currParmName).get(modelMain);
                    }
                    catch (NoSuchFieldException e) {
                        // Assume this is a set_ method written by the user not referring to a field
                        logger.warn("Ignoring assumed user-written method set_" + currParmName
                                                           + "> for class " + c.getName());
                    }
                    catch (IllegalAccessException e) {
                        throw new AssertionError("Unexpected illegal access exception to field <"
                                                 + currParmName + ">");
                    }
                    if (currParmVal == null) {
                        writer.setValue("null");
                    }
                    else {
                        context.convertAnother(currParmVal);
                    }
                    writer.endNode();
                }
            }
            writer.endNode();            // </parameters>
            
        }

        public Object unmarshal(HierarchicalStreamReader reader,
                                UnmarshallingContext context) {            
            // TODO
            throw new UnsupportedOperationException();            
        }
        
    };
        
    
    // *************************** Constructors ***************************************
    
    /*
     * Private constructor: static factory method for instantiation
     */
    private ModelInitialiser_AnyLogic(Experiment<?> experiment,
                                       MainModel_AnyLogic mainModel) {
        
        super(experiment.getClass().getSimpleName(), mainModel);
        this.envSettings = new RunEnvironmentSettingsAnyLogic(mainModel);
        
    }
    
    
    // ****************** Protected/Package-Access Instance Methods **************************
    
    /*
     * Abstract superclass method implementation. Once written, we can discard the stored
     * environment settings
     */  
    @Override
    protected void writeModelSettings(XStream xstream, BufferedWriter writer) throws IOException {
        
        assert envSettings != null;
        xstream.alias("environmentSettings", RunEnvironmentSettingsAnyLogic.class);
        writer.write(xstream.toXML(envSettings));
        writer.newLine();
        xstream.registerConverter(new AnyLogicHyperArrayConverter());
        xstream.registerConverter(new AnyLogicMainConverter());
        writer.write(xstream.toXML(modelMain));
        envSettings = null;            // Garbage-collect the settings object
        
    }
    
    @Override
    protected Sampler createFrameworkSpecificSampler() {
        
        logger.debug("Creating AnyLogic-specific JSIT Sampler...");
        return new Sampler_AnyLogic((MainModel_AnyLogic) modelMain);
        
    }
    
    @Override
    protected void setupForInfoSerialisation(XStream xstream) {
        
        HyperArrayLookup.setupForInfoSerialisation(xstream);
        
    }
    
}
