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

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.slf4j.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.configuration.PropertiesConfiguration;

// Imports for non-legacy Apache JavaHL package. Test code for the legacy Tigris API
// uses explicit package names referring to org.tigris.subversion.javahl

import org.apache.commons.io.FileUtils;
import org.apache.subversion.javahl.*;
import org.apache.subversion.javahl.callback.CommitCallback;
import org.apache.subversion.javahl.callback.CommitMessageCallback;
import org.apache.subversion.javahl.types.Depth;

/**
 * Unit tests for ModelVersioningAssistantSVN. Everything assumes that tests are being run from
 * the root JSIT directory (which is what happens under Eclipse). If running under Eclipse, make
 * sure that (non-JUnit) assertions (-ea JVM option) has been specified to trigger assertions in
 * the code under test: see http://stackoverflow.com/questions/1798016.
 * 
 * @author Stuart Rossiter
 * @since 0.1
 */
public class TestModelVersioningAssistantSVN {

    private static Logger logger = LoggerFactory.getLogger(TestModelVersioningAssistantSVN.class);

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private static final String RESOURCE_PATH = "src/test/resources/";
    private static final String DUMMY_MODEL_DIR = "DummyModelSVN";
    private static final String DUMMY_MODEL_DIR_LEGACY = "DummyModelSVN_Legacy";
    private static final String LOCAL_SVN_REPO = "TestSVN";
    private static final String WORKING_COPY_DIR = "WC";

    /**
     * Before all tests, give user heads-up on working dir.
     */
    @BeforeClass
    public static void preTests() {

        String workingDirPath = System.getProperty("user.dir");
        logger.warn(TestModelVersioningAssistantSVN.class.getSimpleName()
                + " running from working dir "
                + workingDirPath + "; set-up will fail if this is not the JSIT root dir");

        System.out.println("Test classpath: " + System.getProperty("java.class.path"));
        
    }

    /**
     * Test that version details returned appropriately for never-committed
     * model code.
     */
    @Test
    public void neverCommittedVersionDetails() throws IOException {

        logger.info("Testing version details for never-committed model code");

        setUpTempAreaWithModel(false);       
        neverCommittedVersionDetailsShared(false);

    }


    /**
     * Test that version details returned appropriately for never-committed
     * model code.
     */
    @Test
    public void neverCommittedVersionDetailsLegacy() throws IOException {

        logger.info("Testing version details for never-committed model code (legacy API)");

        setUpTempAreaWithModel(true);
        neverCommittedVersionDetailsShared(true);

    }

    private void neverCommittedVersionDetailsShared(boolean isLegacy) throws IOException {

        // Assistant needs model *code* directory path
        ModelVersioningAssistant assistant = ModelVersioningAssistantSVN.createAssistantInternal(
                tempFolder.getRoot().getAbsolutePath() + File.separator
                + DUMMY_MODEL_DIR + File.separator + "Code");

        Assert.assertNull(assistant.getLastCommitID());
        Assert.assertNull(assistant.getModelCommittedURL());
        if (isLegacy) {
            Assert.assertEquals("SVN Legacy", assistant.getModelVCS());
        }
        else {
            Assert.assertEquals("SVN", assistant.getModelVCS());
        }
        Assert.assertEquals("DummyModel", assistant.getUserModelName());
        Assert.assertEquals("0.1", assistant.getUserModelVersion());

    }

    /**
     * Test that version details returned appropriately for non-JSIT-committed working
     * copy code.
     */
    @Test
    public void nonJSIT_Commit() throws IOException, ClientException {

        logger.info("Testing version details for non-JSIT-committed model code");
        setUpTempAreaWithModel(false);
        SVNClient svnClient = new SVNClient();
        String repoModelURI = setUpModelWorkingCopy(svnClient);
        nonJSIT_CommitModel(svnClient);        
        nonJSIT_CommitShared(repoModelURI, false);

    }

    /**
     * Test that version details returned appropriately for non-JSIT-committed working
     * copy code.
     */
    @Test
    public void nonJSIT_CommitLegacy() throws IOException,
                org.tigris.subversion.javahl.ClientException {

        logger.info("Testing version details for non-JSIT-committed model code (legacy API)");
        setUpTempAreaWithModel(true);
        org.tigris.subversion.javahl.SVNClient svnClient
        = new org.tigris.subversion.javahl.SVNClient();
        String repoModelURI = setUpModelWorkingCopyLegacy(svnClient);
        nonJSIT_CommitModelLegacy(svnClient);
        nonJSIT_CommitShared(repoModelURI, true);

    }

