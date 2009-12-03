/**
 * Copyright 1998-2003 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package com.sun.speech.engine;

import java.util.ArrayList;
import java.util.List;

import javax.speech.AudioListener;
import javax.speech.AudioManager;

/**
 * Supports the JSAPI 1.0 <code>AudioManager</code> interface. 
 * Actual JSAPI implementations might want to extend or
 * modify this implementation.
 */
public class BaseAudioManager implements AudioManager {

    /** List of <code>AudioListeners</code> registered for <code>AudioEvents</code> on this object. */
    protected final List<AudioListener> listeners  = new ArrayList<AudioListener>();

    /**
     * Requests notification of <code>AudioEvents</code> from the <code>AudioManager</code>.
     *
     * @param listener the listener to add
     */
    public void addAudioListener(AudioListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Removes an <code>AudioListener</code> from the list of <code>AudioListeners</code>.
     *
     * @param listener the listener to remove
     */
    public void removeAudioListener(AudioListener listener) {
        listeners.remove(listener);
    }
}

