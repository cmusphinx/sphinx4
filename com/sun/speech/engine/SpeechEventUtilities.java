/**
 * Copyright 1998-2003 Sun Microsystems, Inc.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package com.sun.speech.engine;

import javax.speech.SpeechEvent;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.Component;
import java.security.AccessControlException;

/**
 * Utilities to help with dispatch JSAPI 1.0 events on the event
 * dispatching thread of AWT/Swing.  This is needed to help
 * applications conform with the Swing Event Thread model.  If these
 * utilities were not used, then a GUI application would have to
 * implement Runnables to handle JSAPI events that result in updates
 * to the GUI.
 */
public class SpeechEventUtilities {

    /**
     * If true, the AWT EventQueue has been set up in the VM.  This flag
     * is used to determine whether we should use the AWT EventQueue for
     * synchronizing SpeechEvents with the AWT EventQueue or not.
     */
    protected static boolean awtRunning = false;
    
    /**
     * The AWT EventQueue.  This is lazily created in postSpeechEvent to
     * delay the need to initialize the Toolkit until it is necessary.
     *
     * @see #postSpeechEvent
     */
    protected static EventQueue systemEventQueue = null;

    /**
     * A target used to process speechAWTEvent objects.  This target
     * is a component that expresses interest in SpeechAWTEvents.  It
     * is lazily created along with systemEventQueue in postSpeechEvent.
     *
     * @see #postSpeechEvent
     */
    protected static SpeechAWTEventTarget speechAWTEventTarget = null;

    /**
     * If true, wait until an event has been dispatched before returning
     * from the post method.  This is meant to be a global debugging flag.
     * If a class calling postSpeechEvent wants to wait until the
     * SpeechEvent has been dispatched, it should call the postSpeechEvent
     * method that has the waitUntilDispatched parameter.
     *
     * @see #postSpeechEvent
     */
    public static boolean waitUntilDispatched = false;

    /**
     * Determine if the AWT event queue is running.  This method is one big
     * hack, and we will be entering a bug against AWT to provide us with
     * a real method for determining if AWT is active or not.  The problem
     * with asking AWT if it is active right now is that it will activate
     * it if it isn't already active.
     */
    static protected boolean isAWTRunning() {
         
        if (awtRunning) {
            return true;
        }
 
	try {
	    ThreadGroup rootGroup;
	    ThreadGroup parent;
	    ThreadGroup g = Thread.currentThread().getThreadGroup();
	    rootGroup = g;
	    parent = rootGroup.getParent();
	    while (parent != null) {
		rootGroup = parent;
		parent = parent.getParent();
	    }

	    int activeCount = rootGroup.activeCount();
	    Thread[] threads = new Thread[activeCount];
	    rootGroup.enumerate(threads,true);
	    for (int i = 0; i < threads.length; i++) {
		if (threads[i] != null) {
		    String name = threads[i].getName();
		    if (name.startsWith("AWT-EventQueue")) {
			awtRunning = true;
			return true;
		    }
		}
	    }
	} catch (AccessControlException ace) {
	    // if we receive an access control exception then
	    // it is likely that we are running in an applet
	    // in which case AWT is running. 
	    // I'm not sure if this is always true, perhaps
	    // there is another way to tell if we are running in an
	    // applet.

	    return true;
	}

        return false;        
    }

    /**
     * Post a JSAPI SpeechEvent.  This is to be used by multiple processes
     * to synchronize SpeechEvents.  It currently uses the AWT EventQueue
     * as a means for doing this, which has the added benefit of causing
     * all SpeechEvent notification to be done from the event dispatch
     * thread.  This is important because the Swing Thread Model requires
     * all interaction with Swing components to be done from the event
     * dispatch thread.
     *
     * This method will immediately return once the event has been
     * posted if the global waitUntilDispatched flag is set to false.
     * Otherwise, it will wait until the event has been dispatched
     * before returning.
     *
     * @param dispatcher the dispatcher that will dispatch the event
     * @param event the SpeechEvent to post
     */
    static public void postSpeechEvent(SpeechEventDispatcher dispatcher,
                                       SpeechEvent           event) {
        postSpeechEvent(dispatcher, event, waitUntilDispatched);
    }

