/**
 * Copyright 1998-2003 Sun Microsystems, Inc.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */

package com.sun.speech.engine.recognition;

import javax.speech.*;
import javax.speech.recognition.*;

import java.net.*;
import java.io.*;
import java.util.*;

import com.sun.speech.engine.*;

import com.sun.speech.engine.SpeechEventUtilities;
import com.sun.speech.engine.SpeechEventDispatcher;

/**
 * Skeletal Implementation of the JSAPI Recognizer interface.
 * 
 * This class is useful by itself for debugging, e.g. you
 * can load grammars and simulate a recognizer recognizing
 * some text, etc.
 * <P>
 *
 * Actual JSAPI recognizer implementations might want to extend or
 * modify this implementation.
 * <P>
 *
 * Also contains utility routines for:
 * <UL>
 *  <LI>Loading imported grammars and resolving inter-grammar
 *      references.
 *
 *  <LI>Printing/dumping grammars in an extensible way
 *      (used to dump grammar to under-lying recognizer
 *       implementation via ascii strins)
 *
 *  <LI>Routines for copying grammars from one recognizer implementation
 *      to another.
 * </UL>
 *
 * @author Stuart Adams
 * @version 1.20 09/10/99 17:02:58
 */
public class BaseRecognizer extends BaseEngine
    implements Recognizer, SpeechEventDispatcher {
    protected Vector    resultListeners;
    protected Hashtable grammarList;
    protected boolean   caseSensitiveGrammarNames = true;
    protected boolean   hasModalGrammars = false;

    protected boolean supportsNULL = true;
    protected boolean supportsVOID = true;

    // used when printing grammars
    public RuleGrammar currentGrammar = null;  
  
    // Set to true if recognizer cannot handle partial
    // grammar loading.
    protected boolean reloadAll = false;          

//////////////////////
// Begin Constructors
//////////////////////
    /**
     * Create a new Recognizer in the DEALLOCATED state.
     */
    public BaseRecognizer() {
	this(false,null);
    }

    /**
     * Create a new Recognizer in the DEALLOCATED state.
     */
    public BaseRecognizer(RecognizerModeDesc mode) {
	this(false,mode);
    }

    /**
     * Create a new Recognizer in the DEALLOCATED state.
     * @param reloadAll set to true if recognizer cannot handle 
     * partial grammar loading.  Default = false.
     */
    public BaseRecognizer(boolean reloadAll, RecognizerModeDesc mode) {
	super(mode);
        this.reloadAll = reloadAll;
        resultListeners = new Vector();
        grammarList = new Hashtable();
	audioManager = new BaseRecognizerAudioManager();
    }
//////////////////////
// End Constructors
//////////////////////

//////////////////////
// Begin overridden Engine Methods
//////////////////////
    /**
     * Allocate the resources for the Engine and put it in the ALLOCATED,
     * RESUMED, QUEUE_EMPTY state.
     */
    public void allocate() 
        throws EngineException, EngineStateError {
        // We don't need the following steps to be atomic
        // so there's no need to synchronize on engineStateLock
        if (testEngineState(ALLOCATED)) {
            return;
        }
        
        // Temporarily go in to the ALLOCATING_RESOURCES state.
        long[] states = setEngineState(CLEAR_ALL_STATE, ALLOCATING_RESOURCES);
        postEngineAllocatingResources(states[0], states[1]);
        
        // Go in to the ALLOCATED, RESUMED, LISTENING, and FOCUS_ON states.
        // Subclasses with shared engines should check all states before
        // changing them here.
        synchronized (engineStateLock) {
            long newState = ALLOCATED | RESUMED | LISTENING | FOCUS_ON;
            states = setEngineState(CLEAR_ALL_STATE, newState);
        }
        postEngineAllocated(states[0], states[1]);
    }

    /**
     * Deallocate the resources for the Engine and put it in the 
     * DEALLOCATED state.
     */

    public void deallocate()
      throws EngineException, EngineStateError
    {            
      // We don't need the following steps to be atomic
      // so there's no need to synchronize on engineStateLock

      if (testEngineState(DEALLOCATED))
	  return;

      // Clean up the focus state
      releaseFocus();

      // Temporarily go in to the DEALLOCATING_RESOURCES state.
      // Make sure we kill the PAUSE/RESUME, LISTENING, FOCUS states etc.
      long[] states = setEngineState(CLEAR_ALL_STATE, DEALLOCATING_RESOURCES);
      postEngineDeallocatingResources(states[0], states[1]);
      
      // Go in to the DEALLOCATED state.
      states = setEngineState(CLEAR_ALL_STATE, DEALLOCATED);
      postEngineDeallocated(states[0], states[1]);
    }


    /**
     * Return an object that provides management of the audio input
     * or output of the Engine.
     * From javax.speech.Engine.
     */
    public AudioManager getAudioManager() {
	if (audioManager == null) {
	    audioManager = new BaseRecognizerAudioManager();
	}
        return audioManager;
    }    
//////////////////////
// End overridden Engine Methods
//////////////////////
    
//////////////////////
// Begin Recognizer Methods
//////////////////////
    /**
     * Create a new RuleGrammar with the given name.
     * From javax.speech.recognition.Recognizer.
     * @param name the name of the RuleGrammar.
     */
    public RuleGrammar newRuleGrammar(String name) 
        throws IllegalArgumentException, EngineStateError
    {
        checkEngineState(DEALLOCATED | DEALLOCATING_RESOURCES);
        BaseRuleGrammar G = new BaseRuleGrammar(this, name);
        storeGrammar(G);
        return G;
    }
    
    /** 
     * NOT IMPLEMENTED YET.
     * We use a JavaCC generated parser for reading jsgf file. It 
     * does not support reading from java.io.Reader yet - once it
     * does we will implement this method.  The current implementation
     * assumes the Reader can be converted into an ASCII input stream.
     * From javax.speech.recognition.Recognizer.
     * @param JSGFinput the Reader containing JSGF input.
     */
    public RuleGrammar loadJSGF(Reader JSGFinput) 
        throws GrammarException, IOException, EngineStateError
    {
        checkEngineState(DEALLOCATED | DEALLOCATING_RESOURCES);
        
        RuleGrammar G = JSGFParser.newGrammarFromJSGF(JSGFinput, 
                                                      (Recognizer)this);
        if (G == null) {
            throw new IOException();	// Should never happen
        }
        if (G.getName() != null) {
            storeGrammar(G);
        }
        return G;
    }

    /**
     * Load a RuleGrammar and its imported grammars from a URL containing
     * JSGF text.
     * From javax.speech.recognition.Recognizer.
     * @param baseURL the base URL containing the JSGF grammar file.
     * @param grammarName the name of the JSGF grammar to load.
     */
    public RuleGrammar loadJSGF(java.net.URL baseURL, String grammarName) 
        throws GrammarException, MalformedURLException, 
            IOException, EngineStateError 
    {
        checkEngineState(DEALLOCATED | DEALLOCATING_RESOURCES);

	return loadJSGF(baseURL,grammarName,true,false,null);
    }

  private static URL gnameToURL(URL baseURL, String grammarName) throws MalformedURLException {
    
    // Convert each period in the grammar name to a slash "/"
    // Append a slash and the converted grammar name to the base URL
    // Append the ".gram" suffix
    grammarName = grammarName.replace('.', '/');
    String urlstr = "";
    if (baseURL != null) urlstr = baseURL.toString();
    urlstr = urlstr.concat(((urlstr.lastIndexOf((int)'/')
			     == (urlstr.length() - 1)) 
			    ? "" 
			    : "/") + grammarName + ".gram");
    
    URL grammarURL = null;
    try {
      grammarURL = new URL(urlstr);
    }
    catch (MalformedURLException me) {
      grammarURL = ClassLoader.getSystemResource(urlstr);
      if (grammarURL == null) 
	throw new MalformedURLException(urlstr);
    }
    
    return grammarURL;
  }
      
   /** 
     * 
     * From javax.speech.recognition.Recognizer.
     */
    public RuleGrammar loadJSGF(URL context, String grammarName,
                                boolean loadImports, boolean reloadGrammars,
                                Vector loadedGrammars) 
        throws GrammarException, MalformedURLException, 
            IOException, EngineStateError 
    {
        checkEngineState(DEALLOCATED | DEALLOCATING_RESOURCES);
	
        URL grammarURL = gnameToURL(context, grammarName);

        RuleGrammar G = JSGFParser.newGrammarFromJSGF(grammarURL, this);

        if (G == null) {
            throw new IOException();	// Should never happen
        }

        if (loadImports) loadImports(this,G,context,true,reloadGrammars,loadedGrammars);

        if (G.getName() != null) {
            storeGrammar(G);
        }
        return G;
    }
    
    /**
     * Return the named RuleGrammar 
     * From javax.speech.recognition.Recognizer.
     * @param name the name of the RuleGrammar.
     */
    public RuleGrammar getRuleGrammar(String name) throws EngineStateError {
        checkEngineState(DEALLOCATED | DEALLOCATING_RESOURCES);
        return retrieveGrammar(name);
    }

    /**
     * Get a list of loaded or defined RuleGrammars known to the
     * Recognizer.
     * From javax.speech.recognition.Recognizer.
     */
    public RuleGrammar[] listRuleGrammars() throws EngineStateError {
        checkEngineState(DEALLOCATED | DEALLOCATING_RESOURCES);
	if (grammarList == null) {
	    return new RuleGrammar[0];
	}
        RuleGrammar rl[] = new RuleGrammar[grammarList.size()];
        int i=0;
        Enumeration e = grammarList.elements();
        while (e.hasMoreElements()) {
            rl[i++] = (RuleGrammar) e.nextElement(); 
        }
        return rl;
    }
    
    /**
     * Delete a RuleGrammar from the Recognizer.
     * From javax.speech.recognition.Recognizer.
     * @param grammar the RuleGrammar to delete.
     */
    public void deleteRuleGrammar(RuleGrammar grammar) 
        throws IllegalArgumentException, EngineStateError 
    {
        checkEngineState(DEALLOCATED | DEALLOCATING_RESOURCES);
        String name = grammar.getName();
        grammarList.remove(name);
    }
    
    /**
     * Get the DicationGrammar for this Recognizer.
     * Always returns null.  Should be overridden by
     * recognizers that support dictation.
     */
    public DictationGrammar getDictationGrammar(String name) 
        throws EngineStateError 
    {
        checkEngineState(DEALLOCATED | DEALLOCATING_RESOURCES);
        return null;
    }

    /** 
     * Commit grammar changes.
     * From javax.speech.recognition.Recognizer.
     */
    public void commitChanges()
        throws GrammarException, EngineStateError
    {
        checkEngineState(DEALLOCATED | DEALLOCATING_RESOURCES);

	// All cases:
	//   If there is a pending suspend call, it can be cleared.
	//   THIS IS NOT IMPLEMENTED YET
	// Case 1:
	//   If the recognizer is in the LISTENING state we should
	//   first transition to the SUSPENDED state, then commit,
	//   then return the LISTENING state.
	// Case 2: 
	//   If the recognizer is in the SUSPENDED state, we remain
	//   in that state, commit, then return to LISTENING state.
	//   EXCEPTION: if the recognizer is still issuing events
	//   for a Result that brought it to the SUSPENDED state,
	//   the commit should be deferred until the event processing
	//   is complete -- THIS IS NOT IMPLEMENTED YET.
	// Case 3:
	//   If the recognizer is in the PROCESSING state, the
	//   commit should be deferred until following result is
	//   finalized and appropriate events have been issued.

	// The following hack avoids inappropriately going from
	// PROCESSING to LISTENING states.  It does not correctly
	// handle clearing of explicit suspend requests.
	boolean goBackToListening = false;

	if (testEngineState(LISTENING)) {
	  goBackToListening = true;
	  long[] states = setEngineState(LISTENING | PROCESSING, SUSPENDED);
	  postRecognizerSuspended(states[0], states[1]);
	}
        
	// If we're in the PROCESSING state we shouldn't modify grammars.

	// Prepare the grammars to be committed
        linkGrammars();

        //GRAMMAR_CHANGES_COMMITTED events are sent in commitChangesInternal

        // Activate or Deactivate any grammars
        checkForModalGrammars();
        notifyGrammarActivation();

	if (goBackToListening) {
  	    long[] states = setEngineState(SUSPENDED | PROCESSING, LISTENING);
	    postChangesCommitted(states[0], states[1], null);
	}
    }

    /**
     * Temporarily suspend recognition while the application updates
     * grammars prior to a call to commitChanges.
     */
    public void suspend() throws EngineStateError {
        checkEngineState(DEALLOCATED | DEALLOCATING_RESOURCES);

        //[[[WDW - need to set a flag saying we got here via a method call
        //from the application.]]]

        if (testEngineState(SUSPENDED)) {
            return;
        }

	long[] states = setEngineState(LISTENING | PROCESSING, SUSPENDED);
	postRecognizerSuspended(states[0], states[1]);
    }
    
    /**
     * NOT IMPLEMENTED YET.
     * If the Recognizer is in the PROCESSING state, force the Recognizer
     * to immediately complete processing of that result by finalizing it.
     * From javax.speech.recognition.Recognizer.
     * @param flush whether audio buffer should be processed or flushed.
     */
    public void forceFinalize(boolean flush) throws EngineStateError {
        checkEngineState(DEALLOCATED | DEALLOCATING_RESOURCES);
        throw new RuntimeException(
            "forceFinalize not yet implemented.");
    }

    /**
     * Request speech focus for this Recognizer from the underlying speech
     * recognition system.
     */
    public void requestFocus() throws EngineStateError {
        checkEngineState(DEALLOCATED | DEALLOCATING_RESOURCES);

	// Do nothing if the state is already OK
        if (testEngineState(FOCUS_ON))
            return;

	long[] states = setEngineState(FOCUS_OFF, FOCUS_ON);
	postFocusGained(states[0], states[1]);

        notifyGrammarActivation();        
    }
    
    /**
     * Release speech focus for this Recognizer from the underlying speech
     * recognition system.
     */
    public void releaseFocus() throws EngineStateError {
        checkEngineState(DEALLOCATED | DEALLOCATING_RESOURCES);

        if (testEngineState(FOCUS_OFF))
            return;

	long[] states = setEngineState(FOCUS_ON, FOCUS_OFF);
	postFocusLost(states[0], states[1]);

        notifyGrammarActivation();        
    }

    /**
     * Request notification of Result events from the Recognizer.
     * From javax.speech.recognition.Recognizer.
     * @param listener the listener to add.
     */
    public void addResultListener(ResultListener listener) {
        if (!resultListeners.contains(listener)) {
            resultListeners.addElement(listener);
        }
    }

    /**
     * Remove a ResultListener from the list of ResultListeners.
     * From javax.speech.recognition.Recognizer.
     * @param listener the listener to remove.
     */
    public void removeResultListener(ResultListener listener) {
        resultListeners.removeElement(listener);
    }

    /**
     * Get the RecognizerProperties of this Recognizer.
     * From javax.speech.recognition.Recognizer.
     */
    public RecognizerProperties getRecognizerProperties() {
        return (RecognizerProperties) getEngineProperties();
    }

    /**
     * NOT IMPLEMENTED YET.
     * Get the object that manages the speakers of the Recognizer.
     * From javax.speech.recognition.Recognizer.
     */
    public SpeakerManager getSpeakerManager() {
        return null;
    }

    /**
     * Create a new gramar by reading in a grammar stored in a
     * vendor-specific format.  Since BaseGrammar is serializable we just 
     * use read/write object to store/restore grammars.
     * From javax.speech.recognition.Recognizer.
     */
    public Grammar readVendorGrammar(InputStream input) 
        throws VendorDataException, IOException, EngineStateError
    {
        checkEngineState(DEALLOCATED | DEALLOCATING_RESOURCES);
        BaseGrammar G = null;
        try {
            ObjectInputStream p = new ObjectInputStream(input);
            G = (BaseGrammar)p.readObject();
        } catch (Exception e) {
            RecognizerUtilities.debugMessageOut("ERROR: readVendorGrammar: " +
                                                e);
        }
        return (Grammar) G;
    }

    /**
     * Create a new grammar by reading in a grammar stored in a
     * vendor-specific format.  Since BaseGrammar is serializable we just 
     * use read/write object to store/restore grammars.
     * From javax.speech.recognition.Recognizer.
     */
    public void writeVendorGrammar(OutputStream output, Grammar gram) 
        throws IOException, EngineStateError 
    {
        checkEngineState(DEALLOCATED | DEALLOCATING_RESOURCES);
        ObjectOutputStream p = new ObjectOutputStream(output);
        p.writeObject(gram);
    }

    /**
     * Read a Result from a stream in a vendor-specific format.
     * Since BaseResult is serializable we just 
     * use read/write object to store/restore grammars.
     * From javax.speech.recognition.Recognizer.
     */
    public Result readVendorResult(InputStream output) 
        throws VendorDataException, IOException, EngineStateError 
    {
        checkEngineState(DEALLOCATED | DEALLOCATING_RESOURCES);
        BaseResult res = null;
        try {
            ObjectInputStream p = new ObjectInputStream(output);
            res = (BaseResult)p.readObject();
        } catch (Exception e) {
            RecognizerUtilities.debugMessageOut("ERROR: readVendorResult: " + 
                                                e);
        }
        return (FinalResult) res;
    }

    /**
     * Store a Result to a stream in a vendor-specific format.
     * Since BaseResult is serializable we just 
     * use read/write object to store/restore grammars.
     * From javax.speech.recognition.Recognizer.
     */
    public void writeVendorResult(OutputStream output, Result result) 
        throws IOException, ResultStateError, EngineStateError 
    {
        checkEngineState(DEALLOCATED | DEALLOCATING_RESOURCES);
        ObjectOutputStream p = new ObjectOutputStream(output);
        p.writeObject(result);
    }
//////////////////////
// End Recognizer Methods
//////////////////////

//////////////////////
// Begin utility methods for sending ResultEvents
//////////////////////
    /**
     * Utility function to generate AUDIO_RELEASED event and post it
     * to the event queue.  Eventually fireAudioReleased will be called
     * by dispatchSpeechEvent as a result of this action.
     */
    public void postAudioReleased(Result result) {
        SpeechEventUtilities.postSpeechEvent(
            this,
            new ResultEvent(result, ResultEvent.AUDIO_RELEASED));
    }

    /**
     * Utility function to send a AUDIO_RELEASED event to all result
     * listeners.  
     */
    public void fireAudioReleased(ResultEvent event) {
        Enumeration E;
	if (resultListeners != null) {
            E = resultListeners.elements();
            while (E.hasMoreElements()) {
                ResultListener rl = (ResultListener) E.nextElement();
                rl.audioReleased(event);
            }
        }
    }

    /**
     * Utility function to generate GRAMMAR_FINALIZED event and post it
     * to the event queue.  Eventually fireGrammarFinalized will be called
     * by dispatchSpeechEvent as a result of this action.
     */
    public void postGrammarFinalized(Result result) {
        SpeechEventUtilities.postSpeechEvent(
            this,
            new ResultEvent(result, ResultEvent.GRAMMAR_FINALIZED));
    }

    /**
     * Utility function to send a GRAMMAR_FINALIZED event to all result
     * listeners.  
     */
    public void fireGrammarFinalized(ResultEvent event) {
        Enumeration E;
	if (resultListeners != null) {
            E = resultListeners.elements();
            while (E.hasMoreElements()) {
                ResultListener rl = (ResultListener) E.nextElement();
                rl.grammarFinalized(event);
            }
        }
    }

    /**
     * Utility function to generate RESULT_ACCEPTED event and post it
     * to the event queue.  Eventually fireResultAccepted will be called
     * by dispatchSpeechEvent as a result of this action.
     */
    public void postResultAccepted(Result result) {
        SpeechEventUtilities.postSpeechEvent(
            this,
            new ResultEvent(result, ResultEvent.RESULT_ACCEPTED));
    }

    /**
     * Utility function to send a RESULT_ACCEPTED event to all result
     * listeners.  
     */
    public void fireResultAccepted(ResultEvent event) {
        Enumeration E;
	if (resultListeners != null) {
            E = resultListeners.elements();
            while (E.hasMoreElements()) {
                ResultListener rl = (ResultListener) E.nextElement();
                rl.resultAccepted(event);
            }
        }
    }

    /**
     * Utility function to generate RESULT_CREATED event and post it
     * to the event queue.  Eventually fireResultCreated will be called
     * by dispatchSpeechEvent as a result of this action.
     */
    public void postResultCreated(Result result) {
        SpeechEventUtilities.postSpeechEvent(
            this,
            new ResultEvent(result, ResultEvent.RESULT_CREATED));
    }

    /**
     * Utility function to send a RESULT_CREATED event to all result
     * listeners.  
     */
    public void fireResultCreated(ResultEvent event) {
        Enumeration E;
	if (resultListeners != null) {
            E = resultListeners.elements();
            while (E.hasMoreElements()) {
                ResultListener rl = (ResultListener) E.nextElement();
                rl.resultCreated(event);
            }
        }
    }

    /**
     * Utility function to generate RESULT_REJECTED event and post it
     * to the event queue.  Eventually fireResultRejected will be called
     * by dispatchSpeechEvent as a result of this action.
     */
    public void postResultRejected(Result result) {
        SpeechEventUtilities.postSpeechEvent(
            this,
            new ResultEvent(result, ResultEvent.RESULT_REJECTED));
    }

    /**
     * Utility function to send a RESULT_REJECTED event to all result
     * listeners.  
     */
    public void fireResultRejected(ResultEvent event) {
        Enumeration E;
	if (resultListeners != null) {
            E = resultListeners.elements();
            while (E.hasMoreElements()) {
                ResultListener rl = (ResultListener) E.nextElement();
                rl.resultRejected(event);
            }
        }
    }

    /**
     * Utility function to generate RESULT_UPDATED event and post it
     * to the event queue.  Eventually fireResultUpdated will be called
     * by dispatchSpeechEvent as a result of this action.
     */
    public void postResultUpdated(Result result) {
        SpeechEventUtilities.postSpeechEvent(
            this,
            new ResultEvent(result, ResultEvent.RESULT_UPDATED));
    }

    /**
     * Utility function to send a RESULT_UPDATED event to all result
     * listeners.  
     */
    public void fireResultUpdated(ResultEvent event) {
        Enumeration E;
	if (resultListeners != null) {
            E = resultListeners.elements();
            while (E.hasMoreElements()) {
                ResultListener rl = (ResultListener) E.nextElement();
                rl.resultUpdated(event);
            }
        }
    }

    /**
     * Utility function to generate TRAINING_INFO_RELEASED event and post it
     * to the event queue.  Eventually fireTrainingInfoReleased will be called
     * by dispatchSpeechEvent as a result of this action.
     */
    public void postTrainingInfoReleased(Result result) {
        SpeechEventUtilities.postSpeechEvent(
            this,
            new ResultEvent(result, ResultEvent.TRAINING_INFO_RELEASED));
    }

    /**
     * Utility function to send a TRAINING_INFO_RELEASED event to all result
     * listeners.  
     */
    public void fireTrainingInfoReleased(ResultEvent event) {
        Enumeration E;
	if (resultListeners != null) {
            E = resultListeners.elements();
            while (E.hasMoreElements()) {
                ResultListener rl = (ResultListener) E.nextElement();
                rl.trainingInfoReleased(event);
            }
        }
    }
//////////////////////
// End utility methods for sending ResultEvents
//////////////////////

//////////////////////
// NON-JSAPI METHODS
//////////////////////
    /**
     * Add a grammar to the grammar list.
     */
    protected void storeGrammar(RuleGrammar G) {
	if (caseSensitiveGrammarNames) {
	    grammarList.put(G.getName(),G);
	} else {
	    grammarList.put(G.getName().toLowerCase(),G);
        }	
    }

    /**
     * Retrieve a grammar from the grammar list.
     */
    protected RuleGrammar retrieveGrammar(String name) {
	if (caseSensitiveGrammarNames) {
	    return (RuleGrammar) grammarList.get(name);
	} else {
	    return (RuleGrammar) grammarList.get(name.toLowerCase());
        }	
    }

    /**
     * NOT JSAPI.
     * In the mean time since the above method is not implemented we support
     * loading JSGF grammars from an InputStream which is non-standard
     */
    public RuleGrammar loadJSGF(InputStream JSGFinput) 
        throws GrammarException, IOException, EngineStateError 
    {
        checkEngineState(DEALLOCATED | DEALLOCATING_RESOURCES);
        RuleGrammar G = JSGFParser.newGrammarFromJSGF(JSGFinput, 
                                                      (Recognizer)this);
        if ((G != null) && (G.getName() != null)) {
            storeGrammar(G);
        }
        return G;
    }

    /** 
     * Let listeners know the recognizer rejected something.  NOT JSAPI.
     */
    public void rejectUtterance() {
        BaseResult R = new BaseResult(null);
        this.postResultCreated(R);
        R.postResultRejected();
        this.postResultRejected(R);
    }

    /**
     * Let listeners know the recognizer recognized/finalized/accepted
     * result.  NOT JSAPI.
     */
    public void notifyResult(String gname, String words) {
        BaseRuleGrammar G = (BaseRuleGrammar) getRuleGrammar(gname);
        if (G == null) {
            RecognizerUtilities.debugMessageOut(
                "ERROR: UNKNOWN GRAMMAR FOR RESULT " + gname);
            return;
        }
        BaseResult R = new BaseResult(G,words);

        G.postResultCreated(R);
        this.postResultCreated(R);
        
        R.postGrammarFinalized();
        G.postGrammarFinalized(R);
        this.postGrammarFinalized(R);

	R.setResultState(Result.ACCEPTED);
        R.postResultAccepted();
        G.postResultAccepted(R);
        this.postResultAccepted(R);
    }

//////////////////////
// Begin utility methods for sending RecognizerEvents.
//////////////////////
    /**
     * Utility function to generate CHANGES_COMMITTED event and post it
     * to the event queue.  Eventually fireChangesCommitted will be called
     * by dispatchSpeechEvent as a result of this action.
     */
    protected void postChangesCommitted(long oldState, long newState,
                                        GrammarException ge) {
        SpeechEventUtilities.postSpeechEvent(
            this,
            new RecognizerEvent(this,
                                RecognizerEvent.CHANGES_COMMITTED,
                                oldState, newState, ge));
    }

    /**
     * Utility function to send a CHANGES_COMMITTED event to all engine
     * listeners.  
     */
    public void fireChangesCommitted(RecognizerEvent event) {
        if (engineListeners == null) {
            return;
        }
        Enumeration E = engineListeners.elements();
        while (E.hasMoreElements()) {
            EngineListener el = (EngineListener) E.nextElement();
            if (el instanceof RecognizerListener) {
                ((RecognizerListener) el).changesCommitted(event);
            }
        }
    }
    
    /**
     * Utility function to generate FOCUS_GAINED event and post it
     * to the event queue.  Eventually fireFocusGained will be called
     * by dispatchSpeechEvent as a result of this action.
     */
    protected void postFocusGained(long oldState, long newState) {
        SpeechEventUtilities.postSpeechEvent(
            this,
            new RecognizerEvent(this,
                                RecognizerEvent.FOCUS_GAINED,
                                oldState, newState, null));
    }

    /**
     * Utility function to generate FOCUS_GAINED event and 
     * send it to all recognizer listeners.
     */
    public void fireFocusGained(RecognizerEvent event) {
	if (engineListeners == null) {
	    return;
	}
        Enumeration E = engineListeners.elements();
        while (E.hasMoreElements()) {
            EngineListener el = (EngineListener) E.nextElement();
            if (el instanceof RecognizerListener) {
                ((RecognizerListener) el).focusGained(event);
            }
        }
    }

    /**
     * Utility function to generate FOCUS_LOST event and post it
     * to the event queue.  Eventually fireFocusLost will be called
     * by dispatchSpeechEvent as a result of this action.
     */
    protected void postFocusLost(long oldState, long newState) {
        SpeechEventUtilities.postSpeechEvent(
            this,
            new RecognizerEvent(this,
                                RecognizerEvent.FOCUS_LOST,
                                oldState, newState, null));
    }

    /**
     * Utility function to generate FOCUS_LOST event and 
     * send it to all recognizer listeners.
     */
    public void fireFocusLost(RecognizerEvent event) {
	if (engineListeners == null) {
	    return;
	}
        Enumeration E = engineListeners.elements();
        while (E.hasMoreElements()) {
            EngineListener el = (EngineListener) E.nextElement();
            if (el instanceof RecognizerListener) {
                ((RecognizerListener) el).focusLost(event);
            }
        }
    }

    /**
     * Utility function to generate RECOGNIZER_PROCESSING event and post it
     * to the event queue.  Eventually fireRecognizerProcessing will be called
     * by dispatchSpeechEvent as a result of this action.
     */
    protected void postRecognizerProcessing(long oldState, long newState) {
        SpeechEventUtilities.postSpeechEvent(
            this,
            new RecognizerEvent(this,
                                RecognizerEvent.RECOGNIZER_PROCESSING,
                                oldState, newState, null));
    }

    /**
     * Utility function to generate RECOGNIZER_PROCESSING event and 
     * send it to all recognizer listeners.
     */
    public void fireRecognizerProcessing(RecognizerEvent event) {
	if (engineListeners == null) {
	    return;
	}
        Enumeration E = engineListeners.elements();
        while (E.hasMoreElements()) {
            EngineListener el = (EngineListener) E.nextElement();
            if (el instanceof RecognizerListener) {
                ((RecognizerListener) el).recognizerProcessing(event);
            }
        }
    }

    /**
     * Utility function to generate RECOGNIZER_SUSPENDED event and post it
     * to the event queue.  Eventually fireRecognizerSuspended will be called
     * by dispatchSpeechEvent as a result of this action.
     */
    protected void postRecognizerSuspended(long oldState, long newState) {
        SpeechEventUtilities.postSpeechEvent(
            this,
            new RecognizerEvent(this,
                                RecognizerEvent.RECOGNIZER_SUSPENDED,
                                oldState, newState, null));
    }

    /**
     * Utility function to generate RECOGNIZER_SUSPENDED event and 
     * send it to all recognizer listeners.
     */
    public void fireRecognizerSuspended(RecognizerEvent event) {
	if (engineListeners == null) {
	    return;
	}
        Enumeration E = engineListeners.elements();
        while (E.hasMoreElements()) {
            EngineListener el = (EngineListener) E.nextElement();
            if (el instanceof RecognizerListener) {
                ((RecognizerListener) el).recognizerSuspended(event);
            }
        }
    }
//////////////////////
// End utility methods for sending RecognizerEvents
//////////////////////

    /**
     * Cycle through each rule in each grammar
     * calling the method changeRule for each method
     * that had changed or is new. Also make a list of
     * all enabled rules.
     *
     * Nominally calling changeRule method will propogate
     * the new rule to the underlying recognizer
     *
     */
    protected void commitChangeInternal() {
        boolean haveChanges = false;
        Vector enabled = new Vector();
        RuleGrammar G[] = listRuleGrammars();
	if (!supportsNULL || !supportsVOID) {
	  try { 
	    G = RecognizerUtilities.transform(G,!supportsNULL,!supportsVOID);
	  } catch (GrammarException e) {
	    e.printStackTrace();
	    return;
	  }
	}
        for (int i=0; i < G.length; i++) {
            BaseRuleGrammar JG = (BaseRuleGrammar) G[i];
            if (JG.grammarChanged) { 
                haveChanges=true; 
                break; 
            }
        }
        // if (reloadAll) haveChanges=true;
        if (haveChanges) {
            startGrammarChanges();
        }
        // find and output rules that have changes
        for (int i=0; i < G.length; i++) {
            String gname = G[i].getName();
            BaseRuleGrammar JG = (BaseRuleGrammar)G[i];
            String rnames[] = G[i].listRuleNames();
            for (int j=0; j < rnames.length; j++) {
                String ruleName = rnames[j];
                if (G[i].isEnabled(ruleName)) {
                    enabled.addElement(gname + "_" + ruleName);
                }
                if (!haveChanges 
                    || (!JG.isRuleChanged(ruleName) && !reloadAll)) {
                    continue;
                }
                JG.setRuleChanged(ruleName,false);
                boolean isPublic = false;
                try { 
                    isPublic = G[i].isRulePublic(ruleName);
                } catch (IllegalArgumentException nse) {
                }
                Rule rule = G[i].getRule(ruleName);
                currentGrammar = G[i];
                changeRule(gname,ruleName,rule,isPublic);
            }
            JG.grammarChanged = false;
            JG.postGrammarChangesCommitted(); // send events
        }
        if (haveChanges) {
            endGrammarChanges();
        }
        changeEnabled(enabled);
    }
  
    /** 
     * Called at the start of the commit process.
     */
    protected void startGrammarChanges() { 
    }

    /** 
     * Called at the end of the commit process.
     */
    protected void endGrammarChanges() { 
    }

    /** 
     * Called with list of rules that should be enabled.
     */
    protected void changeEnabled(Vector enabled) { 
    }
    
    /**
     * Called to propogate new rule to underying recognizer.
     */
    protected void changeRule(String gname, 
                              String ruleName,
                              Rule rule,
                              boolean isPublic) {
    }
  
    /**
     * Check each grammar and load and imported grammars
     * that are not already loaded.
     */
    static public void loadAllImports(Recognizer R) 
        throws GrammarException, IOException 
    {
        RuleGrammar rlist[] = R.listRuleGrammars();
        for (int i=0; i<rlist.length; i++) {
            loadImports(R,rlist[i],null,false,false,null);
        }
    }
  
    /**
     * Load grammars imported by the specified RuleGrammar
     * if they are not already loaded.
     */
    static private void loadImports(Recognizer R, RuleGrammar G,URL context,
				    boolean recurse,boolean relo,Vector grams)
        throws GrammarException, IOException
    {
        RuleGrammar G2=null;
        RuleName imports[] = G.listImports();
        if (imports != null) {
            for (int i=0; i<imports.length; i++) {
                RecognizerUtilities.debugMessageOut("Checking import " + 
                                                    imports[i].getRuleName());
                String gname = imports[i].getFullGrammarName();
                RuleGrammar GI = R.getRuleGrammar(gname);
		if ((GI != null) && relo) {
		  
		}
                if (GI == null) {
		    URL grammarURL = gnameToURL(context,imports[i].getFullGrammarName());

		    RecognizerUtilities.debugMessageOut("loading " + grammarURL);
		  
		    G2 = JSGFParser.newGrammarFromJSGF(grammarURL, R);
		  
		    if (G2 == null) {
                        RecognizerUtilities.debugMessageOut(
                            "ERROR LOADING GRAMMAR " + grammarURL);
                    } else {
		      if (grams!=null) grams.addElement(G2);
		      if (recurse) loadImports(R,G2,context,recurse,relo,grams);
		    }
                }
            }
        }
    }

    /**
     * Resolve and linkup all rule references contained
     * in all rules in all grammars.
     */
    protected void linkGrammars() throws GrammarException {
        RuleGrammar rlist[] = listRuleGrammars();
        for (int i=0; i<rlist.length; i++) {
            ((BaseRuleGrammar)(rlist[i])).resolveAllRules();
        }
    }

    /**
     * Determine if the Recognizer has any modal grammars.  This
     * sets the global flag named hasModalGrammars.
     */
    protected void checkForModalGrammars() {
        hasModalGrammars = false;        
	if (grammarList == null) {
	    return;
	}
        Enumeration e = grammarList.elements();
        while (e.hasMoreElements()) {
            RuleGrammar rg = (RuleGrammar) e.nextElement(); 
            if (rg.getActivationMode() == Grammar.RECOGNIZER_MODAL) {
                hasModalGrammars = true;
                return;
            }
        }
    }
    
    /**
     * Determine if the given Grammar is active.  This is a combination
     * of the enabled state and activation modes of the Grammar as well
     * as the current focus state of the recognizer.  NOT JSAPI.
     */
    protected boolean isActive(Grammar grammar) {
        //[[[WDW - check engineState?]]]
        if (!grammar.isEnabled()) {
            return false;
        } else if (grammar.getActivationMode() == Grammar.GLOBAL) {
            return true;
        } else if (testEngineState(FOCUS_ON)) {
            if (grammar.getActivationMode() == Grammar.RECOGNIZER_MODAL) {
                return true;
            } else if (!hasModalGrammars) {
                return true;
            }
        }
        return false;    
    }

    /** 
     * Notify any grammars if their activation state has been changed.
     */
    protected void notifyGrammarActivation() {
	if (grammarList == null) {
	    return;
	}
        Enumeration e = grammarList.elements();
        while (e.hasMoreElements()) {
            BaseRuleGrammar rg = (BaseRuleGrammar) e.nextElement(); 
            boolean active = isActive(rg);
            if (active != rg.grammarActive) {
                rg.grammarActive = active;
                if (active) {
                    rg.postGrammarActivated();
                } else {
                    rg.postGrammarDeactivated();
                }
            }   
        }
    }

    /**
     * Factory constructor for <code>EngineProperties</code> object.
     *
     * @return a <code>BaseEngineProperties</code> object specific to
     *   a subclass.
     */
    protected BaseEngineProperties createEngineProperties() {
	return null;
    }

    /**
     * Called from the <code>resume</code> method.  Override in subclasses.
     */
    protected void handleResume() {
    }

    /**
     * Called from the <code>pause</code> method.  Override this in subclasses.
     */
    protected void handlePause() {
    }

    /**
     * Called from the <code>allocate</code> method.  Override this in
     * subclasses.
     *
     * @see #allocate
     *
     * @throws EngineException if problems are encountered
     */
    protected void handleAllocate() throws EngineException {
    }

    /**
     * Called from the <code>deallocate</code> method.  Override this in
     * subclasses.
     *  
     * @throws EngineException if this <code>Engine</code> cannot be
     *   deallocated.
     */
    protected void handleDeallocate() throws EngineException {
    }


    /**
     * Dispatch a SpeechEvent.  This is a method from SpeechEventDispatcher.
     * The dispatcher should notify all listeners of the speech event
     * from this method.
     */
    public void dispatchSpeechEvent(SpeechEvent event) {
        switch (event.getId()) {
            case RecognizerEvent.CHANGES_COMMITTED:
                fireChangesCommitted((RecognizerEvent) event);
                break;
            case RecognizerEvent.FOCUS_GAINED:
                fireFocusGained((RecognizerEvent) event);
                break;
            case RecognizerEvent.FOCUS_LOST:
                fireFocusLost((RecognizerEvent) event);
                break;
            case RecognizerEvent.RECOGNIZER_PROCESSING:
                fireRecognizerProcessing((RecognizerEvent) event);
                break;
            case RecognizerEvent.RECOGNIZER_SUSPENDED:
                fireRecognizerSuspended((RecognizerEvent) event);
                break;
                
            case ResultEvent.AUDIO_RELEASED:
                fireAudioReleased((ResultEvent) event);
                break;
            case ResultEvent.GRAMMAR_FINALIZED:
                fireGrammarFinalized((ResultEvent) event);
                break;
            case ResultEvent.RESULT_ACCEPTED:
                fireResultAccepted((ResultEvent) event);
                break;
            case ResultEvent.RESULT_CREATED:
                fireResultCreated((ResultEvent) event);
                break;
            case ResultEvent.RESULT_REJECTED:
                fireResultRejected((ResultEvent) event);
                break;
            case ResultEvent.RESULT_UPDATED:
                fireResultUpdated((ResultEvent) event);
                break;
            case ResultEvent.TRAINING_INFO_RELEASED:
                fireTrainingInfoReleased((ResultEvent) event);
                break;

            // Defer to BaseEngine to handle the rest.
            //
            default:
                super.dispatchSpeechEvent(event);
                break;
        }
    }
}