    private void nonJSIT_CommitShared(String repoModelURI, boolean isLegacy) {

        // Assistant needs model *code* directory path
        ModelVersioningAssistant assistant = ModelVersioningAssistantSVN.createAssistantInternal(
                tempFolder.getRoot().getAbsolutePath() + File.separator
                + WORKING_COPY_DIR + File.separator + "Code");

        Assert.assertEquals("r2", assistant.getLastCommitID());
        Assert.assertEquals(repoModelURI + "/Code",
                assistant.getModelCommittedURL());
        if (isLegacy) {
            Assert.assertEquals("SVN Legacy", assistant.getModelVCS());
        }
        else {
            Assert.assertEquals("SVN", assistant.getModelVCS());
        }
        Assert.assertEquals("DummyModel", assistant.getUserModelName());
        Assert.assertEquals("0.1", assistant.getUserModelVersion());

    }

    /**
     * Test the JSIT commit process via a number of sequential actions.
     * Does not use the command-line interface. (Could test this by launching it
     * via ProcessBuilder, but the extra main logic is very basic.)
     */
    @Test
    public void validJSIT_Commit() throws IOException, ClientException {

        logger.info("Testing version details for JSIT-committed model code");
        setUpTempAreaWithModel(false);
        SVNClient svnClient = new SVNClient();
        String repoModelURI = setUpModelWorkingCopy(svnClient);  // WC is now for rev 1
        validJSIT_CommitShared(repoModelURI, false);

    }

    /**
     * Test the JSIT commit process via a number of sequential actions.
     * Does not use the command-line interface. (Could test this by launching it
     * via ProcessBuilder, but the extra main logic is very basic.)
     */
    @Test
    public void validJSIT_CommitLegacy() throws IOException,
                        org.tigris.subversion.javahl.ClientException {

        logger.info("Testing version details for JSIT-committed model code (legacy API)");
        setUpTempAreaWithModel(true);
        org.tigris.subversion.javahl.SVNClient svnClient
        = new org.tigris.subversion.javahl.SVNClient();
        String repoModelURI = setUpModelWorkingCopyLegacy(svnClient);  // WC is now for rev 1
        validJSIT_CommitShared(repoModelURI, true);

    }

