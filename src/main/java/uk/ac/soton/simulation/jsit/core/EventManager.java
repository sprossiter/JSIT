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
 * meaningful events to be kept.
 * 
 * This allows for two alternative architectures to be used (which can be freely mixed if desired):
 * <dl>
 * <dt>Marker-Based Events</dt>
 * <dd>The receivers just receive a 'marker' that an event has occurred (with useful human-readable
 * detail in a log message), and then query the event source object to get the
 * information they need (requiring those objects/agents to have the relevant information available,
 * and the caller to know about these model classes). The markers are implemented as the event sources
 * providing an source-class-specific enum and a method returning the enum that indicates the event
 * alternative that just occurred. (A given model class may generate any number of alternative events.)</dd>
 * <dt>Message-Class Events</dt>
 * <dd>Have messages (objects of event-source-specific message classes) sent to receivers which may
 * contain either all required information (this isolating receivers from anything other than the message
 * classes), and/or contain references to model classes (or informational interfaces for them) to obtain
 * further information.</dd>
 * </dl>
 * 
 * @author Stuart Rossiter
 * @since 0.1
 */
public class EventManager {

    // Uses special events logger (as well as general one for diagnostic messages)
    private final Logger eventsLogger;
    private final Logger diagnosticsLogger;

    // Data structures for enum-based events, covering general handlers (receivers receiving all
    // messages from a source class) and source-specific ones (receivers receiving all messages
    // only from specific source objects).
    private final Map<Class<?>, LinkedList<EventReceiver>> generalHandlers
    					= new HashMap<Class<?>, LinkedList<EventReceiver>>();
    private final Map<EventSource<?>, LinkedList<EventReceiver>> sourceSpecificHandlers
    					= new HashMap<EventSource<?>, LinkedList<EventReceiver>>();
    
    // Data structure for event message subscriptions. Maps the message class to a map of receivers to
	// a list of specific source objects. (If this list is null, that signifies that this receiver receives
	// messages of that class from any source object.)
	private final Map<Class<?>, Map<EventMessageReceiver<?>, List<EventMessageSource>>> eventMsgSubscriptionsMap
				= new HashMap<Class<?>, Map<EventMessageReceiver<?>, List<EventMessageSource>>>();

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
    
    /* ------------------------- MARKER-BASED EVENTS --------------------------------- */

    /**
     * Register (subscribe) a receiver for marker-based events from a given source class
     * (i.e., from all instances of that class).
     * @since 0.1
     * 
     * @param sourceClass The source class.
     * @param receiver The receiver for the events.
     */
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

    /**
     * Register (subscribe) a receiver for marker-based events from a given source object.
     * @since 0.1
     * 
     * @param eventSource The source object.
     * @param receiver The receiver for the events.
     */
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

    /**
     * Publish a marker-based event from an EventSource. We call source-instance-specific handlers
     * first (in order of registration) and then source-class-specific ones. (It makes
     * sense that more 'targeted' handlers run first.)
     * 
     * @since 0.1
     * 
     * @param source The source object.
     * @param eventsLogMsg Log message for the events log. (Nothing will be logged if null.) 
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

    /**
     * Deregister receiver for marker-based events from a source class. Warning if not
     * currently registered.
     * 
     * @since 0.1
     * 
     * @param sourceClass The source class.
     * @param receiver The receiver to deregister.
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

    /**
     * Deregister receiver for marker-based events from a source object. Warning if not
     * currently registered.
     * 
     * @since 0.1
     * 
     * @param source The source object.
     * @param receiver The receiver to deregister.
     */
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
    
    /* ------------------------- MESSAGE-CLASS EVENTS --------------------------------- */
    
	/**
	 * Register (subscribe) to receive all event message objects for a particular source class.
	 * 
	 * @since 0.2
	 * 
	 * @param messageClass The class of the message.
	 * @param receiver The receiver for the messages.
	 * 
	 * This will override any previous source-specific subscriptions.
	 */
	public void register(Class<?> messageClass, EventMessageReceiver<?> receiver) {
		
		register(messageClass, null, receiver);
		
	}
	
	/**
	 * Register (subscribe) to receive all event message objects for a particular source class
	 * and a particular source object.
	 * 
	 * @since 0.2
	 * 
	 * @param messageClass The class of the message.
	 * @param specificSource The specific source object.
	 * @param receiver The receiver for the messages.
	 * 
	 * This will override any previous subscription for this receiver to receive event message objects
	 * from all source class instances.
	 */
	public void register(Class<?> messageClass, EventMessageSource specificSource, EventMessageReceiver<?> receiver) {
		
		Map<EventMessageReceiver<?>, List<EventMessageSource>> classSubscribersMap = eventMsgSubscriptionsMap.get(messageClass);
		if (classSubscribersMap == null) {		// First receiver for this message class
			classSubscribersMap = new HashMap<EventMessageReceiver<?>, List<EventMessageSource>>();
			
			eventMsgSubscriptionsMap.put(messageClass, classSubscribersMap);
		}
		
		List<EventMessageSource> specificSources;
		
		if (specificSource == null) {
			specificSources = classSubscribersMap.put(receiver, null);
			// TODO: WARN if previously non-null value
			return;
		}
				
		if (classSubscribersMap.containsKey(receiver)) {
			specificSources = classSubscribersMap.get(receiver);
			if (specificSources == null) {
				// TODO: WARN that overwriting receive-all setting
			}
		} else {
			specificSources = new ArrayList<EventMessageSource>();
			classSubscribersMap.put(receiver, specificSources);
		}
		
		specificSources.add(specificSource);
		
	}
	
	/**
	 * Publish a particular event message object (which will be passed on to any subscribers
	 * via their EventMessageReceiver interface).
	 * 
	 * @since 0.2
	 * 
	 * @param msg The message.
	 * @param source The message source.
	 * @param logMessage Whether to log the message as well. (The message's toString() method
	 * 					 provides the logged version of the message.)
	 */
	public void publish(Object msg,
						EventMessageSource source,
						boolean logMessage) {
		
		// Log the event in the special logger if requested. The Logback configuration
		// ensures the event message is also included in the diagnostics log
        if (logMessage) {
            eventsLogger.info(msg.toString());
        }
        
		Map<EventMessageReceiver<?>, List<EventMessageSource>> classSubscribersMap
							= eventMsgSubscriptionsMap.get(msg.getClass());
		
		if (classSubscribersMap == null) {
			return;
		}
		
		for (EventMessageReceiver<?> r : classSubscribersMap.keySet()) {
			List<EventMessageSource> specificSources = classSubscribersMap.get(r);
			if (specificSources == null || specificSources.contains(source)) {
				r.process(msg.getClass().cast(msg));
			}
		}
	
	}

}
