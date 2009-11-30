/**
 * Copyright 1998-2001 Sun Microsystems, Inc.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package com.sun.speech.engine.synthesis;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;

import javax.speech.SpeechEvent;
import javax.speech.synthesis.JSMLException;
import javax.speech.synthesis.Speakable;
import javax.speech.synthesis.SpeakableEvent;
import javax.speech.synthesis.SpeakableListener;
import javax.speech.synthesis.SynthesizerQueueItem;

import org.w3c.dom.Document;

import com.sun.speech.engine.SpeechEventDispatcher;
import com.sun.speech.engine.SpeechEventUtilities;

/**
 * Extends the JSAPI 1.0 <code>SynthesizerQueueItem</code> with handling
 * for JSML, generation of engine-specific text, and other features.
 */
public class BaseSynthesizerQueueItem extends SynthesizerQueueItem
    implements SpeechEventDispatcher {
    private volatile boolean done = false;
    private volatile boolean cancelled = false;

    /**
     * The object containing the DOM of the parsed JSML.
     */
    private Document document = null;
    
    /**
     * Global count of queue items used for debug.
     */
    protected static int itemNumber = 0;


    /**
     * Count for this item used for debug.
     */
    protected int thisItemNumber = 0;
  

    /**
     * <code>Synthesizer</code> that has queued this item.
     */
    protected BaseSynthesizer synth;

    /**
     * Class constructor.
     */
    public BaseSynthesizerQueueItem() {
        super(null, null, false, null);
        thisItemNumber = itemNumber++;
    }

    /**
     * Sets queue item data with a <code>Speakable</code> source.
     *
     * @param synth the synthesizer
     * @param source the <code>Speakable</code>
     * @param listener the <code>SpeakableListener</code> to be
     *   notified as this object is processed
     *
     * @throws JSMLException if the <code>source</code> contains JSML errors
     */
    protected void setData(BaseSynthesizer synth,
                           Speakable source, 
                           SpeakableListener listener)
        throws JSMLException {
        this.synth = synth;
        this.source = source;
        this.text = source.getJSMLText();
        this.plainText = false;
        this.listener = listener;
        document = new JSMLParser(this.text, false).getDocument();
    }

    /**
     * Sets queue item data with a <code>String</code> source that is
     * either plain text or JSML.
     *
     * @param synth the synthesizer
     * @param source the text
     * @param plainText <code>true</code> only if the
     *   <code>source</code> is plain text
     * @param listener the <code>SpeakableListener</code> to be
     *   notified as this object is processed
     *
     * @throws JSMLException if the <code>source</code> contains JSML errors
     */
    protected void setData(BaseSynthesizer synth,
                           String source, 
                           boolean plainText,
                           SpeakableListener listener)
        throws JSMLException {
        this.synth = synth;
        this.source = source;
        this.text = source;
        this.plainText = plainText;
        this.listener = listener;
        if (!plainText) {
            document = new JSMLParser(this.text, false).getDocument();
        }
    }


    /**
     * Sets queue item data with a <code>URL</code> source.
     * 
     * @param synth the synthesizer
     * @param source the <code>URL</code> containing JSML text
     * @param listener the <code>SpeakableListener</code> to be
     *   notified as this object is processed
     *
     * @throws JSMLException if the <code>source</code> contains JSML errors
     * @throws IOException if there are problems working with the URL.
     */
    protected void setData(BaseSynthesizer synth,
                           URL source, 
                           SpeakableListener listener)
        throws JSMLException, IOException {
        this.synth = synth;
        this.source = source;
        this.text = null;
        this.plainText = false;
        this.listener = listener;
        document = new JSMLParser(source, false).getDocument();
    }

    /**
     * Gets the DOM document for this object.
     *
     * @return the DOM document for this object.
     */
    protected Document getDocument() {
        return document;
    }


    /**
     * determines if this queue item has been canceled
     * 
     * @return <code> true </code> if this item has been canceled; 
     *   otherwise <code> false </code>
     */
    protected synchronized boolean isCancelled() {
	return cancelled;
    }


    /**
     * returns true if this queue item has been 
     * processed.
     * @return true if it has been processed
     */
    public synchronized boolean isCompleted() {
	return done;
    }

    /**
     * wait for this queue item to be completed
     *
     * @return true if the item was completed successfully, false if
     * the item was canceled or an error occurred.
     */
    public synchronized boolean waitCompleted() {
	while  (!isCompleted()) {
	    try {
		wait();
	    } catch (InterruptedException ie) {
		System.err.println(
			"FreeTTSSynthesizerQueueItem.Wait interrupted");
		return false;
	    }
	}
	return !isCancelled();
    }

    /**
     * indicate that this item has been canceled
     */
    public synchronized void cancelled() {
	postSpeakableCancelled();
	notifyAll();
    }

    /**
     * indicate that this item has been completed
     */
    public synchronized void completed() {
	postSpeakableEnded();
	notifyAll();
    }

    /**
     * indicate that this item has been started
     */
    public void started() {
	postSpeakableStarted();
    }
    
    /**
     * Gets the item number for debug purposes only.  Each queue item
     * is given a unique ID.
     *
     * @return the unique ID for this queue item
     */
    public int getItemNumber() {
        return thisItemNumber;
    }
    
    /**
     * Utility function that generates a
     * <code>MARKER_REACHED</code> event and posts it
     * to the event queue.  Eventually
     * <code>fireMarkerReached</code> will be called
     * by <code>dispatchSpeechEvent</code> as a result
     * of this action.
     *
     * @param text the text of the marker
     * @param markerType the type of marker
     *
     * @see SpeakableEvent#getMarkerType
     * @see #fireMarkerReached
     * @see #dispatchSpeechEvent
     */
    public void postMarkerReached(String text, int markerType) {
        SpeechEventUtilities.postSpeechEvent(
            this,
            new SpeakableEvent(source,
                               SpeakableEvent.MARKER_REACHED,
                               text, markerType));
    }

    /**
     * Utility function that sends a <code>MARKER_REACHED</code> event
     * to all speakable listeners.
     *
     * @param event the <code>MARKER_REACHED</code> event
     *
     * @see #postMarkerReached
     */
    public void fireMarkerReached(SpeakableEvent event) {
        if (listener != null) {
            listener.markerReached(event);
        }
        

        if (synth.speakableListeners != null) {
            Iterator iterator = synth.speakableListeners.iterator();
            while (iterator.hasNext()) {
                SpeakableListener sl = (SpeakableListener) iterator.next();
                sl.markerReached(event);
            }
        }
    }

    /**
     * Utility function that generates a
     * <code>SPEAKABLE_CANCELLED</code> event and posts it
     * to the event queue.  Eventually
     * <code>fireSpeakableCancelled</code> will be called
     * by <code>dispatchSpeechEvent</code> as a result
     * of this action.
     *
     * @see #fireSpeakableCancelled
     * @see #dispatchSpeechEvent
     */
    public void postSpeakableCancelled() {
	boolean shouldPost;

    // The JSAPI docs say that once a canceled event is sent, no
    // others will be. This makes sure that a canceled will never be
    // sent twice. This deals with the race that can occur when an
    // item that is playing is canceled.

	synchronized(this) {
	    shouldPost = !done;
	    done = true;
	    cancelled = true;
	}
	if (shouldPost) {
	    SpeechEventUtilities.postSpeechEvent(
		this,
		new SpeakableEvent(source, SpeakableEvent.SPEAKABLE_CANCELLED));
	}
    }

    /**
     * Utility function that sends a <code>SPEAKABLE_CANCELLED</code> event
     * to all speakable listeners.
     *
     * @param event the <code>SPEAKABLE_CANCELLED</code> event
     *
     * @see #postSpeakableCancelled
     */
    public void fireSpeakableCancelled(SpeakableEvent event) {
        if (listener != null) {
            listener.speakableCancelled(event);
        }
        

        if (synth.speakableListeners != null) {
            Iterator iterator = synth.speakableListeners.iterator();
            while (iterator.hasNext()) {
                SpeakableListener sl = (SpeakableListener) iterator.next();
                sl.speakableCancelled(event);
            }
        }
    }

    /**
     * Utility function that generates a
     * <code>SPEAKABLE_ENDED</code> event and posts it
     * to the event queue.  Eventually
     * <code>fireSpeakableEnded</code> will be called
     * by <code>dispatchSpeechEvent</code> as a result
     * of this action.
     *
     * @see #fireSpeakableEnded
     * @see #dispatchSpeechEvent
     */
    public void postSpeakableEnded() {
	boolean shouldPost;

    // The JSAPI docs say that once a canceled event is sent, no
    // others will be. This makes sure that a canceled will never be
    // sent twice. This deals with the race that can occur when an
    // item that is playing is canceled.
	synchronized(this) {
	    shouldPost = !done;
            done = true;
	}
	if (shouldPost) {
	    SpeechEventUtilities.postSpeechEvent(
		this,
		new SpeakableEvent(source, SpeakableEvent.SPEAKABLE_ENDED));
	}
    }


    /**
     * Utility function that sends a <code>SPEAKABLE_ENDED</code> event
     * to all speakable listeners.
     *
     * @param event the <code>SPEAKABLE_ENDED</code> event
     *
     * @see #postSpeakableEnded
     */
    public void fireSpeakableEnded(SpeakableEvent event) {
        if (listener != null) {
            listener.speakableEnded(event);
        }
        

        if (synth.speakableListeners != null) {
            Iterator iterator = synth.speakableListeners.iterator();
            while (iterator.hasNext()) {
                SpeakableListener sl = (SpeakableListener) iterator.next();
                sl.speakableEnded(event);
            }
        }
    }

    /**
     * Utility function that generates a
     * <code>SPEAKABLE_PAUSED</code> event and posts it
     * to the event queue.  Eventually
     * <code>fireSpeakablePaused</code> will be called
     * by <code>dispatchSpeechEvent</code> as a result
     * of this action.
     *
     * @see #fireSpeakablePaused
     * @see #dispatchSpeechEvent
     */
    public void postSpeakablePaused() {
        SpeechEventUtilities.postSpeechEvent(
            this,
            new SpeakableEvent(source, SpeakableEvent.SPEAKABLE_PAUSED));
    }

    /**
     * Utility function that sends a <code>SPEAKABLE_PAUSED</code> event
     * to all speakable listeners.
     *
     * @param event the <code>SPEAKABLE_PAUSED</code> event
     *
     * @see #postSpeakablePaused
     */
    public void fireSpeakablePaused(SpeakableEvent event) {
        if (listener != null) {
            listener.speakablePaused(event);
        }
        

        if (synth.speakableListeners != null) {
            Iterator iterator = synth.speakableListeners.iterator();
            while (iterator.hasNext()) {
                SpeakableListener sl = (SpeakableListener) iterator.next();
                sl.speakablePaused(event);
            }
        }
    }

    /**
     * Utility function that generates a
     * <code>SPEAKABLE_RESUMED</code> event and posts it
     * to the event queue.  Eventually
     * <code>fireSpeakableResumed</code> will be called
     * by <code>dispatchSpeechEvent</code> as a result
     * of this action.
     *
     * @see #fireSpeakableResumed
     * @see #dispatchSpeechEvent
     */
    public void postSpeakableResumed() {
        SpeechEventUtilities.postSpeechEvent(
            this,
            new SpeakableEvent(source, SpeakableEvent.SPEAKABLE_RESUMED));
    }

    /**
     * Utility function that sends a <code>SPEAKABLE_RESUMED</code> event
     * to all speakable listeners.
     *
     * @param event the <code>SPEAKABLE_RESUMED</code> event
     *
     * @see #postSpeakableResumed
     */
    public void fireSpeakableResumed(SpeakableEvent event) {
        if (listener != null) {
            listener.speakableResumed(event);
        }
        

        if (synth.speakableListeners != null) {
            Iterator iterator = synth.speakableListeners.iterator();
            while (iterator.hasNext()) {
                SpeakableListener sl =
                    (SpeakableListener) iterator.next();
                sl.speakableResumed(event);
            }
        }
    }

    /**
     * Utility function that generates a
     * <code>SPEAKABLE_STARTED</code> event and posts it
     * to the event queue.  Eventually
     * <code>fireSpeakableStarted</code> will be called
     * by <code>dispatchSpeechEvent</code> as a result
     * of this action.
     *
     * @see #fireSpeakableStarted
     * @see #dispatchSpeechEvent
     */
    public void postSpeakableStarted() {
        SpeechEventUtilities.postSpeechEvent(
            this,
            new SpeakableEvent(source, SpeakableEvent.SPEAKABLE_STARTED));
    }

    /**
     * Utility function that sends a <code>SPEAKABLE_STARTED</code> event
     * to all speakable listeners.
     *
     * @param event the <code>SPEAKABLE_STARTED</code> event
     *
     * @see #postSpeakableStarted
     */
    public void fireSpeakableStarted(SpeakableEvent event) {
        if (listener != null) {
            listener.speakableStarted(event);
        }
        

        if (synth.speakableListeners != null) {
            Iterator iterator = synth.speakableListeners.iterator();
            while (iterator.hasNext()) {
                SpeakableListener sl = (SpeakableListener) iterator.next();
                sl.speakableStarted(event);
            }
        }
    }

    /**
     * Utility function that generates a
     * <code>TOP_OF_QUEUE</code> event and posts it
     * to the event queue.  Eventually
     * <code>fireTopOfQueue</code> will be called
     * by <code>dispatchSpeechEvent</code> as a result
     * of this action.
     *
     * @see #fireTopOfQueue
     * @see #dispatchSpeechEvent
     */
    public void postTopOfQueue() {
        SpeechEventUtilities.postSpeechEvent(
            this,
            new SpeakableEvent(source, SpeakableEvent.TOP_OF_QUEUE));
    }

    /**
     * Utility function that sends a <code>TOP_OF_QUEUE</code> event
     * to all speakable listeners.
     *
     * @param event the <code>TOP_OF_QUEUE</code> event
     *
     * @see #postTopOfQueue
     */
    public void fireTopOfQueue(SpeakableEvent event) {
        if (listener != null) {
            listener.topOfQueue(event);
        }
        

        if (synth.speakableListeners != null) {
            Iterator iterator = synth.speakableListeners.iterator();
            while (iterator.hasNext()) {
                SpeakableListener sl = (SpeakableListener) iterator.next();
                sl.topOfQueue(event);
            }
        }
    }

    /**
     * Utility function that generates a
     * <code>WORD_STARTED</code> event and posts it
     * to the event queue.  Eventually
     * <code>fireWordStarted</code> will be called
     * by <code>dispatchSpeechEvent</code> as a result
     * of this action.
     *
     * @see #fireWordStarted
     * @see #dispatchSpeechEvent
     */
    public void postWordStarted(String text, int wordStart, int wordEnd) {
        SpeechEventUtilities.postSpeechEvent(
            this,
            new SpeakableEvent(source, SpeakableEvent.WORD_STARTED,
                               text, wordStart, wordEnd));
    }

    /**
     * Utility function that sends a <code>WORD_STARTED</code> event
     * to all speakable listeners.
     *
     * @param event the <code>WORD_STARTED</code> event
     *
     * @see #postWordStarted
     */
    public void fireWordStarted(SpeakableEvent event) {
        if (listener != null) {
            listener.wordStarted(event);
        }
        

        if (synth.speakableListeners != null) {
            Iterator iterator = synth.speakableListeners.iterator();
            while (iterator.hasNext()) {
                SpeakableListener sl = (SpeakableListener) iterator.next();
                sl.wordStarted(event);
            }
        }
    }

    /**
     * Dispatches a <code>SpeechEvent</code>.
     * The dispatcher should notify all <code>EngineListeners</code>
     * from this method.  The <code>SpeechEvent</code> was added
     * via the various post methods of this class.
     *
     * @param event the <code>SpeechEvent</code> to dispatch
     *
     * @see #postMarkerReached
     * @see #postSpeakableCancelled
     * @see #postSpeakableEnded
     * @see #postSpeakablePaused
     * @see #postSpeakableResumed
     * @see #postSpeakableStarted
     * @see #postTopOfQueue
     * @see #postWordStarted
     */
    public void dispatchSpeechEvent(SpeechEvent event) {
        switch (event.getId()) {
            case SpeakableEvent.MARKER_REACHED:
                fireMarkerReached((SpeakableEvent) event);
                break;
            case SpeakableEvent.SPEAKABLE_CANCELLED:
                fireSpeakableCancelled((SpeakableEvent) event);
                break;
            case SpeakableEvent.SPEAKABLE_ENDED:
                fireSpeakableEnded((SpeakableEvent) event);
                break;
            case SpeakableEvent.SPEAKABLE_PAUSED:
                fireSpeakablePaused((SpeakableEvent) event);
                break;
            case SpeakableEvent.SPEAKABLE_RESUMED:
                fireSpeakableResumed((SpeakableEvent) event);
                break;
            case SpeakableEvent.SPEAKABLE_STARTED:
                fireSpeakableStarted((SpeakableEvent) event);
                break;
            case SpeakableEvent.TOP_OF_QUEUE:
                fireTopOfQueue((SpeakableEvent) event);
                break;
            case SpeakableEvent.WORD_STARTED:
                fireWordStarted((SpeakableEvent) event);
                break;
        }
    }
}

