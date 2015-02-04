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

/**
 * Generic JSIT exception for problems using a version control system. Will
 * have a related root-cause exception (relating to the specific VCS used) if
 * exception is a result of a VCS operation.
 * 
 * @author Stuart Rossiter
 * @since 0.2
 */
public class VersionControlException extends ModelVersioningException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor where there is no root cause exception.
     * @param message
     * The exception message
     */
    public VersionControlException(String message) {

        super(message);

    }
    /**
     * Constructor where a root cause exception exists.
     * @param message
     * The exception message
     * @param cause
     * The root cause issue (Throwable)
     */
    public VersionControlException(String message, Throwable cause) {

        super(message, cause);

    }

}
