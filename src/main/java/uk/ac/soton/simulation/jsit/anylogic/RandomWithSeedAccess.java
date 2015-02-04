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

//import org.slf4j.*;

import com.anylogic.engine.Engine;
import com.anylogic.engine.Utilities;

/**
 * Random subclass that keeps a copy of the seed so that it is accessible
 * (and so can be written out to promote experiment reproducibility), and allocates sequential
 * seeds for each instantiation (as AnyLogic does internally for multi-run experiments).
 * This class has to be specified in the experiment's Randomness section.
 * 
 * <p>AnyLogic does strange things internally. When giving a seed S in the experiment:
 * 
 * <ul>
 * <li>single run experiments: creates one Engine instance but with 2 RNGs (presumably one for the Experiment).
 * Run gets seed S+1</li>
 * 
 * <li>N-run multi-run non-parallel experiments: creates one Engine instance but with N+1 RNGs
 * (yet only N calls to the 2-parameter constructor show up, so presumably 1 is using the 1-parameter
 * one unlike in the single run case!). Runs get seeds S+1, S+2, ...</li>
 *   
 * <li>N-run multi-run parallel experiments: creates one Engine instance with C (num cores) RNGs,
 * and one separate Engine instance (even if there are &lt;C runs). Runs get seeds S+C+1, S+C+2, ...</li>
 * 
 * </ul>
 * 
 * (Use the two-parameter constructor in your experiments to see this.)
 * 
 * @author Stuart Rossiter
 * @since 0.1
 */	
public class RandomWithSeedAccess extends java.util.Random
                                  implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    // Guarded copy of base seed and last instance's seed so that the seed can be varied for multi-run
    // experiments. (The seed entered on the experiment config screen is used to generate per-run
    // seeds for each run. AnyLogic does strange things internally, also generating 5 other RNG
    // instances for 5 Engine instances before setting up runs. Each run gets its own RNG, and reuses 
    // one of the 5 Engine instances for the first 5 runs.)

    private static Long baseSeed = null;
    private static Long lastInstanceSeed = null;

    public synchronized static long getBaseSeed() {

        if (baseSeed == null) {
            throw new IllegalStateException("Can only get base seed after a run has been setup");
        }
        return baseSeed;

    }

    private final long seedCopy;

    /**
     * Default constructor (where we want a 'random' seed)
     */
    public RandomWithSeedAccess() {

        this(System.currentTimeMillis());		// Chain to with-seed constructor
        //traceln("Thread " + Thread.currentThread().getId()
        //		+ " called RandomWithSeedAccess no-parm constructor");

    }

    /**
     * With seed constructor, as called by AnyLogic code when a seed is used.
     * For multi-run experiments, runs after the first use seeds that are increments
     * of the initial one (the argument to this constructor).
     */
    public RandomWithSeedAccess(long seed) {

        this(null, seed);						// Chain to engine plus seed constructor
        //traceln("Thread " + Thread.currentThread().getId()
        //		+ " called RandomWithSeedAccess one-parm constructor with seed " + seed);

    }

    /**
     * Testing version of the constructor: call with an Engine instance (getEngine() in
     * an Experiment or Agent) to see the different Engine instances RNGs are assigned to
     */ 
    public RandomWithSeedAccess(Engine _e, long seed) {

        super(seed);	// Has to be first so reset the seed later if we need to change it
        if (_e != null) {
            Utilities.traceln("Thread " + Thread.currentThread().getId()
                    + " called RandomWithSeedAccess two-parm constructor with seed "
                    + seed + ", engine " + _e.toString());
            for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
                Utilities.traceln(ste);
            }
        }
        synchronized(RandomWithSeedAccess.class) {
            if (lastInstanceSeed == null) {
                RandomWithSeedAccess.baseSeed = seed;
            }
            else {
                if (lastInstanceSeed == Long.MAX_VALUE) {		// Wrap if we've got to max long val
                    seed = 1;
                }
                else {
                    seed = ++lastInstanceSeed;	// Just increment for each new instantiation
                }
                setSeed(seed);					// Reset the seed
            }
            this.seedCopy = seed;
            RandomWithSeedAccess.lastInstanceSeed = seed;
        }

    }

    public long getSeed() {

        return seedCopy;

    }

}
