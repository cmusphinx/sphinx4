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


// a test search manager.

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
import edu.cmu.sphinx.decoder.linguist.Linguist;
import edu.cmu.sphinx.decoder.linguist.SearchState;
import edu.cmu.sphinx.decoder.linguist.SearchStateArc;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;



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

    private boolean showTokenCount;
    private Map bestTokenMap;


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

	    SearchState state = linguist.getInitialSearchState();

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
        bestTokenMap = new HashMap(activeList.size() * 10);;

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
        moreTokens = scorer.calculateScores(activeList.getTokens());
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
     * Gets the best token for this state
     *
     * @param state the state of interest
     *
     * @return the best token
     */
    protected Token getBestToken(SearchState state) {
        return (Token) bestTokenMap.get(state);
    }

    /**
     * Sets the best token for a given state
     *
     * @param token the best token
     *
     * @param state the state
     *
     * @return the previous best token for the given state, or null if
     *    no previous best token
     */
    protected Token setBestToken(Token token, SearchState state) {
        return (Token) bestTokenMap.put(state, token);
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

	// If this is a final state, add it to the final list

	if (token.isFinal()) {
	    resultList.add(token);
	}

	SearchState state = token.getSearchState();
	SearchStateArc[] arcs = state.getSuccessors();

	// For each successor
	    // calculate the entry score for the token based upon the
	    // predecessor token score and the transition probabilities
	    // if the score is better than the best score encountered for
	    // the SearchState and frame then create a new token, add
	    // it to the lattice and the SearchState. 
	    // If the token is an emitting token add it to the list,
	    // othewise recursively collect the new tokens successors.

	 for (int i = 0; i < arcs.length; i++) {
	    SearchStateArc arc = arcs[i];
	    SearchState nextState = arc.getState();

	    // We're actually multiplying the variables, but since
	    // these come in log(), multiply gets converted to add
	    float logEntryScore = token.getScore() +  arc.getProbability();

            Token bestToken = getBestToken(nextState);
	    boolean firstToken = bestToken == null ;

	    if (firstToken || bestToken.getScore() <= logEntryScore) {
                Token newToken = new Token(token, nextState,
                        logEntryScore, 
                        arc.getLanguageProbability(),
                        arc.getInsertionProbability(),
                        currentFrameNumber);

                tokensCreated.value++;


                setBestToken(newToken, nextState);

                if (!newToken.isEmitting()) {
                    collectSuccessorTokens(newToken);
                } else {
                    if (firstToken) {
                        activeList.add(newToken);
                    } else {
                        activeList.replace(bestToken, newToken); 
                    }
                }
	    } 
        }
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
     * Returns the best token map.
     *
     * @return the best token map
     */
    protected Map getBestTokenMap() {
        return bestTokenMap;
    }


    /**
     * Sets the best token Map.
     *
     * @param bestTokenMap the new best token Map
     */
    protected void setBestTokenMap(Map bestTokenMap) {
        this.bestTokenMap = bestTokenMap;
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
    public void setResultList(List resultList) {
	this.resultList = resultList;
    }


    /**
     * Returns the current frame number.
     *
     * @return the current frame number
     */
    public int getCurrentFrameNumber() {
	return currentFrameNumber;
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
}
