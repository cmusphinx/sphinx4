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
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import edu.cmu.sphinx.knowledge.language.LanguageModel;
import edu.cmu.sphinx.knowledge.acoustic.Unit;
import edu.cmu.sphinx.knowledge.acoustic.LeftRightContext;

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.Timer;
import edu.cmu.sphinx.util.StatisticsVariable;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.search.*;
import edu.cmu.sphinx.decoder.scorer.AcousticScorer;
import edu.cmu.sphinx.decoder.linguist.SentenceHMMState;
import edu.cmu.sphinx.decoder.linguist.SentenceHMMStateArc;
import edu.cmu.sphinx.decoder.linguist.Linguist;


/**
 * Provides the breadth first search. To perform recognition
 * an application should call initialize before
 * recognition begins, and repeatedly call <code> recognize </code>
 * until Result.isFinal() returns true.  Once a final result has been
 * obtained, <code> terminate </code> should be called. 
 *
 *
 * Note that all scores are maintained in the LogMath log domain
 */

public class FastBreadthFirstSearchManager implements  SearchManager {
    public final static String PROP_ACTIVE_LIST_TYPE =
	"edu.cmu.sphinx.decoder.search.BreadthFirstSearchManager.activeListType";


    public final static String PROP_LANGUAGE_WEIGHT  =
	"edu.cmu.sphinx.decoder.search.BreadthFirstSearchManager.languageWeight";

    /**
     * A sphinx property than, when set to <code>true</code> will
     * cause the recognizer to count up all the tokens in the active
     * list after every frame.
     */
    public final static String PROP_SHOW_TOKEN_COUNT =
	"edu.cmu.sphinx.decoder.search.BreadthFirstSearchManager.showTokenCount";

    private final static String SENTENCE_START = "<s>";

    private Linguist linguist;		// Provides grammar/language info
    private Pruner pruner;		// used to prune the active list
    private AcousticScorer scorer;	// used to score the active list

    private int currentFrameNumber;	// current frame number
    private ActiveList activeList;	// list of active tokens
    private ActiveList nonEmittingList;	// list of non-emitting active tokens
    private List resultList;		// current set of results
    private SphinxProperties props;	// sphinx properties
    private LogMath logMath;

    private Timer scoreTimer;
    private Timer pruneTimer;
    private Timer growTimer;
    private boolean validateResults;	// if true validate results
    private StatisticsVariable totalTokensScored;
    private StatisticsVariable curTokensScored;
    private StatisticsVariable tokensCreated;
    private float languageWeight;
    private float relativeBeamWidth;
    private int absoluteBeamWidth;
    private boolean showTokenCount;
    private boolean debug = false ; // FIXME


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

        languageWeight = props.getFloat(PROP_LANGUAGE_WEIGHT, 1.0f);

        double linearRelativeBeamWidth  
            = props.getDouble(ActiveList.PROP_RELATIVE_BEAM_WIDTH, 0);
        relativeBeamWidth
            = (float) logMath.linearToLog(linearRelativeBeamWidth);
	absoluteBeamWidth = props.getInt(
		ActiveList.PROP_ABSOLUTE_BEAM_WIDTH, 2000);


	showTokenCount = props.getBoolean(PROP_SHOW_TOKEN_COUNT, false);

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
	if (showTokenCount) {
	    showTokenCount();
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
	boolean more = scoreTokens(); // score emitting tokens
        if (more) {	
	    pruneBranches(); 		// eliminate poor branches
            growBranches(); 	// extend remaining branches  
	    currentFrameNumber++;
	} 

        return !more;
    }

    private void M(String s) {
        if (false) {
            System.out.println(s);
        }
    }
   
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

	    newActiveList.add(new Token(state, currentFrameNumber));
	    activeList = newActiveList;
	    nonEmittingList = newActiveList.createNew();
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
     * Goes through the active list of tokens and expands each
     * token, finding the set of successor tokens until all the successor
     * tokens are emitting tokens.
     *
     */
    private void growBranches() {

	growTimer.start();

        ActiveList oldActiveList = activeList;

	Iterator oldListIterator = activeList.iterator();
	resultList = new LinkedList();

	activeList = activeList.createNew();
        nonEmittingList = nonEmittingList.createNew();
		
	while (oldListIterator.hasNext()) {
	    Token token = (Token) oldListIterator.next();

            if (oldActiveList.isWorthGrowing(token)) {
                collectSuccessorTokens(token);
            }
	}

        while (nonEmittingList.size() > 0) {
            ActiveList oldNonEmittingList = nonEmittingList;
            nonEmittingList = nonEmittingList.createNew();
            Iterator oldNonEmittingListIterator = oldNonEmittingList.iterator();
            while (oldNonEmittingListIterator.hasNext()) {
                Token token = (Token) oldNonEmittingListIterator.next();
                collectSuccessorTokens(token);
            }
        }
	growTimer.stop();

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
    private void pruneBranches() {
	 pruneTimer.start();
         activeList =  pruner.prune(activeList); 
	 pruneTimer.stop();
    }

    static int tieCount = 0;

    /**
     * Collects the next set of emitting tokens from a token 
     * and accumulates them in the active or result lists
     *
     * @param token  the token to collect successors from
     * @param delayedExpansionList the place where tokens that cannot
     * be immediately expaned are placed. Null if we should always
     * expand all nodes.
     */
    private final void collectSuccessorTokens(Token token) {
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
                ActiveList currentList;


                if (newToken.isEmitting()) {
                    currentList = activeList;
                } else {
                    currentList = nonEmittingList;
                }

                if (firstToken) {
                    currentList.add(newToken);
                } else {
                    currentList.replace(oldBestToken, newToken); 
                }
	    } 
        }
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
     * @return the language probability in the LogMath log domain
     */
    private float getLanguageProbability(Token token, SentenceHMMStateArc arc) {
	float logProbability = arc.getLanguageProbability();
	return logProbability * languageWeight;
    }


    /**
     * Debugging tool
     *
     * @param msg the message
     */
    private void T(String msg) {
	if (false)  {
	    System.out.println(msg);
	}
    }
}