    private void validJSIT_CommitShared(String repoModelURI, boolean isLegacy)
            throws IOException {

    	final String rootPath = tempFolder.getRoot().getAbsolutePath()
                + File.separator + WORKING_COPY_DIR;
    	final File rootPathFile = new File(rootPath);
        final String codePath = rootPath + File.separator + "Code";

        // Assistant needs in-VCS model code directory path
        ModelVersioningAssistant assistant
        = ModelVersioningAssistantSVN.createAssistantInternal(codePath);

        /*
         * First try without any working copy changes. Should work because no
         * previous JSIT commits
         */

        logger.info("JSIT commit without WC changes (first JSIT commit)");
        boolean committedStuff = assistant.commitModelMaterial(rootPathFile);
        Assert.assertTrue(committedStuff);
        File prevChangesFile = new File(rootPath + File.separator
                                        + ModelVersioningAssistant.PREV_CHANGES_FILE);
        Assert.assertTrue(prevChangesFile.exists());    // We check content later

        // Re-instantiate to read hopefully altered version details
        assistant = ModelVersioningAssistantSVN.createAssistantInternal(codePath);
        Assert.assertEquals("r2", assistant.getLastCommitID());
        Assert.assertEquals(repoModelURI + "/Code", assistant.getModelCommittedURL());
        if (isLegacy) {
            Assert.assertEquals("SVN Legacy", assistant.getModelVCS());
        }
        else {
            Assert.assertEquals("SVN", assistant.getModelVCS());
        }
        Assert.assertEquals("DummyModel", assistant.getUserModelName());
        Assert.assertEquals("0.1", assistant.getUserModelVersion());
        logger.debug("Hash code after commit: " + assistant.getCommitTimeSourceDirsHash());

        /*
         * Now check further commit doesn't commit anything
         */

        logger.info("JSIT commit without WC changes (already JSIT committed)");
        committedStuff = assistant.commitModelMaterial(rootPathFile);
        Assert.assertFalse(committedStuff);

        /*
         * Now make a change (to the version file, using assistant code to write
         * the props for us) and check the commit fails because of missing
         * working changes file
         */

        logger.info("JSIT commit changed code without working changes file");
        PropertiesConfiguration verProps = assistant.getVersionProperties();
        String revisedVer = "0.1FINAL";
        verProps.setProperty(ModelVersioningAssistant.MODEL_VER_PROPERTY, revisedVer);
        assistant.saveProperties();
        // Re-instantiate to read hopefully altered version details
        assistant = ModelVersioningAssistantSVN.createAssistantInternal(codePath);
        try {
            committedStuff = assistant.commitModelMaterial(rootPathFile);
            Assert.fail("Expected VersionControlException");
        }
        catch (VersionControlException e) {
            Assert.assertTrue(e.getMessage().startsWith("Code changes need specifying"));
        }

        /* 
         * Create the changes file and check commit works OK, and that prev changes
         * file has appended the details
         */
        
        logger.info("JSIT commit changed code with working changes file");
        File changesFile = new File(rootPath + File.separator
                                    + ModelVersioningAssistant.WORKING_CHANGES_FILE);
        PrintWriter writer = new PrintWriter(new FileWriter(changesFile));
        String changeText = "Changed model version number";
        writer.println(changeText);
        writer.close();

        // Re-instantiate just to ensure correctness of test
        assistant = ModelVersioningAssistantSVN.createAssistantInternal(codePath);
        committedStuff = assistant.commitModelMaterial(rootPathFile);
        Assert.assertTrue(committedStuff);
        // Temp file should have been deleted
        Assert.assertFalse(new File(assistant.getModelVersionFileBackupPath()).exists());
        // Prev changes file should exist and have appended content
        Assert.assertTrue(prevChangesFile.exists());
        BufferedReader prevReader = new BufferedReader(new FileReader(prevChangesFile)); 
        String prevDesc = "prev chgs file";
        String commitLinePattern = ">>> Commit \\d{4}-\\d{2}-\\d{2} \\d{9},.+";
        Assert.assertTrue(readAndShowLine(prevDesc, prevReader).matches(commitLinePattern));
        Assert.assertTrue(readAndShowLine(prevDesc, prevReader).equals("Initial JSIT commit"));
        Assert.assertTrue(readAndShowLine(prevDesc, prevReader).equals(""));     // Blank 3rd line
        Assert.assertTrue(readAndShowLine(prevDesc, prevReader).matches(commitLinePattern));
        Assert.assertTrue(readAndShowLine(prevDesc, prevReader).equals(changeText));
        prevReader.close();
              
        // Working changes file should be empty
        Assert.assertEquals(0, changesFile.length());

        // Re-instantiate for fresh property values
        assistant = ModelVersioningAssistantSVN.createAssistantInternal(codePath);
        Assert.assertEquals("r3", assistant.getLastCommitID());
        Assert.assertEquals(repoModelURI + "/Code", assistant.getModelCommittedURL());
        if (isLegacy) {
            Assert.assertEquals("SVN Legacy", assistant.getModelVCS());
        }
        else {
            Assert.assertEquals("SVN", assistant.getModelVCS());
        }
        Assert.assertEquals("DummyModel", assistant.getUserModelName());
        Assert.assertEquals(revisedVer, assistant.getUserModelVersion());
        
    }
    
    private String readAndShowLine(String fileDesc, BufferedReader reader) throws IOException {
        
        String line = reader.readLine();
        logger.info("Read from " + fileDesc + ": " + line);
        return line;       
        
    }


    /*
     * Put the dummy model source into the temp folder created by the per-test rule
     */
    private void setUpTempAreaWithModel(boolean isLegacy) throws IOException {

        File tempArea = tempFolder.getRoot();
        logger.info("Created temporary folder at " + tempArea.getAbsolutePath());       

        File dummyModelMaterial;

        if (isLegacy) {
            dummyModelMaterial = new File(RESOURCE_PATH + DUMMY_MODEL_DIR_LEGACY);
        }
        else {
            dummyModelMaterial = new File(RESOURCE_PATH + DUMMY_MODEL_DIR);
        }
        Assert.assertTrue("Can't find dummy model material which should be at "
                + dummyModelMaterial.getCanonicalPath(),
                dummyModelMaterial.exists() && dummyModelMaterial.isDirectory());        
        FileUtils.copyDirectory(
                dummyModelMaterial, 
                new File(tempArea.getAbsolutePath() + File.separator + DUMMY_MODEL_DIR),
                false);       // Don't preserve file dates

    }

