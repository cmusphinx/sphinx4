/**
 * Copyright 1998-2003 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */

package com.sun.speech.engine.recognition;

import com.sun.speech.engine.BaseEngine;
import com.sun.speech.engine.BaseEngineProperties;
import com.sun.speech.engine.SpeechEventDispatcher;
import com.sun.speech.engine.SpeechEventUtilities;

import edu.cmu.sphinx.jsgf.JSGFGrammarException;
import edu.cmu.sphinx.jsgf.JSGFGrammarParseException;
import edu.cmu.sphinx.jsgf.JSGFRuleGrammar;
import edu.cmu.sphinx.jsgf.JSGFRuleGrammarFactory;
import edu.cmu.sphinx.jsgf.JSGFRuleGrammarManager;
import edu.cmu.sphinx.jsgf.parser.JSGFParser;

import javax.speech.*;
import javax.speech.recognition.*;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Skeletal Implementation of the JSAPI Recognizer interface.
 * <p/>
 * This class is useful by itself for debugging, e.g. you can load grammars and
 * simulate a recognizer recognizing some text, etc.
 * <p/>
 * Actual JSAPI recognizer implementations might want to extend or modify this
 * implementation.
 * <p/>
 * Also contains utility routines for:
 * <ul>
 * <li>Loading imported grammars and resolving inter-grammar references.
 * <li>Printing/dumping grammars in an extensible way (used to dump grammar to
 * under-lying recognizer implementation via ASCII strings)
 * <li>Routines for copying grammars from one recognizer implementation to
 * another.
 * </ul>
 * 
 * @author Stuart Adams
 */
