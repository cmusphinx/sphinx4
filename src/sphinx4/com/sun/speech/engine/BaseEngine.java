/**
 * Copyright 1998-2003 Sun Microsystems, Inc.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package com.sun.speech.engine;

import javax.speech.Engine;
import javax.speech.EngineEvent;
import javax.speech.EngineListener;
import javax.speech.EngineModeDesc;
import javax.speech.EngineProperties;
import javax.speech.EngineException;
import javax.speech.EngineStateError;
import javax.speech.AudioManager;
import javax.speech.AudioException;
import javax.speech.VocabManager;
import javax.speech.SpeechEvent;

import java.util.Enumeration;
import java.util.Vector;

/**
 * Supports the JSAPI 1.0 <code>Engine</code> interface.
 * Actual JSAPI implementations might want to extend or modify this
 * implementation.
 */
abstract public class BaseEngine implements Engine, SpeechEventDispatcher {
    /**
     * A bitmask holding the current state of this <code>Engine</code>.
     */
    protected long             engineState;

    /**
     * An <code>Object</code> used for synchronizing access to
     * <code>engineState</code>.
     * @see #engineState
     */
    protected Object           engineStateLock;

    /**
     * List of <code>EngineListeners</code> registered for
     * <code>EngineEvents</code> on this <code>Engine</code>.
     */
    protected Vector           engineListeners;

    /**
     * The <code>AudioManager</code> for this <code>Engine</code>.
     */
    protected AudioManager     audioManager = null;

    /**
     * The <code>EngineModeDesc</code> for this <code>Engine</code>.
     */
    protected EngineModeDesc   engineModeDesc = null;

    /**
     * The <code>EngineProperties</code> for this <code>Engine</code>.
     */
    protected EngineProperties engineProperties = null;

    /**
     * Utility state for clearing the <code>engineState</code>.
     */
    protected final static long CLEAR_ALL_STATE = ~(0L);

    /**
     * Creates a new <code>Engine</code> in the
     * <code>DEALLOCATED</code> state.
     */
    public BaseEngine() {
        this(null);
    }

    /**
     * Creates a new <code>Engine</code> in the
     * <code>DEALLOCATED</code> state.
     *
     * @param desc the operating mode of this <code>Engine</code>
     */
    public BaseEngine(EngineModeDesc desc) {
        engineModeDesc = desc;
        engineListeners = new Vector();
        engineState = DEALLOCATED;
        engineStateLock = new Object();
        engineProperties = createEngineProperties();
    }
    
    /** 
     * Returns a or'ed set of flags indicating the current state of
     * this <code>Engine</code>.
     *
     * <p>An <code>EngineEvent</code> is issued each time this
     * <code>Engine</code> changes state.
     *
     * <p>The <code>getEngineState</code> method can be called successfully
     * in any <code>Engine</code> state.
     *
     * @return the current state of this <code>Engine</code>
     *
     * @see #getEngineState
     * @see #waitEngineState
     */
    public long getEngineState() {
        return engineState;
    }
    
    /** 
     * Blocks the calling thread until this <code>Engine</code>
     * is in a specified state.
     *
     * <p>All state bits specified in the <code>state</code> parameter
     * must be set in order for the method to return, as defined
     * for the <code>testEngineState</code> method.  If the <code>state</code>
     * parameter defines an unreachable state
     * (e.g. <code>PAUSED | RESUMED</code>) an exception is thrown.
     *
     * <p>The <code>waitEngineState</code> method can be called successfully
     * in any <code>Engine</code> state.
     *
     * @param state a bitmask of the state to wait for
     *
     * @see #testEngineState
     * @see #getEngineState
     *
     * @throws InterruptedException
     *   if another thread has interrupted this thread.
     * @throws IllegalArgumentException
     *   if the specified state is unreachable
     */
    public void waitEngineState(long state) 
        throws InterruptedException, IllegalArgumentException {
        synchronized (engineStateLock) {
	    while (!testEngineState(state))
	        engineStateLock.wait();
	}
    }    

