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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.io.*;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import java.text.SimpleDateFormat;

import uk.ac.soton.simulation.jsit.core.ModelVersioningAssistant.LibraryDetail.PartOfSimSource;
import uk.ac.soton.simulation.jsit.core.RunEnvironmentSettings.ModificationStatus;

/**
 * Abstract utility class to be able to programmatically store and check
 * information about the version of simulation model code, integrating with a
 * version control system (VCS) and commit process. VCS-specific subclasses are
 * instantiated by the static factory method <code>createAssistant</code>.
 * 
 * <p>This class should be used in two places:
 * <br><br>
 * (i) To do commits of model material. This *replaces* the normal commit
 * process, and is typically run via a batch file in the root model directory.
 * (The commit is always being done for the current directory ".".) The
 * change comments should always be in a local file called workingChanges.txt,
 * which will be renamed to lastCommitChanges.txt on a successful commit.
 * The main entry point and commitModelMaterial method handles this.
 * <br><br>
 * (ii) When running a model, to check which version is running and log this
 * information for traceability and experiment replication.
 * The various public get methods handle this, returning appropriate strings.
 * 
 * <p>In both cases, the directories forming the in-VCS simulation code
 * (ideally separated from experiments) are passed to the factory method.
 * 
 * <p>Information on the model version is stored in a model version XML file within
 * a sim code directory. The user sets a model name, version number and VCS
 * used (currently NONE or SVN) manually, but every other entry is set
 * automatically by this tool (and entries will differ per-VCS).
 * 
 * <p>To be able to check whether model code has changed locally from the version
 * checked-out or exported from the VCS, we store MD5 hashes of the model source
 * files in the version file, updated automatically at commit time.
 * 
 * @author Stuart Rossiter
 * @since 0.1
 */
public abstract class ModelVersioningAssistant {

    // ************************ Static Member Classes **********************************

    /**
     * Version control system (VCS) alternatives supported by this tool. 
     * <code>NONE</code> can still be used for quasi-version-control: a 'commit'
     * will update information in the model version file, and thus can still be
     * used to 'mark' a particular version of the model (where this information
     * is recorded during model runs) and determine if a particular run used
     * model code which has 'local' differences from this.
     * <p>
     * Currently only Subversion (<code>SVN</code>) is supported.
     * <p>
     * <b>NB</b>: Because of licensing issues, JSIT uses JavaHL (which uses native
     * libraries) for Java access to SVN functionality, rather than the pure Java
     * SVNKit library. This therefore requires that JavaHL is installed on the
     * user's machine (see the JSIT usage instructions).
     * 
     * @since 0.1
     */
    public static enum VersionControlSystem {

        NONE ("None"),
        SVN_TIGRIS_ONLY_CLIENT ("SVN Legacy"),
        SVN_APACHE_CLIENT ("SVN");

        private final String asString;

        VersionControlSystem(String asString) {
            this.asString = asString;
        }
        @Override
        public String toString() {
            return asString;
        }

    };
    
    static class LibraryDetail {
        
        static enum PartOfSimSource { YES, NO, UNKNOWN; }; 
        String jarName;
        PartOfSimSource partOfSimSource;
        
        LibraryDetail(String jarName, PartOfSimSource partOfSimSource) {
            this.jarName = jarName;
            this.partOfSimSource = partOfSimSource;
        }
    };
    

    // ************************* Static Fields *****************************************

    private static final Logger logger
                = LoggerFactory.getLogger(ModelVersioningAssistant.class);

    public static final String MODEL_SOURCE_PATHS_FILE_NAME = "modelSourcePaths.properties";
    public static final String VERSION_FILE_NAME = "modelVersion.properties";
    public static final String WORKING_CHANGES_FILE = "workingChanges.txt";
    public static final String PREV_CHANGES_FILE = "jsitCommitHistory.txt";

    // Required properties completed by the user
    public static final String MODEL_NAME_PROPERTY = "ModelName";
    public static final String MODEL_VER_PROPERTY = "ModelVersionNum";
    public static final String VCS_PROPERTY = "ModelVCS";

    public static final String VERSION_FILE_DIR_PROPERTY = "ModelVersionFileDir";
    public static final String SIM_SOURCE_PATH_PROPERTY = "SimSourcePath";

    // Properties auto-added/updated as part of the commit process
    public static final String SOURCE_DIRS_HASH_PROPERTY = "SourceDirsHash";
    public static final String VERSION_FILE_REPO_URL_PROPERTY = "VersionFileRepoURL";
    public static final String MODEL_REV_PROPERTY = "LastCommitRev";
    public static final String COMMIT_TIME_PROPERTY = "LastCommitTime";


