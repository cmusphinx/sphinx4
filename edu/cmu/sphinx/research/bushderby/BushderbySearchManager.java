
/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electronic Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.research.bushderby;

import java.util.List;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collection;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

import edu.cmu.sphinx.knowledge.language.LanguageModel;
import edu.cmu.sphinx.knowledge.dictionary.Dictionary;

import edu.cmu.sphinx.knowledge.acoustic.Unit;
import edu.cmu.sphinx.knowledge.acoustic.LeftRightContext;

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.Timer;
import edu.cmu.sphinx.util.StatisticsVariable;

import edu.cmu.sphinx.result.Result;

import edu.cmu.sphinx.decoder.linguist.Color;
import edu.cmu.sphinx.decoder.linguist.Linguist;
import edu.cmu.sphinx.decoder.linguist.SearchState;
import edu.cmu.sphinx.decoder.linguist.SearchStateArc;
import edu.cmu.sphinx.decoder.linguist.UnitSearchState;
import edu.cmu.sphinx.decoder.linguist.WordSearchState;
import edu.cmu.sphinx.decoder.linguist.simple.SentenceHMMState;
import edu.cmu.sphinx.decoder.linguist.simple.SentenceHMMStateArc;
import edu.cmu.sphinx.decoder.linguist.simple.HMMStateState;
import edu.cmu.sphinx.decoder.linguist.simple.UnitState;
import edu.cmu.sphinx.decoder.linguist.simple.WordState;

import edu.cmu.sphinx.decoder.search.Pruner;
import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.decoder.search.ActiveList;

import edu.cmu.sphinx.decoder.search.SimpleBreadthFirstSearchManager;

import edu.cmu.sphinx.decoder.scorer.AcousticScorer;



/**
 * Provides the breadth first search. To perform recognition
 * an application should call initialize before
 * recognition begins, and repeatedly call <code> recognize </code>
 * until Result.isFinal() returns true.  Once a final result has been
 * obtained, <code> terminate </code> should be called. 
 *
 * Note that all scores and probabilities are maintained internally in
 * the LogMath log domain.
 */

public class BushderbySearchManager extends SimpleBreadthFirstSearchManager {

    private final static String PROP_PREFIX = 
	"edu.cmu.sphinx.research.bushderby.BushderbySearchManager.";

    /**
     * The sphinx property for the Bushderby eta value.
     */
    public final static String PROP_BUSHDERBY_ETA =
	PROP_PREFIX + "bushderbyEta";

    /**
     * The default value for the Bushderby eta value, which is 1e99.
     */
    public final static double PROP_BUSHDERBY_ETA_DEFAULT = 1E99;

    /**
     * The sphinx property that defines whether to filter successor
     * states during the search.
     */
    public final static String PROP_FILTER_SUCCESSORS =
	PROP_PREFIX + "filterSuccessors";

    /**
     * The default value for whether to filter success states during
     * the search, which is true.
     */
    public final static boolean PROP_FILTER_SUCCESSORS_DEFAULT = false;

        
    private LanguageModel languageModel;

    private boolean filterSuccessors;

    private double bushderbyEta;
    

    /**
     * Initializes this BreadthFirstSearchManager with the given
     * context, linguist, scorer and pruner.
     *
     * @param context the context to use
     * @param linguist the Linguist to use
     * @param scorer the AcousticScorer to use
     * @param pruner the Pruner to use
     */
    public void initialize(String context, Linguist linguist,
			   AcousticScorer scorer, Pruner pruner) {

	super.initialize(context, linguist, scorer, pruner);

	SphinxProperties props = getSphinxProperties();
		
	bushderbyEta = props.getDouble(PROP_BUSHDERBY_ETA, 
				       PROP_BUSHDERBY_ETA_DEFAULT);
	
	System.out.println("bushderbyEta is " + bushderbyEta);

	filterSuccessors 
	    = props.getBoolean(PROP_FILTER_SUCCESSORS, 
			       PROP_FILTER_SUCCESSORS_DEFAULT);
        
	if (getLinguist() != null) {
	    languageModel  = getLinguist().getLanguageModel();
	}
    }


    private void M(String s) {
        if (false) {
            System.out.println(s);
        }
    }
   