    /** 
     * Returns <code>true</code> if this state of this
     * <code>Engine</code> matches the specified state.
     *
     * <p>The test performed is not an exact match to the current
     * state.  Only the specified states are tested.  For
     * example the following returns true only if the
     * <code>Synthesizer</code> queue is empty, irrespective
     * of the pause/resume and allocation states.
     * 
     * <PRE>
     *    if (synth.testEngineState(Synthesizer.QUEUE_EMPTY)) ...
     * </PRE>
     *
     * <p>The <code>testEngineState</code> method is equivalent to:
     * 
     * <PRE>
     *      if ((engine.getEngineState() & state) == state)
     * </PRE>
     *
     * <p>The <code>testEngineState</code> method can be called 
     * successfully in any <code>Engine</code> state.
     *
     * @param state a bitmask of the states to test for
     *
     * @return <code>true</code> if this <code>Engine</code> matches
     *   <code>state</code>; otherwise <code>false</code>
     * @throws IllegalArgumentException
     *   if the specified state is unreachable
     */
    public boolean testEngineState(long state) 
        throws IllegalArgumentException {
        return ((getEngineState() & state) == state);
    }

    /**
     * Updates this <code>Engine</code> state by clearing defined bits,
     * then setting other specified bits.
     *
     * @return a length-2 array with old and new state values.
     */
    protected long[] setEngineState(long clear, long set) {
        long states[] = new long[2];        
        synchronized (engineStateLock) {
	    states[0] = engineState;
	    engineState = engineState & (~clear);
	    engineState = engineState | set;
	    states[1] = engineState;
	    engineStateLock.notifyAll();
	}        
	return states;
    }

    /** 
     * Allocates the resources required for this <code>Engine</code> and
     * puts it into the <code>ALLOCATED</code> state.  When this method
     * returns successfully the <code>ALLOCATED</code> bit of this
     * <code>Engine</code> state is set, and the
     * <code>testEngineState(Engine.ALLOCATED)</code> method returns
     * <code>true</code>.
     *
     * <p>During the processing of the method, this <code>Engine</code> is
     * temporarily in the <code>ALLOCATING_RESOURCES</code> state.
     *
     * @see #deallocate
     *
     * @throws EngineException if this <code>Engine</code> cannot be allocated
     * @throws EngineStateError if this <code>Engine</code> is in the
     *   <code>DEALLOCATING_RESOURCES</code> state
     */
     public void allocate() throws EngineException, EngineStateError {
     if (testEngineState(ALLOCATED)) {
            return;
        }

	long[] states = setEngineState(CLEAR_ALL_STATE, ALLOCATING_RESOURCES);
        postEngineAllocatingResources(states[0], states[1]);

        handleAllocate();        
    }

    /**
     * Called from the <code>allocate</code> method.  Override this in
     * subclasses.
     *
     * @see #allocate
     *
     * @throws EngineException if problems are encountered
     */
    abstract protected void handleAllocate() throws EngineException;

    /** 
     * Frees the resources of this <code>Engine</code> that were
     * acquired during allocation and during operation and return this
     * <code>Engine</code> to the <code>DEALLOCATED</code>.  When this
     * method returns the <code>DEALLOCATED</code> bit of this
     * <code>Engine</code> state is set so the
     * <code>testEngineState(Engine.DEALLOCATED)</code> method returns 
     * <code>true</code>.
     *
     * <p>During the processing of the method, this
     * <code>Engine</code> is temporarily in the
     * <code>DEALLOCATING_RESOURCES</code> state.
     *
     * <p>A deallocated engine can be re-started with a subsequent
     * call to <code>allocate</code>.
     *
     * @see #allocate
     *
     * @throws EngineException if this <code>Engine</code> cannot be
     *   deallocated
     * @throws EngineStateError if this <code>Engine</code> is in the
     *   <code>ALLOCATING_RESOURCES</code> state
     */
    public void deallocate() throws EngineException, EngineStateError {
        if (testEngineState(DEALLOCATED)) {
            return;
        }

	long[] states = setEngineState(CLEAR_ALL_STATE,
                                       DEALLOCATING_RESOURCES);
        postEngineDeallocatingResources(states[0], states[1]);

        handleDeallocate();
    }

    /**
     * Called from the <code>deallocate</code> method.  Override this in
     * subclasses.
     *  
     * @throws EngineException if this <code>Engine</code> cannot be
     *   deallocated.
     */
    abstract protected void handleDeallocate() throws EngineException;