    // ************************* Static Methods ****************************************

    /**
     * Entry point. Only used to commit model code.
     * 
     * @since 0.1
     * 
     * @param args
     * Command-line parameters. Should be COMMIT and the under-VC sim code path. The
     * model version file modelVersion.xml should be in one of the directories
     * on the under-VC sim code path.
     */
    public static void main(String[] args) {

        boolean parmsOK = true;         // Innocent until proven guilty
        List<File> simCodePath = null;
        
        if (args.length != 2
                || !(args[0].equalsIgnoreCase("COMMIT"))) {
            parmsOK = false;
        }
        else {
            simCodePath = getPathConstituents(args[1]);
            if (simCodePath == null) {
                parmsOK = false;
            }
            // TODO: Check that all code paths are dirs, are strict children of "." and
            // none are the same or children of any others
        }
        
        if (!parmsOK) {
            System.err.println("Requires parameters for mode (COMMIT) "
                    + "and a valid under-version-control sim code path");
            System.exit(8);
        }

        ModelVersioningAssistant verController = null;

        try {
             File modelVersionFile = getModelVersionFile(simCodePath);
             if (modelVersionFile == null) {
                 throw new IllegalArgumentException(
                     "Model version file not found in any directory on sim code path");
             }
            verController = createAssistantInternal(modelVersionFile, simCodePath);
        }
        catch (Throwable t) {
            System.err.println("Error getting model version information; see exception below");
            t.printStackTrace();
            System.exit(8);
        }

        boolean committedStuff = false;

        try {
            // Root of material committed is always the folder the batch script is run from
            committedStuff = verController.commitModelMaterial(new File("."));
        }
        catch (Throwable t) {
            System.err.println("Error committing model code; see exception below");
            t.printStackTrace();
            System.exit(8);
        }

        if (committedStuff) {
            System.out.println("Commit successful");
        }
        else {
            System.out.println("No changes to commit; nothing changed");
        }

    }

    /**
     * Static factory method to create a model versioning assistant (of a suitable
     * VCS-specific subclass to this one). The <code>ModelVCS</code> setting in the
     * required model version file is used to determine what is needed.
     * 
     * @since 0.1
     * 
     * @param inputsBasePath
     * Path to the inputs directory. A modelPath.properties file should exist there
     * with a ModelPath property therein which gives the path to the main model code
     * directory (where the modelVersion.xml file can be found). If the model path
     * file does not exist, a path of "." (i.e. the current working directory) is
     * assumed for the model code.
     * @return
     * A suitable subclass of <code>ModelVersioningAssistant</code>.
     */
    public static ModelVersioningAssistant createAssistant(String inputsBasePath) {

        Properties pathProps = new Properties();
        String modelPathsFilePath = inputsBasePath + File.separator + MODEL_SOURCE_PATHS_FILE_NAME;
        File modelPathsFile = new File(modelPathsFilePath);

        try {
            pathProps.load(new FileInputStream(modelPathsFile));
        }
        catch (IOException e) {
            throw new ModelVersioningException("Error reading model paths file " + modelPathsFilePath, e);
        }

        String modelVerFileDirPath = pathProps.getProperty(VERSION_FILE_DIR_PROPERTY);
        if (modelVerFileDirPath == null) {
            throw new ModelVersioningException("Expected " + VERSION_FILE_DIR_PROPERTY
                    + " property not in file " + modelPathsFilePath);
        }
        File modelVersionFile = new File(modelVerFileDirPath + File.separator + VERSION_FILE_NAME);
        if (!modelVersionFile.exists()) {
            throw new ModelVersioningException(VERSION_FILE_NAME + " not found in specified "
                                               + VERSION_FILE_DIR_PROPERTY + " path");
        }
        
        String simSourcePath = pathProps.getProperty(SIM_SOURCE_PATH_PROPERTY);
        List<File> simSourceDirs;
        
        if (simSourcePath == null) {
            logger.warn("No " + SIM_SOURCE_PATH_PROPERTY
                        + " property in model paths file; will not check source code at runtime");
            simSourceDirs = null;
        }
        else {
            simSourceDirs = getPathConstituents(simSourcePath);
            if (simSourceDirs == null) {
                throw new ModelVersioningException("Invalid sim source path specified in "
                                                   + SIM_SOURCE_PATH_PROPERTY + " property");
            }
        }

        return createAssistantInternal(modelVersionFile, simSourceDirs);

    }
    
