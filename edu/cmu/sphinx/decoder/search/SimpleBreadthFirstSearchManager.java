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

import edu.cmu.sphinx.knowledge.acoustic.LeftRightContext;
import edu.cmu.sphinx.knowledge.acoustic.Unit;
import edu.cmu.sphinx.knowledge.language.LanguageModel;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.StatisticsVariable;
import edu.cmu.sphinx.util.Timer;
import edu.cmu.sphinx.decoder.search.Pruner;
import edu.cmu.sphinx.decoder.search.SearchManager;
import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.decoder.search.ActiveList;
import edu.cmu.sphinx.decoder.scorer.AcousticScorer;
import edu.cmu.sphinx.decoder.linguist.SentenceHMMState;
import edu.cmu.sphinx.decoder.linguist.SentenceHMMStateArc;
import edu.cmu.sphinx.decoder.linguist.Linguist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


/**
 * Provides the breadth first search. To perform recognition
 * an application should call initialize before
 * recognition begins, and repeatedly call <code> recognize </code>
 * until Result.isFinal() returns true.  Once a final result has been
 * obtained, <code> terminate </code> should be called. 
 *
 * All scores and probabilities are maintained in the log math log
 * domain.
 */

public class SimpleBreadthFirstSearchManager implements  SearchManager {

    private final static String PROP_PREFIX =
	"edu.cmu.sphinx.decoder.search.BreadthFirstSearchManager.";
    
    /**
     * Sphinx property that defines the type of active list to use
     */
    public final static String PROP_ACTIVE_LIST_TYPE =
	PROP_PREFIX + "activeListType";

    /**
     * The default value for the PROP_ACTIVE_LIST_TYPE property
     */
    public final static String PROP_ACTIVE_LIST_TYPE_DEFAULT =
	"edu.cmu.sphinx.decoder.search.SimpleActiveList";


    /**
     * Sphinx property that defines the language weight for the search
     */
    public final static String PROP_LANGUAGE_WEIGHT  =
	PROP_PREFIX + "languageWeight";

    /**
     * The default value for the PROP_LANGUAGE_WEIGHT property
     */
    public final static float PROP_LANGUAGE_WEIGHT_DEFAULT  = 1.0f;

    /**
     * A sphinx property than, when set to <code>true</code> will
     * cause the recognizer to count up all the tokens in the active
     * list after every frame.
     */
    public final static String PROP_SHOW_TOKEN_COUNT =
	PROP_PREFIX + "showTokenCount";

    /**
     * The default value for the PROP_SHOW_TOKEN_COUNT property
     */
    public final static boolean PROP_SHOW_TOKEN_COUNT_DEFAULT = false;


    private Linguist linguist;		// Provides grammar/language info
    private Pruner pruner;		// used to prune the active list
    private AcousticScorer scorer;	// used to score the active list

    private int currentFrameNumber;	// the current frame number
    private ActiveList activeList;	// the list of active tokens
    private List resultList;		// the current set of results
    private SphinxProperties props;	// the sphinx properties
    private LogMath logMath;

    private Timer scoreTimer;
    private Timer pruneTimer;
    private Timer growTimer;

    private StatisticsVariable totalTokensScored;
    private StatisticsVariable curTokensScored;
    private StatisticsVariable tokensCreated;

    private float languageWeight;
    private boolean showTokenCount;


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
	tokensCreated = 
            StatisticsVariable.getStatisticsVariable(props.getContext(),
		    "tokensCreated");

        languageWeight = props.getFloat(PROP_LANGUAGE_WEIGHT,
                PROP_LANGUAGE_WEIGHT_DEFAULT);