    /**
     * Grow branches using Bushderby.
     *
     * Goes through the active list of tokens and expands each
     * token, finding the set of successor tokens until all the successor
     * tokens are emitting tokens.  With bushderby, non-emitting green
     * nodes are collected into a listed called the
     * delayedExpansionList.  This is a list of green nodes that are
     * non-emitting. Non-emitting nodes are usually grown immediately
     * until we find emitting nodes, but with bushderby, we have to
     * delay this expansion until the node is completely scored. To do
     * this, the non-emitting green nodes are placed in a
     * 'delayedExpansionList'. After all normal tokens are grown, the
     *  tokens in this delayedExpansionList are given the second
     *  bushderby pass and then grown. This process repeats until the
     *  delayedExpansionList is empty.
     *
     */
    protected void growBranches() {
	getGrowTimer().start();

        setBestTokenMap(new HashMap(getActiveList().size() * 10));
	
	int pass = 0;
	boolean moreTokensToExpand = true;
	
	ActiveList oldActiveList = getActiveList();

	Iterator iterator = getActiveList().iterator();
	setResultList(new LinkedList());

	setActiveList(getActiveList().createNew());
			
	while (moreTokensToExpand) {
	    pass++;
	    List delayedExpansionList = new ArrayList();

	    while (iterator.hasNext()) {
		Token token = (Token) iterator.next();

		if (oldActiveList.isWorthGrowing(token)) {
		    collectSuccessorTokens(token, delayedExpansionList);
		}
	    }

	    if (delayedExpansionList != null && 
		delayedExpansionList.size() > 0) {
		finalizeBushderby(delayedExpansionList.iterator());
		iterator = delayedExpansionList.iterator();
		if (false) {
		    System.out.println("Pass " + pass + " Processing " +
			    delayedExpansionList.size() + " delayed tokens");
		}
	    } else {
		moreTokensToExpand = false;
	    }
	}

	finalizeBushderby(getActiveList().iterator());

	getGrowTimer().stop();
    }


    /**
     * Chase through the list, find all Green nodes and convert the
     * scores to the final bushderby score
     */
    private void finalizeBushderby(Iterator iterator) {

	while (iterator.hasNext()) {
	    Token token = (Token) iterator.next();
            SentenceHMMState state = (SentenceHMMState) token.getSearchState();
	    if (isGreenState(state)) {
		double logNewScore = (float) 
		    (token.getWorkingScore() / bushderbyEta);
		if (false) {
		    System.out.println("OS: " + token.getScore() + " NS: "
			    + logNewScore);
		}
		token.setScore((float) logNewScore);
	    }
	}
    }


    /**
     * Returns true if the given SentenceHMMState is considered a GREEN
     * state by Bushderby.
     *
     * @param state the SentenceHMMState to be tested
     *
     * @return true if the given SearchState is a GREEN state, false otherwise
     */
    private boolean isGreenState(SentenceHMMState state) {
	boolean green = ((state.getColor() == Color.GREEN) ||
			 (state instanceof HMMStateState));
	if (green) {
	    state.setColor(Color.GREEN);
	}
	return green;
    }