    /** 
     * Pauses the audio stream for this <code>Engine</code> and put
     * this <code>Engine</code> into the <code>PAUSED</code> state.
     *
     * @throws EngineStateError if this <code>Engine</code> is in the
     *   <code>DEALLOCATING_RESOURCES</code> or
     *   <code>DEALLOCATED</code> state.
     */
    public void pause() throws EngineStateError {
        synchronized (engineStateLock) {
            checkEngineState(DEALLOCATED | DEALLOCATING_RESOURCES);
            
            if (testEngineState(PAUSED)) {
                return;
            }
            
            handlePause();
          
            long[] states = setEngineState(RESUMED, PAUSED);
            postEnginePaused(states[0], states[1]);
        }
    }

    /**
     * Called from the <code>pause</code> method.  Override this in subclasses.
     */
    abstract protected void handlePause();

    /** 
     * Resumes the audio stream for this <code>Engine</code> and put
     * this <code>Engine</code> into the <code>RESUMED</code> state.
     *
     * @throws AudioException if unable to gain access to the audio channel
     * @throws EngineStateError if this <code>Engine</code> is in the
     *   <code>DEALLOCATING_RESOURCES</code> or
     *   <code>DEALLOCATED</code> state
     */
    public void resume() throws AudioException, EngineStateError {
        synchronized (engineStateLock) {
            checkEngineState(DEALLOCATED | DEALLOCATING_RESOURCES);
            
            if (testEngineState(RESUMED))
                return;
            
            handleResume();
            
            long[] states = setEngineState(PAUSED, RESUMED);
            postEngineResumed(states[0], states[1]);
        }
    }

    /**
     * Called from the <code>resume</code> method.  Override in subclasses.
     */
    abstract protected void handleResume();
    
    /**
     * Returns an object that provides management of the audio input
     * or output of this <code>Engine</code>.
     *
     * @return the audio manader for this <code>Engine</code>
     */
    public AudioManager getAudioManager() {
	if (audioManager == null) {
	    audioManager = new BaseAudioManager();
	}
        return audioManager;
    }

    /**
     * Returns an object that provides management of the vocabulary for
     * this <code>Engine</code>.  Returns <code>null</code> if this
     * <code>Engine</code> does not support vocabulary management.
     *
     * @return the vocabulary manager of this <code>Engine</code>
     *
     * @throws EngineStateError if this <code>Engine</code> in the
     *   <code>DEALLOCATING_RESOURCES</code> or
     *   <code>DEALLOCATED</code> state
     */
    public VocabManager getVocabManager() throws EngineStateError {
        return null;
    }

    /**
     * Gets the <code>EngineProperties</code> of this <code>Engine</code>.
     * Must be set in subclasses.
     *
     * @return the <code>EngineProperties</code> of this <code>Engine</code>.
     */
    public EngineProperties getEngineProperties() {
        return engineProperties;
    }

    /**
     * Gets the current operating properties and mode of
     * this <code>Engine</code>.
     *
     * @return the operating mode of this <code>Engine</code>
     *
     * @throws SecurityException
     */
    public EngineModeDesc getEngineModeDesc() throws SecurityException {
        return engineModeDesc;
    }

    /**
     * Sets the current operating properties and mode of
     * this <code>Engine</code>.
     *
     * @param desc the new operating mode of this <code>Engine</code>
     */
    protected void setEngineModeDesc(EngineModeDesc desc) {
        engineModeDesc = desc;
    }

    /**
     * Requests notification of <code>EngineEvents</code> from this
     * <code>Engine</code>.
     *
     * @param listener the listener to add.
     */
    public void addEngineListener(EngineListener listener) {
        synchronized (engineListeners) {
            if (!engineListeners.contains(listener)) {
                engineListeners.addElement(listener);
            }
        }
    }

    /**
     * Removes an <code>EngineListener</code> from the list of
     * <code>EngineListeners</code>.
     *
     * @param listener the listener to remove.
     */
    public void removeEngineListener(EngineListener listener) {
        synchronized (engineListeners) {
            engineListeners.removeElement(listener);
        }
    }
    
