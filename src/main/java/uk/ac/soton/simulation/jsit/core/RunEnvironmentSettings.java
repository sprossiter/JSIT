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

import java.io.File;
import java.util.List;

import org.slf4j.*;

import uk.ac.soton.simulation.jsit.core.ModelVersioningAssistant.VersionControlSystem;

/**
 * Internal class (exposed for technical reasons) used to hold data on toolkit-independent
 * run settings (which is later serialised to the model settings XML file along with
 * toolkit-specific data).
 * 
 * @author Stuart Rossiter
 * @since 0.1
 */
public class RunEnvironmentSettings {

    private static final Logger logger
                = LoggerFactory.getLogger(RunEnvironmentSettings.class);
    
    public static enum ModificationStatus {
        YES_NO_VCS_METADATA ("Yes (version-controlled code not distinguishable)"),
        YES_IN_CHECKED_OUT ("Yes and in version-controlled/checkpointed code"),
        YES_IN_NON_VCS ("Yes but in non-version-controlled code"),
        NO ("No"),
        UNKNOWN_NO_COMMIT ("Unknown (no previous JSIT commits/checkpoints)"),
        UNKNOWN_NO_CHECK ("Unknown (check not made due to missing "
                           + ModelVersioningAssistant.SIM_SOURCE_PATH_PROPERTY
                           + " model source paths file property)");
        
        private final String asText;
        private ModificationStatus(String asText) {
            this.asText = asText;
        }
        @Override
        public String toString() {
            return asText;
        }
    };
    

    // ************************* Instance Fields ***************************************
    // Package-access just to prevent compiler non-use warnings

    final String modelName;
    final String modelVersion;
    final String modelVCS;
    final String modelVersionSource;
    final String modelVCS_CommitID;
    final String runtimeCodeHash;
    final ModificationStatus modificationStatus;
    final String javaVersion;
    final String javaVM;
    final ModelVersioningAssistant.LibraryDetail[] librariesDetail;


    // ************************** Constructors *****************************************

    /**
     * Create a settings instance, auto-populated with information.
     */
    public RunEnvironmentSettings(MainModel modelMain) {

        ModelVersioningAssistant verChecker;

        synchronized (RunEnvironmentSettings.class) {    // Ensure parallel runs do this sequentially            
            verChecker = ModelVersioningAssistant.createAssistant(
                                modelMain.getInputsBasePath());            
        }
        
        List<File> classpathEntries = ModelVersioningAssistant.getPathConstituents(
                                                    System.getProperty("java.class.path"));

        this.modelName = verChecker.getUserModelName();
        this.modelVersion = verChecker.getUserModelVersion();
        this.modelVCS = verChecker.getModelVCS();
        String committedURL = verChecker.getModelCommittedURL();
        this.modelVersionSource = committedURL == null ? "N/A" : committedURL;
        String lastCommitID = verChecker.getLastCommitID();
        this.modelVCS_CommitID = lastCommitID == null ? "N/A" : lastCommitID;
        logger.debug("Calculating hash code for all runtime (classpath) code files...");
        this.runtimeCodeHash = verChecker.calcMD5HashForFileList(
                            classpathEntries,    // File list
                            true,                // Include hidden files
                            null,                // No files excluded
                            new String[] { "logback.xml" },        // Exclude Logback conf files
                            true);                // Print file names
        this.modificationStatus = verChecker.simHasLocalModifications();
        this.javaVersion = System.getProperty("java.version");
        this.javaVM = System.getProperty("java.vm.name") + " "
                + System.getProperty("java.vm.version") + " ("
                + System.getProperty("java.vm.vendor") + ")";
        this.librariesDetail = verChecker.getLibrariesDetail("classpath", classpathEntries);

        logger.info("Running " + modelName + " version " + modelVersion);
        logger.info("Model version-control URL " + modelVersionSource
                + ", commit identifier " + modelVCS_CommitID);

        if (modelVCS.equals(VersionControlSystem.NONE.toString())) {
            logger.warn("MODEL IS NOT STORED IN A VERSION-CONTROL SYSTEM; "
                        + "RUN MAY NOT BE REPRODUCIBLE");
        }        
        if (modificationStatus == ModificationStatus.NO) {
            logger.info("No post-commit/checkpoint modifications to source directories");
        }
        else {
            logger.warn("RUN MAY NOT BE REPRODUCIBLE DUE TO MODIFICATION STATUS: "
                        + modificationStatus.toString());
        }

    }

}