    /**
     * Collects the next set of emitting tokens from a token 
     * and accumulates them in the active or result lists
     *
     * @param token  the token to collect successors from
     * @param delayedExpansionList the place where tokens that cannot
     * be immediately expaned are placed. Null if we should always
     * expand all nodes.
     */
    private void collectSuccessorTokens(Token token,
					List delayedExpansionList) {

	// System.out.println("Entering cst...");

	// If this is a final state, add it to the final list
	if (token.isFinal()) {
	    getResultList().add(token);
	    return;
	}

	SentenceHMMState state = (SentenceHMMState) token.getSearchState();
	SentenceHMMStateArc[] arcs = 
            (SentenceHMMStateArc[]) state.getSuccessors();

	// For each successor
	// calculate the entry score for the token based upon the
	// predecessor token score and the transition probabilities
	// if the score is better than the best score encountered for
	// the SentenceHMMState and frame then create a new token, add
	// it to the lattice and the SentenceHMMState. 
	// If the token is an emitting token add it to the list,
	// othewise recursively collect the new tokens successors.
	
	for (int i = 0; i < arcs.length; i++) {
	    SentenceHMMStateArc arc = arcs[i];
	    SentenceHMMState nextState = (SentenceHMMState) arc.getState();

	    if (filterSuccessors && !isValidTransition(token, nextState)) {
		continue;
	    }

	    float logLanguageProbability = getLanguageProbability(token, arc);
	    
	    // We're actually multiplying the variables, but since
	    // these come in log(), multiply gets converted to add
	    float logCurrentScore = token.getScore() + 
		logLanguageProbability +
		arc.getAcousticProbability() + 
		arc.getInsertionProbability();
	    
	    boolean firstToken = (getBestToken(nextState) == null);
	    boolean greenToken = isGreenState(nextState);

	    double logWorkingScore =  firstToken ? getLogMath().getLogZero() :
		getBestToken(nextState).getWorkingScore();

	    if (firstToken ||  nextState.isFanIn() ||
		getBestToken(nextState).getScore() <= logCurrentScore) {
		
		// we may want to create  green tokens all the time
		if (greenToken || 
		    getActiveList().isInsertable(logCurrentScore)) {

		    Token newToken = new Token(
			token, 			// the predecessor
			nextState, 		// the SentenceHMMState
			logCurrentScore, 		// the score on entry
			logLanguageProbability, 	// entry lang score
			arc.getInsertionProbability(), // insertion prob
			getCurrentFrameNumber() 	// the frame number
		    );
                    getTokensCreated().value++;

		    newToken = collapseToken(newToken);

		    Token oldBestToken = setBestToken(newToken, nextState);

		    if (!newToken.isEmitting()) {
			if (greenToken && delayedExpansionList != null) {
			    if (oldBestToken != null && 
				oldBestToken.getScore() <= logCurrentScore) {
			        int oldTokenIdx = 
				    delayedExpansionList.indexOf(oldBestToken);
				if (oldTokenIdx >= 0)
				    delayedExpansionList.remove(oldTokenIdx);
		            }
			    delayedExpansionList.add(newToken);
			} else  {
			    // System.out.println("Recursing into cst...");
			    collectSuccessorTokens(newToken, 
                                                   delayedExpansionList);
			}
                    } else if (firstToken || nextState.isFanIn()) {
                        getActiveList().add(newToken);
                    } else {
			getActiveList().replace(oldBestToken, newToken); 
		    }
		}
	    } 

	    // with Bushderby nodes are either 'combine' or 'compete'
	    // Green nodes are 'combine' nodes.  If bushderby is not
	    // enabled, all nodes are considered to be 'compete'
	    // nodes.
	    //
	    // If bushderby is enabled then we accumulate a working
	    // score for this token which combines the working scores
	    // for all paths into this token


	    if (greenToken) {
		Token bestToken = getBestToken(nextState);
		if (bestToken != null) {
		    logWorkingScore =  getLogMath().addAsLinear(
                            logWorkingScore, 
			    logCurrentScore * bushderbyEta);
		    bestToken.setWorkingScore(logWorkingScore);
		}
		if (false) {
		    System.out.println("CS: " + logCurrentScore +
			     " WS: " + logWorkingScore);
		}
	    }
        }
    }



    /**
     * Deterimines if the transition from the given token (given its
     * perfect knowledge of context history) to the nextState is valid
     *
     * @param token the current token
     * @param nextState the next sentence hmm state
     *
     * @return true if the transition is valid
     *
     */
    private boolean isValidTransition(Token token, SentenceHMMState nextState) {
	Unit prevUnit;
	Unit thisUnit;
	Unit[] thisLC;
	Unit[] thisRC;
	Unit[] prevLC;
	Unit[] prevRC;

	// if we are not transitioning to a unit state, then it is a
	// valid transition

	if (! (nextState instanceof UnitState)) {
	    return true;
	}


	// if we are transitioning to a unit state we have to check to
	// make sure that the contexts (left and right) of the
	// previous unit align properly with those of the next


	thisUnit = ((UnitState) nextState).getUnit();
	thisLC = getLeftContext(thisUnit);
	thisRC = getRightContext(thisUnit);

	prevUnit = getPreviousUnit(token);

	// if there is no previous unit, then the next unit's left
	// context should be empty


	if (prevUnit == null) {
	    return thisLC.length == 0;
	} 

	prevLC = getLeftContext(prevUnit);
	prevRC = getRightContext(prevUnit);

	// we have a transition between units, 
	// prev  RC had better not be empty or its not a valid transition
	// this  LC had better not be empty or its not a valid transition

	if (prevRC != null && (prevRC.length == 0 || thisLC.length == 0)) {
	    return false;
	}


	// if the previous right context is longer than the new unit
	// and its right context then its not a valid transition

	if (prevRC != null && thisRC != null 
		&& prevRC.length > (thisRC.length + 1)) {
	    return false;
	}

	// if this left context is longer than the previous left
	// context and unit then its not a valid transition

	if (thisLC.length > (prevLC.length + 1)) {
	    return false;
	}

	// now check the the previous right context matches this unit
	// and its context

	if (prevRC != null && !prevRC[0].getName().equals(thisUnit.getName())) {
	    return false;
	}

	for (int i = 1; prevRC != null && thisRC != null 
				&& i < prevRC.length; i++) {
	    if (prevRC[i] != thisRC[i - 1]) {
		return false;
	    }
	}

	// now check that this left context matches the previous  unit
	// and its context

	if (!thisLC[0].getName().equals(prevUnit.getName())) {
	    return false;
	}

	for (int i = 1; i < thisLC.length; i++) {
	    if (thisLC[i] != prevLC[i - 1]) {
		return false;
	    }
	}

	// if we made it to here, its a valid unit-to-unit transition
	// so we can return true

	return true;
    }



