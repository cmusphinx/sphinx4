/**
 * Copyright 1998-2003 Sun Microsystems, Inc.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package com.sun.speech.engine;

import javax.speech.SpeechEvent;
import java.util.EventObject;

/**
 * Wraps an arbitrary event object (from <code>EventObject</code>)
 * in a <code>SpeechEvent</code> so that it can be dispatched through
 * the speech event dispatch mechanism.
 * One use of this is in the <code>BaseEngineProperties</code> class
 * that needs to wrap and issue <code>PropertyChangeEvents</code>.
 *
 * @see SpeechEventUtilities
 * @see EventObject
 */
public class SpeechEventWrapper extends SpeechEvent {
    /**
     * Use an id that won't be confused with JSAPI event ids.
     */
    protected static int WRAPPER_ID = -25468;
    
    /**
     * The wrapped event.
     */
    protected EventObject eventObject;

    /**
     * Class constructor.
     *
     * @param e the <code>EventObject</code> to wrap.
     */
    public SpeechEventWrapper(EventObject e) {
        super(e.getSource(), WRAPPER_ID);
        eventObject = e;
    }

    /**
     * Gets the wrapped event.
     *
     * @return the event that was passed to the constructor
     */
    public EventObject getEventObject() {
        return eventObject;
    }
}