	showTokenCount = props.getBoolean(PROP_SHOW_TOKEN_COUNT, false);
    }


    /**
     * Called at the start of recognition. Gets the search manager
     * ready to recognize
     */
    public void start() {
	linguist.start();
	pruner.start();
	scorer.start();
	localStart();
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

	if (showTokenCount) {
	    showTokenCount();
	}
	return result;
    }


    /**
     * Terminates a recognition
     */
    public void stop() {
        localStop();
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
    protected boolean recognize() {
	boolean more = scoreTokens(); // score emitting tokens
        if (more) {	
	    pruneBranches(); 		// eliminate poor branches
            growBranches(); 	        // extend remaining branches  
	    currentFrameNumber++;
	} 
        return !more;
    }

    /**
     * Gets the initial grammar node from the linguist and
     * creates a GrammarNodeToken 
     */
    protected void localStart() {
        currentFrameNumber = 0;
	curTokensScored.value = 0;

	try {
	    ActiveList newActiveList = (ActiveList)
		Class.forName( props.getString(PROP_ACTIVE_LIST_TYPE,
		PROP_ACTIVE_LIST_TYPE_DEFAULT)).newInstance();
	    newActiveList.setProperties(props);

	    SentenceHMMState state = linguist.getInitialState();

	    newActiveList.add(new Token(state, currentFrameNumber));
	    activeList = newActiveList;
	    growBranches();
	} catch (ClassNotFoundException fe) {
	    throw new Error("Can't create active list", fe);
	} catch (InstantiationException ie) {
	    throw new Error("Can't create active list", ie);
	} catch (IllegalAccessException iea) {
	    throw new Error("Can't create active list", iea);
	}
    }


    /**
     * Local cleanup for this search manager
     */
    protected void localStop() {
    }


    /**
     * Goes through the active list of tokens and expands each
     * token, finding the set of successor tokens until all the successor
     * tokens are emitting tokens.
     *
     */
    protected void growBranches() {

	growTimer.start();

        ActiveList oldActiveList = activeList;

	Iterator oldListIterator = activeList.iterator();
	resultList = new LinkedList();

	activeList = activeList.createNew();
		
	while (oldListIterator.hasNext()) {
	    Token token = (Token) oldListIterator.next();

            if (oldActiveList.isWorthGrowing(token)) {
                collectSuccessorTokens(token);
            }
	}
	growTimer.stop();
    }


    /**
     * Calculate the acoustic scores for the active list. The active
     * list should contain only emitting tokens.
     *
     * @return <code>true</code>  if there are more frames to score,
     * otherwise, false
     *
     */
    protected boolean  scoreTokens() {
	boolean moreTokens;
	scoreTimer.start();
        moreTokens = scorer.calculateScores(activeList);
	scoreTimer.stop();

	curTokensScored.value += activeList.size();
	totalTokensScored.value += activeList.size();

	return moreTokens;

    }

    /**
     * Removes unpromising branches from the active list
     *
     */
    protected void pruneBranches() {
	 pruneTimer.start();
         activeList =  pruner.prune(activeList); 
	 pruneTimer.stop();
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
    protected void collectSuccessorTokens(Token token) {
	int nextFrameNumber = token.getFrameNumber();

	// If this is a final state, add it to the final list

	if (token.isFinal()) {
	    resultList.add(token);
            return;
	}

	// we only bump counter if we are finishing up an emitting
	// state

	if (token.isEmitting()) {
	    nextFrameNumber++;
	}

	SentenceHMMState state = token.getSentenceHMMState();
	SentenceHMMStateArc[] arcs = state.getSuccessorArray();

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
	    SentenceHMMState nextState = arc.getNextState();

	    float logLanguageProbability = getLanguageProbability(token, arc);
	    // We're actually multiplying the variables, but since
	    // these come in log(), multiply gets converted to add
	    float logEntryScore = token.getScore() +  
			        logLanguageProbability +
				arc.getAcousticProbability() + 
				arc.getInsertionProbability();


	    boolean firstToken = nextState.getBestToken() == null ||
		nextState.getBestToken().getFrameNumber() != nextFrameNumber;

	    if (firstToken ||
                    nextState.getBestToken().getScore()<=logEntryScore) {

                Token newToken = new Token( token, nextState,
                        logEntryScore, 
                        logLanguageProbability, arc.getInsertionProbability(), 
                        nextFrameNumber);

                tokensCreated.value++;

                Token oldBestToken = nextState.setBestToken(newToken);

                if (!newToken.isEmitting()) {
                    collectSuccessorTokens(newToken);
                } else {
                    if (firstToken) {
                        activeList.add(newToken);
                    } else {
                        activeList.replace(oldBestToken, newToken); 
                    }
                }
	    } 
        }
    }


    /**
     * Given a linguist and an arc to the next token, determine a
     * language probability for the next state
     *
     * @param token the current token
     * @param arc the arc to the next state
     *
     * @return the language probability in the LogMath log domain
     */
    private float getLanguageProbability(Token token, SentenceHMMStateArc arc) {
	float logProbability = arc.getLanguageProbability();
	return logProbability * languageWeight;
    }

    /**
     * Counts all the tokens in the active list (and displays them).
     * This is an expensive operation.
     */
    private void showTokenCount() {
	Set tokenSet = new HashSet();

	for (Iterator i = activeList.iterator(); i.hasNext(); ) {
	    Token token = (Token) i.next();
	    while (token != null) {
		tokenSet.add(token);
		token = token.getPredecessor();
	    }
	}

	System.out.println("Token Lattice size: " + tokenSet.size());

	tokenSet = new HashSet();

	for (Iterator i = resultList.iterator(); i.hasNext(); ) {
	    Token token = (Token) i.next();
	    while (token != null) {
		tokenSet.add(token);
		token = token.getPredecessor();
	    }
	}

	System.out.println("Result Lattice size: " + tokenSet.size());
    }


    /**
     * Returns the Linguist.
     *
     * @return the Linguist
     */
    public Linguist getLinguist() {
	return linguist;
    }
    

    /**
     * Returns the Pruner.
     *
     * @return the Pruner
     */
    public Pruner getPruner() {
	return pruner;
    }
    

    /**
     * Returns the AcousticScorer.
     *
     * @return the AcousticScorer
     */
    public AcousticScorer getAcousticScorer() {
	return scorer;
    }


    /**
     * Returns the LogMath used.
     *
     * @return the LogMath used
     */
    public LogMath getLogMath() {
	return logMath;
    }


    /**
     * Returns the SphinxProperties used.
     *
     * @return the SphinxProperties used
     */
    public SphinxProperties getSphinxProperties() {
	return props;
    }
    

    /**
     * Returns the ActiveList.
     *
     * @return the ActiveList
     */
    public ActiveList getActiveList() {
	return activeList;
    }
    

    /**
     * Sets the ActiveList.
     *
     * @param activeList the new ActiveList
     */
    public void setActiveList(ActiveList activeList) {
	this.activeList = activeList;
    }
    

    /**
     * Returns the result list.
     *
     * @return the result list
     */
    public List getResultList() {
	return resultList;
    }
    

    /**
     * Sets the result list.
     *
     * @param resultList the new result list
     */
    public void setResultList(List list) {
	this.resultList = resultList;
    }
 

    /**
     * Returns the Timer for growing.
     *
     * @return the Timer for growing
     */
    public Timer getGrowTimer() {
	return growTimer;
    }


    /**
     * Returns the tokensCreated StatisticsVariable.
     *
     * @return the tokensCreated StatisticsVariable.
     */
    public StatisticsVariable getTokensCreated() {
	return tokensCreated;
    }


    /**
     * Returns the language weight.
     *
     * @return the language weight
     */
    public float getLanguageWeight() {
        return languageWeight;
    }
}
