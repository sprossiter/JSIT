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

import java.io.Serializable;
import java.util.*;

import com.thoughtworks.xstream.XStream;

//import org.slf4j.*;

/**
 * A class for storing distributions which can be looked-up via
 * a set of enum-based dimensions. Any stochastic overrides will apply to the lookup (only),
 * and will thus apply implicitly to all distributions therein (i.e., you cannot set different
 * overrides per distribution in the lookup).
 * 
 * @author Stuart Rossiter
 * @since 0.1
 */	
public class DistLookupByEnums<D extends Distribution>
                    extends StochasticItem implements Serializable {

    // ************************ Static Fields *****************************************

    //private static final Logger logger
    //		= LoggerFactory.getLogger(DistLookupByEnums.class.getCanonicalName());

    private static final long serialVersionUID = 1L;


    // ************************* Static Methods **************************************

    static void setupForInfoSerialisation(XStream xstream) {

        xstream.alias("distLookupByEnums", DistLookupByEnums.class);
        MultiDimEnumMap.setupForInfoSerialisation(xstream);

    }

    // ************************ Instance Fields ***************************************

    /*
     * Internally, this class is just a simple wrapper around MultiDimEnumMap
     */
    private final MultiDimEnumMap<D> multiDimMap;

    // ************************* Constructors *****************************************

    /*
     * Supply the set of enum classes (as Class objects) which specify the (ordered)
     * dimensions of the lookup
     */
    public DistLookupByEnums(Class<?>... dimEnumClasses) {

        super();
        this.multiDimMap = new MultiDimEnumMap<D>(dimEnumClasses);

    }


    // ************************* Public Instance Methods ******************************

    /*
     * Add a given distribution to the lookup for the location specified by the given
     * set of enum-instance indices. Distributions can be replaced once added (by re-adding)
     */
    public void addDist(D dist, Object... locationEnumIdx) {

        multiDimMap.addValue(dist, locationEnumIdx);

    }

    /*
     * Retrieve a distribution from the lookup from the location specified by the given set of
     * enum-instance indices
     */
    public D getDist(Object... locationEnumIdx) {

        return multiDimMap.getValue(locationEnumIdx);

    }

    public List<D> getAllDists() {

        return multiDimMap.getAllValues();

    }

    /*
     * Override of toString() to show the contents as a 'flattened' list, using the toString()
     * implementations of the distributions. There may be null contents if distributions
     * have not yet been added for a location
     */
    @Override
    public String toString() {

        return multiDimMap.toString();

    }


    /*
     * Override accessor registration to also register for all the lookup's dists
     */
    @Override
    public void registerAccessor(AbstractStochasticAccessor<?> accessor) {

        super.registerAccessor(accessor);

        List<D> allDists = getAllDists();
        for (D dist : allDists) {
            if (dist != null) {				// May be null entries
                dist.registerAccessor(accessor);
            }
        }

    }

    /*
     * Override sampler registration to also register for all the lookup's dists
     */
    @Override
    public void registerSampler(Sampler sampler) {

        super.registerSampler(sampler);

        List<D> allDists = getAllDists();
        for (D dist : allDists) {
            if (dist != null) {				// May be null entries
                dist.registerSampler(sampler);
            }
        }

    }

}
