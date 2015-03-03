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
import org.slf4j.*;

/**
 * Unit tests for DistLookupByEnums.
 * 
 * @author Stuart Rossiter
 * @since 0.2
 */
public class TestDistLookupByEnums {

    private static Logger logger = LoggerFactory.getLogger(TestDistLookupByEnums.class);
    
    private static enum Dim1 { A1, A2, A3 };
    private static enum Dim2 { B1, B2 };
    private static enum Dim3 { C1, C2 };   

    /**
     * Test for exception with not-all-enums dims.
     */
    @Test
    public void nonEnumDims() {

        logger.info("Testing for handling of non-enum dims");

        try {
            new DistLookupByEnums<DistBernoulli>(Dim1.class, Integer.class);
            Assert.fail();
        }
        catch (IllegalArgumentException e) {
            // OK, expected
        }

    }
    
    @Test
    public void addAndAccess() {
        
        logger.info("Testing correct creation, population and access of dist lookup");
        
       DistLookupByEnums<DistBernoulli> lookup
               = new DistLookupByEnums<DistBernoulli>(Dim1.class, Dim2.class, Dim3.class);
       // Should be null-filled to start with
       Assert.assertNull(lookup.getDist(Dim1.A1, Dim2.B1, Dim3.C1));
       DistBernoulli dist = new DistBernoulli(0.5);
       lookup.addDist(dist, Dim1.A1, Dim2.B2, Dim3.C1);
       Assert.assertEquals(dist, lookup.getDist(Dim1.A1, Dim2.B2, Dim3.C1));
       // Adds same object cf. a clone
       lookup.addDist(dist, Dim1.A3, Dim2.B1, Dim3.C1);
       DistBernoulli retrievedDist = lookup.getDist(Dim1.A3, Dim2.B1, Dim3.C1);
       Assert.assertEquals(dist, retrievedDist);
       dist.setP(0.8);
       retrievedDist = lookup.getDist(Dim1.A3, Dim2.B1, Dim3.C1);
       Assert.assertEquals(0.8, retrievedDist.getP(), 0.001);
       retrievedDist = lookup.getDist(Dim1.A1, Dim2.B2, Dim3.C1);
       Assert.assertEquals(0.8, retrievedDist.getP(), 0.001);      
       
    }

}
