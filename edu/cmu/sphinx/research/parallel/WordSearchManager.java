
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

package edu.cmu.sphinx.research.parallel;

import edu.cmu.sphinx.knowledge.acoustic.AcousticModel;

import edu.cmu.sphinx.result.Result;

import edu.cmu.sphinx.decoder.scorer.AcousticScorer;
import edu.cmu.sphinx.decoder.search.ActiveList;
import edu.cmu.sphinx.decoder.search.TokenStack;

import edu.cmu.sphinx.decoder.linguist.Color;
import edu.cmu.sphinx.decoder.linguist.Linguist;
import edu.cmu.sphinx.decoder.search.Pruner;
import edu.cmu.sphinx.decoder.search.SearchManager;
import edu.cmu.sphinx.decoder.linguist.simple.SentenceHMMState;
import edu.cmu.sphinx.decoder.linguist.simple.SentenceHMMStateArc;
import edu.cmu.sphinx.decoder.linguist.SearchState;
import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.decoder.linguist.simple.WordState;

import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Timer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;


/**
 * Performs recognition on parallel feature streams.
 */
public class WordSearchManager implements SearchManager {

    // The sphinx property prefix for all property names of this class.
    private static final String PROP_PREFIX =
	"edu.cmu.sphinx.research.parallel.WordSearchManager.";

    // The sphinx property name for the active list type.
    private static final String PROP_ACTIVE_LIST_TYPE =
	PROP_PREFIX + "activeListType";

    // The default ActiveList type, which is the SimpleActiveList
    private static final String PROP_ACTIVE_LIST_TYPE_DEFAULT =
	"edu.cmu.sphinx.decoder.search.SimpleActiveList";

    // The sphinx property name for the language weight.
    private static final String PROP_LANGUAGE_WEIGHT =
        PROP_PREFIX + "languageWeight";

    // The default language weight value, which is 1.0.
    private static final float PROP_LANGUAGE_WEIGHT_DEFAULT = 1.0f;

    // The sphinx property name for whether to do feature pruning.
    private static final String PROP_DO_FEATURE_PRUNING =
	PROP_PREFIX + "doFeaturePruning";

    // The default value for whether to do feature pruning, which is false.
    private static final boolean PROP_DO_FEATURE_PRUNING_DEFAULT = false;

    // The sphinx property name for whether to do combine pruning.
    private static final String PROP_DO_COMBINE_PRUNING =
	PROP_PREFIX + "doCombinePruning";

    // The default value for whether to do combine pruning, which is false.
    private static final boolean PROP_DO_COMBINE_PRUNING_DEFAULT = false;

    // The sphinx property name for whether to enforce the same combine time.
    private static final String PROP_SAME_COMBINE_TIME = 
        PROP_PREFIX + "sameCombineTime";

    private static final boolean PROP_SAME_COMBINE_TIME_DEFAULT = false;

    private static final String PROP_TIME_DIFFERENCE =
        PROP_PREFIX + "timeDifference";

    private static final int PROP_TIME_DIFFERENCE_DEFAULT = 0;


    private SphinxProperties props;
    private Linguist linguist;
    private AcousticScorer scorer;
    private Pruner featureScorePruner;
    private Pruner combinedScorePruner;
    private ScoreCombiner scoreCombiner;

    private int currentFrameNumber;           // the current frame number
    private ActiveList combinedActiveList;    // ActiveList for common states
    private Map featureStreams;               // the list of ActiveList(s), one
                                              // for each type of model

    private ActiveList delayedExpansionList;  // for tokens at CombineStates
    private List resultList;

    private Timer scoreTimer;
    private Timer pruneTimer;
    private Timer growTimer;

    private LogMath logMath;
    private float languageWeight;
    private boolean doFeaturePruning;
    private boolean doCombinePruning;
    private boolean sameCombineTime;

    private boolean debugPrint;


