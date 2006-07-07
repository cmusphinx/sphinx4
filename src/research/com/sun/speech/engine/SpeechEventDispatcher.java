/**
 * Copyright 1998-2003 Sun Microsystems, Inc.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package com.sun.speech.engine;

import javax.speech.SpeechEvent;

/**
 * Interface to be implemented by objects that dispatch
 * <code>SpeechEvents</code>.
 *
 * @see SpeechEventUtilities
 */
public interface SpeechEventDispatcher {
    /**
     * Dispatches a <code>SpeechEvent</code> to all appropriate
     * <code>SpeechEventListeners</code>
     * on the object that implements this interface.
     *
     * @param event the event to dispactch
     */
    public void dispatchSpeechEvent(SpeechEvent event);
}

