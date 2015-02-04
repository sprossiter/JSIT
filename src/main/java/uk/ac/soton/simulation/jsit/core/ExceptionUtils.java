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

import org.slf4j.MDC;

/**
 * Utilities for handling JSIT exceptions.
 * 
 * @author Stuart Rossiter
 * @since 0.1
 */
public class ExceptionUtils {

    private static class JSIT_Exception extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public JSIT_Exception(String msgSuffix, Throwable cause) {
            super(cause.getMessage() + ": " + msgSuffix, cause);
        }

    };

    public static void throwWithThreadData(Throwable t) {

        throw new JSIT_Exception(
                "Thread " + Thread.currentThread().getName() + " ("
                        + Thread.currentThread().getId() + "), MDC run ID "
                        + MDC.get(ModelInitialiser.RUN_ID_KEY),
                        t);

    }

}
