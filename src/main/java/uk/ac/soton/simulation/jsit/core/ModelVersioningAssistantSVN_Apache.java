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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.subversion.javahl.ClientException;
import org.apache.subversion.javahl.CommitInfo;
import org.apache.subversion.javahl.CommitItem;
import org.apache.subversion.javahl.types.Depth;
import org.apache.subversion.javahl.SVNClient;
import org.apache.subversion.javahl.types.Status;
import org.apache.subversion.javahl.callback.CommitCallback;
import org.apache.subversion.javahl.callback.CommitMessageCallback;
import org.apache.subversion.javahl.callback.StatusCallback;

/**
 * Model-versioning assistant for SVN using the SVN 1.7+ Apache package API.
 * 
 * @author Stuart Rossiter
 * @since 0.2
 *
 */
public class ModelVersioningAssistantSVN_Apache extends ModelVersioningAssistantSVN {


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
        public void doStatus(String path, Status status) {
            logger.debug("Got Status for " + path);
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
            ModelVersioningAssistantSVN_Apache.class);


    // ************************* Instance Fields ***************************************

    private final SVNClient hlClient;
    private SVN_StatusHandler statusHandler = null;    // Instantiate single instance if needed

    // ************************** Constructors *****************************************

    /*
     * Non-public constructor.
     */
    ModelVersioningAssistantSVN_Apache(List<File> inVCS_SimCodePath,
                                       File modelVersionFile,
                                       PropertiesConfiguration versionProps) {

        super(inVCS_SimCodePath, modelVersionFile, versionProps);
        // Don't use/instantiate native SVN library if sim code path not specified
        // No methods in this class should ever be called
        if (inVCS_SimCodePath == null) {
            this.hlClient = null;
        }
        else {
            this.hlClient = new SVNClient();
            logger.info("Using Java HL native version " + hlClient.getVersion().toString());
        }

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

        assert hlClient != null;
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
    void doCommit(File sourceRootDir, final String changeNotes) {

        assert hlClient != null;
        assert sourceRootDir.isDirectory();
        
        // Check if keywords property set up properly and set it up if needed
        String versionFilePath = getModelVersionFilePath();
        assert versionFilePath != null;
        
        try {
            byte[] propData = hlClient.propertyGet(versionFilePath,
                                                   KEYWORDS_PROP,
                                                   null,
                                                   null);     
            if (propData == null || !(new String(propData).equals(KEYWORDS_VALUE))) {
                HashSet<String> pathsSet = new HashSet<String>(1, 1.0f);
                pathsSet.add(versionFilePath);
                hlClient.propertySetLocal(pathsSet,  // Paths
                        KEYWORDS_PROP,            // Prop name
                        KEYWORDS_VALUE.getBytes(),   // Prop value as byte array
                        Depth.unknown,            // Depth not relevant for single file
                        null,                     // No filtering changelists
                        false);                   // Don't force
                logger.info(KEYWORDS_PROP + " property set up for model version XML file");
            }
        }
        catch (ClientException e) {
            throw new VersionControlException("Problem getting/setting SVN property for version file "
                    + getModelVersionFilePath(), e);
        }

        try {
            HashSet<String> pathsSet = new HashSet<String>(1, 1.0f);
            pathsSet.add(sourceRootDir.getAbsolutePath());
            hlClient.commit(pathsSet,              // Paths set; always just source root
                    Depth.infinity,             // Infinite depth
                    false,                      // Don't not unlock!
                    false,                      // Don't keep changelist
                    null,                       // No changelists
                    null,            // No revprop mapping
                    new CommitMessageCallback() {                            
                @Override
                public String getLogMessage(Set<CommitItem> items) {
                    return changeNotes;
                }
            },                    // Commit msg callback
            new CommitCallback() {
                @Override
                public void commitInfo(CommitInfo commitInfo) {
                    String errMsg = commitInfo.getPostCommitError();
                    if (errMsg != null) {
                        throw new VersionControlException("SVN commit failed with message "
                                + errMsg + ". You may need to do an SVN cleanup");
                    }
                    logger.info("Successfully committed new revision " + commitInfo.getRevision());
                }
            });                    // Commit callback                  
        }
        catch (ClientException e) {
            throw new VersionControlException("SVN commit failed. You may need to do an SVN cleanup", e);
        }

    }

    /*
     * Utility method to get the raw SVN revision string of code in a directory
     * using the external svnversion command. See svnversion --help
     */
    @Override
    String getRawSVN_Version(File codeDir) {

        assert hlClient != null;
        assert codeDir.isDirectory();
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
