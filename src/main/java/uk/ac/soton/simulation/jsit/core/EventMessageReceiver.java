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

/**
 * Abstract superclass for classes receiving JSIT events with explicit messages.
 * Typically the receiver will be a member class (typically non-static) of the parent (which also allows
 * objects to receive multiple message types).
 * 
 * @author Stuart Rossiter
 * @since 0.2
 */
public abstract class EventMessageReceiver<M> {

	private final Class<M> msgClass;
	
	protected EventMessageReceiver(Class<M> msgClass) {
		this.msgClass = msgClass;
	}
		
	public Class<M> getMessageClass() {
		return msgClass;
	}
	
	@SuppressWarnings("unchecked")
	public void process(Object msg) {
		processInternal((M) msg);
	}
	
	protected abstract void processInternal(M msg);
	
}
