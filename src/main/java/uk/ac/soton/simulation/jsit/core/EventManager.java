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

    // Uses special events logger
    private static final Logger eventsLogger = LoggerFactory.getLogger(
            ModelInitialiser.EVENT_LOGGER_NAME);
    private static final Logger diagnosticsLogger = LoggerFactory.getLogger(
            EventManager.class);

    private final HashMap<Class<?>, LinkedList<EventReceiver>> generalHandlers;
    private final HashMap<EventSource<?>, LinkedList<EventReceiver>> sourceSpecificHandlers;

    public EventManager() {

        this.generalHandlers = new HashMap<Class<?>, LinkedList<EventReceiver>>();
        this.sourceSpecificHandlers = new HashMap<EventSource<?>, LinkedList<EventReceiver>>();

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

        // Log the event in the special logger if required
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