    /**
     * Initializes this WordSearchManager with the given context,
     * linguist, scorer, and pruner. Note that the given pruner is unused
     * in this WordSearchManager, since we use the FeatureScorePruner
     * and the CombinedScorePruner.
     *
     * @param context the context to use
     * @param linguist the Linguist to use
     * @param scorer the AcousticScorer to use
     * @param pruner the Pruner to use
     */
    public void initialize(String context, Linguist linguist,
			   AcousticScorer scorer, Pruner pruner) {
	this.props = SphinxProperties.getSphinxProperties(context);
	this.linguist = linguist;
	this.scorer = scorer;

	this.doFeaturePruning = props.getBoolean
	    (PROP_DO_FEATURE_PRUNING, PROP_DO_FEATURE_PRUNING_DEFAULT);

	this.doCombinePruning = props.getBoolean
	    (PROP_DO_COMBINE_PRUNING, PROP_DO_COMBINE_PRUNING_DEFAULT);

	if (doFeaturePruning) {
	    this.featureScorePruner = new FeatureScorePruner();
	    this.featureScorePruner.initialize(context);
	}
	if (doCombinePruning) {
	    this.combinedScorePruner = new CombinedScorePruner();
	    this.combinedScorePruner.initialize(context);
	}

	this.debugPrint = props.getBoolean(PROP_PREFIX + "debugPrint", false);

        this.sameCombineTime = props.getBoolean
            (PROP_SAME_COMBINE_TIME, PROP_SAME_COMBINE_TIME_DEFAULT);

        if (sameCombineTime) {
            int timeDifference = props.getInt
                (PROP_TIME_DIFFERENCE, PROP_TIME_DIFFERENCE_DEFAULT);
            scoreCombiner = new SameTimeScoreCombiner(timeDifference);
        } else {
            scoreCombiner = new FeatureScoreCombiner();
        }

	this.scoreTimer = Timer.getTimer(context, "Score");
	this.pruneTimer = Timer.getTimer(context, "Prune");
	this.growTimer = Timer.getTimer(context, "Grow");

        this.logMath = LogMath.getLogMath(context);
        this.languageWeight = props.getFloat(PROP_LANGUAGE_WEIGHT,
                                             PROP_LANGUAGE_WEIGHT_DEFAULT);

	featureStreams = new HashMap();
        
        // initialize the FeatureStreams for the separate models
        List models = AcousticModel.getNames(context);
        assert (models.size() > 0);

	float defaultEta = 1.f/models.size();

        for (Iterator i = models.iterator(); i.hasNext();) {
            String modelName = (String) i.next();
            float eta = props.getFloat(PROP_PREFIX + modelName + ".eta",
				       defaultEta);
            FeatureStream stream = new FeatureStream(modelName, eta);
            featureStreams.put(modelName, stream);
            System.out.println("Eta for " + modelName + " is: " + eta);
        }
    }


    /**
     * Prints a debug message.
     *
     * @param message the debug message to print
     */
    private void debugPrint(String message) {
	if (debugPrint) {
	    System.out.println(message);
	}
    }


    /**
     * Prepares the SearchManager for recognition.  This method must
     * be called before <code> recognize </code> is called.
     */
    public void start() {
	currentFrameNumber = 0;
	linguist.start();
        if (doFeaturePruning) {
            featureScorePruner.start();
	}
	if (doCombinePruning) {
            combinedScorePruner.start();
        }
	scorer.start();
	createInitialLists();
    }


    /**
     * Creates the ActiveLists used for decoding. There is one ActiveList
     * created for each feature stream (or acoustic model), and also an
     * ActiveList to do the overall pruning.
     */
    private void createInitialLists() {

	String activeListName =
	    props.getString(PROP_ACTIVE_LIST_TYPE, 
                            PROP_ACTIVE_LIST_TYPE_DEFAULT);
	    
	try {
            // initialize the combined ActiveList
	    Class activeListClass = Class.forName(activeListName);
	    
	    // initialize the ActiveLists for the separate models
            for (Iterator i = featureStreams.values().iterator(); 
                 i.hasNext();) {
                FeatureStream stream = (FeatureStream) i.next();
                stream.activeList = 
                    (ActiveList) activeListClass.newInstance();
                stream.activeList.setProperties(props);
            }

	    combinedActiveList = (ActiveList)activeListClass.newInstance();
	    combinedActiveList.setProperties(props);

	    delayedExpansionList = (ActiveList)activeListClass.newInstance();
	    delayedExpansionList.setProperties(props);

	    SentenceHMMState firstState = 
                (SentenceHMMState) linguist.getInitialSearchState();
	    assert !firstState.isEmitting();

            // create the first token and grow it, its first parameter
            // is null because it has no predecessor
            CombineToken firstToken = new CombineToken
                (null, firstState, currentFrameNumber);

            // add the first ParallelTokens to the CombineToken
            for (Iterator i = featureStreams.values().iterator();
                 i.hasNext();) {
                FeatureStream stream = (FeatureStream) i.next();
                ParallelToken token = new ParallelToken
                    (stream.name, firstState, stream.eta, currentFrameNumber);
		token.setLastCombineTime(currentFrameNumber);
                firstToken.addParallelToken(stream.name, token);
                debugPrint("Adding first token for " + stream.name);
            }

            // grow the first CombineToken until we've reach emitting states
            resultList = new LinkedList();

	    calculateCombinedScore(firstToken);

            debugPrint(firstToken.toString());

            growCombineToken(firstToken);

            printActiveListsSizes();
            
	} catch (ClassNotFoundException cnfe) {
	    throw new Error("ActiveList class, " + activeListName +
			    "not found");
	} catch (IllegalAccessException iae) {
	    throw new Error("Cannot access " + activeListName);
	} catch (InstantiationException ise) {
	    throw new Error("Cannot instantiate " + activeListName);
	}
    }

