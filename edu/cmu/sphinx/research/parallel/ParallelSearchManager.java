/*
 * Copyright 1999-2004 Carnegie Mellon University.  
 * Portions Copyright 2004 Sun Microsystems, Inc.  
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.research.parallel;

import edu.cmu.sphinx.linguist.acoustic.AcousticModel;

import edu.cmu.sphinx.result.Result;

import edu.cmu.sphinx.decoder.scorer.AcousticScorer;
import edu.cmu.sphinx.decoder.scorer.Scoreable;

import edu.cmu.sphinx.decoder.search.ActiveList;
import edu.cmu.sphinx.decoder.search.ActiveListFactory;
import edu.cmu.sphinx.decoder.search.SearchManager;

import edu.cmu.sphinx.decoder.pruner.Pruner;

import edu.cmu.sphinx.linguist.Linguist;
import edu.cmu.sphinx.linguist.SearchGraph;
import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.SearchStateArc;
import edu.cmu.sphinx.linguist.flat.Color;
import edu.cmu.sphinx.linguist.flat.SentenceHMMState;
import edu.cmu.sphinx.linguist.flat.SentenceHMMStateArc;

import edu.cmu.sphinx.decoder.search.Token;

import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Timer;

import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;

import java.io.IOException;

import java.util.Collection;
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
     * The sphinx property name for the active list type.
     */
    public static final String PROP_ACTIVE_LIST_FACTORY = "activeListFactory";

    /**
     * The sphinx property name for whether to do feature pruning.
     */
    public static final String PROP_DO_FEATURE_PRUNING = "doFeaturePruning";

    /**
     * The default value for whether to do feature pruning, which is false.
     */
    public static final boolean PROP_DO_FEATURE_PRUNING_DEFAULT = false;

    /**
     * The sphinx property for the feature score pruner.
     */
    public static final String PROP_FEATURE_SCORE_PRUNER = "featureScorePruner";

    /**
     * The sphinx property name for whether to do combine pruning.
     */
    public static final String PROP_DO_COMBINE_PRUNING = "doCombinePruning";

    /**
     * The default value for whether to do combine pruning, which is false.
     */
    public static final boolean PROP_DO_COMBINE_PRUNING_DEFAULT = false;

    /**
     * The sphinx property for the combined score pruner.
     */
    public static final String PROP_COMBINED_SCORE_PRUNER = 
        "combinedScorePruner";

    /**
     * The sphinx property for scorer used.
     */
    public static final String PROP_SCORER = "scorer";

    /**
     * The sphinx property for linguist used.
     */
    public static final String PROP_LINGUIST = "linguist";


    private String name;
    private ParallelSimpleLinguist linguist;
    private AcousticScorer scorer;
    private Pruner featureScorePruner;
    private Pruner combinedScorePruner;
    private ScoreCombiner featureScoreCombiner;

    private int currentFrameNumber;           // the current frame number
    private ActiveListFactory activeListFactory;
    private ActiveList combinedActiveList;    // ActiveList for common states
    private ActiveList delayedExpansionList;  // for tokens at CombineStates
    private List resultList;

    private Map bestTokenMap;

    private Timer scoreTimer;
    private Timer pruneTimer;
    private Timer growTimer;

    private boolean doFeaturePruning;
    private boolean doCombinePruning;


    /* (non-Javadoc)
     * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String, edu.cmu.sphinx.util.props.Registry)
     */
    public void register(String name, Registry registry)
        throws PropertyException {
        this.name = name;
        registry.register(PROP_LINGUIST, PropertyType.COMPONENT);
        registry.register(PROP_SCORER, PropertyType.COMPONENT);
        registry.register(PROP_ACTIVE_LIST_FACTORY, PropertyType.COMPONENT);
        registry.register(PROP_DO_FEATURE_PRUNING, PropertyType.BOOLEAN);
        registry.register(PROP_DO_COMBINE_PRUNING, PropertyType.BOOLEAN);
        registry.register(PROP_FEATURE_SCORE_PRUNER, PropertyType.COMPONENT);
        registry.register(PROP_COMBINED_SCORE_PRUNER, PropertyType.COMPONENT);
    }


    /*
     * (non-Javadoc)
     *
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {

        linguist = (ParallelSimpleLinguist) ps.getComponent
            (PROP_LINGUIST, ParallelSimpleLinguist.class);

        scorer = (AcousticScorer) ps.getComponent
            (PROP_SCORER, AcousticScorer.class);

	activeListFactory = (ActiveListFactory) ps.getComponent
            (PROP_ACTIVE_LIST_FACTORY, ActiveListFactory.class);

	this.doFeaturePruning = ps.getBoolean
	    (PROP_DO_FEATURE_PRUNING, PROP_DO_FEATURE_PRUNING_DEFAULT);

	this.doCombinePruning = ps.getBoolean
	    (PROP_DO_COMBINE_PRUNING, PROP_DO_COMBINE_PRUNING_DEFAULT);

	if (doFeaturePruning) {
	    featureScorePruner = (FeatureScorePruner) ps.getComponent
                (PROP_FEATURE_SCORE_PRUNER, FeatureScorePruner.class);
        }
	if (doCombinePruning) {
	    combinedScorePruner = (CombinedScorePruner) ps.getComponent
                (PROP_COMBINED_SCORE_PRUNER, CombinedScorePruner.class);
	}
	
        featureScoreCombiner = new FeatureScoreCombiner();

        scoreTimer = Timer.getTimer("Score");
        pruneTimer = Timer.getTimer("Prune");
        growTimer = Timer.getTimer("Grow");
        
    }


    /*
     * (non-Javadoc)
     *
     * @see edu.cmu.sphinx.decoder.search.SearchManager#allocate()
     */
    public void allocate() throws IOException {
        bestTokenMap = new HashMap();
        linguist.allocate();
	if (doFeaturePruning) {
	    featureScorePruner.allocate();
	}
	if (doCombinePruning) {
	    combinedScorePruner.allocate();
	}
        scorer.allocate();
    }
    

    /*
     * (non-Javadoc)
     *
     * @see edu.cmu.sphinx.util.props.Configurable#getName()
     */
    public String getName() {
        return name;
    }


    /**
     * Prints a debug message.
     *
     * @param message the debug message to print
     */
    private void debugPrint(String message) {
	if (false) {
	    System.out.println(message);
	}
    }


    /**
     * Prepares the SearchManager for recognition.  This method must
     * be called before <code> recognize </code> is called.
     */
    public void startRecognition() {
	currentFrameNumber = 0;
	linguist.startRecognition();
        if (doFeaturePruning) {
            featureScorePruner.startRecognition();
	}
	if (doCombinePruning) {
            combinedScorePruner.startRecognition();
        }
	scorer.startRecognition();
	createInitialLists();
    }


    /**
     * Creates the ActiveLists used for decoding. There is one ActiveList
     * created for each feature stream (or acoustic model), and also an
     * ActiveList to do the overall pruning.
     */
    private void createInitialLists() {
        
        combinedActiveList = activeListFactory.newInstance();
        delayedExpansionList = activeListFactory.newInstance();
        
        SentenceHMMState firstState = (SentenceHMMState)
            linguist.getSearchGraph().getInitialState();
        
        // create the first token and grow it, its first parameter
        // is null because it has no predecessor
        CombineToken firstToken = new CombineToken
            (null, firstState, currentFrameNumber);
        
        setBestToken(firstState, firstToken);
        
        for (Iterator i = linguist.getFeatureStreams(); i.hasNext();) {
            FeatureStream stream = (FeatureStream) i.next();
            stream.setActiveList(activeListFactory.newInstance());
            
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
        debugPrint("-----\nFrame: " + currentFrameNumber);
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
	for (Iterator i = linguist.getFeatureStreams(); i.hasNext();) {
            FeatureStream stream = (FeatureStream) i.next();
	    Scoreable scoreable = 
		scorer.calculateScores(stream.getActiveList().getTokens());
	    moreFeatures = (scoreable != null);    
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
	    for (Iterator i = linguist.getFeatureStreams(); i.hasNext();) {
		FeatureStream stream = (FeatureStream) i.next();	
                stream.setActiveList
                    (featureScorePruner.prune(stream.getActiveList()));
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
        for (Iterator i = linguist.getFeatureStreams(); i.hasNext();) {
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

        resultList = new LinkedList();
        combinedActiveList = activeListFactory.newInstance();
	delayedExpansionList = activeListFactory.newInstance();

        // grow the ActiveList of each feature stream
	for (Iterator i = linguist.getFeatureStreams(); i.hasNext();) {
            FeatureStream stream = (FeatureStream) i.next();

            // create a new ActiveList for the next frame
            ActiveList oldActiveList = stream.getActiveList();
            stream.setActiveList(activeListFactory.newInstance());
            
	    growActiveList(oldActiveList);
	}

        // now expand the delayedExpansionList, which contains the
        // CombineTokens created when transitioning from GREEN states
        // to RED states (i.e., feature stream states to shared states)

	growDelayedExpansionList();

        // remove all pruned tokens from the active list of all streams
	for (Iterator i = linguist.getFeatureStreams(); i.hasNext();) {
            FeatureStream stream = (FeatureStream) i.next();

            // remove all the pruned tokens
            ActiveList prunedActiveList = activeListFactory.newInstance();
            for (Iterator t = stream.getActiveList().iterator(); 
                 t.hasNext();) {
                ParallelToken token = (ParallelToken) t.next();
                if (!token.isPruned()) {
                    prunedActiveList.add(token);
                }
            }
            stream.setActiveList(prunedActiveList);
        }

	debugPrint(" done Growing");

	growTimer.stop();
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
            token.setLastCombineTime(currentFrameNumber);
            growCombineToken(token);
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
            growParallelToken(token);
	}
    }


    /**
     * Grows the given ParallelToken, put the successor Token(s) into the 
     * given ActiveList, and any possible results in the given resultList.
     *
     * @param token the Token to grow
     */
    private void growParallelToken(ParallelToken token) {

        /*
        debugPrint("Entering growParallelToken: " + 
                   token.getSearchState().toString());
        */

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

	    // debugPrint("  Entering " + nextState.toString());

	    float logEntryScore = token.getScore() + arc.getProbability();
            Token oldNextToken = getBestToken(nextState);

	    boolean firstToken = oldNextToken == null ||
                oldNextToken.getFrameNumber() != nextFrameNumber;

            // RED states are the unsplitted states, or the non-feature
            // stream states
            // GREEN states are the splitted states, or the feature stream
            // states
            
            if (nextState.getColor() == Color.RED) {

                // debugPrint(". RED state");

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

                assert (nextToken.getFrameNumber() == nextFrameNumber);

                ParallelToken oldParallelToken = 
                    nextToken.getParallelToken(token.getFeatureStream());

                // if this is the first token, or if this score is
                // greater than the old one in the next CombineToken
                // add this token or replace the old one with this token

                if (firstToken || oldParallelToken == null ||
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

                // debugPrint("  . GREEN state");
                
                if (firstToken || 
                    getBestToken(nextState).getScore() <= logEntryScore) {

                    // debugPrint("  . adding parallel token");

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
     * @param state the SearchState to look for
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
    private Token setBestToken(SearchState state, Token token) {
        return (Token) bestTokenMap.put(state, token);
    }


    /**
     * Grows the given CombineToken and puts any results into the given
     * list.
     *
     * @param token the CombineToken to grow
     */
    private void growCombineToken(CombineToken token) {
        // debugPrint("Entering growCombineToken");
	// If this is a final state, add it to the result list.
	if (token.isFinal()) {
	    resultList.add(token);
	    // debugPrint("FINAL RESULT found!");
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

	    // debugPrint("Entering " + nextState.toString());

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
     * @param state the state of the current token
     * @param newToken the new Token
     */
    private void replaceParallelToken(SentenceHMMState state,
                                      ParallelToken newToken) {
        ParallelToken oldToken = (ParallelToken) setBestToken(state, newToken);
        
        ActiveList activeList = newToken.getFeatureStream().getActiveList();
        activeList.add(newToken);
                
        if (oldToken != null) {
            oldToken.setPruned(true);
        }
    }


    /**
     * Performs post-recognition cleanup. This method should be called
     * after recognize returns a final result.
     */
    public void stopRecognition() {
	scorer.stopRecognition();
        if (doFeaturePruning) {
            featureScorePruner.stopRecognition();
	}
	if (doCombinePruning) {
            combinedScorePruner.stopRecognition();
        }
	linguist.stopRecognition();
        bestTokenMap = new HashMap();
    }


    /*
     * (non-Javadoc)
     *
     * @see edu.cmu.sphinx.decoder.search.SearchManager#deallocate()
     */
    public void deallocate() {
        scorer.deallocate();
	if (doFeaturePruning) {
	    featureScorePruner.deallocate();
	}
	if (doCombinePruning) {
	    combinedScorePruner.deallocate();
	}
        linguist.deallocate();
    }    
}

