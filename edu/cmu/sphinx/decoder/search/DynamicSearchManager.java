
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

package edu.cmu.sphinx.decoder.search;

import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;
import edu.cmu.sphinx.knowledge.language.LanguageModel;
import edu.cmu.sphinx.knowledge.acoustic.AcousticModel;

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.Timer;
import edu.cmu.sphinx.util.StatisticsVariable;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.decoder.search.*;
import edu.cmu.sphinx.decoder.scorer.AcousticScorer;
import edu.cmu.sphinx.decoder.linguist.*;


/**
 * A dynamic search manager. This search manager uses a linguist, but
 * does not require a sentence hmm
 *
 * NOTE: this search manager does not currently work
 *
 * Note that all scores and probabilities are maintained in the
 * LogMath log domain
 */

public class DynamicSearchManager implements  SearchManager {
    public final static String PROP_ACTIVE_LIST_TYPE =
	"edu.cmu.sphinx.decoder.search.DynamicSearchManager.activeListType";
    public final static String PROP_RESULTS_VALIDATE =
	"edu.cmu.sphinx.decoder.search.validateResults";

    private final static boolean COMBINE_BRANCHES = true;
    private final static String SENTENCE_START = "<s>";

    private Linguist linguist;		// Provides grammar/language info
    private Pruner pruner;		// used to prune the active list
    private AcousticScorer scorer;	// used to score the active list
    private AcousticModel acousticModel;

    private int currentFrameNumber;	// the current frame number
    private ActiveList activeList;	// the list of active tokens
    private ActiveList forwardList;	// the list of active tokens
    private List resultList;		// the current set of results
    private SphinxProperties props;	// the sphinx properties
    private LogMath logMath;

    private Timer scoreTimer;
    private Timer pruneTimer;
    private Timer growTimer;
    private boolean validateResults;	// if true validate results
    private StatisticsVariable totalTokensScored;
    private StatisticsVariable curTokensScored;
    private List activeListQueue;
    private List resultListQueue;

    /**
     * Creates a BreadthFirstSearchManager with the given
     * subcomponents

     * @param context the context
     * @param linguist the linguist to use in the search
     * @param scorer the acoustic score to use in the search
     * @param pruner the state pruner to use in the search
     */
    public DynamicSearchManager(String context,
	    	Linguist linguist, AcousticScorer scorer, Pruner
		pruner, AcousticModel acousticModel) {
	this.linguist = linguist;
	this.scorer = scorer;
	this.pruner = pruner;
	this.props = SphinxProperties.getSphinxProperties(context);
	this.logMath = LogMath.getLogMath(context);

	scoreTimer = Timer.getTimer(context, "Score");
	pruneTimer = Timer.getTimer(context, "Prune");
	growTimer = Timer.getTimer(context, "Grow");
	totalTokensScored = StatisticsVariable.getStatisticsVariable(
		props.getContext(), "totalTokensScored");
	curTokensScored = StatisticsVariable.getStatisticsVariable(
		props.getContext(), "curTokensScored");

	validateResults = props.getBoolean(PROP_RESULTS_VALIDATE, false);
	activeListQueue = new ArrayList(acousticModel.getRightContextSize());
	resultListQueue = new ArrayList(acousticModel.getRightContextSize());
    }


    /**
     * Initialize this DynamicSearchManager with the given context,
     * Linguist, AcousticScorer, and Pruner.
     *
     * @param context the context to use
     * @param linguist the Linguist to use
     * @param scorer the scorer to use
     * @param pruner the pruner to use
     */
    public void initialize(String context, Linguist linguist,
			   AcousticScorer scorer, Pruner pruner) {
			   
	// TODO: implement this method
	throw new Error("DynamicSearchManager.initialize() not implemented");
    }

    /**
     * Called at the start of recognition. Gets the search manager
     * ready to recognize
     */
    public void start() {
        currentFrameNumber = 0;
	linguist.start();
	pruner.start();
	scorer.start();
	createInitialLists();
	curTokensScored.value = 0;
    }


    /**
     * Performs the recognition for the given number of frames.
     *
     * @param nFrames the number of frames to recognize
     *
     * @return the current result
     */
    public Result recognize(int nFrames) {
	boolean done = false;
	Result result;

        for (int i = 0; i < nFrames && !done; i++) {
	    done = recognize();
	}
	result = new Result(activeList, resultList, currentFrameNumber, done);

	if (validateResults) {
	    if (result.validate()) {
		System.out.println("** Results are VALID **");
	    } 
	}
	return result;
    }