    private void printActiveListsSizes() {
        debugPrint("CombinedActiveList: " + combinedActiveList.size());
        // add the first ParallelTokens to the CombineToken
        for (Iterator i = featureStreams.values().iterator();
             i.hasNext();) {
            FeatureStream stream = (FeatureStream) i.next();
            debugPrint("ActiveList " + stream.name + ": " + 
                       stream.activeList.size());
        }
    }

    /**
     * Performs recognition. Processes no more than the given number
     * of frames before returning. This method returns a partial
     * result after nFrames have been processed, or a final result if
     * recognition completes while processing frames.  If a final
     * result is returned, the actual number of frames processed can
     * be retrieved from the result.  This method may block while
     * waiting for frames to arrive.
     *
     * @param nFrames the maximum number of frames to process. A
     * final result may be returned before all nFrames are processed.
     *
     * @return the recognition result. The result may be a partial or
     * a final result.
     */
    public Result recognize(int nFrames) {
	boolean done = false;
	Result result;

        for (int i = 0; i < nFrames && !done; i++) {
	    done = recognize();
	}
	result = new Result
	    (combinedActiveList, resultList, currentFrameNumber, done);

	return result;
    }


    /**
     * Performs recognition for one frame. Returns true if recognition
     * has been completed.
     *
     * @return <code>true</code> if recognition is completed.
     */
    private boolean recognize() {
        debugPrint("-----");
	boolean moreTokens = score();
        if (moreTokens) {
	    prune();
	    ParallelHMMStateState.clearAllStates();            
	    grow();
	    currentFrameNumber++;
	}
        debugPrint("-----");
	return !moreTokens;
    }


    /**
     * Calculate the acoustic scores for the active lists for the
     * acoustic models, which should contain only emitting tokens.
     *
     * @return true if there are more tokens, otherwise false
     */
    private boolean score() {
	scoreTimer.start();
	debugPrint("Scoring");
	boolean moreFeatures = false;
	for (Iterator i = featureStreams.values().iterator(); i.hasNext();) {
            FeatureStream stream = (FeatureStream) i.next();
	    moreFeatures =
                scorer.calculateScores(stream.activeList.getTokens());
	}
	debugPrint("done Scoring");
	scoreTimer.stop();
	return moreFeatures;
    }


    /**
     * Removes unpromising branches from the active list
     *
     */
    private void prune() {
	pruneTimer.start();

        debugPrint("Pruning");

	if (doFeaturePruning) {
	    for (Iterator i = featureStreams.values().iterator(); 
                 i.hasNext();) {
		FeatureStream stream = (FeatureStream) i.next();	
		debugPrint(" ActiveList, " + stream.name + ": " +
                           stream.activeList.size());
		stream.activeList = 
		    featureScorePruner.prune(stream.activeList);
		debugPrint(" ActiveList, " + stream.name + ": " + 
                           stream.activeList.size());
	    }
	}

	debugPrint("done Pruning");
        pruneTimer.stop();
    }


