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
import edu.cmu.sphinx.decoder.linguist.WordSearchState;
import edu.cmu.sphinx.decoder.linguist.SearchStateArc;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;



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
     * A sphinx property that defines the size of the word beam, that
     * is, the maximum number of words propagated per frame. Note that a
     * zero value indicates that all words should be propagated.
     */
    public final static String PROP_ABSOLUTE_WORD_BEAM_WIDTH =
        PROP_PREFIX + "absoluteWordBeamWidth";

    /**
     * The default value for the PROP_ABSOLUTE_WORD_BEAM_WIDTH  property
     */
    public final static int PROP_ABSOLUTE_WORD_BEAM_WIDTH_DEFAULT = 0;

    /**
     * Property that sets the minimum score relative to the maximum
     * score in the word list for pruning.  Words with a score less than
     * relativeBeamWidth * maximumScore will be pruned from the list
     */
    public final static String PROP_RELATIVE_WORD_BEAM_WIDTH =
        PROP_PREFIX + "relativeWordBeamWidth";

    /**
     * The default value for the PROP_RELATIVE_WORD_BEAM_WIDTH  property
     */
    public final static double PROP_RELATIVE_WORD_BEAM_WIDTH_DEFAULT = 0.0;

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
    private StatisticsVariable viterbiPruned;
    private StatisticsVariable beamPruned;
    private StatisticsVariable wordsPruned;

    private boolean showTokenCount;
    private Map bestTokenMap;
    private List wordList;

    private int absoluteWordBeamWidth;
    private float logRelativeWordBeamWidth;

    private int totalHmms;


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
        this.absoluteWordBeamWidth = 200;

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
	viterbiPruned =
            StatisticsVariable.getStatisticsVariable(props.getContext(),
                    "viterbiPruned");
	beamPruned =
            StatisticsVariable.getStatisticsVariable(props.getContext(),
                    "beamPruned");
	wordsPruned =
            StatisticsVariable.getStatisticsVariable(props.getContext(),
                    "wordsPruned");

        absoluteWordBeamWidth = props.getInt(PROP_ABSOLUTE_WORD_BEAM_WIDTH,
                    PROP_ABSOLUTE_WORD_BEAM_WIDTH_DEFAULT);

        double relativeWordBeamWidth = 
               props.getDouble(PROP_RELATIVE_WORD_BEAM_WIDTH,
                    PROP_RELATIVE_WORD_BEAM_WIDTH_DEFAULT);
	logRelativeWordBeamWidth = logMath.linearToLog(relativeWordBeamWidth);

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
	    currentFrameNumber++;
            growBranches(); 	        // extend remaining branches
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
        bestTokenMap = new HashMap(activeList.size() * 10);

        ActiveList oldActiveList = activeList;

        Iterator oldListIterator = activeList.iterator();
        resultList = new LinkedList();

        activeList = activeList.createNew();
        wordList = new ArrayList(oldActiveList.size() / 10);
        while (oldListIterator.hasNext()) {
            Token token = (Token) oldListIterator.next();

            if (oldActiveList.isWorthGrowing(token)) {
                collectSuccessorTokens(token);
            }
        }

        pruneWords();
        int words = wordList.size();
        growWords();
        growTimer.stop();
        int hmms  = activeList.size();

        totalHmms += hmms;

        if (false) {
            System.out.println("Frame: " + currentFrameNumber 
                     + " Hmms: " + hmms + " Words: " + words + " total " 
                     + totalHmms );
        }
        // dump out the active list

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
         int startSize = activeList.size();
         pruneTimer.start();
         if (false) {
             System.out.print("Frame: " + currentFrameNumber + " ");
         }
         activeList =  pruner.prune(activeList);
         beamPruned.value += startSize - activeList.size();
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
                Token newToken = token.child(
                        nextState,
                        logEntryScore,
                        arc.getLanguageProbability(),
                        arc.getInsertionProbability(),
                        currentFrameNumber );

                tokensCreated.value++;


                setBestToken(newToken, nextState);

                if (newToken.isWord() && wordList != null) {
                    wordList.add(newToken);
                } else if (!newToken.isEmitting()) {
                    collectSuccessorTokens(newToken);
                } else {
                    if (firstToken) {
                        activeList.add(newToken);
                    } else {
                        activeList.replace(bestToken, newToken);
                        viterbiPruned.value++;
                    }
                }
	    } else {
                viterbiPruned.value++;
            }
        }
    }

    /**
     * Prune the words collected in the wordlist based upon the
     * maxWordsPerFrame size
     */
    public void pruneWords() {
	if (absoluteWordBeamWidth > 0 && wordList.size() > 0) {

            Collections.sort(wordList, Token.COMPARATOR);

	    Token bestToken = (Token) wordList.get(0);
	    float highestScore = bestToken.getScore();
	    float pruneScore = highestScore + logRelativeWordBeamWidth;
            int count = 0;

	    for (Iterator i = wordList.iterator();
		    i.hasNext() && count < absoluteWordBeamWidth; count++) {
		Token token = (Token) i.next();
		if (token.getScore() <= pruneScore) {
		    break;
		}
	    }
            if (false) {
                System.out.println("Words: "+ wordList.size() + " to " + count);
            }
            wordsPruned.value += (wordList.size() - count);
	    wordList = wordList.subList(0, count);
	}
    }


    /**
     * Grow the remaining word tokens onto the active list
     */
    private void growWords() {
        List oldWordList = wordList;
        wordList = null;

        for (Iterator i = oldWordList.iterator(); i.hasNext(); ) {
            collectSuccessorTokens((Token) i.next());
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