    /**
     * Terminates a recognition
     */
    public void stop() {
	scorer.stop();
	pruner.stop();
	linguist.stop();
    }

    /**
     * Performs recognition for one frame. Returns true if recognition
     * has been completed.
     *
     * @return <code>true</code> if recognition is completed.
     */
    private boolean recognize() {
	activeList = (ActiveList) activeListQueue.remove(0);
	resultList = (List) resultListQueue.remove(0);

	boolean more = scoreTokens(); // score emitting tokens
        if (more) {	
	    pruneBranches(); 		// eliminate poor branches
	    growBranches(); 		// extend remaining branches  
	    currentFrameNumber++;
	} 

	if (false) {
	    System.out.print("Active " + activeList.size() + " ");
	    Token.showCount();
	}

        return !more;
    }
    

    private final static int STATES_PER_UNIT = 3; // BUG

    /**
     * Gets the initial grammar node from the linguist and
     * creates a GrammarNodeToken 
     *
     * @return the intial active grammar node token for the grammar
     */
    private void createInitialLists() {
	try {
	    ActiveList newActiveList = (ActiveList)
		Class.forName( props.getString(PROP_ACTIVE_LIST_TYPE,
		"edu.cmu.sphinx.decoder.search.SimpleActiveList")).newInstance();
	    newActiveList.setProperties(props);

	    SentenceHMMState state = linguist.getInitialState();
	    float one = (float) logMath.linearToLog(1.0);
	    newActiveList.add(new Token(null, state, one, one, one, 0));
	    activeList = newActiveList;
	    for (int i = 0; i < (acousticModel.getRightContextSize() +
			1) * STATES_PER_UNIT; i++) {
		growBranches();
	    }
	} catch (ClassNotFoundException fe) {
	    throw new Error("Can't create active list", fe);
	} catch (InstantiationException ie) {
	    throw new Error("Can't create active list", ie);
	} catch (IllegalAccessException iea) {
	    throw new Error("Can't create active list", iea);
	}
    }

    /**
     * Goes through the active list of tokens and expands each
     * token, finding the set of successor tokens until all the successor
     * tokens are emitting tokens.
     *
     */
    private void growBranches() {
	growTimer.start();

	Iterator oldListIterator = forwardList.iterator();
	ActiveList newActiveList = forwardList.createNew();
	List newResultList = new LinkedList();
		
	while (oldListIterator.hasNext()) {
	    Token token = (Token) oldListIterator.next();

	    // only grow if token is still valid
	    if (isValidBranch(token)) {
		 collectSuccessorTokens(token, newActiveList, newResultList);
	    }
	}
	growTimer.stop();

	activeListQueue.add(newActiveList);
	resultListQueue.add(newResultList);
	forwardList = newActiveList;
    }


    /**
     * Determines if the branch that is ended by the given token is
     * still valid or not by checking to see that all tokens within
     * the size of the right context (which is the look ahead amount)
     *  are still alive.
     *
     * @param token the token at the end of the branch
     *
     * @return true if the branch is still valid
     */
    private boolean isValidBranch(Token token) {
	Token activeParent = null;
	for (int i = 0; i < acousticModel.getRightContextSize(); i++) {
	    activeParent = token.getPredecessor();
	    /*
	    if (!activeParent.isAlive()) {
		return false;
	    }
	    */
	}
	return true;
    }

    /**
     * Calculate the acoustic scores for the active list. The active
     * list should contain only emitting tokens.
     *
     * @return true if there are more tokens, otherwise false
     *
     */
    private boolean  scoreTokens() {
	boolean moreTokens;
	scoreTimer.start();

	// update scores with the last emitting 
	for (Iterator i = activeList.iterator(); i.hasNext(); ) {
	    Token token = (Token) i.next();
	    token.applyScore(getPreviousScore(token), null);
	}

        moreTokens = scorer.calculateScores(activeList);
	scoreTimer.stop();

	curTokensScored.value += activeList.size();
	totalTokensScored.value += activeList.size();

	return moreTokens;

    }


    /**
     * Finds the last emitting token for this token and return its
     * score
     *
     * @param token the token of interest
     *
     * @return the score of the previous emitting token or 0.0 if
     * there are not previous emitting tokens. The score is in the
     * LogMath log domain
     */
    private float getPreviousScore(Token token) {
	float logScore = 0.0f;
	Token prev = token.getPredecessor();
	while (prev != null) {
	    if (prev.isEmitting()) {
		logScore = prev.getScore();
		break;
	    } else {
		prev = prev.getPredecessor();
	    }
	}
	return logScore;
    }