    /**
     * Goes through the active list of tokens and expands each
     * token, finding the set of successor tokens until all the successor
     * tokens are emitting tokens.
     *
     */
    private void grow() {
	growTimer.start();

	debugPrint("Growing");

        resultList = new LinkedList();

        // renew the combinedActiveList
        combinedActiveList = combinedActiveList.createNew();

	// clear the delayedExpansionList
	delayedExpansionList = delayedExpansionList.createNew();

        // grow each ActiveList (we have one ActiveList for each stream)
	for (Iterator i = featureStreams.values().iterator(); i.hasNext();) {
            FeatureStream stream = (FeatureStream) i.next();

            // create a new ActiveList for the next frame
            ActiveList oldActiveList = stream.activeList;
            stream.activeList = oldActiveList.createNew();

            growActiveList(oldActiveList);

	    debugPrint(" ActiveList," + stream.name + ": " + 
		       stream.activeList.size());
	}

        // now expand the delayedExpansionList, which contains the
        // CombineTokens created when transitioning from GREEN states
        // to RED states (i.e., feature stream states to shared states)

	growDelayedExpansionList();
    }

    /**
     * Grow the delayedExpansionList by first pruning it, and then
     * grow it.
     */
    private void growDelayedExpansionList() {

	Iterator iterator = delayedExpansionList.iterator();

        while (iterator.hasNext()) {
            CombineToken token = (CombineToken) iterator.next();
            calculateCombinedScore(token);
            if (sameCombineTime) {
                token.setLastCombineTime(currentFrameNumber);
            }
        }

	if (doCombinePruning) {
	    delayedExpansionList =
		combinedScorePruner.prune(delayedExpansionList);
	}

	iterator = delayedExpansionList.iterator();
	
	while (iterator.hasNext()) {
	    CombineToken token = (CombineToken) iterator.next();
	    if (!token.isPruned()) {
		growCombineToken(token);
	    }
	}

	debugPrint("done Growing");

	growTimer.stop();
    }


    /**
     * Calculates the combined score of all the ParallelTokens
     * in the given CombineToken.
     *
     * @param token the CombineToken to calculate the combined score of
     */
    private void calculateCombinedScore(CombineToken token) {
        scoreCombiner.combineScore(token);
    }


    /**
     * Grows the Tokens in the given ActiveList. Returns a new ActiveList
     * of successors.
     *
     * @param activeList the ActiveList to grow
     */
    private void growActiveList(ActiveList activeList) {
	
	Iterator iterator = activeList.iterator();
	
	while (iterator.hasNext()) {
	    ParallelToken token = (ParallelToken) iterator.next();
            if (!token.isPruned()) {
                growParallelToken(token);
            }
	}
    }


