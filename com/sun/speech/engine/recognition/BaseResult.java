/**
 * Copyright 1998-2003 Sun Microsystems, Inc.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */

/**
 * Very simple implementation of JSAPI Result, FinalResult,
 * FinalRuleResult, and FinalDictationResult.
 *
 * Ignores many things like N-Best, partial-results, etc.
 *
 * @version 1.9 09/09/99 14:24:41
 */
package com.sun.speech.engine.recognition;

import java.applet.AudioClip;
import javax.speech.*;
import javax.speech.recognition.*;
import java.util.*;

import com.sun.speech.engine.SpeechEventUtilities;
import com.sun.speech.engine.SpeechEventDispatcher;

public class BaseResult 
    implements Result, FinalResult, FinalRuleResult, FinalDictationResult,
    java.io.Serializable, Cloneable, SpeechEventDispatcher
{
    private Vector resultListeners;
    String theText[] = null;
    int nTokens = 0;
    transient Grammar grammar = null;
    int state = Result.UNFINALIZED; 

    String[] tags = null;
    String   ruleName = null;
    
    /**
     * Create an empty result.
     */
    public BaseResult() {
        this(null);
    }
  
    /**
     * Create an empty result.
     */
    public BaseResult(Grammar g) {
        this(g,null);
    }
  
    /**
     * Create a result with a result string 
     */
    public BaseResult(Grammar G, String S) {
        resultListeners = new Vector();
        grammar = G;
        tryTokens(G, S);
    }

    /**
     * Copy a result. If the result to be copied is a BaseResult
     * then clone it otherwise create a BaseResult and copy the
     * tokens onto it.
     */
    static BaseResult copyResult(Result R) {
        BaseResult copy = null;
        if (R instanceof BaseResult) {
            try {
                copy = (BaseResult) ((BaseResult)R).clone();
            } catch (CloneNotSupportedException e) {
                System.out.println("ERROR: " + e);
            }
            return copy;
        } else {
            copy = new BaseResult(R.getGrammar());
            copy.nTokens = R.numTokens();
            copy.theText = new String[copy.nTokens];
            for (int i = 0; i < R.numTokens(); i++) {
                copy.theText[i] = R.getBestToken(i).getSpokenText();
            }
            return copy;
        }
    }
    
//////////////////////
// Begin Result Methods
//////////////////////
    /**
     * Return the current state of the Result object.
     * From javax.speech.recognition.Result.
     */
    public int getResultState() {
        return state;
    }

    /** 
     * Return the grammar that goes with this Result.
     * From javax.speech.recognition.Result.
     */
    public Grammar getGrammar() {
        return grammar;
    }

    /** 
     * Return the number of finalized tokens in the Result.
     * From javax.speech.recognition.Result.
     */
    public int numTokens() { 
        return nTokens; 
    }

    /**
     * Return the best guess for the nth token.
     * From javax.speech.recognition.Result.
     */
    public ResultToken getBestToken(int nth) throws IllegalArgumentException { 
        if ((nth < 0) || (nth > (nTokens-1))) {
            throw new IllegalArgumentException("Token index out of range.");
        }
        return(new BaseResultToken(theText[nth]));
    }
    
    /**
     * Return the best guess tokens for the Result.
     * From javax.speech.recognition.Result.
     */
    public ResultToken[] getBestTokens() { 
	ResultToken[] bt = new ResultToken[nTokens];
	for (int i = 0; i < nTokens; i++) {
	    bt[i] = getBestToken(i);
	}
	return bt;
    }

    /**
     * NOT IMPLEMENTED YET.
     * Return the current guess of the tokens following the unfinalized 
     * tokens.
     * From javax.speech.recognition.Result.
     */
    public ResultToken[] getUnfinalizedTokens() { 
        return new ResultToken[0]; 
    }

    /**
     * Add a ResultListener to this Result.
     * From javax.speech.recognition.Result.
     */
    public void addResultListener(ResultListener listener) { 
        if (!resultListeners.contains(listener)) {
            resultListeners.addElement(listener);
        }
    }

    /**
     * Remove a ResultListener from this Result.
     * From javax.speech.recognition.Result.
     */
    public void removeResultListener(ResultListener listener) {
        resultListeners.removeElement(listener);
    }
//////////////////////
// End Result Methods
//////////////////////

//////////////////////
// Begin FinalResult Methods
//////////////////////
    /**
     * Returns true if the Recognizer has training information available
     * for this result.
     * From javax.speech.recognition.FinalResult.
     */
    public boolean isTrainingInfoAvailable() throws ResultStateError {
        checkResultState(UNFINALIZED);
        return false;
    }
    
    /**
     * Release training info for this FinalResult.
     * From javax.speech.recognition.FinalResult.
     */
    public void releaseTrainingInfo() throws ResultStateError {
        checkResultState(UNFINALIZED);
    }
    
    /**
     * Inform the recognizer of a correction to one or more tokens in
     * a FinalResult to the recognizer can re-train itself.
     * From javax.speech.recognition.FinalResult.
     */
    public void tokenCorrection(String correctTokens[], 
                                ResultToken fromToken, 
                                ResultToken toToken, 
                                int correctionType) 
        throws ResultStateError, IllegalArgumentException
    {
        checkResultState(UNFINALIZED);
    }

    /**
     * Determine if audio is available for this FinalResult.
     * From javax.speech.recognition.FinalResult.
     */
    public boolean isAudioAvailable() throws ResultStateError { 
        checkResultState(UNFINALIZED);
        return false; 
    }

    /**
     * Release the audio for this FinalResult.
     * From javax.speech.recognition.FinalResult.
     */
    public void releaseAudio() throws ResultStateError { 
        checkResultState(UNFINALIZED);
    }

    /**
     * Get the audio for this FinalResult.
     * From javax.speech.recognition.FinalResult.
     */
    public AudioClip getAudio() throws ResultStateError { 
        checkResultState(UNFINALIZED);
        return null; 
    }

    /**
     * Get the audio for this FinalResult.
     * From javax.speech.recognition.FinalResult.
     */
    public AudioClip getAudio(ResultToken from, ResultToken to) 
        throws ResultStateError
    { 
        checkResultState(UNFINALIZED);
        return null; 
    }
//////////////////////
// End FinalResult Methods
//////////////////////

//////////////////////
// Begin FinalRuleResult Methods
//////////////////////
    /**
     * Return the number of guesses for this FinalRuleResult.
     * From javax.speech.recognition.FinalRuleResult.
     */
    public int getNumberGuesses() throws ResultStateError {
        checkResultState(UNFINALIZED);
        if (!(grammar instanceof RuleGrammar)) {
            throw new ResultStateError("Result is not a FinalRuleResult");
        }
        return 1;
    }
    
    /**
     * Get the nBest token sequence for this FinalRuleResult.
     * From javax.speech.recognition.FinalRuleResult.
     */
    public ResultToken[] getAlternativeTokens(int nBest)
        throws ResultStateError     
    {
        checkResultState(UNFINALIZED);
        if (!(grammar instanceof RuleGrammar)) {
            throw new ResultStateError("Result is not a FinalRuleResult");
        }
        if (nBest == 0) {
            return getBestTokens();
        }
        //[[[WDW - throw InvalidArgumentException?]]]
        return null;
    }

    /**
     * Get the RuleGrammar matched by the nBest guess for this FinalRuleResult.
     * From javax.speech.recognition.FinalRuleResult.
     */
    public RuleGrammar getRuleGrammar(int nBest) 
        throws ResultStateError
    {
        checkResultState(UNFINALIZED);
        if (!(grammar instanceof RuleGrammar)) {
            throw new ResultStateError("Result is not a FinalRuleResult");
        }
        if (nBest == 0) {
            return (RuleGrammar) grammar;
        }
        //[[[WDW - throw InvalidArgumentException?]]]
        return null;
    }

    /**
     * Get the RuleGrammar matched by the nBest guess for this FinalRuleResult.
     * From javax.speech.recognition.FinalRuleResult.
     */
    public String getRuleName(int nBest)
        throws ResultStateError
    {
        checkResultState(UNFINALIZED);
        if (!(grammar instanceof RuleGrammar)) {
            throw new ResultStateError("Result is not a FinalRuleResult");
        }
        if (nBest == 0) {
            return ruleName;
        }
        //[[[WDW - throw InvalidArgumentException?]]]
        return null;
    }

    /**
     * Return the list of tags matched by the best-guess token sequence.
     * From javax.speech.recognition.FinalRuleResult.
     */
    public String[] getTags()
        throws ResultStateError
    {
        checkResultState(UNFINALIZED);
        if (!(grammar instanceof RuleGrammar)) {
            throw new ResultStateError("Result is not a FinalRuleResult");
        }
        return tags;
    }
//////////////////////
// End FinalRuleResult Methods
//////////////////////

//////////////////////
// Begin FinalDictationResult Methods
//////////////////////
    /**
     * NOT IMPLEMENTED YET.
     * Get a set of alternative token guesses for a single known token or
     * sequence of tokens.
     */
    public ResultToken[][] getAlternativeTokens(ResultToken from,
                                                ResultToken to,
                                                int max) 
        throws ResultStateError, IllegalArgumentException
    { 
        checkResultState(UNFINALIZED);
        if (!(grammar instanceof DictationGrammar)) {
            throw new ResultStateError("Result is not a FinalDicationResult");
        }
        return null;
    }
//////////////////////
// End FinalDictationResult Methods
//////////////////////
    
//////////////////////
// Begin utility methods for sending ResultEvents
//////////////////////
    /**
     * Utility function to generate AUDIO_RELEASED event and post it
     * to the event queue.  Eventually fireAudioReleased will be called
     * by dispatchSpeechEvent as a result of this action.
     */
    public void postAudioReleased() {
        SpeechEventUtilities.postSpeechEvent(
            this,
            new ResultEvent(this, ResultEvent.AUDIO_RELEASED));
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
    public void postGrammarFinalized() {
        SpeechEventUtilities.postSpeechEvent(
            this,
            new ResultEvent(this, ResultEvent.GRAMMAR_FINALIZED));
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
    public void postResultAccepted() {
        setResultState(Result.ACCEPTED);
        SpeechEventUtilities.postSpeechEvent(
            this,
            new ResultEvent(this, ResultEvent.RESULT_ACCEPTED));
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
    public void postResultCreated() {
        SpeechEventUtilities.postSpeechEvent(
            this,
            new ResultEvent(this, ResultEvent.RESULT_CREATED));
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
    public void postResultRejected() {
        setResultState(Result.REJECTED);
        SpeechEventUtilities.postSpeechEvent(
            this,
            new ResultEvent(this, ResultEvent.RESULT_REJECTED));
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
    public void postResultUpdated() {
        SpeechEventUtilities.postSpeechEvent(
            this,
            new ResultEvent(this, ResultEvent.RESULT_UPDATED));
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
    public void postTrainingInfoReleased() {
        SpeechEventUtilities.postSpeechEvent(
            this,
            new ResultEvent(this, ResultEvent.TRAINING_INFO_RELEASED));
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

    /** 
     * Concatenate the best tokens in the Result.
     */
    public String toString() {
        StringBuffer sb = new StringBuffer(getBestToken(0).getWrittenText());
        for (int i = 1; i < numTokens(); i++)
            sb.append(" " + getBestToken(i).getWrittenText());
        return sb.toString();
    }
  
    protected class BaseResultToken implements ResultToken {
        String token;
        BaseResultToken(String t) {
            token = t;
        }
        public Result getResult() { return null; }
        public int getAttachmentHint() { return 0; }
        public int getCapitalizationHint() { return 0; }
        public String getWrittenText() { return token; }
        public String getSpokenText() { return token; }
        public long getStartTime() { return 0; }
        public long getEndTime() { return 0; }
        public int getSpacingHint() { return ResultToken.SEPARATE; }
    }

    /**
     * Utility function to set the result state.
     */
    public void setResultState(int state) {
        this.state = state;
    }

    /**
     * If the result is in the given state, throw a ResultStateError.
     */
    protected void checkResultState(int state) throws ResultStateError {
        if (getResultState() == state) {
            throw new EngineStateError("Invalid ResultState: " + 
                                       getResultState());
        }
    }

    /**
     * Dispatch a SpeechEvent.  This is a method from SpeechEventDispatcher.
     * The dispatcher should notify all listeners of the speech event
     * from this method.
     */
    public void dispatchSpeechEvent(SpeechEvent event) {
        switch (event.getId()) {
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
        }
    }
    
    /** 
     * Set the grammar that goes with this Result.  NOT JSAPI.
     */
    public void setGrammar(Grammar g) {
        grammar = g;
    }

    /**
     * Try to set the Grammar and tokens of this result.  NOT JSAPI.
     */
    public boolean tryTokens(Grammar G, String S) {
        if ((S == null) || (G == null)) {
            return false;
        }
        
        if (G instanceof RuleGrammar) {
            try {
                RuleParse rp = ((RuleGrammar) G).parse(S, null);
                if (rp != null) {
                    grammar = G;
                    tags = rp.getTags();
                    ruleName = rp.getRuleName().getSimpleRuleName();
                    StringTokenizer st = new StringTokenizer(S);
                    nTokens = st.countTokens();
                    int i = 0;
                    theText = new String[nTokens];
                    while (st.hasMoreTokens()) {
                        theText[i++]=st.nextToken();
                    }
                    return true;
                }
            } catch (GrammarException e) {
            }
        }
        return false;
    }  
}
