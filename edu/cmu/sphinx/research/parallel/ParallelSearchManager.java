
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
import edu.cmu.sphinx.decoder.linguist.Color;
import edu.cmu.sphinx.decoder.linguist.Linguist;
import edu.cmu.sphinx.decoder.linguist.SearchState;
import edu.cmu.sphinx.decoder.linguist.SearchStateArc;
import edu.cmu.sphinx.decoder.search.Pruner;
import edu.cmu.sphinx.decoder.search.SearchManager;
import edu.cmu.sphinx.decoder.linguist.simple.SentenceHMMState;
import edu.cmu.sphinx.decoder.linguist.simple.SentenceHMMStateArc;
import edu.cmu.sphinx.decoder.search.Token;

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
public class ParallelSearchManager implements SearchManager {

    /**
     * The sphinx property prefix for all property names of this class.
     */
    public static final String PROP_PREFIX =
	"edu.cmu.sphinx.research.parallel.ParallelSearchManager.";

    /**
     * The sphinx property name for the active list type.
     */
    public static final String PROP_ACTIVE_LIST_TYPE =
	PROP_PREFIX + "activeListType";

    /**
     * The default ActiveList type, which is the SimpleActiveList
     */
    public static final String PROP_ACTIVE_LIST_TYPE_DEFAULT =
        "edu.cmu.sphinx.decoder.search.SimpleActiveList";
    
    /**
     * The sphinx property name for whether to do feature pruning.
     */
    public static final String PROP_DO_FEATURE_PRUNING =
	PROP_PREFIX + "doFeaturePruning";

    /**
     * The default value for whether to do feature pruning, which is false.
     */
    public static final boolean PROP_DO_FEATURE_PRUNING_DEFAULT = false;

    /**
     * The sphinx property name for whether to do combine pruning.
     */
    public static final String PROP_DO_COMBINE_PRUNING =
	PROP_PREFIX + "doCombinePruning";

    /**
     * The default value for whether to do combine pruning, which is false.
     */
    public static final boolean PROP_DO_COMBINE_PRUNING_DEFAULT = false;


    private SphinxProperties props;
    private Linguist linguist;
    private AcousticScorer scorer;
    private Pruner featureScorePruner;
    private Pruner combinedScorePruner;
    private ScoreCombiner featureScoreCombiner;

    private int currentFrameNumber;           // the current frame number
    private ActiveList combinedActiveList;    // ActiveList for common states
    private ActiveList delayedExpansionList;  // for tokens at CombineStates
    private List resultList;

    private Map bestTokenMap;

    private Timer scoreTimer;
    private Timer pruneTimer;
    private Timer growTimer;

    private boolean doFeaturePruning;
    private boolean doCombinePruning;


