/*  
    Copyright 2018 Stuart Rossiter
    
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

import java.util.List;

/**
 * Interface for all classes acting as sources of JSIT events with explicit messages
 * (rather than just an event type enum).
 * 
 * @author Stuart Rossiter
 * @since 0.2
 */
public interface EventMessageSource {

	/**
	 * Return the classes of all messages that the implementing class can produce.
	 * @return List of possible message classes.
	 */
	List<Class<?>> getMessageClasses();

}
