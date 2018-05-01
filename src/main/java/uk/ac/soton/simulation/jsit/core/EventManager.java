/*  
    Copyright 2018 University of Southampton, Stuart Rossiter
    
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

import org.slf4j.*;

import java.util.*;

import org.slf4j.LoggerFactory;

/**
 * Simple manager for publish/subscribe events (a flavour of the Observer design
 * pattern). Published events can also optionally be logged to a special events
 * logger which allows, for example, a 'narrative' type file of real-world-domain-
 * meaningful events to be kept
 * 
 * @author Stuart Rossiter
 * @since 0.1
 */
public class EventManager {

    // Uses special events logger (as well as general one for diagnostic messages)
    private final Logger eventsLogger;
    private final Logger diagnosticsLogger;

    private final Map<Class<?>, LinkedList<EventReceiver>> generalHandlers
    					= new HashMap<Class<?>, LinkedList<EventReceiver>>();
    private final Map<EventSource<?>, LinkedList<EventReceiver>> sourceSpecificHandlers
    					= new HashMap<EventSource<?>, LinkedList<EventReceiver>>();

    @Deprecated
    /**
     * No-arguments constructor.
     * @since 0.1
     * 
     * Deprecated because we want to ensure we use the JSIT-initialised Logback
     * logger and not anything else already bound to SLF4J.
     */
    public EventManager() {

    	eventsLogger = LoggerFactory.getLogger(ModelInitialiser.EVENT_LOGGER_NAME);
        diagnosticsLogger = LoggerFactory.getLogger(EventManager.class);

    }
    
    /**
     * Constructor.
     * @since 0.2
     * 
     * @param modelInitialiser The model initialiser.
     */
    public EventManager(ModelInitialiser modelInitialiser) {
    	
    	eventsLogger = modelInitialiser.getLogbackContext().getLogger(
    									ModelInitialiser.EVENT_LOGGER_NAME);
    	diagnosticsLogger = modelInitialiser.getLogbackContext().getLogger(
    									EventManager.class);
    	
    }

    public void register(Class<?> sourceClass, EventReceiver receiver) {

        LinkedList<EventReceiver> classReceivers = generalHandlers.get(sourceClass);

        if (classReceivers == null) {
            classReceivers = new LinkedList<EventReceiver>();
            generalHandlers.put(sourceClass, classReceivers);
        }

        // Not an error if try to register twice
        if (!classReceivers.contains(receiver)) {
            classReceivers.add(receiver);
        }

    }

    public void register(EventSource<?> eventSource, EventReceiver receiver) {

        LinkedList<EventReceiver> sourceReceivers = sourceSpecificHandlers.get(eventSource);

        if (sourceReceivers == null) {
            sourceReceivers = new LinkedList<EventReceiver>();
            sourceSpecificHandlers.put(eventSource, sourceReceivers);
        }

        // Not an error if try to register twice
        if (!sourceReceivers.contains(receiver)) {
            sourceReceivers.add(receiver);
        }

    }

    /*
     * Publish an event from an EventSource. We call source-instance-specific handlers
     * first (in order of registration) and then source-class-specific ones. (It makes
     * sense that more 'targetted' handlers run first.)
     */
    public void publish(EventSource<?> source, String eventsLogMsg) {

        // Log the event in the special logger if required (i.e., if a message was
    	// provided). The Logback configuration ensures the event msg is also included
    	// in the diagnostics log
        if (eventsLogMsg != null) {
            eventsLogger.info(eventsLogMsg);
        }
        
        LinkedList<EventReceiver> receivers = sourceSpecificHandlers.get(source);

        if (receivers != null) {
            for (EventReceiver r : receivers) {
                r.notifyOfEvent(source);
            }
        }

        receivers = generalHandlers.get(source.getEventSourceClass());

        if (receivers != null) {
            for (EventReceiver r : receivers) {
                r.notifyOfEvent(source);
            }
        }        

    }

    /*
     * Deregister receiver for a domain event source class. Warning if not
     * currently registered
     */
    public void deregister(Class<?> sourceClass, EventReceiver receiver) {

        boolean wasRegistered;
        LinkedList<EventReceiver> receivers = generalHandlers.get(sourceClass);
        if (receivers != null) {
            wasRegistered = receivers.remove(receiver);
        }
        else {
            wasRegistered = false;
        }

        if (!wasRegistered) {
            diagnosticsLogger.warn("Type " + receiver.getClass().getSimpleName()
                    + " domain event receiver was not registered for source class "
                    + sourceClass.getSimpleName() + "; deregistration ignored");
        }

    }

    public void deregister(EventSource<?> source, EventReceiver receiver) {

        boolean wasRegistered;
        LinkedList<EventReceiver> receivers = sourceSpecificHandlers.get(source);
        if (receivers != null) {
            wasRegistered = receivers.remove(receiver);
        }
        else {
            wasRegistered = false;
        }

        if (!wasRegistered) {
            diagnosticsLogger.warn("Type " + receiver.getClass().getSimpleName()
                    + " domain event receiver was not registered for source class "
                    + source.getEventSourceClass().getSimpleName()
                    + "; deregistration ignored");
        }

    }

}