    /**
     * Grows the given ParallelToken, put the successor Token(s) into the 
     * given ActiveList, and any possible results in the given resultList.
     *
     * @param token the Token to grow
     */
    private void growParallelToken(ParallelToken token) {

	// If this is a final state, add it to the result list.
	assert !token.isFinal();

	int nextFrameNumber = token.getFrameNumber();

	// If this is an emitting state, we increase the frame number
	if (token.isEmitting()) {
	    nextFrameNumber++;
	}

	SentenceHMMState state = (SentenceHMMState) token.getSearchState();
	SentenceHMMStateArc[] arcs = (SentenceHMMStateArc[]) 
            state.getSuccessors();

	// expand into each successor states
	for (int i = 0; i < arcs.length; i++) {

	    SentenceHMMStateArc arc = arcs[i];
	    SentenceHMMState nextState = arc.getNextState();
	    
	    float currentScore = token.getScore() +
                arc.getProbability();
                
	    boolean firstToken = getBestToken(nextState) == null ||
		getBestToken(nextState).getFrameNumber() != nextFrameNumber;

            // RED states are the unsplitted states, or the non-feature
            // stream states
            // GREEN states are the splitted states, or the feature stream
            // states
            
            if (nextState.getColor() == Color.RED) {

                if (firstToken) {
                    // if this is the first incoming ParallelToken, 
                    // create a CombineToken and set it as the best
                    // (and only) CombineToken of the state

                    CombineToken newToken = new CombineToken
                        (token, nextState, nextFrameNumber);
                    setBestToken(nextState, newToken);
                    delayedExpansionList.add(newToken);
                }

                assert (getBestToken(nextState).getFrameNumber() ==
                        nextFrameNumber);

                // get the combine token at the next state
                CombineToken nextToken = 
                    (CombineToken) getBestToken(nextState);

                ParallelToken oldToken = nextToken.getParallelToken
                    (token.getModelName());

                if (oldToken == null || 
                    oldToken.getFeatureScore() <= currentScore) {
                    
                    ParallelToken newToken = new ParallelToken
                        (token,
                         nextState,
                         token.getEta(),
                         currentScore,
                         token.getCombinedScore(),
                         nextFrameNumber,
                         token.getLastCombineTime());
                    
                    if (sameCombineTime) {
                        nextToken.addParallelToken(newToken, newToken);
                    } else {
                        // add this ParallelToken to the CombineToken.
                        nextToken.addParallelToken(newToken.getModelName(), 
                                                   newToken);
                    }
                }
            } else if (nextState.getColor() == Color.GREEN) {

                TokenStack tokenStack = 
                    ((ParallelState)nextState).getTokenStack();

                // if the score is high enough to enter the token stack
                if (tokenStack.isInsertable(currentScore, nextFrameNumber)) {
                    debugPrint("insertable");
                    ParallelToken newToken = new ParallelToken
                        (token,
                         nextState,
                         token.getEta(), 
                         currentScore,
                         token.getCombinedScore(), 
                         nextFrameNumber,
                         token.getLastCombineTime());
                    
                    ParallelToken oldToken = 
			(ParallelToken) tokenStack.add(newToken);
		    
		    if (nextState.isEmitting()) {
			// replace in the ActiveList for the stream
			replaceParallelToken(oldToken, newToken);
			combinedActiveList.add(newToken);
		    } else {
                        growParallelToken(newToken);
                    }
                } else {
		    debugPrint("not insertable");
		}
	    } else {
		throw new IllegalStateException
		    ("Color of state " + nextState.getName() +
		     " not RED or GREEN, its " + 
		     nextState.getColor().toString() + "!");
	    }
	}
    }


    /**
     * Grows the given CombineToken and puts any results into the given
     * list.
     *
     * @param token the CombineToken to grow
     */
    private void growCombineToken(CombineToken token) {
        
	// If this is a final state, add it to the result list.
	if (token.isFinal()) {
	    resultList.add(token);
	}

        int nextFrameNumber = token.getFrameNumber();

	// make sure that this state is non-emitting
	assert !token.isEmitting();

	SentenceHMMState state = (SentenceHMMState) token.getSearchState();
	SentenceHMMStateArc[] arcs = 
            (SentenceHMMStateArc[]) state.getSuccessors();

	// expand into each successor states
	for (int a = 0; a < arcs.length; a++) {

	    SentenceHMMStateArc arc = arcs[a];
	    SentenceHMMState nextState = arc.getNextState();

	    float transitionScore = arc.getProbability();

            // RED states are the unsplitted states, or the non-feature
            // stream states
            // GREEN states are the splitted states, or the feature stream
            // states

            if (nextState.getColor() == Color.RED) {
		
		boolean firstToken = getBestToken(nextState) == null ||
		    getBestToken(nextState).getFrameNumber() != 
		    nextFrameNumber;
                
                float currentScore = transitionScore + token.getScore();

                if (firstToken || 
                    getBestToken(nextState).getScore() <= currentScore) {

                    // create the next CombineToken for a RED state
                    CombineToken nextToken = new CombineToken
                        (token, nextState, nextFrameNumber);

                    // propagate the combined score unchanged
                    nextToken.setScore(currentScore);

                    // propagate the individual ParallelTokens, taking
                    // into account the new transition score
                    transitionParallelTokens
                        (token, nextToken, transitionScore);
                    
                    setBestToken(nextState, nextToken);

                    // finally grow the new CombineToken
                    growCombineToken(nextToken);
                }
            } else if (nextState.getColor() == Color.GREEN) {

                debugPrint("Entering GREEN state");

                ParallelState pState = (ParallelState) nextState;
                assert pState.getModelName() != null;

                ParallelToken parallelToken = token.getParallelToken
                    (pState.getModelName());

                // we continue into a GREEN/feature stream state only
                // if there is a ParallelToken for that feature stream
                
                if (parallelToken != null) {
		    
		    float currentScore = transitionScore + 
			parallelToken.getFeatureScore();
		    
                    TokenStack tokenStack = 
                        ((ParallelState)nextState).getTokenStack();

                    // if the score is high enough to enter the token stack
                    if (tokenStack.isInsertable
			(currentScore, nextFrameNumber)) {

                        debugPrint("Score " + currentScore + " is insertable");
		    
			ParallelToken newToken = new ParallelToken
			    (parallelToken, 
			     nextState,
			     parallelToken.getEta(),
			     currentScore,
			     parallelToken.getCombinedScore(),
			     nextFrameNumber,
			     parallelToken.getLastCombineTime());
        
                        ParallelToken oldToken = 
                            (ParallelToken) tokenStack.add(newToken);
                        /*
                        if (oldToken != null) {
                            Util.printTokenReplace
                                (oldToken.getScore(), 
                                 oldToken.getFrameNumber(),
                                 newToken.getScore(),
                                 newToken.getFrameNumber());
                        }
                        */
			if (nextState.isEmitting()) {
			    replaceParallelToken(oldToken, newToken);
			    combinedActiveList.add(newToken);
                        } else {
			    growParallelToken(newToken);
			}
		    }
		}
	    } else {
		throw new IllegalStateException
		    ("Color of state not RED or GREEN!");
	    }
        }
    }


