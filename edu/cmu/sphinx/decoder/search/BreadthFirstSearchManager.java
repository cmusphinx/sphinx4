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
import edu.cmu.sphinx.decoder.search.*;
import edu.cmu.sphinx.decoder.scorer.AcousticScorer;
import edu.cmu.sphinx.decoder.linguist.*;


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

public class BreadthFirstSearchManager implements  SearchManager {
    public final static String PROP_ACTIVE_LIST_TYPE =
	"edu.cmu.sphinx.decoder.search.BreadthFirstSearchManager.activeListType";
    public final static String PROP_RESULTS_VALIDATE =
	"edu.cmu.sphinx.decoder.search.validateResults";

    public final static String PROP_ENABLE_BUSHDERBY =
	"edu.cmu.sphinx.decoder.search.BreadthFirstSearchManager.enableBushderby";
    public final static String PROP_BUSHDERBY_ETA =
	"edu.cmu.sphinx.decoder.search.BreadthFirstSearchManager.bushderbyEta";
    public final static String PROP_FILTER_SUCCESSORS =
	"edu.cmu.sphinx.decoder.search.BreadthFirstSearchManager.filterSuccessors";

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
    private LanguageModel languageModel;

    private int currentFrameNumber;	// the current frame number
    private ActiveList activeList;	// the list of active tokens
    private List resultList;		// the current set of results
    private SphinxProperties props;	// the sphinx properties
    private LogMath logMath;

    private Timer scoreTimer;
    private Timer pruneTimer;
    private Timer growTimer;
    private boolean validateResults;	// if true validate results
    private boolean enableBushderby;
    private boolean filterSuccessors;
    private StatisticsVariable totalTokensScored;
    private StatisticsVariable curTokensScored;
    private StatisticsVariable tokensCreated;
    private double bushderbyEta;
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

	validateResults = props.getBoolean(PROP_RESULTS_VALIDATE, false);
	enableBushderby 
	    = props.getBoolean(PROP_ENABLE_BUSHDERBY, false);
	bushderbyEta 
	    = props.getDouble(PROP_BUSHDERBY_ETA, 1E99);

	System.out.println("bushderby is " + enableBushderby);
	System.out.println("bushderbyEta is " + bushderbyEta);
	filterSuccessors 
	    = props.getBoolean(PROP_FILTER_SUCCESSORS, true);
        double linearRelativeBeamWidth  
            = props.getDouble(ActiveList.PROP_RELATIVE_BEAM_WIDTH, 0);
        relativeBeamWidth
            = (float) logMath.linearToLog(linearRelativeBeamWidth);
	absoluteBeamWidth = props.getInt(
		ActiveList.PROP_ABSOLUTE_BEAM_WIDTH, 2000);


	showTokenCount = props.getBoolean(PROP_SHOW_TOKEN_COUNT, false);

	if (linguist != null) {
	    languageModel  = linguist.getLanguageModel();
	}
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
	    if (enableBushderby) {
		growBranchesWithBushderby();
	    } else {
		growBranches(); 	// extend remaining branches  
	    }
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
		
	while (oldListIterator.hasNext()) {
	    Token token = (Token) oldListIterator.next();

            if (oldActiveList.isWorthGrowing(token)) {
                collectSuccessorTokens(token, null);
            }
	}
	growTimer.stop();

    }

    /**
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
    private void growBranchesWithBushderby() {
	int pass = 0;
	boolean moreTokensToExpand = true;
	growTimer.start();

        ActiveList oldActiveList = activeList;

	Iterator iterator = activeList.iterator();
	resultList = new LinkedList();

	activeList = activeList.createNew();
		
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

	finalizeBushderby(activeList.iterator());

	growTimer.stop();
    }


    /**
     * Chase through the list, find all Green nodes and convert the
     * scores to the final bushderby score
     */
    private void finalizeBushderby(Iterator iterator) {

	while (iterator.hasNext()) {
	    Token token = (Token) iterator.next();
	    if (token.getSentenceHMMState().getColor() == Color.GREEN) {
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


    /**
     * Collects the next set of emitting tokens from a token 
     * and accumulates them in the active or result lists
     *
     * @param token  the token to collect successors from
     * @param delayedExpansionList the place where tokens that cannot
     * be immediately expaned are placed. Null if we should always
     * expand all nodes.
     */
    private final void collectSuccessorTokens(Token token,
	    List delayedExpansionList) {
	int nextFrameNumber = token.getFrameNumber();

	// If this is a final state, add it to the final list

	if (token.isFinal()) {
	    resultList.add(token);
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


	    boolean firstToken = nextState.getBestToken() == null ||
		nextState.getBestToken().getFrameNumber() != nextFrameNumber;
	    boolean greenToken = enableBushderby &&
		nextState.getColor() == Color.GREEN;

	    double logWorkingScore =  firstToken ? logMath.getLogZero() :
		nextState.getBestToken().getWorkingScore();

	    if (firstToken ||  nextState.isFanIn() ||
		nextState.getBestToken().getScore() <= logCurrentScore) {
		
		// we may want to create  green tokens all the time
		if (greenToken ||
                        activeList.isInsertable(logCurrentScore)) {

		    Token newToken = new Token(
			token, 			// the predecessor
			nextState, 		// the SentenceHMMState
			logCurrentScore, 		// the score on entry
			logLanguageProbability, 	// entry lang score
			arc.getInsertionProbability(), // insertion prob
			nextFrameNumber 	// the frame number
		    );
                    tokensCreated.value++;

		    newToken = collapseToken(newToken);

		    Token oldBestToken = nextState.setBestToken(newToken);

		    if (!newToken.isEmitting()) {
			if (greenToken && delayedExpansionList != null) {
			    if (oldBestToken != null && 
			             oldBestToken.getScore() <=
                                     logCurrentScore) {
			        int oldTokenIdx = 
                                  delayedExpansionList.indexOf(oldBestToken);
				if (oldTokenIdx >= 0)
				    delayedExpansionList.remove(oldTokenIdx);
		            }
			    delayedExpansionList.add(newToken);
			} else  {
			    collectSuccessorTokens(newToken, 
	                    	   delayedExpansionList);
			}
                    } else if (firstToken || nextState.isFanIn()) {
                        activeList.add(newToken);
                    } else {
			activeList.replace(oldBestToken, newToken); 
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
		Token bestToken = nextState.getBestToken();
		if (bestToken != null) {
		    logWorkingScore =  logMath.addAsLinear(
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
	    if (token.getSentenceHMMState() instanceof UnitState) {
		return ((UnitState) token.getSentenceHMMState()).getUnit();
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
    private float getLanguageProbability(Token token, SentenceHMMStateArc arc) {
	float logProbability = arc.getLanguageProbability();
	if (languageModel != null && arc.getNextState() instanceof WordState) {
	    WordState state = (WordState) arc.getNextState();
	    int depth = languageModel.getMaxDepth();
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
		logProbability = (float) languageModel.getProbability(wordList);
	    }
	}
	return logProbability * languageWeight;
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
    private final Token collapseToken(Token token) {
	// TBD: Does nothing now.
	return token;
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