    /**
     * Post a JSAPI SpeechEvent.  This is to be used by multiple processes
     * to synchronize SpeechEvents.  It currently uses the AWT EventQueue
     * as a means for doing this, which has the added benefit of causing
     * all SpeechEvent notification to be done from the event dispatch
     * thread.  This is important because the Swing Thread Model requires
     * all interaction with Swing components to be done from the event
     * dispatch thread.
     *
     * This method will immediately return once the event has been
     * posted if the waitUntilDispatched parameter is set to false.
     * Otherwise, it will wait until the event has been dispatched
     * before returning.
     *
     * @param dispatcher the dispatcher that will dispatch the event
     * @param event the SpeechEvent to post
     * @param waitUntilDispatched if true, do not return until the
     * event have been dispatched
     */
    static public void postSpeechEvent(
        SpeechEventDispatcher dispatcher,
        SpeechEvent           event,
        boolean               waitUntilDispatched) {

        /* Only use the AWT EventQueue if AWT is running.  If it isn't
         * running, then just call the dispatcher directly.  A more formal
         * event queue mechanism probably should be added at some point
         * so listeners cannot cause a hang in the engine.
         */
        if (isAWTRunning()) {
            /* Create the event target and event queue references if they
             * haven't been created yet.  This is done here to delay the
             * initialization of the AWT Toolkit until it is absolutely
             * necessary.  Creating it earlier may cause conflicts with
             * applets.
             */
            if (speechAWTEventTarget == null) {
                speechAWTEventTarget = new SpeechAWTEventTarget();
                systemEventQueue =
                    Toolkit.getDefaultToolkit().getSystemEventQueue();
            }

            /* Post the event to the AWT EventQueue.  When AWT dispatches
             * the events on its EventQueue, this event will be sent to
             * the speechAWTEventTarget for processing.
             */
            if (waitUntilDispatched) {
                Object lock = new Object();
                synchronized(lock) {
                    systemEventQueue.postEvent(
                        new SpeechAWTEvent(speechAWTEventTarget,
                                           dispatcher,
                                           event,
                                           lock));
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                    }
                }
            } else {
                systemEventQueue.postEvent(
                    new SpeechAWTEvent(speechAWTEventTarget,
                                       dispatcher,
                                       event));
            }
        } else {
            dispatcher.dispatchSpeechEvent(event);
        }
    }

    /**
     * Inner class used to handle events as they are dispatched from the
     * AWT event queue.
     *
     * @see #postSpeechEvent
     */
    protected static class SpeechAWTEventTarget extends Component {
        SpeechAWTEventTarget() {
            super();
            enableEvents(SpeechAWTEvent.EVENT_ID);
        }
        protected void processEvent(AWTEvent event) {
            if (event instanceof SpeechAWTEvent) {
                SpeechAWTEvent sae = (SpeechAWTEvent) event;
                sae.dispatcher.dispatchSpeechEvent(sae.event);
                if (sae.lock != null) {
                    synchronized(sae.lock) {
                        sae.lock.notify();
                    }
                }
            }
        }
    }

    /**
     * Inner class that defines SpeechAWTEvents.  These are created and
     * posted to the AWT EventQueue by the postSpeechEvent method.
     *
     * @see #postSpeechEvent
     */
    protected static class SpeechAWTEvent extends AWTEvent {
        static final int EVENT_ID = AWTEvent.RESERVED_ID_MAX + 14830;
        SpeechEventDispatcher dispatcher = null;
        SpeechEvent event = null;
        Object lock = null;
        SpeechAWTEvent(SpeechAWTEventTarget  target,
                       SpeechEventDispatcher dispatcher,
                       SpeechEvent           event) {
            this(target,dispatcher,event,null);
        }
        SpeechAWTEvent(SpeechAWTEventTarget  target,
                       SpeechEventDispatcher dispatcher,
                       SpeechEvent           event,
                       Object                lock) {
            super(target, EVENT_ID);
            this.dispatcher = dispatcher;
            this.event = event;
            this.lock = lock;
        }
    }
}

        