    /**
     * Utility function that generates an
     * <code>ENGINE_ALLOCATED</code> event and posts it
     * to the event queue.  Eventually
     * <code>fireEngineAllocated</code> will be called
     * by the <code>dispatchSpeechEvent</code> as a result of this
     * action.
     *
     * @param oldState the old state of this <code>Engine</code>
     * @param newState the new state of this <code>Engine</code>
     *
     * @see #fireEngineAllocated
     * @see #dispatchSpeechEvent
     */
    protected void postEngineAllocated(long oldState, long newState) {
        SpeechEventUtilities.postSpeechEvent(
            this,
            new EngineEvent(
                this,
                EngineEvent.ENGINE_ALLOCATED,
                oldState, newState));
    }
    
    /**
     * Utility function that sends an <code>ENGINE_ALLOCATED</code>
     * event to all <code>EngineListeners</code> registered with this
     * <code>Engine</code>.  Called by <code>dispatchSpeechEvent</code>.
     *
     * @param event the <code>ENGINE_ALLOCATED</code> event
     *
     * @see #postEngineAllocated
     * @see #dispatchSpeechEvent
     */
    public void fireEngineAllocated(EngineEvent event) {
	if (engineListeners == null) {
	    return;
	}
        Enumeration E = engineListeners.elements();
        while (E.hasMoreElements()) {
            EngineListener el = (EngineListener) E.nextElement();
            el.engineAllocated(event);
        }
    }
    
    /**
     * Utility function that generates an
     * <code>ENGINE_ALLOCATING_RESOURCES</code> event and
     * posts it to the event queue.  Eventually
     * <code>fireEngineAllocatingResources</code>
     * will be called by <code>dispatchSpeechEvent</code> as a
     * result of this action.
     *
     * @param oldState the old state of this <code>Engine</code>
     * @param newState the new state of this <code>Engine</code>
     *
     * @see #fireEngineAllocatingResources
     * @see #dispatchSpeechEvent
     */
    protected void postEngineAllocatingResources(long oldState,
                                                 long newState) {
        SpeechEventUtilities.postSpeechEvent(
            this,
            new EngineEvent(
                this,
                EngineEvent.ENGINE_ALLOCATING_RESOURCES,
                oldState, newState));
    }
    
    /**
     * Utility function that sends an
     * <code>ENGINE_ALLOCATING_RESOURCES</code> event to all
     * <code>EngineListeners</code> registered with this
     * <code>Engine</code>.  Called by <code>dispatchSpeechEvent</code>.
     *
     * @param event the <code>ENGINE_ALLOCATING_RESOURCES</code> event
     *
     * @see #postEngineAllocatingResources
     * @see #dispatchSpeechEvent
     */
    public void fireEngineAllocatingResources(EngineEvent event) {
	if (engineListeners == null) {
	    return;
	}
        Enumeration E = engineListeners.elements();
        while (E.hasMoreElements()) {
            EngineListener el = (EngineListener) E.nextElement();
            el.engineAllocatingResources(event);
        }
    }
    
    /**
     * Utility function that generates an
     * <code>ENGINE_DEALLOCATED</code> event and posts it
     * to the event queue.  Eventually
     * <code>fireEngineDeallocated</code> will be called
     * by <code>dispatchSpeechEvent</code> as a result of this action.
     *
     * @param oldState the old state of this <code>Engine</code>
     * @param newState the new state of this <code>Engine</code>
     *
     * @see #fireEngineDeallocated
     * @see #dispatchSpeechEvent
     */
    protected void postEngineDeallocated(long oldState, long newState) {
        SpeechEventUtilities.postSpeechEvent(
            this,
            new EngineEvent(
                this,
                EngineEvent.ENGINE_DEALLOCATED,
                oldState, newState));
    }
    
    /**
     * Utility function that sends an
     * <code>ENGINE_DEALLOCATED</code> event to all
     * <code>EngineListeners</code> registered with this
     * <code>Engine</code>.  Called by <code>dispatchSpeechEvent</code>.
     *
     * @param event the <code>ENGINE_DEALLOCATED</code> event
     *
     * @see #postEngineDeallocated
     * @see #dispatchSpeechEvent
     */
    public void fireEngineDeallocated(EngineEvent event) {
	if (engineListeners == null) {
	    return;
	}
        Enumeration E = engineListeners.elements();
        while (E.hasMoreElements()) {
            EngineListener el = (EngineListener) E.nextElement();
            el.engineDeallocated(event);
        }
    }
    