    /*
     * Convenience method for tests where there's a single code directory. Testing only
     * so no up-front checks
     */
    static ModelVersioningAssistant createAssistantInternal(String simCodePath) {

        List<File> simCodeDirs = new LinkedList<File>();
        simCodeDirs.add(new File(simCodePath));
        
        return createAssistantInternal(new File(simCodePath + File.separator + VERSION_FILE_NAME),
                                       simCodeDirs);
        
    }

    /*
     * Internal factory method creating an assistant with paths for the committed
     * simulation code (potentially separate from that for experiments and tests)
     * and the directories for source (not binaries) or libraries used at run-time.
     * The two may differ if you just commit model source and add libraries
     * in a build process.
     */
    static ModelVersioningAssistant createAssistantInternal(
                                            File modelVersionFile,
                                            List<File> simSourceDirs) {
              
        // Set up properties from file and check format   
        PropertiesConfiguration versionProps;

        try {
            versionProps = new PropertiesConfiguration(modelVersionFile);
        }
        catch (ConfigurationException e) {
            throw new ModelVersioningException("Error reading model version file "
                    + modelVersionFile.getAbsolutePath(), e);
        }

        String[] requiredProperties = new String[]
                { MODEL_NAME_PROPERTY, MODEL_VER_PROPERTY, VCS_PROPERTY };
        for (String s : requiredProperties) {
            if (!versionProps.containsKey(s) || versionProps.getString(s).trim().equals("")) {
                throw new ModelVersioningException("Model version file " + VERSION_FILE_NAME
                        + " should contain non-blank property " + s);
            }
        }

        VersionControlSystem vcs = vcsFromString(versionProps.getString(VCS_PROPERTY));
        if (vcs == null) {
            throw new ModelVersioningException("Invalid VCS specified in properties file");
        }

        if (vcs == VersionControlSystem.NONE) {
            return new ModelVersioningAssistantNoVCS(simSourceDirs,
                                                     modelVersionFile,
                                                     versionProps);
        }
        else if (vcs == VersionControlSystem.SVN_TIGRIS_ONLY_CLIENT) {
            return new ModelVersioningAssistantSVN_Tigris(simSourceDirs,
                                                          modelVersionFile,
                                              versionProps);
        }
        else if (vcs == VersionControlSystem.SVN_APACHE_CLIENT) {
            return new ModelVersioningAssistantSVN_Apache(simSourceDirs,
                                                          modelVersionFile,
                                                          versionProps);
        }

        throw new AssertionError();             // Should never get here

    }
    
    /*
     * Get path to model version file by checking through sim code dirs for it.
     */
    static File getModelVersionFile(List<File> simSourceDirs) {

        File modelVersionFile = null;
        for (File pathDir : simSourceDirs) {
            logger.trace("Searching " + pathDir.getAbsolutePath() + " for model version file");
            File checkFile = new File(pathDir + File.separator + VERSION_FILE_NAME);
            if (checkFile.exists()) {
                modelVersionFile = checkFile;
                logger.trace("Found model version file " + VERSION_FILE_NAME);
                break;
            }
        }
        
        return modelVersionFile;            // Returns null if not found

    }
    
    /*
     * Helper method to get and check directory path constituents from a path
     * string. The separator is platform-dependent (File.pathSeparator).
     */
    static List<File> getPathConstituents(String path) {

        LinkedList<File> pathFiles = new LinkedList<File>();
        String[] pathComponents = path.split(File.pathSeparator);
        assert pathComponents.length != 0; // If path empty still returns one
                                           // empty String
        for (String currPath : pathComponents) {
            currPath = currPath.trim();
            if (currPath.equals("")) {
                return null;
            }
            File currFile = new File(currPath);
            if (!currFile.exists()) {
            	logger.warn("Classpath file " + currPath + " does not exist; ignoring");
            	continue;
            }
            logger.trace("Extracted " + currPath + " from path");
            pathFiles.add(currFile);
        }

        return pathFiles;

    }
    
    /*
     * Convenience method to convert string value to enum
     */
    private static VersionControlSystem vcsFromString(String vcsAsString) {

        VersionControlSystem[] values = VersionControlSystem.values();
        for (VersionControlSystem v : values) {
            if (v.toString().equals(vcsAsString)) {
                return v;
            }
        }
        return null;

    }


    // ************************* Instance Fields ***************************************

    final List<File> simSourceDirs;             // Sim source code dirs
    private final File modelVersionFile;        // Stored to save recomputing
    private final PropertiesConfiguration versionProps;
    final String simSourceHash;                 // Hash of code at instantiation

    
    // ************************** Constructors *****************************************

