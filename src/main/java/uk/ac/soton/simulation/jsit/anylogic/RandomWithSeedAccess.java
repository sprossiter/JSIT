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
 * Random subclass that keeps a copy of the seed so that it is accessible (and
 * so can be written out to promote experiment reproducibility). It also has the
 * option to allocate sequential seeds (from a fixed base) for each
 * instantiation, so that you can have AnyLogic multi-run experiments with
 * stochastic variation that are still all reproducible. (This is not possible
 * using the AnyLogic default, since setting a fixed seed applies that to all
 * runs, and thus there is no stochastic variation per run).
 * <p>
 * To use it, this class has to be instantiated as a custom generator in the
 * experiment's Randomness section.
 * <p>
 * If you use incremented seeds from a fixed base, AnyLogic's threading
 * strategies mean that the actual seed per run may not be what you expect. When
 * giving a base seed S in the experiment, then for
 * 
 * <ul>
 * <li>single run experiments: creates one Engine instance but with 2 RNGs
 * (because one is created per thread and the model is initialised in one thread
 * but runs in another). Run gets seed S+1</li>
 * 
 * <li>N-run multi-run non-parallel experiments: creates one Engine instance but
 * with N+1 RNGs. (I think this is one model initialisation thread, and then N
 * model-run threads). Runs get seeds S+1, S+2, ...</li>
 * 
 * <li>N-run multi-run parallel experiments: creates one Engine instance with C
 * (num cores) RNGs, and one separate Engine instance (even if there are &lt;C
 * runs). Runs get seeds S+C+1, S+C+2, ...</li>
 * 
 * </ul>
 * 
 * This also means that random samples during model initialisation may be using
 * a different RNG to those during model execution!
 * <p>
 * (Use the three-parameter constructor in your experiments to see this.)
 * 
 * @author Stuart Rossiter
 * @since 0.1
 */
public class RandomWithSeedAccess extends java.util.Random
                                  implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private static Long baseSeed = null;
    private static Long lastInstanceSeed = null;

    /**
     * Get the base seed used where you have created instances with incrementFromFixedBase
     * set to true (otherwise returns null).
     * 
     * @since 0.1
     * @return The base seed used.
     */
    public synchronized static Long getBaseSeed() {

        return baseSeed;

    }

    private final long seedCopy;

    /**
     * Create an instance with a 'random' seed; we actually use the current
     * system time in milliseconds, which will always vary between instantiations.
     * 
     * @since 0.1
     */
    public RandomWithSeedAccess() {

        this(System.currentTimeMillis());        // Chain to with-seed constructor
        //traceln("Thread " + Thread.currentThread().getId()
        //        + " called RandomWithSeedAccess no-parm constructor");

    }

    /**
     * Convenience constructor to create an instance with the specified seed.
     * This is the same as calling RandomWithSeedAccess(seed, false).
     * 
     * @since 0.1
     * @param seed The seed value.
     */
    public RandomWithSeedAccess(long seed) {

        this(seed, false, null);

    }
    
    /**
     * Create an instance with the given seed, specifying whether you want
     * future invocations to increment the actual seed from the one given here.
     * The latter allows you to have reproducible runs from an AnyLogic
     * multi-run experiment (e.g., Monte Carlo) whilst maintaining stochastic
     * variation per run, since the invocation code in the 'custom generator'
     * dialog will be re-called for each run.
     * <p>
     * See the notes at the top of this class for important information about
     * what incremented seeds will actually get applied for each run.
     * 
     * @since 0.2
     * @param seed
     *            The seed value.
     * @param incrementFromFixedBase
     *            Whether to use the seed value as a base for future invocations
     *            (where the seed value specified will be ignored).
     */
    public RandomWithSeedAccess(long seed, boolean incrementFromFixedBase) {
        
        this(seed, incrementFromFixedBase, null);
        
    }

    /**
     * Test-only version of the constructor where the relevant AnyLogic Engine
     * instance is passed in, and AnyLogic traceln calls are made to show you
     * information about the instantiation calls.
     * 
     * @since 0.2
     * @param seed
     *            The seed value. (Will be ignored if incrementFromFixedBase is
     *            true and this is not the first instantiation.)
     * @param incrementFromFixedBase
     *            Whether to increment the seeds used in each future
     *            instantiation from the seed value (as a fixed base).
     * @param _e
     *            A reference to the AnyLogic Engine. In an Experiment's
     *            randomness settings, this is available as _e or via the
     *            getEngine() call.
     */
    public RandomWithSeedAccess(long seed, boolean incrementFromFixedBase, Engine _e) {

        super(seed);    // Has to be first so reset the seed later if we need to change it
        if (_e != null) {
            Utilities.traceln("Thread " + Thread.currentThread().getId()
                    + " called RandomWithSeedAccess constructor with seed "
                    + seed + ", engine " + _e.toString());
            //for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
            //    Utilities.traceln(ste);
            //}
        }
        
        if (incrementFromFixedBase) {
            synchronized(RandomWithSeedAccess.class) {
                if (lastInstanceSeed == null) {
                    RandomWithSeedAccess.baseSeed = seed;
                }
                else {
                    if (lastInstanceSeed == Long.MAX_VALUE) {     // Wrap if we've got to max long val
                        seed = 1;
                    }
                    else {
                        seed = ++lastInstanceSeed;    // Just increment for each new instantiation
                    }                    
                    setSeed(seed);                    // Reset the seed
                }        
                RandomWithSeedAccess.lastInstanceSeed = seed;
            }
            Utilities.traceln("incrementFromFixedBase set: actual seed used is " + seed);
        }
        
        this.seedCopy = seed;

    }

    /**
     * Get the seed value used for this instance.
     * 
     * @since 0.1
     * @return The seed value.
     */
    public long getSeed() {

        return seedCopy;

    }

}