    /*
     * Set up working copy of dummy model by creating a local SVN server, importing
     * into it and then checking-out. Return the dummy model's repo URI
     */
    private String setUpModelWorkingCopy(SVNClient svnClient) throws IOException, ClientException {

        File tempArea = tempFolder.getRoot();
        SVNRepos svnAdmin = new SVNRepos();

        svnAdmin.create(new File(tempArea.getAbsolutePath() + File.separator + LOCAL_SVN_REPO),  // Path
                false,              // Don't disable fysnc commit
                false,              // Don't keep log
                null,               // No config path
                SVNRepos.FSFS);     // Use FS-based repo

        // Java's File toURI (and related toURL or toURI().toURL()) return file URIs with
        // a single forward slash. JavaHL insists on the double slash form (+ extra slash that
        // starts the path)
        String repoModelURI = "file://" + tempArea.toURI().getRawPath()
                + LOCAL_SVN_REPO + "/" + DUMMY_MODEL_DIR;
        String dummyMaterialPath = tempArea.getAbsolutePath() + File.separator
                + DUMMY_MODEL_DIR;
        Assert.assertTrue(new File(dummyMaterialPath).exists());
        svnClient.doImport(dummyMaterialPath,                       // Path
                repoModelURI,                            // URL
                Depth.infinity,                          // Infinite depth
                false,                                   // Don't process ignores
                false,                                   // Ignore unknown types
                null,									// No revprops                      
                new CommitMessageCallback() {							
            @Override
            public String getLogMessage(Set<CommitItem> items) {
                return "Import dummy model";
            }
        },										// Commit msg callback
        new CommitCallback() {
            @Override
            public void commitInfo(CommitInfo commitInfo) {
                String errMsg = commitInfo.getPostCommitError();
                if (errMsg != null) {
                    throw new RuntimeException("SVN import failed with message "
                            + errMsg);
                }
                System.out.println("Successfully imported dummy model r"
                        + commitInfo.getRevision());
            }
        });						// Commit callback

        String wcPath = tempFolder.newFolder(WORKING_COPY_DIR).getAbsolutePath();
        long checkedOutRev = svnClient.checkout(repoModelURI,                       // 'Module name' (URL)
                wcPath,                             // Dest path
                null,                               // No specific rev
                null,                               // No specific peg rev
                Depth.infinity,                     // Infinite depth
                true,                               // Ignore externals
                false);                             // No unversioned paths
        Assert.assertEquals(1, checkedOutRev);      // Should be checking-out rev 1
        System.out.println("Successfully checked-out dummy model r" + checkedOutRev);

        return repoModelURI;

    }

    /*
     * Set up working copy of dummy model by creating a local SVN server, importing
     * into it and then checking-out. Return the dummy model's repo URI
     */
    private String setUpModelWorkingCopyLegacy(org.tigris.subversion.javahl.SVNClient svnClient)
            throws IOException,
            org.tigris.subversion.javahl.ClientException {

        File tempArea = tempFolder.getRoot();             
        org.tigris.subversion.javahl.SVNAdmin svnAdmin
        = new org.tigris.subversion.javahl.SVNAdmin();

        svnAdmin.create(tempArea.getAbsolutePath() + File.separator + LOCAL_SVN_REPO,  // Path
                false,              // Don't disable fysnc commit
                false,              // Don't keep log
                null,               // No config path
                org.tigris.subversion.javahl.SVNAdmin.FSFS);     // Use FS-based repo

        // Java's File toURI (and related toURL or toURI().toURL()) return file URIs with
        // a single forward slash. JavaHL insists on the double slash form (+ extra slash that
        // starts the path)
        String repoModelURI = "file://" + tempArea.toURI().getRawPath()
                + LOCAL_SVN_REPO + "/" + DUMMY_MODEL_DIR;
        String dummyMaterialPath = tempArea.getAbsolutePath() + File.separator
                + DUMMY_MODEL_DIR;
        Assert.assertTrue(new File(dummyMaterialPath).exists());
        svnClient.doImport(dummyMaterialPath,                       // Path
                repoModelURI,                            // URL
                "Import dummy model",                    // Message
                org.tigris.subversion.javahl.Depth.infinity,    // Infinite depth
                true,                                    // Don't process ignores
                false,                                   // Ignore unknown types
                null);                                   // No rev props


        String wcPath = tempFolder.newFolder(WORKING_COPY_DIR).getAbsolutePath();
        long checkedOutRev = svnClient.checkout(repoModelURI,                       // 'Module name' (URL)
                wcPath,                             // Dest path
                null,                               // No specific rev
                null,                               // No specific peg rev
                org.tigris.subversion.javahl.Depth.infinity,    // Infinite depth
                true,                               // Ignore externals
                false);                             // No unversioned paths
        Assert.assertEquals(1, checkedOutRev);      // Should be checking-out rev 1
        return repoModelURI;

    }