    /*
     * Non-public constructor (for use by factory method and same-package subclasses)
     */
    ModelVersioningAssistant(List<File> simSourceDirs,
                             File modelVersionFile,
                             PropertiesConfiguration versionProps) {

        assert modelVersionFile != null && modelVersionFile.exists();
        // Assume file lists are all good as set up in static methods
        assert versionProps != null;
        this.simSourceDirs = simSourceDirs;
        this.modelVersionFile = modelVersionFile;
        this.versionProps = versionProps;
        if (simSourceDirs == null) {
            this.simSourceHash = null;
        }
        else {
            this.simSourceHash = calcMD5HashForFileList(
                                    simSourceDirs,      // Paths
                                    false,              // Exclude hidden files (i.e. .svn folders)
                                    new File[] { modelVersionFile },  // Exclude model version file
                                    null,               // No filename exclusions
                                    true);              // Print files included
        }

    }
    

    // ********************* Public Instance Methods ***********************************

    /**
     * Abstract method to return the last commit ID (where this ID is in a suitable VCS-specific form),
     * as in the model version file.
     * 
     * @since 0.1
     * 
     * @return The last commit ID.
     */
    public abstract String getLastCommitID();

    /**
     * Returns the user-assigned model name (as in the model version file).
     * 
     * @since 0.1
     * 
     * @return The user model name.
     */
    public String getUserModelName() {

        return versionProps.getString(MODEL_NAME_PROPERTY);

    }

    /**
     * Returns string identifying the VCS being used (as in the model version file).
     * 
     * @since 0.1
     * 
     * @return The VCS-identifying string.
     */
    public String getModelVCS() {

        return versionProps.getString(VCS_PROPERTY);

    }

    /**
     * Returns a URL string for the model code's VCS repository location (derived from
     * that for the model version file in the model version file).
     * 
     * @since 0.1
     * 
     * @return URL string for the model's repository location.
     */
    public String getModelCommittedURL() {

        String location = getVersionProperties().getString(VERSION_FILE_REPO_URL_PROPERTY);
        if (location == null) {
            return null;
        }
        return repoLocationAsURL(location);

    }

    /**
     * Returns whether the model's sim code has local modifications from the
     * version checksummed at commit time (as reported in the model
     * version file; this includes not-in-VCS files in those folders).
     * 
     * @since 0.1
     * 
     * @return A ModificationStatus value as used by RunEnvironmentSettings (and included
     * in the model settings file).
     */
    public ModificationStatus simHasLocalModifications() {

        if (simSourceDirs == null) {
            return ModificationStatus.UNKNOWN_NO_CHECK;
        }
        
        if (simHasBeenJSIT_Committed()) {
            boolean someSourceCheckedOut = hasCheckedOutDir(simSourceDirs);
            if (someSourceCheckedOut) {
                if (hasCheckedOutDirWithChanges(simSourceDirs)) {
                    return ModificationStatus.YES_IN_CHECKED_OUT;
                }
            }           
            String commitTimeSourceHash = versionProps.getString(SOURCE_DIRS_HASH_PROPERTY);
            logger.debug("Got commit-time source dirs hash " + commitTimeSourceHash + " from XML");
            if (commitTimeSourceHash.equals(simSourceHash)) {
                return ModificationStatus.NO;
            }
            else {
                return someSourceCheckedOut ? ModificationStatus.YES_IN_NON_VCS
                                            : ModificationStatus.YES_NO_VCS_METADATA;
            }
        }
        else {           
            return ModificationStatus.UNKNOWN_NO_COMMIT;
        }

    }

    /**
     * Returns the user-set model version (as in the model version file).
     * 
     * @since 0.1
     * 
     * @return The user-set model version string.
     */
    public String getUserModelVersion() {

        String verProp = versionProps.getString(MODEL_VER_PROPERTY);
        assert verProp != null;         // Instantiation should have checked this
        logger.debug("Got model version " + verProp + " from XML");
        return verProp;

    }


    // ****************** Protected/Package-Access Instance Methods ********************

    /*
     * Subclass-required VCS-specific method as to whether code in a directory is checked
     * out or not.
     */
    abstract boolean dirIsCheckedOut(File checkDir);

    /*
     * Subclass-required VCS-specific method as to whether the list of directories given
     * has at least one checked-out directory with changes (i.e., there is something to
     * commit in there).
     */
    abstract boolean hasCheckedOutDirWithChanges(List<File> checkDirs);

