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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for an SVN-specific model-versioning assistant. The implementation
 * uses the JavaHL Java SVN binding (with native libraries), but this has separate
 * Java packages for Subversion 1.6 and Subversion 1.7+ clients (which is what the
 * two concrete subclasses support).
 *  
 * @author Stuart Rossiter
 * @since 0.2
 */
public abstract class ModelVersioningAssistantSVN extends ModelVersioningAssistant {


    // ************************* Static Fields *****************************************

    private static final Logger logger
            = LoggerFactory.getLogger(ModelVersioningAssistantSVN.class);

    // Package access for access by JUnit tests
    static final String KEYWORDS_PROP = "svn:keywords";
    static final String KEYWORDS_VALUE = "Revision HeadURL";


    // ************************** Constructors *****************************************

    /*
     * Non-public constructor.
     */
    ModelVersioningAssistantSVN(List<File> inVCS_SimCodePath,
                                File modelVersionFile,
                                PropertiesConfiguration versionProps) {

        super(inVCS_SimCodePath, modelVersionFile, versionProps);

    }


    // ********************* Public Instance Methods ***********************************

    /**
     * Convert the properties file revision property into a numeric revision
     * string. The property value looks like $Revision: 2 $ (or $Revision$ if file
     * has never been committed, in which case return null)
     */
    @Override
    public String getLastCommitID() {

        String revProp = getVersionProperties().getString(MODEL_REV_PROPERTY);
        logger.debug("Got model revision " + revProp + " from XML");
        if (revProp.equals("$Revision$")) {
            return null;
        }

        Pattern revPattern = Pattern.compile("\\$Revision:(.*)\\$");
        Matcher revMatcher = revPattern.matcher(revProp);
        if (!revMatcher.matches()) {
            throw new IllegalStateException("Property " + MODEL_REV_PROPERTY
                    + " not in correct form. "
                    + "Has the file been set up properly and committed?");
        }

        // Group 0 is whole regexp match; 1 is first real group
        return "r" + revMatcher.group(1).trim();

    }


    // ****************** Protected/Package-Access Instance Methods ********************

    /*
     * Get the repository URL for the running model.
     *
     * Convert the properties file URL property into the correct string for the
     * main model directory. The property value looks something like:
     *
     * $HeadURL: https://mycoolrepo.repoland.com/[...]/Sim/modelVersion.properties $
     * 
     * (or $HeadURL$ if not yet committed to SVN)
     */
    @Override
    String repoLocationAsURL(String location) {

        logger.debug("Got HEAD URL " + location + " from properties file");
        if (location.equals("$HeadURL$")) {
            return null;
        }

        Pattern urlPattern = Pattern.compile(
                "\\$HeadURL: (.*)/modelVersion\\.properties \\$");
        Matcher urlMatcher = urlPattern.matcher(location);
        if (!urlMatcher.matches()) {
            throw new IllegalArgumentException("Property " + VERSION_FILE_REPO_URL_PROPERTY
                    + " not in correct form. "
                    + "Has the file been set up properly and committed?");
        }

        // Group 0 is whole regexp match; 1 is first real group
        return urlMatcher.group(1);

    }

    /*
     * Easiest way to robustly determine is via the revision string for the WC (as done in
     * getModelSVN_Rev()).
     */
    @Override
    boolean dirIsCheckedOut(File codeDir) {

        return (getCodeDir_SVN_Rev(codeDir) != null);

    }

    /*
     * Check whether code has local amendments via the WC version information. Could also have
     * done via issuing a status command (not modified if no Status instances returned, equivalent
     * to an 'empty' response to svn status).
     */
    @Override
    boolean hasCheckedOutDirWithChanges(List<File> checkDirs) {        

        for (File checkDir : checkDirs) {
            assert checkDir.isDirectory();
            String modelLocalRev = getCodeDir_SVN_Rev(checkDir);
            // Null modelLocalRev if dir wasn't checked-out
            if (modelLocalRev != null && modelLocalRev.contains("M")) {
                return true;
            }
        }
        
        return false;

    }

    /*
     * JavaHL-library-specific subclass method to get a WC version string (basically
     * the svnversion cmd output)
     */
    abstract String getRawSVN_Version(File codeDir);


    // *************************** Private Instance Methods ****************************

    /*
     * Utility method to get the SVN revision number of code in a directory
     * using the external svnversion command. (Requires an SVN command-line
     * stack to be installed.). Returns null if code not under version control.
     * The latter case depends on what JavaHL returns for the version string.
     * This is currently "exported" (cf. "Unversioned" with SVNKit) but, to be
     * robust to any changes here---which probably *should* change since assuming
     * it is exported is kind of wrong---we take it as any response without a
     * colon which doesn't start with a number. To be clear, version strings can
     * be as below
     * 
     * exported
     * 120
     * 131M
     * 132:134M   [mixed revisions, which often occur; the last one is what we care about]
     * 
     * See svnversion --help
     */
    private String getCodeDir_SVN_Rev(File codeDir) {

        String rawVersionString = getRawSVN_Version(codeDir);
        logger.debug("Got raw SVN version " + rawVersionString
                     + " for " + codeDir.getAbsolutePath());

        String[] outputParts = rawVersionString.split(":");
        if (outputParts.length == 1) {
            if (outputParts[0].matches("[0-9].*")) {
                return outputParts[0];          // Single revision working copy          
            }
            else {
                return null;                    // Not under version control              
            }
        }
        else if (outputParts.length == 2) {     // Mixed revisions; the norm
            return outputParts[1];              // Return the 'latest' revision
        }
        else {
            throw new AssertionError();         // Should never get here
        }

    }    

}