public class BaseRecognizer extends BaseEngine implements Recognizer,
		SpeechEventDispatcher {

	protected final List<ResultListener> resultListeners = new ArrayList<ResultListener>();
	protected boolean hasModalGrammars;

	protected boolean supportsNULL = true;
	protected boolean supportsVOID = true;

	// Set to true if recognizer cannot handle partial
	// grammar loading.
	protected boolean reloadAll;

	// Grammar manager 
	public JSGFRuleGrammarManager manager;
	HashMap<String, BaseRuleGrammar> grammars = new HashMap<String, BaseRuleGrammar> (); 
	
	// ////////////////////
	// Begin Constructors
	// ////////////////////

	/** Create a new Recognizer in the DEALLOCATED state. */
	public BaseRecognizer() {
		this(false, null);
	}

	/** Create a new Recognizer in the DEALLOCATED state. */
	public BaseRecognizer(RecognizerModeDesc mode) {
		this(false, mode);
	}
	
	public BaseRecognizer(JSGFRuleGrammarManager manager) {
		this.reloadAll = false;
		this.audioManager = new BaseRecognizerAudioManager();
		this.manager = manager;
	}

	/**
	 * Create a new Recognizer in the DEALLOCATED state.
	 * 
	 * @param reloadAll
	 *            set to true if recognizer cannot handle partial grammar
	 *            loading. Default = false.
	 */
	public BaseRecognizer(boolean reloadAll, RecognizerModeDesc mode) {
		super(mode);
		this.reloadAll = reloadAll;
		audioManager = new BaseRecognizerAudioManager();
		manager = new JSGFRuleGrammarManager();
	}

	// ////////////////////
	// End Constructors
	// ////////////////////

	// ////////////////////
	// Begin overridden Engine Methods

	// ////////////////////

	/**
	 * Allocate the resources for the Engine and put it in the ALLOCATED,
	 * RESUMED, QUEUE_EMPTY state.
	 */
	public void allocate() throws EngineException, EngineStateError {
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
		handleAllocate();
	}

	/**
	 * Deallocate the resources for the Engine and put it in the DEALLOCATED
	 * state.
	 */

	public void deallocate() throws EngineException, EngineStateError {
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
		handleDeallocate();
	}

	/**
	 * Return an object that provides management of the audio input or output of
	 * the Engine. From javax.speech.Engine.
	 */
	public AudioManager getAudioManager() {
		if (audioManager == null) {
			audioManager = new BaseRecognizerAudioManager();
		}
		return audioManager;
	}

	// ////////////////////
	// End overridden Engine Methods
	// ////////////////////

	// ////////////////////
	// Begin Recognizer Methods

	// ////////////////////

	/**
	 * Create a new RuleGrammar with the given name. From
	 * javax.speech.recognition.Recognizer.
	 * 
	 * @param name
	 *            the name of the RuleGrammar.
	 */
	public RuleGrammar newRuleGrammar(String name)
			throws IllegalArgumentException, EngineStateError {
		checkEngineState(DEALLOCATED | DEALLOCATING_RESOURCES);
		JSGFRuleGrammar jsgfGrammar = new JSGFRuleGrammar(name, manager);
		BaseRuleGrammar grammar = new BaseRuleGrammar (this, jsgfGrammar);
		grammars.put(name, grammar);
		return grammar;
	}

	/**
	 * From javax.speech.recognition.Recognizer.
	 * 
	 * @param reader
	 *            the Reader containing JSGF input.
	 */
	public RuleGrammar loadJSGF(Reader reader) throws GrammarException,
			IOException, EngineStateError {
		checkEngineState(DEALLOCATED | DEALLOCATING_RESOURCES);

		JSGFRuleGrammar jsgfGrammar;
		try {
			jsgfGrammar = JSGFParser.newGrammarFromJSGF(reader, new JSGFRuleGrammarFactory(this.manager));
		} catch (JSGFGrammarParseException e) {
			throw new GrammarException(e.getMessage());
		}
		BaseRuleGrammar grammar = new BaseRuleGrammar (this, jsgfGrammar);
		grammars.put(jsgfGrammar.getName(), grammar);
		return grammar;
	}

	/**
	 * Load a RuleGrammar and its imported grammars from a URL containing JSGF
	 * text. From javax.speech.recognition.Recognizer.
	 * 
	 * @param baseURL
	 *            the base URL containing the JSGF grammar file.
	 * @param grammarName
	 *            the name of the JSGF grammar to load.
	 */
	public RuleGrammar loadJSGF(java.net.URL baseURL, String grammarName)
			throws GrammarException, MalformedURLException, IOException,
			EngineStateError {
		checkEngineState(DEALLOCATED | DEALLOCATING_RESOURCES);

		return loadJSGF(baseURL, grammarName, true, false, null);
	}

	private static URL grammarNameToURL (URL baseURL, String grammarName)
			throws MalformedURLException {

		// Convert each period in the grammar name to a slash "/"
		// Append a slash and the converted grammar name to the base URL
		// Append the ".gram" suffix
		grammarName = grammarName.replace('.', '/');
		StringBuilder sb = new StringBuilder();
		if (baseURL != null) {
			sb.append(baseURL);
			if (sb.charAt(sb.length() - 1) != '/')
				sb.append('/');
		}
		sb.append(grammarName).append(".gram");
		String urlstr = sb.toString();

		URL grammarURL = null;
		try {
			grammarURL = new URL(urlstr);
		} catch (MalformedURLException me) {
			grammarURL = ClassLoader.getSystemResource(urlstr);
			if (grammarURL == null)
				throw new MalformedURLException(urlstr);
		}

		return grammarURL;
	}

	/** From javax.speech.recognition.Recognizer. */
	@SuppressWarnings("rawtypes")
    public RuleGrammar loadJSGF(URL context, String grammarName,
			boolean loadImports, boolean reloadGrammars, Vector grammarsList)
			throws GrammarException, MalformedURLException, IOException,
			EngineStateError {
		checkEngineState(DEALLOCATED | DEALLOCATING_RESOURCES);

		URL grammarURL = grammarNameToURL(context, grammarName);

		JSGFRuleGrammar jsgfGrammar;
		try {
			jsgfGrammar = JSGFParser.newGrammarFromJSGF(grammarURL, new JSGFRuleGrammarFactory(this.manager));
		} catch (JSGFGrammarParseException e) {
			throw new GrammarException(e.getMessage());
		}
		BaseRuleGrammar grammar = new BaseRuleGrammar (this, jsgfGrammar);
		grammars.put(jsgfGrammar.getName(), grammar);
		
		if (loadImports) {
			loadImports (grammar, context, true, reloadGrammars);
		}
		
		return grammar;
	}

	/**
	 * Return the named RuleGrammar From javax.speech.recognition.Recognizer.
	 * 
	 * @param name
	 *            the name of the RuleGrammar.
	 */
	public RuleGrammar getRuleGrammar(String name) throws EngineStateError {
		checkEngineState(DEALLOCATED | DEALLOCATING_RESOURCES);
		BaseRuleGrammar grammar = grammars.get(name);
		if (grammar == null) {
			JSGFRuleGrammar jsgfGrammar = manager.retrieveGrammar(name);
			if (jsgfGrammar != null) {
				grammar = new BaseRuleGrammar (this, jsgfGrammar);
				grammars.put(jsgfGrammar.getName(), grammar);
			}
 		}
		return grammar;
	}

	/**
	 * Get a list of loaded or defined RuleGrammars known to the Recognizer.
	 * From javax.speech.recognition.Recognizer.
	 */
	public RuleGrammar[] listRuleGrammars() throws EngineStateError {
		checkEngineState(DEALLOCATED | DEALLOCATING_RESOURCES);
        return grammars.values().toArray(new RuleGrammar[grammars.size()]);
	}

	/**
	 * Delete a RuleGrammar from the Recognizer. From
	 * javax.speech.recognition.Recognizer.
	 * 
	 * @param grammar
	 *            the RuleGrammar to delete.
	 */
	public void deleteRuleGrammar(RuleGrammar grammar)
			throws IllegalArgumentException, EngineStateError {
		checkEngineState(DEALLOCATED | DEALLOCATING_RESOURCES);
		manager.remove(grammar.getName());
	}

	/**
	 * Get the DicationGrammar for this Recognizer. Always returns null. Should
	 * be overridden by recognizers that support dictation.
	 */
	public DictationGrammar getDictationGrammar(String name)
			throws EngineStateError {
		checkEngineState(DEALLOCATED | DEALLOCATING_RESOURCES);
		return null;
	}

	/** Commit grammar changes. From javax.speech.recognition.Recognizer. */
	public void commitChanges() throws GrammarException, EngineStateError {
		checkEngineState(DEALLOCATED | DEALLOCATING_RESOURCES);

		// All cases:
		// If there is a pending suspend call, it can be cleared.
		// THIS IS NOT IMPLEMENTED YET
		// Case 1:
		// If the recognizer is in the LISTENING state we should
		// first transition to the SUSPENDED state, then commit,
		// then return the LISTENING state.
		// Case 2:
		// If the recognizer is in the SUSPENDED state, we remain
		// in that state, commit, then return to LISTENING state.
		// EXCEPTION: if the recognizer is still issuing events
		// for a Result that brought it to the SUSPENDED state,
		// the commit should be deferred until the event processing
		// is complete -- THIS IS NOT IMPLEMENTED YET.
		// Case 3:
		// If the recognizer is in the PROCESSING state, the
		// commit should be deferred until following result is
		// finalized and appropriate events have been issued.

		// The following hack avoids inappropriately going from
		// PROCESSING to LISTENING states. It does not correctly
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

		// GRAMMAR_CHANGES_COMMITTED events are sent in commitChangesInternal

		// Activate or deactivate any grammars
		// hasModalGrammars = manager.checkForModalGrammars();
		notifyGrammarActivation();

		if (goBackToListening) {
			long[] states = setEngineState(SUSPENDED | PROCESSING, LISTENING);
			postChangesCommitted(states[0], states[1], null);
		}
	}

	/**
	 * Temporarily suspend recognition while the application updates grammars
	 * prior to a call to commitChanges.
	 */
	public void suspend() throws EngineStateError {
		checkEngineState(DEALLOCATED | DEALLOCATING_RESOURCES);

		// [[[WDW - need to set a flag saying we got here via a method call
		// from the application.]]]

		if (testEngineState(SUSPENDED)) {
			return;
		}

		long[] states = setEngineState(LISTENING | PROCESSING, SUSPENDED);
		postRecognizerSuspended(states[0], states[1]);
	}

	/**
	 * NOT IMPLEMENTED YET. If the Recognizer is in the PROCESSING state, force
	 * the Recognizer to immediately complete processing of that result by
	 * finalizing it. From javax.speech.recognition.Recognizer.
	 * 
	 * @param flush
	 *            whether audio buffer should be processed or flushed.
	 */
	public void forceFinalize(boolean flush) throws EngineStateError {
		checkEngineState(DEALLOCATED | DEALLOCATING_RESOURCES);
		throw new RuntimeException("forceFinalize not yet implemented.");
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
	 * Request notification of Result events from the Recognizer. From
	 * javax.speech.recognition.Recognizer.
	 * 
	 * @param listener
	 *            the listener to add.
	 */
	public void addResultListener(ResultListener listener) {
		if (!resultListeners.contains(listener)) {
			resultListeners.add(listener);
		}
	}

	/**
	 * Remove a ResultListener from the list of ResultListeners. From
	 * javax.speech.recognition.Recognizer.
	 * 
	 * @param listener
	 *            the listener to remove.
	 */
	public void removeResultListener(ResultListener listener) {
		resultListeners.remove(listener);
	}

	/**
	 * Get the RecognizerProperties of this Recognizer. From
	 * javax.speech.recognition.Recognizer.
	 */
	public RecognizerProperties getRecognizerProperties() {
		return (RecognizerProperties) getEngineProperties();
	}

	/**
	 * NOT IMPLEMENTED YET. Get the object that manages the speakers of the
	 * Recognizer. From javax.speech.recognition.Recognizer.
	 */
	public SpeakerManager getSpeakerManager() {
		return null;
	}

	/**
	 * Create a new grammar by reading in a grammar stored in a vendor-specific
	 * format. Since BaseGrammar is serializable we just use read/write object
	 * to store/restore grammars. From javax.speech.recognition.Recognizer.
	 */
	public Grammar readVendorGrammar(InputStream input)
			throws VendorDataException, IOException, EngineStateError {
		checkEngineState(DEALLOCATED | DEALLOCATING_RESOURCES);
		BaseGrammar G = null;
		try {
			ObjectInputStream p = new ObjectInputStream(input);
			G = (BaseGrammar) p.readObject();
		} catch (Exception e) {
			throw new VendorDataException ("ERROR: readVendorGrammar: " + e);
		}
		return G;
	}

	/**
	 * Create a new grammar by reading in a grammar stored in a vendor-specific
	 * format. Since BaseGrammar is serializable we just use read/write object
	 * to store/restore grammars. From javax.speech.recognition.Recognizer.
	 */
	public void writeVendorGrammar(OutputStream output, Grammar gram)
			throws IOException, EngineStateError {
		checkEngineState(DEALLOCATED | DEALLOCATING_RESOURCES);
		ObjectOutputStream p = new ObjectOutputStream(output);
		p.writeObject(gram);
	}

	/**
	 * Read a Result from a stream in a vendor-specific format. Since BaseResult
	 * is serializable we just use read/write object to store/restore grammars.
	 * From javax.speech.recognition.Recognizer.
	 */
	public Result readVendorResult(InputStream output)
			throws VendorDataException, IOException, EngineStateError {
		checkEngineState(DEALLOCATED | DEALLOCATING_RESOURCES);
		BaseResult res = null;
		try {
			ObjectInputStream p = new ObjectInputStream(output);
			res = (BaseResult) p.readObject();
		} catch (Exception e) {
			throw new VendorDataException ("ERROR: readVendorResult: " + e);
		}
		return res;
	}

	/**
	 * Store a Result to a stream in a vendor-specific format. Since BaseResult
	 * is serializable we just use read/write object to store/restore grammars.
	 * From javax.speech.recognition.Recognizer.
	 */
	public void writeVendorResult(OutputStream output, Result result)
			throws IOException, ResultStateError, EngineStateError {
		checkEngineState(DEALLOCATED | DEALLOCATING_RESOURCES);
		ObjectOutputStream p = new ObjectOutputStream(output);
		p.writeObject(result);
	}

	// ////////////////////
	// End Recognizer Methods
	// ////////////////////

	// ////////////////////
	// Begin utility methods for sending ResultEvents

	// ////////////////////

	/**
	 * Utility function to generate AUDIO_RELEASED event and post it to the
	 * event queue. Eventually fireAudioReleased will be called by
	 * dispatchSpeechEvent as a result of this action.
	 */
	public void postAudioReleased(Result result) {
		SpeechEventUtilities.postSpeechEvent(this, new ResultEvent(result,
				ResultEvent.AUDIO_RELEASED));
	}

	/** Utility function to send a AUDIO_RELEASED event to all result listeners. */
	public void fireAudioReleased(ResultEvent event) {
		for (ResultListener rl : resultListeners)
			rl.audioReleased(event);
	}

	/**
	 * Utility function to generate GRAMMAR_FINALIZED event and post it to the
	 * event queue. Eventually fireGrammarFinalized will be called by
	 * dispatchSpeechEvent as a result of this action.
	 */
	public void postGrammarFinalized(Result result) {
		SpeechEventUtilities.postSpeechEvent(this, new ResultEvent(result,
				ResultEvent.GRAMMAR_FINALIZED));
	}

	/**
	 * Utility function to send a GRAMMAR_FINALIZED event to all result
	 * listeners.
	 */
	public void fireGrammarFinalized(ResultEvent event) {
		for (ResultListener rl : resultListeners)
			rl.grammarFinalized(event);
	}

	/**
	 * Utility function to generate RESULT_ACCEPTED event and post it to the
	 * event queue. Eventually fireResultAccepted will be called by
	 * dispatchSpeechEvent as a result of this action.
	 */
	public void postResultAccepted(Result result) {
		SpeechEventUtilities.postSpeechEvent(this, new ResultEvent(result,
				ResultEvent.RESULT_ACCEPTED));
	}

	/**
	 * Utility function to send a RESULT_ACCEPTED event to all result listeners.
	 */
	public void fireResultAccepted(ResultEvent event) {
		for (ResultListener rl : resultListeners)
			rl.resultAccepted(event);
	}

	/**
	 * Utility function to generate RESULT_CREATED event and post it to the
	 * event queue. Eventually fireResultCreated will be called by
	 * dispatchSpeechEvent as a result of this action.
	 */
	public void postResultCreated(Result result) {
		SpeechEventUtilities.postSpeechEvent(this, new ResultEvent(result,
				ResultEvent.RESULT_CREATED));
	}

	/** Utility function to send a RESULT_CREATED event to all result listeners. */
	public void fireResultCreated(ResultEvent event) {
		for (ResultListener rl : resultListeners)
			rl.resultCreated(event);
	}

	/**
	 * Utility function to generate RESULT_REJECTED event and post it to the
	 * event queue. Eventually fireResultRejected will be called by
	 * dispatchSpeechEvent as a result of this action.
	 */
	public void postResultRejected(Result result) {
		SpeechEventUtilities.postSpeechEvent(this, new ResultEvent(result,
				ResultEvent.RESULT_REJECTED));
	}

	/**
	 * Utility function to send a RESULT_REJECTED event to all result listeners.
	 */
	public void fireResultRejected(ResultEvent event) {
		for (ResultListener rl : resultListeners)
			rl.resultRejected(event);
	}

	/**
	 * Utility function to generate RESULT_UPDATED event and post it to the
	 * event queue. Eventually fireResultUpdated will be called by
	 * dispatchSpeechEvent as a result of this action.
	 */
	public void postResultUpdated(Result result) {
		SpeechEventUtilities.postSpeechEvent(this, new ResultEvent(result,
				ResultEvent.RESULT_UPDATED));
	}

	/** Utility function to send a RESULT_UPDATED event to all result listeners. */
	public void fireResultUpdated(ResultEvent event) {
		for (ResultListener rl : resultListeners)
			rl.resultUpdated(event);
	}

	/**
	 * Utility function to generate TRAINING_INFO_RELEASED event and post it to
	 * the event queue. Eventually fireTrainingInfoReleased will be called by
	 * dispatchSpeechEvent as a result of this action.
	 */
	public void postTrainingInfoReleased(Result result) {
		SpeechEventUtilities.postSpeechEvent(this, new ResultEvent(result,
				ResultEvent.TRAINING_INFO_RELEASED));
	}

	/**
	 * Utility function to send a TRAINING_INFO_RELEASED event to all result
	 * listeners.
	 */
	public void fireTrainingInfoReleased(ResultEvent event) {
		for (ResultListener rl : resultListeners)
			rl.trainingInfoReleased(event);
	}

	// ////////////////////
	// End utility methods for sending ResultEvents
	// ////////////////////

	// ////////////////////
	// NON-JSAPI METHODS

	/** Let listeners know the recognizer rejected something. NOT JSAPI. */
	public void rejectUtterance() {
		BaseResult R = new BaseResult(null);
		this.postResultCreated(R);
		R.postResultRejected();
		this.postResultRejected(R);
	}

	// ////////////////////
	// Begin utility methods for sending RecognizerEvents.

	// ////////////////////

	/**
	 * Utility function to generate CHANGES_COMMITTED event and post it to the
	 * event queue. Eventually fireChangesCommitted will be called by
	 * dispatchSpeechEvent as a result of this action.
	 */
	protected void postChangesCommitted(long oldState, long newState,
			GrammarException ge) {
		SpeechEventUtilities.postSpeechEvent(this, new RecognizerEvent(this,
				RecognizerEvent.CHANGES_COMMITTED, oldState, newState, ge));
	}

	/**
	 * Utility function to send a CHANGES_COMMITTED event to all engine
	 * listeners.
	 */
	public void fireChangesCommitted(RecognizerEvent event) {
		if (engineListeners == null) {
			return;
		}
		for (EngineListener el : engineListeners)
			if (el instanceof RecognizerListener)
				((RecognizerListener) el).changesCommitted(event);
	}

	/**
	 * Utility function to generate FOCUS_GAINED event and post it to the event
	 * queue. Eventually fireFocusGained will be called by dispatchSpeechEvent
	 * as a result of this action.
	 */
	protected void postFocusGained(long oldState, long newState) {
		SpeechEventUtilities.postSpeechEvent(this, new RecognizerEvent(this,
				RecognizerEvent.FOCUS_GAINED, oldState, newState, null));
	}

	/**
	 * Utility function to generate FOCUS_GAINED event and send it to all
	 * recognizer listeners.
	 */
	public void fireFocusGained(RecognizerEvent event) {
		if (engineListeners == null) {
			return;
		}
		for (EngineListener el : engineListeners)
			if (el instanceof RecognizerListener)
				((RecognizerListener) el).focusGained(event);
	}

	/**
	 * Utility function to generate FOCUS_LOST event and post it to the event
	 * queue. Eventually fireFocusLost will be called by dispatchSpeechEvent as
	 * a result of this action.
	 */
	protected void postFocusLost(long oldState, long newState) {
		SpeechEventUtilities.postSpeechEvent(this, new RecognizerEvent(this,
				RecognizerEvent.FOCUS_LOST, oldState, newState, null));
	}

	/**
	 * Utility function to generate FOCUS_LOST event and send it to all
	 * recognizer listeners.
	 */
	public void fireFocusLost(RecognizerEvent event) {
		if (engineListeners == null) {
			return;
		}
		for (EngineListener el : engineListeners)
			if (el instanceof RecognizerListener)
				((RecognizerListener) el).focusLost(event);
	}

	/**
	 * Utility function to generate RECOGNIZER_PROCESSING event and post it to
	 * the event queue. Eventually fireRecognizerProcessing will be called by
	 * dispatchSpeechEvent as a result of this action.
	 */
	protected void postRecognizerProcessing(long oldState, long newState) {
		SpeechEventUtilities.postSpeechEvent(this,
				new RecognizerEvent(this,
						RecognizerEvent.RECOGNIZER_PROCESSING, oldState,
						newState, null));
	}

	/**
	 * Utility function to generate RECOGNIZER_PROCESSING event and send it to
	 * all recognizer listeners.
	 */
	public void fireRecognizerProcessing(RecognizerEvent event) {
		if (engineListeners == null) {
			return;
		}
		for (EngineListener el : engineListeners)
			if (el instanceof RecognizerListener)
				((RecognizerListener) el).recognizerProcessing(event);
	}

	/**
	 * Utility function to generate RECOGNIZER_SUSPENDED event and post it to
	 * the event queue. Eventually fireRecognizerSuspended will be called by
	 * dispatchSpeechEvent as a result of this action.
	 */
	protected void postRecognizerSuspended(long oldState, long newState) {
		SpeechEventUtilities
				.postSpeechEvent(this, new RecognizerEvent(this,
						RecognizerEvent.RECOGNIZER_SUSPENDED, oldState,
						newState, null));
	}

	/**
	 * Utility function to generate RECOGNIZER_SUSPENDED event and send it to
	 * all recognizer listeners.
	 */
	public void fireRecognizerSuspended(RecognizerEvent event) {
		if (engineListeners == null) {
			return;
		}
		for (EngineListener el : engineListeners)
			if (el instanceof RecognizerListener)
				((RecognizerListener) el).recognizerSuspended(event);
	}

	// ////////////////////
	// End utility methods for sending RecognizerEvents
	// ////////////////////

	/**
	 * Cycle through each rule in each grammar calling the method changeRule for
	 * each method that had changed or is new. Also make a list of all enabled
	 * rules.
	 * <p/>
	 * Nominally calling changeRule method will propagate the new rule to the
	 * underlying recognizer
	 */
	protected void commitChangeInternal() {
		boolean haveChanges = false;
		List<String> enabled = new ArrayList<String>();
		RuleGrammar[] grammarArray = listRuleGrammars();
		if (!supportsNULL || !supportsVOID) {
			try {
				grammarArray = RecognizerUtilities.transform(grammarArray, !supportsNULL,
						!supportsVOID);
			} catch (GrammarException e) {
				e.printStackTrace();
				return;
			}
		}
		for (RuleGrammar grammar : grammarArray) {
			BaseRuleGrammar JG = (BaseRuleGrammar) grammar;
			if (JG.grammarChanged) {
				haveChanges = true;
				break;
			}
		}
		// if (reloadAll) 
		//		haveChanges=true;
		if (haveChanges) {
			startGrammarChanges();
		}
		// find and output rules that have changes
		for (RuleGrammar ruleGrammar : grammarArray) {
			String grammarName = ruleGrammar.getName();
			BaseRuleGrammar grammar = (BaseRuleGrammar) ruleGrammar;
			for (String ruleName : grammar.listRuleNames()) {
				if (grammar.isEnabled(ruleName)) {
					enabled.add(grammarName + '_' + ruleName);
				}
				if (!haveChanges || (!grammar.isRuleChanged(ruleName) && !reloadAll)) {
					continue;
				}
				grammar.setRuleChanged(ruleName, false);
				
				boolean isPublic = grammar.isRulePublic(ruleName);

				Rule rule = grammar.getRule(ruleName);
				changeRule(grammarName, ruleName, rule, isPublic);
			}
			grammar.grammarChanged = false;
			grammar.postGrammarChangesCommitted(); // send events
		}
		if (haveChanges) {
			endGrammarChanges();
		}
	}

	/** Called at the start of the commit process. */
	protected void startGrammarChanges() {
	}

	/** Called at the end of the commit process. */
	protected void endGrammarChanges() {
	}

	/** Called with list of rules that should be enabled. */
	protected void changeEnabled(List<String> enabled) {
	}

	/** Called to propagate new rule to underlying recognizer. */
	protected void changeRule(String gname, String ruleName, Rule rule,
			boolean isPublic) {
	}

	/**
	 * Check each grammar and load and imported grammars that are not already
	 * loaded.
	 */
	public void loadAllImports() throws GrammarException,
			IOException {
		for (BaseRuleGrammar grammar : grammars.values()) {
			loadImports(grammar, null, false, false);
		}
	}

	/**
	 * Load grammars imported by the specified RuleGrammar if they are not
	 * already loaded.
	 */
	private void loadImports(RuleGrammar grammar, URL context, boolean recurse, boolean reload) throws GrammarException, IOException {

		for (RuleName ruleName : grammar.listImports()) {
			// System.out.println ("Checking import " + ruleName);
			String grammarName = ruleName.getFullGrammarName();
			RuleGrammar importedGrammar = getRuleGrammar(grammarName);

			if (importedGrammar == null) {
				// System.out.println ("Grammar " + grammarName + " not found. Loading.");
				importedGrammar = loadJSGF (context, ruleName.getFullGrammarName());
			}
			if (importedGrammar != null && recurse) {
				loadImports(importedGrammar, context, recurse, reload);
			}
		}
		loadFullQualifiedRules(grammar, context, recurse, reload);
	}

	/**
	 * Load grammars imported by a fully qualified Rule Token if they are not
	 * already loaded.
	 * 
	 * @param grammar
	 * @param context
	 * @param recurse
	 * @param reload
	 * @throws IOException
	 * @throws GrammarException
	 */
	private void loadFullQualifiedRules (
			RuleGrammar grammar, URL context,
			boolean recurse, boolean reload)
			throws GrammarException, IOException {

		// Go through every rule
		for (String ruleName : grammar.listRuleNames()) {
			String rule = grammar.getRuleInternal(ruleName).toString();
			// check for rule-Tokens
			int index = 0;
			while (index < rule.length()) {
				index = rule.indexOf('<', index);
				if (index < 0) {
					break;
				} 
				// Extract rule name
				RuleName extractedRuleName = new RuleName(rule.substring(index + 1,
						rule.indexOf('>', index + 1)).trim());
				index = rule.indexOf('>', index) + 1;

				// Check for full qualified rule name
				if (extractedRuleName.getFullGrammarName() != null) {
					String grammarName = extractedRuleName.getFullGrammarName();
					RuleGrammar importedGrammar = getRuleGrammar(grammarName);
					if (importedGrammar == null) {
						importedGrammar = loadJSGF(context, grammarName);
					}
					if (importedGrammar != null && recurse) {
						loadImports(importedGrammar, context, recurse, reload);
					}
				}
			}
		}
	}

	/**
	 * Resolve and link all rule references contained in all rules in all
	 * grammars.
	 */
	protected void linkGrammars() throws GrammarException {
		try {
			manager.linkGrammars();
		} catch (JSGFGrammarException e) {
			throw new GrammarException(e.getMessage());
		}
	}

	/**
	 * Determine if the given Grammar is active. This is a combination of the
	 * enabled state and activation modes of the Grammar as well as the current
	 * focus state of the recognizer. NOT JSAPI.
	 */
	protected boolean isActive(Grammar grammar) {
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

	/** Notify any grammars if their activation state has been changed. */
	protected void notifyGrammarActivation() {
		for (BaseRuleGrammar grammar : grammars.values()) {
			boolean active = isActive(grammar);
			if (active != grammar.grammarActive) {
				grammar.grammarActive = active;
				if (active) {
					grammar.postGrammarActivated();
				} else {
					grammar.postGrammarDeactivated();
				}
			}
		}
	}

	/**
	 * Factory constructor for <code>EngineProperties</code> object.
	 * 
	 * @return a <code>BaseEngineProperties</code> object specific to a
	 *         subclass.
	 */
	protected BaseEngineProperties createEngineProperties() {
		return null;
	}

	/** Called from the <code>resume</code> method. Override in subclasses. */
	protected void handleResume() {
	}

	/** Called from the <code>pause</code> method. Override this in subclasses. */
	protected void handlePause() {
	}

	/**
	 * Called from the <code>allocate</code> method. Override this in
	 * subclasses.
	 * 
	 * @throws EngineException
	 *             if problems are encountered
	 * @see #allocate
	 */
	protected void handleAllocate() throws EngineException {
	}

	/**
	 * Called from the <code>deallocate</code> method. Override this in
	 * subclasses.
	 * 
	 * @throws EngineException
	 *             if this <code>Engine</code> cannot be deallocated.
	 */
	protected void handleDeallocate() throws EngineException {
	}

	/**
	 * Dispatch a SpeechEvent. This is a method from SpeechEventDispatcher. The
	 * dispatcher should notify all listeners of the speech event from this
	 * method.
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
		default:
			super.dispatchSpeechEvent(event);
			break;
		}
	}
}
