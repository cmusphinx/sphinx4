/**
 * Copyright 1998-2003 Sun Microsystems, Inc.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package com.sun.speech.engine.recognition;

import com.sun.speech.engine.*;
import javax.speech.*;
import javax.speech.recognition.*;
import java.util.*;

import com.sun.speech.engine.SpeechEventUtilities;
import com.sun.speech.engine.SpeechEventDispatcher;

/**
 * Skeletal Implementation of the JSAPI AudioManager interface for
 * Recognizers.  Merely provides convenience function for calling
 * the listeners.
 * 
 * <P>Actual JSAPI implementations might want to extend or
 * modify this implementation.
 * <P>
 *
 * @author Willie Walker
 * @version 1.2 01/27/99 13:43:57
 */
public class BaseRecognizerAudioManager extends BaseAudioManager
    implements SpeechEventDispatcher {
    
//////////////////////
// Begin utility methods for calling RecognizerAudioListeners
//////////////////////
    /**
     * Utility function to generate AUDIO_LEVEL event and post it
     * to the event queue.  Eventually fireAudioLevel will be called
     * by dispatchSpeechEvent as a result of this action.
     * @param source the Recognizer causing the event
     * @param audioLevel the audio level between 0.0 and 1.0
     */
    public void postAudioLevel(Recognizer source, float audioLevel) {
        SpeechEventUtilities.postSpeechEvent(
            this,
            new RecognizerAudioEvent(source,
                                     RecognizerAudioEvent.AUDIO_LEVEL,
                                     audioLevel));
    }
    
    /**
     * Utility function to send a AUDIO_LEVEL event to all
     * listeners.  
     */
    public void fireAudioLevel(RecognizerAudioEvent event) {
	if (listeners == null) {
	    return;
	}
        Enumeration E = listeners.elements();
        while (E.hasMoreElements()) {
            AudioListener al = (AudioListener) E.nextElement();
            if (al instanceof RecognizerAudioListener) {
                ((RecognizerAudioListener) al).audioLevel(event);
            }
        }
    }

    /**
     * Utility function to generate SPEECH_STARTED event and post it
     * to the event queue.  Eventually fireSpeechStarted will be called
     * by dispatchSpeechEvent as a result of this action.
     * @param source the Recognizer causing the event
     */
    public void postSpeechStarted(Recognizer source) {
        SpeechEventUtilities.postSpeechEvent(
            this,
            new RecognizerAudioEvent(source,
                                     RecognizerAudioEvent.SPEECH_STARTED));
    }
    
    /**
     * Utility function to send a SPEECH_STARTED event to all
     * listeners.  
     */
    public void fireSpeechStarted(RecognizerAudioEvent event) {
	if (listeners == null) {
	    return;
	}
        Enumeration E = listeners.elements();
        while (E.hasMoreElements()) {
            AudioListener al = (AudioListener) E.nextElement();
            if (al instanceof RecognizerAudioListener) {
                ((RecognizerAudioListener) al).speechStarted(event);
            }
        }
    }

    /**
     * Utility function to generate SPEECH_STOPPED event and post it
     * to the event queue.  Eventually fireSpeechStopped will be called
     * by dispatchSpeechEvent as a result of this action.
     * @param source the Recognizer causing the event
     */
    public void postSpeechStopped(Recognizer source) {
        SpeechEventUtilities.postSpeechEvent(
            this,
            new RecognizerAudioEvent(source,
                                     RecognizerAudioEvent.SPEECH_STOPPED));
    }
    
    /**
     * Utility function to send a SPEECH_STOPPED event to all
     * listeners.  
     */
    public void fireSpeechStopped(RecognizerAudioEvent event) {
	if (listeners == null) {
	    return;
	}
        Enumeration E = listeners.elements();
        while (E.hasMoreElements()) {
            AudioListener al = (AudioListener) E.nextElement();
            if (al instanceof RecognizerAudioListener) {
                ((RecognizerAudioListener) al).speechStopped(event);
            }
        }
    }
//////////////////////
// End utility methods for calling RecognizerAudioListeners
//////////////////////

    /**
     * Dispatch a SpeechEvent.  This is a method from SpeechEventDispatcher.
     * The dispatcher should notify all listeners of the speech event
     * from this method.
     */
    public void dispatchSpeechEvent(SpeechEvent event) {
        switch (event.getId()) {
            case RecognizerAudioEvent.AUDIO_LEVEL:
                fireAudioLevel((RecognizerAudioEvent) event);
                break;
            case RecognizerAudioEvent.SPEECH_STARTED:
                fireSpeechStarted((RecognizerAudioEvent) event);
                break;
            case RecognizerAudioEvent.SPEECH_STOPPED:
                fireSpeechStopped((RecognizerAudioEvent) event);
                break;
        }
    }
}