    /*
     * Subclass-required VCS-specific method to do the actual commit (given change
     * notes).
     */
    abstract void doCommit(File sourceRootDir, String changeNotes);

    /*
     * Subclass-required VCS-specific method to convert a repo location (as in the
     * model version file) into an actual URL string (e.g., Subversion uses SVN
     * keywords to auto-generate this, and thus the URL is wrapped in control text).
     */
    abstract String repoLocationAsURL(String location);

    /*
     * Subclass-required VCS-specific method as to whether a file is under version
     * control or not. (Needed to ensure that things like the working changes file
     * aren't under version control.)
     */
    abstract boolean fileIsUnderVersionControl(File checkFile);

    /*
     * Commit user changes, applying appropriate updates to the model version
     * file.
     * 
     * Returns whether anything was committed (false implies there were no
     * changes to commit; an exception is thrown if the commit fails rather than
     * returning false).
     * 
     * We determine if there is stuff to commit and set up the change notes.
     * It's OK if nothing has changed but this is the first JSIT commit. (The
     * XML file will be changed anyway, so SVN will register it as a commit, and
     * the commit 'sorts out' the XML file.) If the simulation (cf. experiments)
     * code, as defined in the model code path properties file, has changed then
     * we need to update the model version XML file. In general, we want to
     * check everything needed for the commit (i.e. the change notes here)
     * *before* altering the properties file to minimise the likelihood of
     * needing to back out those changes.
     * 
     * We want to be able to specify the root dir (even though we're always using
     * "." when running via main) for ease of testing, since it's not possible to
     * directly change the current working directory.
     */
    boolean commitModelMaterial(File commitRootDir) {
        
        assert simSourceDirs != null;
        
        // TODO: Check that the sim source dirs are all under this commit root and
        // that at least one is checked-out as well (could be sparse check-out)
        
        assert commitRootDir.isDirectory();
        if (!dirIsCheckedOut(commitRootDir)) {
            throw new VersionControlException("Model material to commit is not checked-out!");
        }

        File workingChangesFile = new File(commitRootDir.getAbsolutePath()
                                        + File.separator + WORKING_CHANGES_FILE);
        File prevChangesFile = new File(commitRootDir.getAbsolutePath()
                                        + File.separator + PREV_CHANGES_FILE);
        String changeNotes;

        List<File> checkDirs = new LinkedList<File>();
        checkDirs.add(commitRootDir);
        if (hasCheckedOutDirWithChanges(checkDirs)) {
            logger.debug("Checked-out code in root has been changed");
            changeNotes = getChangeNotes(workingChangesFile);
        }
        else {
            if (simHasBeenJSIT_Committed()) {
                logger.debug("No code changes and already JSIT committed; nothing to do");
                return false;                   // Nothing to commit
            }
            logger.debug("First JSIT commit");
            changeNotes = "Initial JSIT commit";
            try {                               // Create working changes file
                PrintWriter writer = new PrintWriter(new FileWriter(workingChangesFile));
                writer.println(changeNotes);
                writer.close();
            }
            catch (IOException e) {
                throw new VersionControlException(
                        "Error writing default " + WORKING_CHANGES_FILE + " contents", e);
            }
        }

        // Check change notes curr/prev files are not under version control
        if (fileIsUnderVersionControl(workingChangesFile)) {
            throw new VersionControlException("Working changes files "
                    + WORKING_CHANGES_FILE
                    + " should not be under version control");
        }
        
        File tempPrevFile = new File(getPrevChangesFileBackupPath());
        PrintWriter prevAppender = null;
        BufferedReader workingReader = null;
        String commitTime = new SimpleDateFormat("yyyy-MM-dd HHmmssSSS")
                                        .format(new Date());
        try {
            if (prevChangesFile.exists()) {
                FileUtils.copyFile(prevChangesFile, tempPrevFile);
            }
            // Creates file if doesn't exist
            prevAppender = new PrintWriter(
                    new BufferedWriter(new FileWriter(prevChangesFile, true)));
            workingReader = new BufferedReader(new FileReader(workingChangesFile));
            prevAppender.println(">>> Commit " + commitTime
                                 + ", Simulation Source Hash " + simSourceHash);
            String currLine = workingReader.readLine();
            while (currLine != null) {
                prevAppender.println(currLine);
                currLine = workingReader.readLine();
            }
            prevAppender.println();
        }
        catch (IOException e) {
            throw new ModelVersioningException(
                    "I/O problem backing up previous changes file", e);
        }
        finally {
            if (prevAppender != null) {
                prevAppender.close();
            }
            if (workingReader != null) {
                try {
                    workingReader.close();
                }
                catch (IOException e) {
                    // Swallow it
                }
            }
        }

        File tempPropsFile = null;
        if (!simHasBeenJSIT_Committed() || hasCheckedOutDirWithChanges(simSourceDirs)) {
            // Backup the properties file
            tempPropsFile = new File(getModelVersionFileBackupPath());
            saveProperties(tempPropsFile);
            versionProps.setProperty(SOURCE_DIRS_HASH_PROPERTY, simSourceHash);
            logger.info("Updated MD5 hash for simulation source dirs to "
                        + versionProps.getProperty(SOURCE_DIRS_HASH_PROPERTY));
            versionProps.setProperty(COMMIT_TIME_PROPERTY, commitTime);
            saveProperties();
        }

        try {
            doCommit(commitRootDir, changeNotes);
        }
        catch (Throwable t) {       // Restore properties file on any problem
            if (tempPrevFile != null) {
                try {
                    if (prevChangesFile.delete()) {
                        tempPrevFile.renameTo(prevChangesFile);
                    }
                }
                catch (Throwable t1) {
                    throw new VersionControlException(
                            "Error recovering previous changes file on commit error",
                            t1);
                }
            }
            if (tempPropsFile != null) {        // We changed the props file
                try {
                    if (modelVersionFile.delete()) {
                        tempPropsFile.renameTo(modelVersionFile);
                    }
                }
                catch (Throwable t1) {
                    throw new VersionControlException(
                            "Error recovering property file on commit error",
                            t1);
                }
            }
            throw new VersionControlException("Error in commit process", t);
        }
        resetWorkingFiles(workingChangesFile, tempPrevFile, tempPropsFile);
        return true;

    }

