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

import uk.ac.soton.simulation.jsit.core.RunEnvironmentSettings;

/**
 * Package-access class used to store run environment settings for serialisation via XStream to
 * model settings file. This extends the generic settings to include AnyLogic specific ones.
 * 
 * @author Stuart Rossiter
 * @since 0.1
 */
class RunEnvironmentSettingsAnyLogic extends RunEnvironmentSettings {


    // ************************ Static Member Classes **********************************

    static class RandomnessSettings {
        final String seed;
        final String baseSeed;
        private RandomnessSettings(String seed, String baseSeed) {
            this.seed = seed;
            this.baseSeed = baseSeed;
        }
    };


    // ************************* Instance Fields ***************************************
    // Package-access just to prevent compiler non-use warnings

    final RandomnessSettings randomnessSettings;
    final String anyLogicVersion = "UNKNOWN";        // No API to get it. TODO: get from .alp file header


    // ************************** Constructors *****************************************

    /*
     * Package-access constructor
     */
    RunEnvironmentSettingsAnyLogic(MainModel_AnyLogic mainModel) {

        super(mainModel);

        if (mainModel.getEngine().getDefaultRandomGenerator() instanceof RandomWithSeedAccess) {
            RandomWithSeedAccess rng = (RandomWithSeedAccess)
                    mainModel.getEngine().getDefaultRandomGenerator();
            Long baseSeed = RandomWithSeedAccess.getBaseSeed();
            this.randomnessSettings = new RandomnessSettings(
                    Long.toString(rng.getSeed()),
                    baseSeed == null ? "N/A" : Long.toString(baseSeed));
        }
        else {
            this.randomnessSettings = null;
        }

    }

}