    /**
     * Initializes this ParallelSearchManager with the given context,
     * linguist, scorer, and pruner. Note that the given pruner is unused
     * in this ParallelSearchManager, since we use the FeatureScorePruner
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
	
	this.featureScoreCombiner = new FeatureScoreCombiner();

	this.scoreTimer = Timer.getTimer(context, "Score");
	this.pruneTimer = Timer.getTimer(context, "Prune");
	this.growTimer = Timer.getTimer(context, "Grow");

        bestTokenMap = new HashMap();
	        
        // initialize the FeatureStreams for the separate models
        List models = AcousticModel.getNames(context);
        assert (models.size() > 0);

	float defaultEta = 1.f/models.size();

        for (Iterator i = models.iterator(); i.hasNext();) {
            String modelName = (String) i.next();
            float eta = props.getFloat(PROP_PREFIX + modelName + ".eta",
				       defaultEta);
            FeatureStream stream = 
                FeatureStream.getFeatureStream(modelName);
            stream.setEta(eta);
            System.out.println("Eta for " + modelName + " is: " + eta);
        }
    }


    /**
     * Prints a debug message.
     *
     * @param message the debug message to print
     */
    private void debugPrint(String message) {
	if (true) {
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
	    
	    combinedActiveList = getActiveList(activeListClass, props);
            delayedExpansionList = getActiveList(activeListClass, props);

	    SentenceHMMState firstState = (SentenceHMMState)
                linguist.getInitialSearchState();

            // create the first token and grow it, its first parameter
            // is null because it has no predecessor
            CombineToken firstToken = new CombineToken
                (null, firstState, currentFrameNumber);

            setBestToken(firstState, firstToken);

            for (Iterator i = FeatureStream.iterator(); i.hasNext();) {
                FeatureStream stream = (FeatureStream) i.next();
                stream.setActiveList(getActiveList(activeListClass, props));

                // add the first ParallelTokens to the CombineToken
                ParallelToken token = new ParallelToken
                    (firstState, stream, currentFrameNumber);
		token.setLastCombineTime(currentFrameNumber);
                firstToken.addParallelToken(stream, token);
            }

            // grow the first CombineToken until we've reach emitting states
            resultList = new LinkedList();

	    calculateCombinedScore(firstToken);
            growCombineToken(firstToken);
            printActiveLists();
            
	} catch (ClassNotFoundException cnfe) {
	    throw new Error("ActiveList class, " + activeListName +
			    "not found");
	} catch (IllegalAccessException iae) {
	    throw new Error("Cannot access " + activeListName);
	} catch (InstantiationException ise) {
	    throw new Error("Cannot instantiate " + activeListName);
	}
    }

    /**
     * Returns an ActiveList of the given class with the given properties.
     *
     * @param activeListClass the ActiveList class
     * @param props the properties of the Active List
     *
     * @return a new ActiveList
     */
    private ActiveList getActiveList(Class activeListClass, 
                                     SphinxProperties props) 
        throws InstantiationException, IllegalAccessException {
        ActiveList activeList = (ActiveList) activeListClass.newInstance();
        activeList.setProperties(props);
        return activeList;
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
        debugPrint("Frame: " + currentFrameNumber);
	boolean moreTokens = score();
        if (moreTokens) {
	    prune();
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
	for (Iterator i = FeatureStream.iterator(); i.hasNext();) {
            FeatureStream stream = (FeatureStream) i.next();
	    moreFeatures =
                scorer.calculateScores(stream.getActiveList().getTokens());
	}
	debugPrint(" done Scoring");
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
	    for (Iterator i = FeatureStream.iterator(); i.hasNext();) {
		FeatureStream stream = (FeatureStream) i.next();	
		debugPrint(" ActiveList, " + stream.getName() + ": " +
                           stream.getActiveList().size());
		stream.setActiveList
                    (featureScorePruner.prune(stream.getActiveList()));
		debugPrint(" ActiveList, " + stream.getName() + ": " + 
                           stream.getActiveList().size());
	    }
	}

	debugPrint(" done Pruning");
        pruneTimer.stop();
    }


    /**
     * Prints all the active lists.
     */
    private void printActiveLists() {
        debugPrint(" CombinedActiveList: " + combinedActiveList.size());
        for (Iterator i = FeatureStream.iterator(); i.hasNext();) {
            FeatureStream stream = (FeatureStream) i.next();	
            debugPrint(" ActiveList, " + stream.getName() + ": " +
                       stream.getActiveList().size());
        }
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
        printActiveLists();

        resultList = new LinkedList();
        combinedActiveList = combinedActiveList.createNew();
	delayedExpansionList = delayedExpansionList.createNew();

        // grow each ActiveList (we have one ActiveList for each stream)
	for (Iterator i = FeatureStream.iterator(); i.hasNext();) {
            FeatureStream stream = (FeatureStream) i.next();

            // create a new ActiveList for the next frame
            ActiveList oldActiveList = stream.getActiveList();
            stream.setActiveList(oldActiveList.createNew());
            
	    growActiveList(oldActiveList);
	}

        // now expand the delayedExpansionList, which contains the
        // CombineTokens created when transitioning from GREEN states
        // to RED states (i.e., feature stream states to shared states)

	growDelayedExpansionList();

	debugPrint(" done Growing");

	growTimer.stop();

        printActiveLists();
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
        }