    /**
     * Utility function that generates
     * <code>ENGINE_DEALLOCATING_RESOURCES</code> event and
     * posts it to the event queue.  Eventually
     * <code>fireEngineAllocatingResources</code> will be called
     * by <code>dispatchSpeechEvent</code> as a result of this action.
     *
     * @param oldState the old state of this <code>Engine</code>
     * @param newState the new state of this <code>Engine</code>
     *
     * @see #fireEngineDeallocatingResources
     * @see #dispatchSpeechEvent
     */
    protected void postEngineDeallocatingResources(long oldState,
                                                   long newState) {
        SpeechEventUtilities.postSpeechEvent(
            this,
            new EngineEvent(
                this,
                EngineEvent.ENGINE_DEALLOCATING_RESOURCES,
                oldState, newState));
    }
    
    /**
     * Utility function that sends a
     * <code>ENGINE_DEALLOCATING_RESOURCES</code> event to all
     * <code>EngineListeners</code> registered with this
     * <code>Engine</code>.  Called by <code>dispatchSpeechEvent</code>.
     *
     * @param event the <code>ENGINE_DEALLOCATING_RESOURCES</code> event
     *
     * @see #postEngineDeallocatingResources
     * @see #dispatchSpeechEvent
     */
    public void fireEngineDeallocatingResources(EngineEvent event) {
	if (engineListeners == null) {
	    return;
	}
        Enumeration E = engineListeners.elements();
        while (E.hasMoreElements()) {
            EngineListener el = (EngineListener) E.nextElement();
            el.engineDeallocatingResources(event);
        }
    }
    
    /**
     * Utility function that generates an
     * <code>ENGINE_PAUSED</code> event and posts it
     * to the event queue.  Eventually
     * <code>fireEnginePaused</code> will be called
     * by <code>dispatchSpeechEvent</code> as a result of this action.
     *
     * @param oldState the old state of this <code>Engine</code>
     * @param newState the new state of this <code>Engine</code>
     *
     * @see #fireEnginePaused
     * @see #dispatchSpeechEvent
     */
    protected void postEnginePaused(long oldState, long newState) {
        SpeechEventUtilities.postSpeechEvent(
            this,
            new EngineEvent(
                this,
                EngineEvent.ENGINE_PAUSED,
                oldState, newState));
    }
    
    /**
     * Utility function that sends an <code>ENGINE_PAUSED</code> event
     * to all
     * <code>EngineListeners</code> registered with this
     * <code>Engine</code>.  Called by <code>dispatchSpeechEvent</code>.
     *
     * @param event the <code>ENGINE_PAUSED</code> event
     *
     * @see #postEnginePaused
     * @see #dispatchSpeechEvent
     */
    public void fireEnginePaused(EngineEvent event) {
	if (engineListeners == null) {
	    return;
	}
        Enumeration E = engineListeners.elements();
        while (E.hasMoreElements()) {
            EngineListener el = (EngineListener) E.nextElement();
            el.enginePaused(event);
        }
    }

    /**
     * Utility function that generates an <code>ENGINE_RESUMED</code>
     * event and posts it to the event queue.  Eventually
     * <code>fireEngineResumed</code> will be called
     * by <code>dispatchSpeechEvent</code> as a result of this action.
     *
     * @param oldState the old state of this <code>Engine</code>
     * @param newState the new state of this <code>Engine</code>
     *
     * @see #fireEngineResumed
     * @see #dispatchSpeechEvent
     */
    protected void postEngineResumed(long oldState, long newState) {
        SpeechEventUtilities.postSpeechEvent(
            this,
            new EngineEvent(
                this,
                EngineEvent.ENGINE_RESUMED,
                oldState, newState));
    }
    
