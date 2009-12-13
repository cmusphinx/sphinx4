/**
 * Copyright 1998-2001 Sun Microsystems, Inc.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package com.sun.speech.engine.synthesis;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;

import javax.speech.EngineListener;
import javax.speech.EngineStateError;
import javax.speech.SpeechEvent;
import javax.speech.synthesis.JSMLException;
import javax.speech.synthesis.Speakable;
import javax.speech.synthesis.SpeakableListener;
import javax.speech.synthesis.Synthesizer;
import javax.speech.synthesis.SynthesizerEvent;
import javax.speech.synthesis.SynthesizerListener;
import javax.speech.synthesis.SynthesizerModeDesc;
import javax.speech.synthesis.SynthesizerProperties;

import com.sun.speech.engine.BaseEngine;
import com.sun.speech.engine.BaseEngineProperties;
import com.sun.speech.engine.SpeechEventDispatcher;
import com.sun.speech.engine.SpeechEventUtilities;

/**
 * Supports the JSAPI 1.0 <code>Synthesizer</code> interface that
 * performs the core non-engine-specific functions.
 * 
 * <p>An actual JSAPI synthesizer implementation needs to extend or
 * modify this implementation.
 */
abstract public class BaseSynthesizer extends BaseEngine
    implements Synthesizer, SpeechEventDispatcher {

    /**
     * Set of speakable listeners belonging to the <code>Synthesizer</code>.
     * Each item on queue may have an individual listener too.
     *
     * @see SpeakableListener
     */
    protected Collection<SpeakableListener> speakableListeners;

    /**
     * The set of voices available in this <code>Synthesizer</code>.
     * The list can be created in the constructor methods.
     */
    protected VoiceList voiceList;
    
    /**
     * Creates a new Synthesizer in the <code>DEALLOCATED</code> state.
     *
     * @param mode the operating mode of this <code>Synthesizer</code>
     */
    public BaseSynthesizer(SynthesizerModeDesc mode) {
        super(mode);
        speakableListeners = new java.util.ArrayList<SpeakableListener>();
        voiceList = new VoiceList(mode);
    }

    /**
     * Speaks JSML text provided as a <code>Speakable</code> object.
     *
     * @param jsmlText the JSML text to speak
     * @param listener the listener to be notified as the
     *   <code>jsmlText</code> is processed
     *
     * @throws JSMLException if the JSML text contains errors
     * @throws EngineStateError 
     *   if this <code>Synthesizer</code> in the <code>DEALLOCATED</code> or 
     *   <code>DEALLOCATING_RESOURCES</code> states
     */
    public void speak(Speakable jsmlText, SpeakableListener listener)
        throws JSMLException, EngineStateError {
        checkEngineState(DEALLOCATED | DEALLOCATING_RESOURCES);
        BaseSynthesizerQueueItem item = createQueueItem();
        item.setData(this, jsmlText, listener);
        appendQueue(item);
    }

    /**
     * Speaks JSML text provided as a <code>URL</code>.
     *
     * @param jsmlURL the <code>URL</code> containing JSML text
     * @param listener the listener to be notified as the
     *   JSML text is processed
     *
     * @throws EngineStateError 
     *   if this <code>Synthesizer</code> in the <code>DEALLOCATED</code> or 
     *   <code>DEALLOCATING_RESOURCES</code> states
     * @throws IOException
     *    if errors are encountered with the <code>JSMLurl</code>
     * @throws JSMLException if the JSML text contains errors
     * @throws MalformedURLException
     *    if errors are encountered with the <code>JSMLurl</code>
     */
    public void speak(URL jsmlURL, SpeakableListener listener)
        throws JSMLException, MalformedURLException,
        IOException, EngineStateError {
        checkEngineState(DEALLOCATED | DEALLOCATING_RESOURCES);
        BaseSynthesizerQueueItem item = createQueueItem();
        item.setData(this, jsmlURL, listener);
        appendQueue(item);
    }

    /**
     * Speaks JSML text provided as a <code>String</code>.
     *
     * @param jsmlText a <code>String</code> containing JSML.
     * @param listener the listener to be notified as the
     *   JSML text is processed
     *
     * @throws EngineStateError 
     *   if this <code>Synthesizer</code> in the <code>DEALLOCATED</code> or 
     *   <code>DEALLOCATING_RESOURCES</code> states
     * @throws JSMLException if the JSML text contains errors
     */
    public void speak(String jsmlText, SpeakableListener listener)
        throws JSMLException, EngineStateError {
        checkEngineState(DEALLOCATED | DEALLOCATING_RESOURCES);
        BaseSynthesizerQueueItem item = createQueueItem();
        item.setData(this, jsmlText, false, listener);
        appendQueue(item);
    }

    /**
     * Speaks a plain text <code>String</code>.  No JSML parsing is
     * performed.
     *
     * @param text a <code>String</code> containing plain text.
     * @param listener the listener to be notified as the
     *   text is processed
     *
     * @throws EngineStateError 
     *   if this <code>Synthesizer</code> in the <code>DEALLOCATED</code> or 
     *   <code>DEALLOCATING_RESOURCES</code> states
     */
    public void speakPlainText(String text, SpeakableListener listener)
        throws EngineStateError {
        checkEngineState(DEALLOCATED | DEALLOCATING_RESOURCES);
        try {
            BaseSynthesizerQueueItem item = createQueueItem();
            item.setData(this, text, true, listener);
            appendQueue(item);
        } catch (JSMLException e) {
            throw new RuntimeException("JSMLException should never occur");
        }
    }

    /**
     * Returns a String of the names of all the states implied
     * in the given bit pattern.
     *
     * @param state the bit pattern of states
     *
     * @return a String of the names of all the states implied
     *   in the given bit pattern.
     */
    protected String stateToString(long state) {
	    StringBuilder buf = new StringBuilder();
        if ((state & Synthesizer.QUEUE_EMPTY) != 0)
            buf.append(" QUEUE_EMPTY ");
        if ((state & Synthesizer.QUEUE_NOT_EMPTY) != 0)
            buf.append(" QUEUE_NOT_EMPTY ");
	    return super.stateToString(state) + buf.toString();
    }

    /**
     * Puts an item on the speaking queue and sends a queue updated
     * event.
     *
     * @param item the item to add to the queue
     *
     */
    abstract protected void appendQueue(BaseSynthesizerQueueItem item);

    /**
     * Optional method that converts a text string to a phoneme string.
     *
     * @param text
     *   plain text to be converted to phonemes
     *
     * @return
     *   IPA phonemic representation of text or <code>null</code>
     *
     * @throws EngineStateError 
     *   if this <code>Synthesizer</code> in the <code>DEALLOCATED</code> or 
     *   <code>DEALLOCATING_RESOURCES</code> states
     */
    public String phoneme(String text) throws EngineStateError {
        checkEngineState(DEALLOCATED | DEALLOCATING_RESOURCES);
        
        // BaseSynthesizer does not implement phoneme.  The sub-class
        // should override this method if it supports text to phoneme
        // conversion.  Returning null is legal behavior.
        //
        return null;
    }

    /**
     * Returns an enumeration of the queue.
     *
     * @return
     *   an <code>Enumeration</code> of the speech output queue or
     *   <code>null</code>.
     *
     * @throws EngineStateError 
     *   if this <code>Synthesizer</code> in the <code>DEALLOCATED</code> or 
     *   <code>DEALLOCATING_RESOURCES</code> states
     */
    abstract public Enumeration<?> enumerateQueue() throws EngineStateError;

    /**
     * Cancels the item at the top of the queue.
     *
     * @throws EngineStateError 
     *   if this <code>Synthesizer</code> in the <code>DEALLOCATED</code> or 
     *   <code>DEALLOCATING_RESOURCES</code> states
     */
    abstract public void cancel() throws EngineStateError;

    /**
     * Cancels a specific object on the queue.
     *
     * @param source
     *    object to be removed from the speech output queue
     *
     * @throws IllegalArgumentException
     *  if the source object is not found in the speech output queue.
     * @throws EngineStateError 
     *   if this <code>Synthesizer</code> in the <code>DEALLOCATED</code> or 
     *   <code>DEALLOCATING_RESOURCES</code> states
     */
    abstract public void cancel(Object source)
        throws IllegalArgumentException, EngineStateError;

    /**
     * Cancels all items on the output queue.
     *
     * @throws EngineStateError 
     *   if this <code>Synthesizer</code> in the <code>DEALLOCATED</code> or 
     *   <code>DEALLOCATING_RESOURCES</code> states
     */
    abstract public void cancelAll() throws EngineStateError;

    /**
     * Returns the <code>SynthesizerProperties</code> object (a JavaBean). 
     * The method returns exactly the same object as the
     * <code>getEngineProperties</code> method in the <code>Engine</code>
     * interface.  However, with the <code>getSynthesizerProperties</code>
     * method, an application does not need to cast the return value.
     *
     * @return the <code>SynthesizerProperties</code> object for this
     *   <code>Synthesizer</code>
     */
    public SynthesizerProperties getSynthesizerProperties() {
        checkEngineState(DEALLOCATED | DEALLOCATING_RESOURCES);
        return (SynthesizerProperties) getEngineProperties();
    }

    /**
     * Adds a <code>SpeakableListener</code> to this <code>Synthesizer</code>.
     *
     * @param listener the listener to add
     *
     * @see #removeSpeakableListener
     */
    public void addSpeakableListener(SpeakableListener listener) {
        if (!speakableListeners.contains(listener)) {
            speakableListeners.add(listener);
        }
    }

    /**
     * Removes a <code>SpeakableListener</code> from this
     * <code>Synthesizer</code>.
     *
     * @param listener the listener to remove
     *
     * @see #addSpeakableListener
     */
    public void removeSpeakableListener(SpeakableListener listener) {
        speakableListeners.remove(listener);
    }

    /**
     * Factory constructor for <code>EngineProperties</code> object.
     * Gets the default speaking voice from the
     * <code>SynthesizerModeDesc</code>.
     * Takes the default prosody values (pitch, range, volume, rate)
     * from the default voice.  Override to set engine-specific defaults.
     *
     * @return a <code>BaseEngineProperties</code> object specific to
     *   a subclass.
     */
    protected BaseEngineProperties createEngineProperties() {
        SynthesizerModeDesc desc = (SynthesizerModeDesc)engineModeDesc;
        BaseVoice defaultVoice = (BaseVoice)(desc.getVoices()[0]);
        
        float defaultPitch = defaultVoice.defaultPitch;
        float defaultPitchRange = defaultVoice.defaultPitchRange;
        float defaultSpeakingRate = defaultVoice.defaultSpeakingRate;
        float defaultVolume = defaultVoice.defaultVolume;
        
        return new BaseSynthesizerProperties(defaultVoice,
                                             defaultPitch,
                                             defaultPitchRange,
                                             defaultSpeakingRate,
                                             defaultVolume);
    }

    /**
     * Factory method that creates a <code>BaseSynthesizerQueueItem</code>.
     * Override if the synthesizer specializes the
     * <code>BaseSynthesizerQueueItem</code> class.
     */
    protected BaseSynthesizerQueueItem createQueueItem() {
        return new BaseSynthesizerQueueItem();
    }

    /**
     * Returns the list of voices for this <code>Synthesizer</code>.
     *
     * @return the list of voices for this <code>Synthesizer</code>.
     */
    protected VoiceList getVoiceList() {
        return voiceList;
    }    

    /**
     * Utility function that generates <code>QUEUE_UPDATED</code>
     * event and posts it to the event queue.  Eventually
     * <code>fireQueueUpdated</code> will be called
     * by <code>dispatchSpeechEvent</code> as a result of this action.
     *
     * @param topOfQueueChanged <code>true</code> if the top of the
     *   queue has changed
     * @param oldState the old state of this <code>Synthesizer</code>
     * @param newState the new state of this <code>Synthesizer</code>
     *
     * @see #fireQueueUpdated
     * @see #dispatchSpeechEvent
     * 
     */
    public void postQueueUpdated(boolean topOfQueueChanged,
                                 long oldState, long newState) {
        SpeechEventUtilities.postSpeechEvent(
            this,
            new SynthesizerEvent(this,
                                 SynthesizerEvent.QUEUE_UPDATED,
                                 topOfQueueChanged,
                                 oldState, newState));
    }
    
    /**
     * Utility function that sends a <code>QUEUE_UPDATED</code>
     * event to all <code>SynthesizerListeners</code>.  
     *
     * @param event the <code>QUEUE_UPDATED</code> event
     *
     * @see #postQueueUpdated
     * @see #dispatchSpeechEvent
     */
    public void fireQueueUpdated(SynthesizerEvent event) {
        if (engineListeners == null) {
            return;
        }
        for (EngineListener el : engineListeners) {
            if (el instanceof SynthesizerListener) {
                SynthesizerListener sl = (SynthesizerListener)el;
                sl.queueUpdated(event);
            }
        }
    }
    
    /**
     * Utility function that generates <code>QUEUE_EMPTIED</code>
     * event and posts it to the event queue.  Eventually
     * <code>fireQueueEmptied</code> will be called
     * by <code>dispatchSpeechEvent</code> as a result of this action.
     *
     * @param oldState the old state of this <code>Synthesizer</code>
     * @param newState the new state of this <code>Synthesizer</code>
     *
     * @see #fireQueueEmptied
     * @see #dispatchSpeechEvent
     */
    public void postQueueEmptied(long oldState, long newState) {
        SpeechEventUtilities.postSpeechEvent(
            this,
            new SynthesizerEvent(this,
                                 SynthesizerEvent.QUEUE_EMPTIED,
                                 false,
                                 oldState, newState));
    }
    
    /**
     * Utility function that sends a <code>QUEUE_EMPTIED</code>
     * event to all <code>SynthesizerListeners</code>.  
     *
     * @param event the <code>QUEUE_EMPTIED</code> event
     *
     * @see #postQueueEmptied
     * @see #dispatchSpeechEvent
     */
    public void fireQueueEmptied(SynthesizerEvent event) {
        if (engineListeners == null) {
            return;
        }
        for (EngineListener el : engineListeners) {
            if (el instanceof SynthesizerListener) {
                SynthesizerListener sl = (SynthesizerListener)el;
                sl.queueEmptied(event);
            }
        }
    }

    /**
     * Dispatches a <code>SpeechEvent</code>.
     * The dispatcher should notify all <code>SynthesizerListeners</code>
     * from this method.  The <code>SpeechEvent</code> was added
     * via the various post methods of this class.
     *
     * @param event the <code>SpeechEvent</code> to dispatch
     *
     * @see #postQueueUpdated
     * @see #postQueueEmptied
     */
    public void dispatchSpeechEvent(SpeechEvent event) {
        switch (event.getId()) {
            case SynthesizerEvent.QUEUE_EMPTIED:
                fireQueueEmptied((SynthesizerEvent) event);
                break;
            case SynthesizerEvent.QUEUE_UPDATED:
                fireQueueUpdated((SynthesizerEvent) event);
                break;

            // Defer to BaseEngine to handle the rest.
            //
            default:
                super.dispatchSpeechEvent(event);
                break;
        }
    }
}