    /**
     * Propagates all the ParallelToken(s) within the given old CombineToken
     * to the given new CombineToken. The new ParallelToken(s) in the
     * new CombineToken will have the transition score incorporated.
     *
     * @param oldToken the previous CombineToken
     * @param newToken the new CombineToken
     * @param transitionScore the transition score from oldToken to newToken
     */
    private void transitionParallelTokens(CombineToken oldToken,
                                          CombineToken newToken,
                                          float transitionScore) {
        // propagate the individual ParallelTokens, taking
        // into account the new transition scores
        for (Iterator i = oldToken.getTokenIterator(); i.hasNext();) {
            
            ParallelToken pToken = (ParallelToken) i.next();
            ParallelToken newParallelToken = new ParallelToken
                (pToken,
                 (SentenceHMMState) newToken.getSearchState(),
                 pToken.getEta(),
                 pToken.getFeatureScore() + transitionScore,
                 pToken.getCombinedScore(),
                 pToken.getFrameNumber(),
		 pToken.getLastCombineTime());
            
            newToken.addParallelToken(newParallelToken.getModelName(), 
                                      newParallelToken);
        }
    }


    /**
     * Replace the old token with the new token in the ActiveList 
     * of the corresponding feature stream.
     *
     * @param oldToken the old token to replace
     * @param newToken the new Token
     */
    private void replaceParallelToken(ParallelToken oldToken,
				      ParallelToken newToken) {
        if (oldToken != null) {
            assert (oldToken.getModelName() == newToken.getModelName());
        }

        ActiveList activeList = getActiveList(newToken.getModelName());
        assert activeList != null;
        activeList.replace(oldToken, newToken);
        
        if (oldToken != null) {
            oldToken.setPruned(true);
        }
    }

    /**
     * Replaces the token in the given SentenceHMMState with 
     * the given newToken, and replace the token in the ActiveList
     * of the feature stream as well.
     *
     * @param state the SentenceHMMState to replace token
     * @param newToken the new Token
     */
    private void replaceParallelToken(SentenceHMMState state,
                                      ParallelToken newToken) {
        ParallelToken oldToken = (ParallelToken) getBestToken(state);
        setBestToken(state, newToken);
	replaceParallelToken(oldToken, newToken);
    }


    /**
     * Returns the appropriate ActiveList for the given state.
     *
     * @param state the SentenceHMMState
     */
    private ActiveList getActiveList(String modelName) {
        FeatureStream stream = (FeatureStream) featureStreams.get(modelName);
        return stream.activeList;
    }



    // BUG fix me
    Token getBestToken(SearchState state) {
        return null;    
    }

    // BUG fix me
    void setBestToken(SearchState state, Token token) {
    }

    /**
     * Performs post-recognition cleanup. This method should be called
     * after recognize returns a final result.
     */
    public void stop() {
	scorer.stop();
        if (doFeaturePruning) {
            featureScorePruner.stop();
	}
	if (doCombinePruning) {
            combinedScorePruner.stop();
        }
	linguist.stop();
    }
}