    /**
     * Given a unit return its left context
     *
     * @param unit the unit of interest
     *
     * @return the left context
     */
    private Unit[] getLeftContext(Unit unit) {
	return ((LeftRightContext) unit.getContext()).getLeftContext();
    }

    /**
     * Given a unit return its right context
     *
     * @param unit the unit of interest
     *
     * @return the right context
     */
    private Unit[] getRightContext(Unit unit) {
	return ((LeftRightContext) unit.getContext()).getRightContext();
    }

    /**
     * Given a token return the previous unit
     *
     * @param token the token where the unit search is started
     *
     * @return the unit or null if no unit is found
     */
    private Unit getPreviousUnit(Token token) {
	while (token != null) {
	    if (token.getSearchState() instanceof UnitSearchState) {
		return ((UnitSearchState) token.getSearchState()).getUnit();
	    }
	    token = token.getPredecessor();
	}
	return null;
    }


    /**
     * Given a linguist and an arc to the next token, determine a
     * language probability for the next state
     *
     * @param linguist  the linguist to use, null if there is no
     * linguist, in which case, the language probability for the
     * SentenceHMMStateArc will be used.
     *
     * @param arc the arc to the next state
     *
     * @return the language probability for the transition to the next
     * state (in LogMath log domain)
     */
    private float getLanguageProbability(Token token, 
					 SentenceHMMStateArc arc) {
	float logProbability = arc.getLanguageProbability();
	if (languageModel != null && arc.getNextState() instanceof WordState) {
	    WordState state = (WordState) arc.getNextState();
	    int depth = languageModel.getMaxDepth();
	    String word = state.getWord().getSpelling();

	    if (isWord(word)) {
		List wordList = new ArrayList(depth);
		wordList.add(word);
		while (token != null && wordList.size() < depth) {
		    if (token.getSearchState() 
				    instanceof WordSearchState) {
			WordSearchState prevWord =
			    (WordSearchState) token.getSearchState();
			String prevSpelling =
			    prevWord.getPronunciation().getWord();
			if (isWord(prevSpelling)) {
			    wordList.add(prevSpelling);
			}
		    }
		    token = token.getPredecessor();
		}

		if (token == null && wordList.size() < depth) {
		    wordList.add(Dictionary.SENTENCE_START_SPELLING);
		}
		Collections.reverse(wordList);
		logProbability = (float) languageModel.getProbability(wordList);
	    }
	}
	return logProbability;
    }


    /**
     * Determines if a word is a real word and not silence, garbage or
     * some other sort of filler
     *
     * @param word the word to check
     *
     * @return true if the word is a real word.
     */
    private boolean isWord(String word) {
	return !word.equals(Dictionary.SILENCE_SPELLING);
    }

    /**
     * May collapse a set of tokens in a token branch to a single
     * high-level token. May also remove references to the
     * SentenceHMMState tree
     *
     * @param token the token to try to collaplse
     */
    private final Token collapseToken(Token token) {
	// TBD: Does nothing now.
	return token;
    }

}
