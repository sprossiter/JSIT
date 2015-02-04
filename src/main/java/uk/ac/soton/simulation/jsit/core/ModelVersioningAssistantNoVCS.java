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

import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * Model-versioning assistant where no-VCS is being used.
 * 
 * @author Stuart Rossiter
 * @since 0.2
 *
 */
public class ModelVersioningAssistantNoVCS extends ModelVersioningAssistant {

    /*
     * Non-public constructor.
     */
    ModelVersioningAssistantNoVCS(List<File> inVCS_SimCodePath,
                                  File modelVersionFile,
    				  PropertiesConfiguration versionProps) {

        super(inVCS_SimCodePath, modelVersionFile, versionProps);

    }

    /**
     * Last commit ID is effectively the model hash in this case (not using a VCS).
     */
    @Override
    public String getLastCommitID() {

        return getVersionProperties().getString(MODEL_HASH_PROPERTY);

    }

    /*
     * Code always considered 'checked-out' when no VCS.
     */
    @Override
    boolean codeIsCheckedOut(File codeDir) {

        return true;

    }

    /*
     * This is only ever called for the commit 'root' or the sim
     * code directories (where the latter are under the former).
     * In both cases, the code counts as changed in this no-VCS
     * case only if the sim code has changed (since that's all that's
     * covered by the pseudo-VCS-rev hash).
     */
    @Override
    boolean checkedOutCodeHasBeenChanged(List<File> checkDirs) {

        if (simHasBeenJSIT_Committed()) {
            return !simSourceHash.equals(getCommittedCodeHash());
        }
        else {
            return false;    // Want it to be treated as a first JSIT commit
        }

    }

    /* 
     * Nothing to do; superclass does all the work.
     */
    @Override
    void doCommit(File sourceRootDir, String changeNotes) {

        // Nothing to do

    }

    /*
     * Is only called if a location property was in the file,
     * which it shouldn't be!
     */
    @Override
    String repoLocationAsURL(String location) {

        throw new IllegalArgumentException("When not using a VCS, no "
                + VERSION_FILE_REPO_URL_PROPERTY
                + " property should exist in the model version file");

    }

    /*
     * For a no-VCS case, only files in the sim source directories
     * count as under VC (i.e., covered by the pseudo-rev-ID hash).
     */
    @Override
    boolean fileIsUnderVersionControl(File checkFile) {

        return isInDirectoryList(inVCS_SimCodeDirs, checkFile);

    }

}
