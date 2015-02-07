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

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * An enum-dimensions-indexed lookup map ('field') for a given
 * type entry. Used internally (so package-access only) to provide DistLookupByEnums.
 * 
 * @author Stuart Rossiter
 * @since 0.1
 */    
@SuppressWarnings({ "rawtypes", "unchecked" })
class MultiDimEnumMap<V> {

    // ************************* Static Fields **************************************

    private static class MultiDimEnumMapConverter implements Converter {

        private static final Logger logger
                = LoggerFactory.getLogger(MultiDimEnumMapConverter.class);

        public boolean canConvert(Class clazz) {
            return MultiDimEnumMap.class == clazz;
        }

        public void marshal(Object value,
                            HierarchicalStreamWriter writer,
                            MarshallingContext context) {
            logger.debug("Serialising multi-dim enum map");
            assert (value instanceof MultiDimEnumMap);
            MultiDimEnumMap m = (MultiDimEnumMap) value;
            for (Class d : m.getDimEnums()) {
                logger.debug("  Dim " + d.getSimpleName());
                writer.startNode("dimensionEnum");
                writer.setValue(d.getCanonicalName());
                writer.endNode();
            }
            writer.startNode("values");
            for (Object d : m.getAllValues()) {
                writer.startNode("entry");
                if (d == null) {        // Nulls possible
                    logger.debug("  Null");
                    writer.addAttribute("type", "null");                    
                }
                else {
                    assert d instanceof Distribution;
                    logger.debug("  Distribution " + d.toString());
                    writer.addAttribute("type", d.getClass().getSimpleName());
                    context.convertAnother(d);        // As per dist class serialisation
                }
                writer.endNode();
            }            
            writer.endNode();   
        }

        public Object unmarshal(HierarchicalStreamReader reader,
                                UnmarshallingContext context) {
            //TODO: Add unmarshal logic
            throw new UnsupportedOperationException();
        }

    };


    // ************************* Static Methods **************************************

    static void setupForInfoSerialisation(XStream xstream) {

        xstream.alias("multiDimEnumMap", MultiDimEnumMap.class);
        xstream.registerConverter(new MultiDimEnumMapConverter());

    }

    // ************************* Instance Fields **************************************

    private final LinkedList<Class> dims;
    private final EnumMap multiDimEnumMap;


    // ************************** Constructors ****************************************

    /*
     * Create multi-dim mapping with the set of enum classes given as the indexing dimensions.
     * (Each Class in the parameter should be an enum; IllegalArgumentException otherwise.)
     * The map will be filled with nulls as part of this instantiation
     */
    MultiDimEnumMap(Class... dimEnumClasses) {

        this.dims = new LinkedList<Class>();

        if (dimEnumClasses.length == 0) {
            throw new IllegalArgumentException("No dimensions specified for lookup");
        }

        for (int i = 0; i < dimEnumClasses.length; i++) {
            if (dimEnumClasses[i].getEnumConstants() == null) { 
                throw new IllegalArgumentException("Dimension category " + (i+1) + " is not an enum");
            }
            else {
                if (dimEnumClasses[i].getEnumConstants().length == 0) {
                    throw new IllegalArgumentException("Dimension category " + (i+1) + " has no values");
                }
            }            
            dims.add(dimEnumClasses[i]);    
        }

        this.multiDimEnumMap = createNullFilledMap(dimEnumClasses[0]);
        for (int i = 1; i < dimEnumClasses.length; i++) {       
            // Create new dim array and add copies of it to all current 'leaf nodes' via recursion
            EnumMap newDimMaster = createNullFilledMap(dimEnumClasses[i]);
            walkOrAddDim(multiDimEnumMap, newDimMaster);
        }

    }


    // ********************* Public Instance Methods *********************************

    /*
     * Add a value to a location in the map defined by a set of enum instances (given in the
     * same order as used to define the multi-dim map)
     */
    public void addValue(V value, Object... locationEnumIdx) {

        checkLocationEnumIdx(locationEnumIdx);
        walkOrAddLeafObj(multiDimEnumMap, locationEnumIdx, 0, value);

    }

    /*
     * Get a value from a location in the map defined by a set of enum instances (given in the
     * same order as used to define the multi-dim map)
     */
    public V getValue(Object... locationEnumIdx) {

        checkLocationEnumIdx(locationEnumIdx);
        return walkOrGetLeafObj(multiDimEnumMap, locationEnumIdx, 0);

    }

    public Class[] getDimEnums() {

        return dims.toArray(new Class[dims.size()]);

    }

