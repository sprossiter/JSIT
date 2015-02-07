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

import org.apache.commons.configuration.PropertiesConfiguration;

import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tigris.subversion.javahl.ClientException;
import org.tigris.subversion.javahl.Depth;
import org.tigris.subversion.javahl.PropertyData;
import org.tigris.subversion.javahl.Revision;
import org.tigris.subversion.javahl.SVNClient;
import org.tigris.subversion.javahl.Status;
import org.tigris.subversion.javahl.StatusCallback;

/**
 * Model-versioning assistant for SVN using the pre-1.7 Tigris package.
 * 
 * @author Stuart Rossiter
 * @since 0.2
 *
 */
public class ModelVersioningAssistantSVN_Tigris extends ModelVersioningAssistantSVN {


    // ************************ Static Member Classes **********************************

    private static class SVN_StatusHandler implements StatusCallback { 
        public int totalManagedItems = 0;
        public int totalUnmanagedItems = 0;

        public void reset() {
            totalManagedItems = 0;
            totalUnmanagedItems = 0;
        }

        public int totalItems() {
            return totalManagedItems + totalUnmanagedItems;
        }

        @Override
        public void doStatus(Status status) {
            logger.debug("Got Status for " + status.getPath());
            if (status.isManaged()) {
                totalManagedItems++;
            }
            else {
                totalUnmanagedItems++;
            }
        }
    };


    // ************************* Static Fields *****************************************

    private static final Logger logger = LoggerFactory.getLogger(
                        ModelVersioningAssistantSVN_Tigris.class);


    // ************************* Instance Fields ***************************************

    private final SVNClient hlClient;
    private SVN_StatusHandler statusHandler = null;    // Instantiate single instance if needed


    // ************************** Constructors *****************************************

    /*
     * Non-public constructor.
     */
    ModelVersioningAssistantSVN_Tigris(List<File> inVCS_SimCodePath,
                                       File modelVersionFile,
                                       PropertiesConfiguration versionProps) {

        super(inVCS_SimCodePath, modelVersionFile, versionProps);
        this.hlClient = new SVNClient();
        logger.info("Using Java HL native version " + hlClient.getVersion().toString());

    }    


    // ****************** Protected/Package-Access Instance Methods ********************

    /*
     * Could use svnversion for this (it returns "'X' not versioned, and not exported"
     * rather confusingly) but that is intended for directories (working copies). Use
     * svn status instead and check for non-managed status.
     * 
     * Returns false if file doesn't exist.
     */
    @Override
    boolean fileIsUnderVersionControl(File checkFile) {

        if (!(checkFile.exists())) {
            return false;
        }

        if (statusHandler == null) {
            statusHandler = new SVN_StatusHandler();
        }
        else {
            statusHandler.reset();
        }

        try {
            hlClient.status(checkFile.getAbsolutePath(),   // Path
                    Depth.unknown,      // Depth irrelevant
                    false,              // No server checking
                    false,              // Don't get 'uninteresting' files
                    false,              // Don't get normally-ignored files
                    true,               // Ignore externals
                    null,               // No changelists
                    statusHandler);     // StatusCallback instance
        }
        catch (ClientException e) {
            throw new VersionControlException("Error getting file status", e);
        }

        if (statusHandler.totalItems() != 1) {
            throw new AssertionError("Only expected one SVN status response for a single file!");
        }

        if (statusHandler.totalManagedItems == 1) {
            return true;
        }
        else {
            return false;
        }

    }

    /*
     * This should only be run from the command-line, but use Logback for messages
     * for consistency (will log to the command line in a standard invocation)
     */
    @Override
    void doCommit(File sourceRootDir, String changeNotes) {

        assert sourceRootDir.isDirectory();
        
        // Check if keywords property set up properly and set it up if needed
        String versionFilePath = getModelVersionFilePath();
        assert versionFilePath != null;
        
        try {
            PropertyData propData = null;
            // Work round bug in 1.7.0+ of the JavaHL library where a non-existent property throws a
            // NullPointerException. Raised on dev mailing list
            try {
                propData = hlClient.propertyGet(versionFilePath, KEYWORDS_PROP, null);
            }
            catch (NullPointerException e) {
                // Swallow it; it meant that there was no property (so propData should stay null)
            }
            if (propData == null || !(propData.getValue().equals(KEYWORDS_VALUE))) {                
                hlClient.propertySet(versionFilePath,      // Path
                        KEYWORDS_PROP,            // Prop name
                        KEYWORDS_VALUE,           // Prop value
                        Depth.unknown,            // Depth not relevant for single file
                        null,                     // No filtering changelists
                        false,                    // Don't force
                        null);                    // No revprop mapping on commit
                logger.info(KEYWORDS_PROP + " property set up for model version XML file");
            }
        }
        catch (ClientException e) {
            throw new VersionControlException("Problem getting/setting SVN property for version file "
                    + getModelVersionFilePath(), e);
        }

        Long commitResult = null;

        try {
            commitResult = hlClient.commit(
                    new String[] {sourceRootDir.getAbsolutePath()},  // Source root dir
                    changeNotes,                 // Commit msg
                    Depth.infinity,              // Infinite depth
                    false,                       // Don't not unlock!
                    false,                       // Don't keep changelist
                    null,                        // No changelists
                    null);                       // No revprop mapping
        }
        catch (ClientException e) {
            throw new VersionControlException("SVN commit failed. You may need to run svn cleanup "
                    + "(Clean up in TortoiseSVN) on the main model directory", e);
        }        

        // Since doCommit is only called if there are code changes to commit, we should never get
        // an 'invalid' revision number returned
        if (commitResult == Revision.SVN_INVALID_REVNUM) {
            throw new VersionControlException("Unexpected invalid revision number returned on commit;"
                    + " check the repo to see if the commit actually succeeded");
        }
        logger.info("Commit successful; new revision " + commitResult);

    }

    /*
     * Utility method to get the raw SVN revision string of code in a directory
     * using the external svnversion command. See svnversion --help
     */
    @Override
    String getRawSVN_Version(File codeDir) {

        try {
            return hlClient.getVersionInfo(codeDir.getAbsolutePath(),
                    null,             // No trail URL
                    false);           // Revisions not last changed
        }
        catch (ClientException e) {
            throw new VersionControlException("Problem getting SVN info for model code", e);
        }

    }    

}