    PropertiesConfiguration getVersionProperties() {

        return versionProps;

    }
    
    String getModelVersionFilePath() {
        
        return modelVersionFile.getAbsolutePath();
        
    }
    
    String getModelVersionFileBackupPath() {

        return modelVersionFile.getAbsolutePath() + ".bkup";

    }
    
    String getPrevChangesFileBackupPath() {
        
        return PREV_CHANGES_FILE + ".bkup";
        
    }
    
    String getCommitTimeSourceDirsHash() {
        
        return versionProps.getString(SOURCE_DIRS_HASH_PROPERTY);
        
    }

    /*
     * Has model ever been JSIT committed. This check is not absolutely guaranteed
     * to be correct: if a commit fails at the VCS end and the original properties
     * file cannot be recovered (only some strange I/O failure would cause this),
     * then the hash property will be present
     */
    boolean simHasBeenJSIT_Committed() {

        return versionProps.containsKey(SOURCE_DIRS_HASH_PROPERTY);

    }   

    /*
     * Utility methods to save properties and do the actual commit. Package
     * access for use by JUnit tests.
     */
    void saveProperties() {

        saveProperties(modelVersionFile);

    }
    
    /*
     * Get detail of all JAR libraries on the classpath, including whether each of
     * them is in one of the VCS-controlled for the model. (If it is, then separate
     * hash code checks will confirm whether libraries have changed from what was
     * under version control.)
     */
    LibraryDetail[] getLibrariesDetail(String listName, List<File> fileList) {
              
        logger.info("JAR libraries in " + listName);
        ArrayList<LibraryDetail> librariesDetail = new ArrayList<LibraryDetail>();
        
        for (File f : fileList) {
            String currName = f.getName();
            if (currName.endsWith(".jar") || currName.endsWith(".JAR")) {
                logger.info("\t{}", currName);
                if (simSourceDirs == null) {
                    librariesDetail.add(new LibraryDetail(f.getName(), PartOfSimSource.UNKNOWN));
                }
                else if (isInDirectoryList(simSourceDirs, f)) {
                    librariesDetail.add(new LibraryDetail(f.getName(), PartOfSimSource.YES));
                }
                else {
                    librariesDetail.add(new LibraryDetail(f.getName(), PartOfSimSource.NO));
                }
            }
        }
        
        return librariesDetail.toArray(new LibraryDetail[librariesDetail.size()]);
        
    }
    