    public List<V> getAllValues() {

        List<V> entriesList = new LinkedList<V>();
        walkOrGatherEntries(multiDimEnumMap, entriesList);
        return entriesList;

    }


    /*
     * Print as a 'flattened' array of entries. There will be null entries where values have not
     * been added via addValue
     */
    @Override
    public String toString() {

        StringBuilder content = new StringBuilder();
        content.append("[");
        walkOrPrintDist(multiDimEnumMap, content);
        // An extraneous ", " will exist at the end
        content.replace(content.length() - 2, content.length(), "]");
        return content.toString();

    }


    // ********************** Private Instance Methods *********************************

    private void walkOrAddDim(EnumMap currSubMap, EnumMap newDimMaster) {

        for (Object currKey : currSubMap.keySet()) {
            Object currEntry = currSubMap.get(currKey);
            if (currEntry == null) {        // At a 'leaf node'; add new dim array
                currSubMap.put(currKey, newDimMaster.clone());        // Shallow copy is fine for null array
            }
            else {                            // Recurse through next dimension
                assert currEntry instanceof EnumMap;
                walkOrAddDim((EnumMap) currEntry, newDimMaster);
            }
        }

    }

    private void walkOrPrintDist(EnumMap currSubMap, StringBuilder content) {

        for (Object currKey : currSubMap.keySet()) {
            Object currEntry = currSubMap.get(currKey);
            if (currEntry == null) {                        // Dist not yet added
                content.append("null, ");
            }
            else if (currEntry instanceof EnumMap) {        // Recurse through next dimension
                walkOrPrintDist((EnumMap) currEntry, content);
            }
            else  {                                            // At a 'leaf node'; add to string rep
                content.append(currEntry.toString());               
                content.append(", ");
            }            
        }      

    }

    private void walkOrGatherEntries(EnumMap currSubMap, List<V> entriesList) {

        for (Object currKey : currSubMap.keySet()) {
            Object currEntry = currSubMap.get(currKey);
            if (currEntry == null) {                        // Dist not yet added
                entriesList.add(null);
            }
            else if (currEntry instanceof EnumMap) {        // Recurse through next dimension
                walkOrGatherEntries((EnumMap) currEntry, entriesList);
            }
            else  {                                            // At a 'leaf node'; add to string rep
                entriesList.add((V) currEntry);
            }            
        }      

    }

    /*
     * Recursive method to add a D 'leaf object' to the multi-dimensional EnumMap, where the object
     * needs to go at a 'location' (array index) e.g. (1,0,4). The current location index tracks which
     * dimension we're currently on (e.g., if this is 1, we're on the second dimension of three)
     */
    private void walkOrAddLeafObj(EnumMap currSubMap,
                                  Object[] newObjLocation,
                                  int currLocationIdx,
                                  V leafObj) {

        if (currLocationIdx == newObjLocation.length - 1) {            // We've got to our slot
            currSubMap.put(newObjLocation[newObjLocation.length - 1], leafObj);
        }
        else {                                                        // Recurse to the appropriate sub-array
            walkOrAddLeafObj((EnumMap) currSubMap.get(newObjLocation[currLocationIdx]),
                    newObjLocation, ++currLocationIdx, leafObj);
        }       

    }

    private V walkOrGetLeafObj(EnumMap currSubMap,
                               Object[] reqObjLocation,
                               int currLocationIdx) {

        if (currLocationIdx == reqObjLocation.length - 1) {            // We've got to our slot
            return (V) currSubMap.get(reqObjLocation[reqObjLocation.length - 1]);
        }
        else {                                                        // Recurse to the appropriate sub-array
            return walkOrGetLeafObj((EnumMap) currSubMap.get(reqObjLocation[currLocationIdx]),
                    reqObjLocation, ++currLocationIdx);
        }       

    }

    private EnumMap createNullFilledMap(Class enumClass) {

        EnumMap map = new EnumMap(enumClass);
        Object[] enumVals = enumClass.getEnumConstants();

        for (Object o : enumVals) {
            map.put(o, null);
        }
        return map;

    }

    private void checkLocationEnumIdx(Object... locationEnumIdx) {

        if (locationEnumIdx.length == 0) {
            throw new IllegalArgumentException("No dimensions specified for lookup");
        }

        for (int i = 0; i < locationEnumIdx.length; i++) {
            if (locationEnumIdx[i].getClass() != dims.get(i)) {
                throw new IllegalArgumentException("Lookup dimension " + (i + 1) + " should be "
                        + dims.get(i).getSimpleName());
            }
        }

    }

}