    /**
     * Utility function that sends an <code>ENGINE_RESUMED</code> event
     * to all
     * <code>EngineListeners</code> registered with this
     * <code>Engine</code>.  Called by <code>dispatchSpeechEvent</code>.
     *
     * @param event the <code>ENGINE_RESUMED</code> event
     *
     * @see #postEngineResumed
     * @see #dispatchSpeechEvent
     */
    public void fireEngineResumed(EngineEvent event) {
	if (engineListeners == null) {
	    return;
	}
        Enumeration E = engineListeners.elements();
        while (E.hasMoreElements()) {
            EngineListener el = (EngineListener) E.nextElement();
            el.engineResumed(event);
        }
    }

    /**
     * Factory constructor for EngineProperties object.
     *
     * @return a <code>BaseEngineProperties</code> object specific to
     *   a subclass.
     */
    abstract protected BaseEngineProperties createEngineProperties();

    /**
     * Convenience method that throws an <code>EngineStateError</code>
     * if any of the bits in the passed state are set in the
     * <code>state</code>.
     *
     * @param state the <code>Engine</code> state to check
     *
     * @throws EngineStateError if any of the bits in the passed state
     *   are set in the <code>state</code>
     */
    protected void checkEngineState(long state) throws EngineStateError {
        long currentState = getEngineState();
        if ((currentState & state) != 0) {
            throw new EngineStateError
		("Invalid EngineState: expected=(" 
		 + stateToString(state) + ") current state=("
		 + stateToString(currentState) + ")");
        }
    }

    /**
     * Returns a <code>String</code> of the names of all the
     * <code>Engine</code> states in the given <code>Engine</code>
     * state.
     *
     * @param state the bitmask of states
     *
     * @return a <code>String</code> containing the names of all the
     *   states set in <code>state</code>
     */
    protected String stateToString(long state) {
	StringBuffer buf = new StringBuffer();
        if ((state & Engine.DEALLOCATED) != 0)
            buf.append(" DEALLOCATED ");
        if ((state & Engine.ALLOCATING_RESOURCES) != 0)
            buf.append(" ALLOCATING_RESOURCES ");
        if ((state & Engine.ALLOCATED) != 0)
            buf.append(" ALLOCATED ");
        if ((state & Engine.DEALLOCATING_RESOURCES) != 0)
            buf.append(" DEALLOCATING_RESOURCES ");
        if ((state & Engine.PAUSED) != 0)
            buf.append(" PAUSED ");
        if ((state & Engine.RESUMED) != 0)
            buf.append(" RESUMED ");
        return buf.toString();
    }
    
    /**
     * Dispatches a <code>SpeechEvent</code>.
     * The dispatcher should notify all <code>EngineListeners</code>
     * from this method.  The <code>SpeechEvent</code> was added
     * via the various post methods of this class.
     *
     * @param event the <code>SpeechEvent</code> to dispatch
     *
     * @see #postEngineAllocatingResources
     * @see #postEngineAllocated
     * @see #postEngineDeallocatingResources
     * @see #postEngineDeallocated
     * @see #postEnginePaused
     * @see #postEngineResumed
     */
    public void dispatchSpeechEvent(SpeechEvent event) {
        switch (event.getId()) {
            case EngineEvent.ENGINE_ALLOCATED:
                fireEngineAllocated((EngineEvent) event);
                break;
            case EngineEvent.ENGINE_ALLOCATING_RESOURCES:
                fireEngineAllocatingResources((EngineEvent) event);
                break;
            case EngineEvent.ENGINE_DEALLOCATED:
                fireEngineDeallocated((EngineEvent) event);
                break;
            case EngineEvent.ENGINE_DEALLOCATING_RESOURCES:
                fireEngineDeallocatingResources((EngineEvent) event);
                break;
            //case EngineEvent.ENGINE_ERROR:
            //fireEngineError((EngineErrorEvent) event);
            //break;
            case EngineEvent.ENGINE_PAUSED:
                fireEnginePaused((EngineEvent) event);
                break;
            case EngineEvent.ENGINE_RESUMED:
                fireEngineResumed((EngineEvent) event);
                break;
        }
    }

    /**
     * Returns the engine name and mode for debug purposes.
     *
     * @return the engine name and mode.
     */
    public String toString() {
        return getEngineModeDesc().getEngineName() + 
            ":" + getEngineModeDesc().getModeName();
    }
}