    /*
     * Utility method to calculate MD5 hash for all files in a given list (where some
     * may be directories)
     * using the Apache Commons Codec library, where this is taken as the hash of
     * a stream concatenating the streams of all files in the list, including all
     * files at any depth within any directory Files on the list.
     * 
     * Note that we need canonical File instances in our internal exclusions
     * list (copied from the input array) so that equals tests work properly: see
     * http://stackoverflow.com/questions/8930859.
     */
    String calcMD5HashForFileList(List<File> pathsToHash,
                          boolean includeHiddenFiles,
                          File[] fileExclusions,
                          String[] filenameExclusions,
                          boolean printFileNames) {

        Vector<FileInputStream> fileStreams = new Vector<FileInputStream>();
        ArrayList<File> workingFileExcludeList = null;

        if (printFileNames) {
            logger.info("Found files for hashing:");
        }

        try {
            if (fileExclusions != null) {
                workingFileExcludeList = new ArrayList<File>(fileExclusions.length);
                for (File f : fileExclusions) {
                    assert f.exists();
                    workingFileExcludeList.add(f.getAbsoluteFile());
                }
            }
            for (File currPath : pathsToHash) {
                assert currPath.exists();
                if (currPath.isDirectory()) {
                    collectInputStreams(currPath,
                                        fileStreams,
                                        workingFileExcludeList,
                                        filenameExclusions,
                                        includeHiddenFiles,
                                        printFileNames);
                }
                else if (currPath.isFile()) {
                    if (printFileNames) {
                        logger.info("\t" + currPath.getAbsolutePath());
                    }
                    if (!fileIsExcluded(includeHiddenFiles,
                                    workingFileExcludeList,
                            filenameExclusions,
                            currPath)) {
                    fileStreams.add(new FileInputStream(currPath));
                    }                   
                }
                else {
                    assert false;    // Should never happen!
                }
            }
            
            SequenceInputStream seqStream = 
                    new SequenceInputStream(fileStreams.elements());
            String md5Hash = DigestUtils.md5Hex(seqStream);
            seqStream.close();
            return md5Hash;
        }
        catch (IOException e) {
            throw new VersionControlException("Error reading files to hash", e);
        }

    }
    
    /*
     * Utility method to check whether a file is under one of a list of
     * directories (under rather than directly in, and not the same as). Note
     * the need to use *canonical files* to ensure File equals checks work: at
     * least on Windows, you can have 2 Files with identical absolute paths that
     * yet fail equals checks. (It seems that one common case is when Windows
     * libraries like Documents are involved, because there is some inherent
     * symbolic-link-type processing related to libraries.)
     * 
     * This method is wasteful in that it goes all the way up to the root
     * directory of the check file (in cases where it isn't under the
     * directory); one could do something like counting the directory level of
     * the 'reference' directory and stopping the parent checks when the parent
     * directory level went beyond this. But that requires loops through parents
     * to pre-calculate the levels, and so doesn't really buy you anything (plus
     * it's problematic due to symbolic links and relative paths).
     * 
     * An alternative would have been to recurse through sub-directories of the
     * dirs to check under and use exists() checks (for the current dir +
     * getName() of checkFile), but that does not guarantee that a file 'found'
     * is definitively the same without then doing an equals test (again needing
     * canonical paths), and you could get lots of 'false positives' needing
     * this equals test for common file names such as README.
     * 
     * Java 1.7 nio classes would help slightly here (e.g., isSameFile and walk
     * methods in java.nio.file.Files) but want to retain Java 5 compatibility.
     */
    boolean isInDirectoryList(List<File> dirs, File checkFile) {
        
        assert checkFile.isFile();

        try {
            for (File dir : dirs) {
                assert dir.isDirectory();
                File checkDir = dir.getCanonicalFile();
                logger.trace("Checking if {} is under {}...",
                        checkFile.getAbsolutePath(), checkDir.getAbsolutePath());
                File currCheckParent = checkFile.getParentFile();
                while (currCheckParent != null) {
                    currCheckParent = currCheckParent.getCanonicalFile();
                    if (currCheckParent.equals(checkDir)) {
                        logger.trace("Found {} in {}", checkFile.getName(),
                                currCheckParent.getAbsolutePath());
                        return true;
                    }

                    currCheckParent = currCheckParent.getParentFile();
                }
            }
        }
        catch (IOException e) {
            throw new ModelVersioningException(
                    "Error getting canonical files from filesystem", e);
        }
        return false; // Didn't find it anywhere

    }
 

    // *************************** Private Instance Methods ****************************

    /*
     * Allows for properties to also be saved in a temp backup file.
     */
    private void saveProperties(File saveFile) {

        try {
            FileOutputStream out = new FileOutputStream(saveFile);
            versionProps.save(saveFile);
            out.close();
        }
        catch (IOException e) {
            throw new VersionControlException("Error writing version file (or backup) "
                    + saveFile.getAbsolutePath(), e);
        }
        catch (ConfigurationException e) {
            throw new VersionControlException("Internal error creating version file "
                    + saveFile.getAbsolutePath(), e);
        }

    }