    /*
     * Commit the dummy model but not using the JSIT process
     */
    private void nonJSIT_CommitModel(SVNClient svnClient) throws ClientException {

        String wcPath = tempFolder.getRoot().getAbsolutePath() + File.separator
                + WORKING_COPY_DIR;
        String verPropsPath = wcPath + File.separator + "Code"
                + File.separator + "modelVersion.properties";

        HashSet<String> pathsSet = new HashSet<String>(1, 1.0f);
        pathsSet.add(verPropsPath);
        svnClient.propertySetLocal(pathsSet,      						// Paths
                ModelVersioningAssistantSVN.KEYWORDS_PROP,              // Prop name
                ModelVersioningAssistantSVN.KEYWORDS_VALUE.getBytes(),  // Prop value as byte array
                Depth.unknown,            // Depth not relevant for single file
                null,                     // No filtering changelists
                false);                   // Don't force

        pathsSet = new HashSet<String>(1, 1.0f);
        pathsSet.add(wcPath);
        svnClient.commit(pathsSet,  		// Paths set
                Depth.infinity,             // Infinite depth
                false,                      // Don't not unlock!
                false,                      // Don't keep changelist
                null,                       // No changelists
                null,						// No revprop mapping
                new CommitMessageCallback() {							
            @Override
            public String getLogMessage(Set<CommitItem> items) {
                return "Test Commit";
            }
        },							// Commit msg callback
        new CommitCallback() {
            @Override
            public void commitInfo(CommitInfo commitInfo) {
                String errMsg = commitInfo.getPostCommitError();
                if (errMsg != null) {
                    throw new RuntimeException("SVN commit failed with message "
                            + errMsg);
                }
                Assert.assertEquals(2, commitInfo.getRevision());
                System.out.println("Successfully committed new revision "
                        + commitInfo.getRevision());
            }
        });						// Commit callback

    }

    /*
     * Commit the dummy model but not using the JSIT process
     */
    private void nonJSIT_CommitModelLegacy(org.tigris.subversion.javahl.SVNClient svnClient)
            throws org.tigris.subversion.javahl.ClientException {

        String wcPath = tempFolder.getRoot().getAbsolutePath() + File.separator
                + WORKING_COPY_DIR;
        String verPropsPath = wcPath + File.separator + "Code"
                + File.separator + "modelVersion.properties";

        svnClient.propertySet(verPropsPath,             // Path
                ModelVersioningAssistantSVN.KEYWORDS_PROP,  // Prop name
                ModelVersioningAssistantSVN.KEYWORDS_VALUE, // Prop value
                org.tigris.subversion.javahl.Depth.unknown, // Depth not relevant for single file
                null,                     // No filtering changelists
                false,                    // Don't force
                null);                    // No revprop mapping on commit

        long commitResult = svnClient.commit(
                new String[] {wcPath},       // Working copy dir
                "Test commit",               // Commit msg
                org.tigris.subversion.javahl.Depth.infinity,   // Infinite depth
                false,                       // Don't not unlock!
                false,                       // Don't keep changelist
                null,                        // No changelists
                null);                       // No revprop mapping

        Assert.assertEquals(2, commitResult);

    }

}