    /**
     * Removes unpromising branches from the active list
     *
     */
    private void pruneBranches() {
	 pruneTimer.start();
         activeList =  pruner.prune(activeList); 
	 pruneTimer.stop();
	 
    }

    /**
     * Collects the next set of emitting tokens from a token 
     * and accumulates them in the active or result lists
     *
     * @param token  the token to collect successors from
     */
    private void collectSuccessorTokens(Token token,
	    ActiveList newActiveList, List newResultList) {
	int nextFrameNumber = token.getFrameNumber();


	// If this is a final state, add it to the final list

	if (token.isFinal()) {
	    newResultList.add(token);
	}

	// we only bump counter if we are finishing up an emitting
	// state

	if (token.isEmitting()) {
	    nextFrameNumber++;
	}

	SentenceHMMState state = token.getSentenceHMMState();
	Collection arcs = getSuccessors(state);

	// For each successor
	    // calculate the entry score for the token based upon the
	    // predecessor token score and the transition probabilities
	    // if the score is better than the best score encountered for
	    // the SentenceHMMState and frame then create a new token, add
	    // it to the lattice and the SentenceHMMState. 
	    // If the token is an emitting token add it to the list,
	    // othewise recursively collect the new tokens successors.
	    // (Note that 'COMBINE_BRANCHES' is a configuration variable.
	    // Assume for now that it is always set to 'true'

	 for (Iterator i = arcs.iterator(); i.hasNext(); ) {
	    SentenceHMMStateArc arc = (SentenceHMMStateArc) i.next();
	    SentenceHMMState nextState = arc.getNextState();

	    float logLanguageProbability = getLanguageProbability(token, arc);
	    float logCurrentScore = 0f;

	    Token newToken = new Token(
		token, 			// the predecessor
		nextState, 		// the SentenceHMMState
		logCurrentScore, 		// the score on entry
		logLanguageProbability, 	// entry lang score
		arc.getInsertionProbability(), // entry insertion prob
		nextFrameNumber 	// the frame number
	    );

	    if (!newToken.isEmitting()) {
		collectSuccessorTokens(newToken,
			newActiveList, newResultList);
	    }
	    else {
		newActiveList.add(newToken); 
	    }
	 } 
    }

    private Collection getSuccessors(SentenceHMMState state) {
	List list = new ArrayList();

	if (state instanceof GrammarState) {
	    collectGrammarSuccesors(list, (GrammarState) state);
	} else if (state instanceof AlternativeState) {
	    collectAltSuccesors(list, (AlternativeState) state);
	}
	return null;
    }


    private void collectGrammarSuccesors(List list, GrammarState gState) {
	GrammarNode gNode = gState.getGrammarNode();
	GrammarWord[][] alternatives = gNode.getAlternatives();

	for (int i = 0; i < alternatives.length; i++) {
	    AlternativeState aState = new AlternativeState(gState, i);
	    list.add(aState);
	}
    }

    private void collectAltSuccesors(List list, AlternativeState aState) {
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
     * @return the language probability (in the logmath log domain)
     */
    private float getLanguageProbability(Token token, SentenceHMMStateArc arc) {
	float logProbability = arc.getLanguageProbability();
	if (arc.getNextState() instanceof WordState) {
	    WordState state = (WordState) arc.getNextState();
	    if (linguist != null) {
		LanguageModel model = linguist.getLanguageModel();
		if (model != null) {
		    int depth = model.getMaxDepth();
		    String word = state.getWord().getSpelling();

		    if (isWord(word)) {
			List wordList = new ArrayList(depth);
			wordList.add(word);
			while (token != null && wordList.size() < depth) {
			    if (token.getSentenceHMMState() 
					    instanceof WordState) {
				WordState prevWord =
				    (WordState) token.getSentenceHMMState();
				String prevSpelling =
				    prevWord.getWord().getSpelling();
				if (isWord(prevSpelling)) {
				    wordList.add(prevSpelling);
				}
			    }
			    token = token.getPredecessor();
			}

			if (token == null && wordList.size() < depth) {
			    wordList.add(SENTENCE_START);
			}
			Collections.reverse(wordList);
			logProbability = (float) model.getProbability(wordList);
		    }
		}
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
	return !word.equals("<sil>");
    }

    /**
     * May collapse a set of tokens in a token branch to a single
     * high-level token. May also remove references to the
     * SentenceHMMState tree
     *
     * @param token the token to try to collaplse
     */
    private static Token collapseToken(Token token) {
	// TBD: Does nothing now.
	return token;
    }
}