    /*
     * Get change notes as a single string from the file contents.
     */
    private String getChangeNotes(File changeNotesFile) {

        if (!(changeNotesFile.exists()) || changeNotesFile.length() == 0) {
            throw new VersionControlException("Code changes need specifying in "
                    + changeNotesFile.getAbsolutePath());
        }

        StringBuilder changeNotesBuilder
                            = new StringBuilder((int) changeNotesFile.length());
        try {
            Scanner fileScanner = new Scanner(changeNotesFile);
            while (fileScanner.hasNext()) {
                changeNotesBuilder.append(fileScanner.nextLine());
                changeNotesBuilder.append("\n");
            }
            fileScanner.close();
        }
        catch (IOException e) {
            throw new VersionControlException("Error reading code changes file "
                    + changeNotesFile.getAbsolutePath(), e);
        }

        return changeNotesBuilder.toString();

    }

    /*
     * Reset changes files so that just-committed changes now in
     * lastCommitChanges.txt and currentChanges.txt is emptied (plus
     * delete the temporary backup version file). The model version file
     * backup may not exist. (The commit may have just been for new/
     * altered files outside the source directories.)
     */
    private void resetWorkingFiles(File workingChanges,
                                   File tempPrevChanges,
                                   File tempModelVersion) {

        try {
            workingChanges.delete();
            workingChanges.createNewFile();
            tempPrevChanges.delete();
            if (tempModelVersion != null) {
                tempModelVersion.delete();
            }
        }
        catch (IOException e) {
            throw new VersionControlException(
                    "Failed to clear up working changes files and remove "
                    + "temp backups but commit succeeded");
        }

    }

    /*
     * Recursive method to walk the directory tree and collect input streams in
     * the foundStreams input list. We need to use canonical files to check the
     * exclude list as per http://stackoverflow.com/questions/8930859.
     * 
     * Files need to be presented in a repeatable order to ensure the hash is
     * the same for the same inputs (hence the internal sort).
     */
    private void collectInputStreams(File dir,
                                     List<FileInputStream> foundStreams,
                                     List<File> fileExclusions,
                                     String[] filenameExclusions,
                                     boolean includeHiddenFiles,
                                     boolean printFileNames)
                                             throws IOException {

        File[] fileList = dir.listFiles();        
        Arrays.sort(fileList,               // Need in reproducible order
                new Comparator<File>() {
            public int compare(File f1, File f2) {                       
                return f1.getName().compareTo(f2.getName());
            }
        });

        for (File f : fileList) {
            if (fileIsExcluded(includeHiddenFiles, fileExclusions, filenameExclusions, f)) {
                // Skip it
            }
            else if (f.isDirectory()) {        // Recurse
                collectInputStreams(f, foundStreams, fileExclusions,
                                    filenameExclusions, includeHiddenFiles, printFileNames);
            }
            else {
                try {
                    if (printFileNames) {
                        logger.info("\t" + f.getAbsolutePath());
                    }
                    foundStreams.add(new FileInputStream(f));
                }
                catch (FileNotFoundException e) {
                    throw new AssertionError(e.getMessage()
                            + ": file should never not be found!");
                }
            }
        }

    }
    
    /*
     * Helper method to group up all possible file exclusion tests for the hashing
     * functionality. Note how we have to use canonical files to ensure we really
     * match File-list-based exclusions.
     */
    private boolean fileIsExcluded(boolean includeHiddenFiles,
                                   List<File> fileExclusions,
                                   String[] filenameExclusions,
                                   File checkFile) throws IOException {

        if (!includeHiddenFiles && checkFile.getName().startsWith(".")) {
            return true;
        }

        if (fileExclusions != null) {
            if (fileExclusions.remove(checkFile.getCanonicalFile())) {
                return true;
            }
        }

        if (filenameExclusions != null) {
            for (String s : filenameExclusions) {
                if (s.equals(checkFile.getName())) {
                    return true;
                }
            }
        }

        return false;

    }
    
    /*
     * Helper method to check that >0 of a list of dirs is checked-out 
     */
    private boolean hasCheckedOutDir(List<File> checkDirs) {
        
        for (File checkDir : checkDirs) {
            assert checkDir.isDirectory();
            if (dirIsCheckedOut(checkDir)) {
                return true;
            }
        }
        return false;
        
    }


}