	if (doCombinePruning) {
	    delayedExpansionList =
		combinedScorePruner.prune(delayedExpansionList);
	}

	iterator = delayedExpansionList.iterator();
	
	while (iterator.hasNext()) {
	    CombineToken token = (CombineToken) iterator.next();
	    if (!token.isPruned()) {
		token.setLastCombineTime(currentFrameNumber);
		growCombineToken(token);
	    }
	}
    }


    /**
     * Calculates the combined score of all the ParallelTokens
     * in the given CombineToken.
     *
     * @param token the CombineToken to calculate the combined score of
     */
    private void calculateCombinedScore(CombineToken token) {
	featureScoreCombiner.combineScore(token);
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
	SearchStateArc[] arcs = state.getSuccessors();

	// expand into each successor states
	for (int i = 0; i < arcs.length; i++) {

	    SearchStateArc arc = arcs[i];
	    SentenceHMMState nextState = (SentenceHMMState) arc.getState();
	    
	    float logEntryScore = token.getScore() + arc.getProbability();
            Token oldNextToken = getBestToken(nextState);

	    boolean firstToken = oldNextToken == null ||
                oldNextToken.getFrameNumber() != nextFrameNumber;

            // RED states are the unsplitted states, or the non-feature
            // stream states
            // GREEN states are the splitted states, or the feature stream
            // states
            
            if (nextState.getColor() == Color.RED) {

                CombineToken nextToken;

                if (firstToken) {
                    // if this is the first incoming ParallelToken, 
                    // create a CombineToken and set it as the best
                    // (and only) CombineToken of the state
                    nextToken = new CombineToken
                        (token, nextState, nextFrameNumber);
                    setBestToken(nextState, nextToken);
                    delayedExpansionList.add(nextToken);
                } else {
                    // get the combine token at the next state
                    nextToken = (CombineToken) getBestToken(nextState);
                }

                assert (oldNextToken.getFrameNumber() == nextFrameNumber);

                ParallelToken oldParallelToken = 
                    nextToken.getParallelToken(token.getFeatureStream());

                // if this is the first token, or if this score is
                // greater than the old one in the next CombineToken
                // add this token or replace the old one with this token

                if (oldParallelToken == null ||
                    oldParallelToken.getScore() <= logEntryScore) {

                    ParallelToken newToken = new ParallelToken
                        (token,
                         nextState,
                         logEntryScore,
                         token.getCombinedScore(),
                         nextFrameNumber,
			 token.getLastCombineTime());

                    // add this ParallelToken to the CombineToken.
                    nextToken.addParallelToken(newToken.getFeatureStream(),
					       newToken);
                }
            } else if (nextState.getColor() == Color.GREEN) {
                
                if (firstToken || 
                    getBestToken(nextState).getScore() <= logEntryScore) {
                    
                    ParallelToken newToken = new ParallelToken
                        (token,
                         nextState,
                         logEntryScore,
                         token.getCombinedScore(), 
                         nextFrameNumber,
			 token.getLastCombineTime());

                    if (newToken.isEmitting()) {
                        // this is an emitting token (or an emitting state)
                        replaceParallelToken(nextState, newToken);
                        combinedActiveList.add(newToken);
                    } else {
                        growParallelToken(newToken);
                    }
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
     * Returns the best Token for the given SearchState.
     *
     * @param the SearchState to look for
     *
     * @return the best Token for the given SearchState
     */
    private Token getBestToken(SearchState state) {
        return (Token) bestTokenMap.get(state);
    }


    /**
     * Sets the best Token for the given SearchState
     *
     * @param state the SearchState
     * @param token the best Token for the given SearchState
     */
    private void setBestToken(SearchState state, Token token) {
        bestTokenMap.put(state, token);
    }


    /**
     * Grows the given CombineToken and puts any results into the given
     * list.
     *
     * @param token the CombineToken to grow
     */
    private void growCombineToken(CombineToken token) {
        System.out.println("Entering growCombineToken");
	// If this is a final state, add it to the result list.
	if (token.isFinal()) {
	    resultList.add(token);
            System.out.println("FINAL RESULT found!");
	}

        int nextFrameNumber = token.getFrameNumber();

	// make sure that this state is non-emitting
	assert !token.isEmitting();

	SearchState state = (SearchState) token.getSearchState();
	SearchStateArc[] arcs = (SearchStateArc[]) state.getSuccessors();

	// expand into each successor states
	for (int a = 0; a < arcs.length; a++) {

	    SentenceHMMStateArc arc = (SentenceHMMStateArc) arcs[a];
	    SentenceHMMState nextState = (SentenceHMMState) arc.getState();

            Token oldNextToken = getBestToken(nextState);

	    boolean firstToken = 
                (oldNextToken == null ||
                 oldNextToken.getFrameNumber() != nextFrameNumber);

            // RED states are the unsplitted states, or the non-feature
            // stream states
            // GREEN states are the splitted states, or the feature stream
            // states

            if (nextState.getColor() == Color.RED) {
                
                float logEntryScore = token.getScore() + arc.getProbability();

                if (firstToken || oldNextToken.getScore() <= logEntryScore) {
                    
                    // create the next CombineToken for a RED state
                    CombineToken nextToken = new CombineToken
                        (token, nextState, nextFrameNumber);

                    // propagate the combined score unchanged
                    nextToken.setScore(logEntryScore);

                    // propagate the individual ParallelTokens, taking
                    // into account the new transition score
                    transitionParallelTokens
                        (token, nextToken, arc.getProbability());
                    
                    setBestToken(nextState, nextToken);

                    // finally grow the new CombineToken
                    growCombineToken(nextToken);
                }
            } else if (nextState.getColor() == Color.GREEN) {

                ParallelState pState = (ParallelState) nextState;
                
                ParallelToken parallelToken = token.getParallelToken
                    (pState.getFeatureStream());

                // we continue into a GREEN/feature stream state only
                // if there is a ParallelToken for that feature stream

		if (parallelToken != null) {
		    
		    float logEntryScore = arc.getProbability() + 
			parallelToken.getFeatureScore();
		    ParallelToken oldToken = (ParallelToken)oldNextToken;

		    if (firstToken || 
                        oldToken.getFeatureScore() <= logEntryScore) {
			
			ParallelToken nextToken = new ParallelToken
			    (parallelToken, 
			     nextState,
			     logEntryScore,
                             parallelToken.getCombinedScore(),
			     nextFrameNumber,
			     parallelToken.getLastCombineTime());

                        // replace the oldBestToken with this new token
			if (nextState.isEmitting()) {
			    // call the replaceParallelToken method
			    replaceParallelToken(nextState, nextToken);
			    combinedActiveList.add(nextToken);
			} else {
			    growParallelToken(nextToken);
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
                 pToken.getFeatureScore() + transitionScore,
                 pToken.getCombinedScore(),
                 pToken.getFrameNumber(),
		 pToken.getLastCombineTime());
            
            newToken.addParallelToken(newParallelToken.getFeatureStream(),
				      newParallelToken);
        }
    }



    /**
     * Replaces the token in the given SentenceHMMState with 
     * the given newToken.
     * This method will automatically find the correct ActiveList.
     *
     * @param oldToken the old Token to replace
     * @param newToken the new Token
     */
    private void replaceParallelToken(SentenceHMMState state,
                                      ParallelToken newToken) {
        ParallelToken oldToken = (ParallelToken) getBestToken(state);
        setBestToken(state, newToken);
        if (oldToken != null) {
            assert (oldToken.getFeatureStream() == 
                    newToken.getFeatureStream());
        }

        ActiveList activeList = newToken.getFeatureStream().getActiveList();
        assert activeList != null;
        activeList.replace(oldToken, newToken);
        
        if (oldToken != null) {
            oldToken.setPruned(true);
        }
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
        bestTokenMap = new HashMap();
    }
}

