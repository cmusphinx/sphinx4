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

package edu.cmu.sphinx.trainer;

import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.FeatureFrame;
import edu.cmu.sphinx.frontend.Feature;
import edu.cmu.sphinx.frontend.Signal;
import edu.cmu.sphinx.frontend.util.StreamAudioSource;
import edu.cmu.sphinx.frontend.util.StreamCepstrumSource;
import edu.cmu.sphinx.frontend.DataSource;

import edu.cmu.sphinx.knowledge.acoustic.TrainerAcousticModel;
import edu.cmu.sphinx.knowledge.acoustic.TrainerScore;
import edu.cmu.sphinx.knowledge.acoustic.HMM;
import edu.cmu.sphinx.knowledge.acoustic.HMMState;

import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

import java.util.List;
import java.util.ArrayList;

/**
 * Provides mechanisms for computing statistics given a set of states
 * and input data.
 */
public class BaumWelchLearner implements Learner {


    private final static String PROP_PREFIX = 
	"edu.cmu.sphinx.trainer.";


    /**
     * The SphinxProperty name for the input data type.
     */
    public final static String PROP_INPUT_TYPE = PROP_PREFIX+"inputDataType";


    /**
     * The default value for the property PROP_INPUT_TYPE.
     */
    public final static String PROP_INPUT_TYPE_DEFAULT = "cepstrum";

    /**
     * The sphinx property for the front end class.
     */
    public final static String PROP_FRONT_END = PROP_PREFIX + "frontend";


    /**
     * The default value of PROP_FRONT_END.
     */
    public final static String PROP_FRONT_END_DEFAULT
        = "edu.cmu.sphinx.frontend.SimpleFrontEnd";


    private FrontEnd frontEnd;
    private DataSource dataSource;
    private String context;
    private String inputDataType;
    private SphinxProperties props;
    private LogMath logMath;
    private Feature curFeature;
    private UtteranceGraph graph;
    private Object[] scoreArray;
    private int lastFeatureIndex;
    private int currentFeatureIndex;
    private float[] alphas;
    private float[] betas;
    private float[] outputProbs;
    private float[] componentScores;
    private float[] probCurrentFrame;
    private float totalLogScore;

    /**
     * Constructor for this learner.
     */
    public BaumWelchLearner(SphinxProperties props)
	throws IOException {
	this.props = props;
	context = props.getContext();
	logMath = LogMath.getLogMath(context);
	initialize();
    }

    /**
     * Initializes the Learner with the proper context and frontend.
     *
     * @throws IOException
     */
    private void initialize() throws IOException  {

	inputDataType = props.getString(PROP_INPUT_TYPE, 
                                        PROP_INPUT_TYPE_DEFAULT);

	if (inputDataType.equals("audio")) {
	    dataSource = new StreamAudioSource
		("batchAudioSource", context, null, null);
	} else if (inputDataType.equals("cepstrum")) {
	    dataSource = new StreamCepstrumSource
		("batchCepstrumSource", context);
	} else {
	    throw new Error("Unsupported data type: " + inputDataType + "\n" +
			    "Only audio and cepstrum are supported\n");
	}

	frontEnd = getFrontEnd();
    }

    // Cut and paste from e.c.s.d.Recognizer.java
    /**
     * Initialize and return the frontend based on the given sphinx
     * properties.
     */
    protected FrontEnd getFrontEnd() {
        String path = null;
        try {
            path = props.getString(PROP_FRONT_END, PROP_FRONT_END_DEFAULT);
            FrontEnd fe = (FrontEnd)Class.forName(path).newInstance();
            fe.initialize("BWFrontEnd", context, dataSource);
            return fe;
        } catch (ClassNotFoundException fe) {
            throw new Error("CNFE:Can't create front end " + path, fe);
        } catch (InstantiationException ie) {
            throw new Error("IE: Can't create front end " + path, ie);
        } catch (IllegalAccessException iea) {
            throw new Error("IEA: Can't create front end " + path, iea);
        } catch (IOException ioe) {
            throw new Error("IOE: Can't create front end " + path + " "
                    + ioe, ioe);
        }
    }

    /**
     * Sets the learner to use a utterance.
     *
     * @param utterance the utterance
     *
     * @throws IOException
     */
    public void setUtterance(Utterance utterance) throws IOException {
	String file = utterance.toString();

        InputStream is = new FileInputStream(file);

	inputDataType = props.getString(PROP_INPUT_TYPE, 
                                        PROP_INPUT_TYPE_DEFAULT);

        if (inputDataType.equals("audio")) {
            ((StreamAudioSource) dataSource).setInputStream(is, file);
        } else if (inputDataType.equals("cepstrum")) {
            boolean bigEndian = Utilities.isCepstraFileBigEndian(file);
            ((StreamCepstrumSource) dataSource).setInputStream(is, bigEndian);
        }
    }

    /**
     * Returns a single frame of speech.
     *
     * @return a feature frame
     *
     * @throws IOException
     */
    private boolean getFeature() {
	FeatureFrame ff;

	try {
	    ff = frontEnd.getFeatureFrame(1, null);

            if (!hasFeatures(ff)) {
                return false;
            }

	    curFeature = ff.getFeatures()[0];

	    if (curFeature.getSignal() == Signal.UTTERANCE_START) {
                ff = frontEnd.getFeatureFrame(1, null);
                if (!hasFeatures(ff)) {
                    return false;
                }
                curFeature = ff.getFeatures()[0];
            }

	    if (curFeature.getSignal() == Signal.UTTERANCE_END) {
		return false;
	    }

            if (!curFeature.hasContent()) {
                throw new Error("Can't score non-content feature");
            }

	} catch (IOException ioe) {
	    System.out.println("IO Exception " + ioe);
	    ioe.printStackTrace();
	    return false;
	}

	return true;
    }

    /**
     * Checks to see if a FeatureFrame is null or if there are Features in it.
     *
     * @param ff the FeatureFrame to check
     *
     * @return false if the given FeatureFrame is null or if there
     * are no Features in the FeatureFrame; true otherwise.
     */
    private boolean hasFeatures(FeatureFrame ff) {
        if (ff == null) {
            System.out.println("BaumWelchLearner: FeatureFrame is null");
            return false;
        }
        if (ff.getFeatures() == null) {
            System.out.println
                ("BaumWelchLearner: no features in FeatureFrame");
            return false;
        }
        return true;
    }


    /**
     * Starts the Learner.
     */
    public void start(){
    }

    /**
     * Stops the Learner.
     */
    public void stop(){
    }

    /**
     * Initializes computation for current utterance and utterance graph.
     *
     * @param utterance the current utterance
     * @param graph the current utterance graph
     *
     * @throws IOException
     */
    public void initializeComputation(Utterance utterance, 
		      UtteranceGraph graph)  throws IOException {
	setUtterance(utterance);
	setGraph(graph);
    }

    /**
     * Implements the setGraph method. 
     *
     * @param graph the graph
     */
    public void setGraph(UtteranceGraph graph) {
	this.graph = graph;
    }

    /**
     * Prepares the learner for returning scores, one at a time. To do
     * so, it performs the full forward pass, but returns the scores
     * for the backwaard pass one feature frame at a time.
     */
    private Object[] prepareScore() {
	List scoreList = new ArrayList();
	// Let's make our life easier, and type cast the graph
	int numStates = graph.size();
	TrainerScore[] score = new TrainerScore[numStates];
	float[] alphas = new float[numStates];
	float[] betas = new float[numStates];
	float[] outputProbs = new float[numStates];
	lastFeatureIndex = 0;
	// First we do the forward pass. We need this before we can
	// return any probability. When we're doing the backward pass,
	// we can finally return a score for each call of this method.

	float[] probCurrentFrame = new float[numStates];
	// Initialization of probCurrentFrame for the alpha computation
	int indexInitialNode = graph.indexOf(graph.getInitialNode());
	for (int i = 0; i < numStates; i++) {
	    probCurrentFrame[i] = LogMath.getLogZero();
	}
	// Overwrite in the right position
	probCurrentFrame[indexInitialNode] = 0.0f;
	// If getFeature() is true, curFeature contains a valid
	// Feature. If not, a problem or EOF was encountered.
	while (getFeature()) {
	    forwardPass(score);
	    scoreList.add(score);
	    lastFeatureIndex++;
	}
	// Prepare for beta computation
	for (int i = 0; i < probCurrentFrame.length; i++) {
	    probCurrentFrame[i] = LogMath.getLogZero();
	}
	return scoreList.toArray();
    }

    /**
     * Gets the TrainerScore for the next frame
     *
     * @return the TrainerScore, or null if EOF was found
     */
    public TrainerScore[] getScore() {
	TrainerScore[] score;
	if (scoreArray == null) {
	    currentFeatureIndex = lastFeatureIndex;
	    // Do the forward pass, and creates the necessary arrays
	    scoreArray = prepareScore();
	}
	probCurrentFrame = new float[betas.length];
	int indexFinalNode = graph.indexOf(graph.getFinalNode());
	// Overwrite in the right position
	probCurrentFrame[indexFinalNode] = 0.0f;
	currentFeatureIndex--;
	if (currentFeatureIndex >= 0) {
	    float logScore = LogMath.getLogZero();
	    score = (TrainerScore []) scoreArray[currentFeatureIndex];
	    assert score.length == betas.length;
	    backwardPass(score);
	    for (int i = 0; i < betas.length; i++) {
		score[i].setGamma();
		logScore = logMath.addAsLinear(logScore, score[i].getGamma());
	    }
	    if (currentFeatureIndex < lastFeatureIndex - 1) {
		TrainerScore.setLogLikelihood(logScore);
		totalLogScore = logScore;
	    } else {
		if (Math.abs(totalLogScore - logScore) > 1e-3) {
		    System.out.println("WARNING: log probabilities differ: " +
				       totalLogScore + " and " + logScore);
		}
	    }
	    return score;
	} else {
	    return null;
	}
    }

    /**
     * Computes the acoustic scores using the current Feature and a
     * given node in the graph.
     *
     * @param index the graph index
     *
     * @return the overall acoustic score
     */
    private float calculateScores(int index) {
	float logScore;
	// Find the HMM state for this node
	HMMState state = (HMMState) graph.getNode(index).getObject();
	if (state.isEmitting()) {
	    // Compute the scores for each mixture component in this state
	    componentScores = state.calculateComponentScore(curFeature);
	    // Compute the overall score for this state
	    logScore = state.getScore(curFeature);
	    // For CI models, for now, we only try to use mixtures
	    // with one component
	    assert componentScores.length == 1;
	} else {
	    componentScores = null;
	    logScore = 0.0f;
	}
	return logScore;
    }

    /**
     * Does the forward pass, one frame at a time.
     *
     * @param score the objects transferring info to the buffers
     */
    private void forwardPass(TrainerScore[] score) {
	// Let's precompute the acoustic probabilities and create the
	// score object, one for each state
	for (int i = 0; i < graph.size(); i++) {
	    outputProbs[i] = calculateScores(i);
	    score[i] = new TrainerScore(curFeature,
			outputProbs[i],
			(HMMState) graph.getNode(i).getObject(),
			componentScores);
	}
	// Now, the forward pass.
	float[] probPreviousFrame = probCurrentFrame;
	probCurrentFrame = new float[graph.size()];
	// First, the emitting states. We have to do this because the
	// emitting states use probabilities from the previous
	// frame. The non-emitting states, however, since they don't
	// consume frames, use probabilities from the current frame
	for (int indexNode = 0; indexNode < graph.size(); indexNode++) {
	    Node node = graph.getNode(indexNode);
	    HMMState state = (HMMState) node.getObject();
	    HMM hmm = state.getHMM();
	    if (!state.isEmitting()) {
		continue;
	    }
	    // Initialize the current frame probability with this
	    // state's output probability for the current Feature
	    probCurrentFrame[indexNode] = outputProbs[indexNode];
	    for (node.startIncomingEdgeIterator();
		 node.hasMoreIncomingEdges(); ) {
		// Finds out what the previous node and previous state are
		Node previousNode = node.nextIncomingEdge().getSource();
		int indexPreviousNode = graph.indexOf(previousNode);
		HMMState previousState = (HMMState) previousNode.getObject();
		// Make sure that the transition happened from a state
		// that either is in the same model, or was a
		// non-emitting state
		assert ((!previousState.isEmitting()) || 
			(previousState.getHMM() == hmm));
		float logTransitionProbability;
		if (!previousState.isEmitting()) {
		    logTransitionProbability = 0.0f;
		} else {
		    logTransitionProbability = 
			hmm.getTransitionProbability(previousState.getState(),
						     state.getState());
		}
		// Adds the alpha and transition from the previous
		// state into the current alpha
		probCurrentFrame[indexNode] = 
		    logMath.addAsLinear(probCurrentFrame[indexNode],
					probPreviousFrame[indexPreviousNode] +
					logTransitionProbability);
	    }
	    score[indexNode].setAlpha(probCurrentFrame[indexNode]);
	}

	// Finally, the non-emitting states
	for (int indexNode = 0; indexNode < graph.size(); indexNode++) {
	    Node node = graph.getNode(indexNode);
	    HMMState state = (HMMState) node.getObject();
	    HMM hmm = state.getHMM();
	    if (state.isEmitting()) {
		continue;
	    }
	    // Initialize the current frame probability with log
	    // probability of 0f
	    probCurrentFrame[indexNode] = 0.0f;
	    for (node.startIncomingEdgeIterator();
		 node.hasMoreIncomingEdges(); ) {
		// Finds out what the previous node and previous state are
		Node previousNode = node.nextIncomingEdge().getSource();
		int indexPreviousNode = graph.indexOf(previousNode);
		HMMState previousState = (HMMState) previousNode.getObject();
		// Make sure that the transition happened from an
		// emitting state, or is a self loop
		assert ((previousState.isEmitting()) || 
			(previousState == state));
		float logTransitionProbability;
		if (previousState.isEmitting()) {
		    logTransitionProbability = 0.0f;
		} else {
		    // previousState == state
		    logTransitionProbability = 
			hmm.getTransitionProbability(previousState.getState(),
						     state.getState());
		}
		// Adds the alpha and transition from the previous
		// state into the current alpha
		probCurrentFrame[indexNode] = 
		    logMath.addAsLinear(probCurrentFrame[indexNode],
					probCurrentFrame[indexPreviousNode] +
					logTransitionProbability);
	    }
	    score[indexNode].setAlpha(probCurrentFrame[indexNode]);
	}
    }

    /**
     * Does the backward pass, one frame at a time.
     *
     * @param feature the feature to be used
     */
    private void backwardPass(TrainerScore[] score) {
	// Now, the backward pass.
	for (int i = 0; i < graph.size(); i++) {
	    outputProbs[i] = score[i].getScore();
	}
	float[] probNextFrame = probCurrentFrame;
	probCurrentFrame = new float[graph.size()];
	// First, the non-emitting states. Here we go in the opposite
	// direction as in the forward case.
	for (int indexNode = 0; indexNode < graph.size(); indexNode++) {
	    Node node = graph.getNode(indexNode);
	    HMMState state = (HMMState) node.getObject();
	    HMM hmm = state.getHMM();
	    if (state.isEmitting()) {
		continue;
	    }
	    // Initialize the current frame probability with log(0f)
	    probCurrentFrame[indexNode] = LogMath.getLogZero();
	    for (node.startOutgoingEdgeIterator();
		 node.hasMoreOutgoingEdges(); ) {
		// Finds out what the next node and next state are
		Node nextNode = node.nextOutgoingEdge().getSource();
		int indexNextNode = graph.indexOf(nextNode);
		HMMState nextState = (HMMState) nextNode.getObject();
		// Make sure that the transition happened from a state
		// that either is in the same, or is emitting
		assert ((nextState.isEmitting()) || (nextState == state));
		float logTransitionProbability;
		if (!nextState.isEmitting()) {
		    logTransitionProbability = 0.0f;
		} else {
		    logTransitionProbability = 
			hmm.getTransitionProbability(state.getState(),
						     nextState.getState());
		}
		// Adds the beta, the transition, and the output prob
		// from the next state into the current beta
		probCurrentFrame[indexNode] = 
		    logMath.addAsLinear(probCurrentFrame[indexNode],
					probNextFrame[indexNextNode] +
					logTransitionProbability);
	    }
	    score[indexNode].setBeta(probCurrentFrame[indexNode]);
	}

	// Finally, the emitting states
	for (int indexNode = 0; indexNode < graph.size(); indexNode++) {
	    Node node = graph.getNode(indexNode);
	    HMMState state = (HMMState) node.getObject();
	    HMM hmm = state.getHMM();
	    if (state.isEmitting()) {
		continue;
	    }
	    // Initialize the current frame probability with log
	    // probability of log(0f)
	    probCurrentFrame[indexNode] = LogMath.getLogZero();
	    for (node.startOutgoingEdgeIterator();
		 node.hasMoreOutgoingEdges(); ) {
		// Finds out what the next node and next state are
		Node nextNode = node.nextOutgoingEdge().getSource();
		int indexNextNode = graph.indexOf(nextNode);
		HMMState nextState = (HMMState) nextNode.getObject();
		// Make sure that the transition happened to a
		// non-emitting state, or to the same model
		assert ((!nextState.isEmitting()) || 
			(nextState.getHMM() == hmm));
		float logTransitionProbability;
		if (!nextState.isEmitting()) {
		    logTransitionProbability = 0.0f;
		} else {
		    logTransitionProbability = 
			hmm.getTransitionProbability(state.getState(),
						     nextState.getState());
		}
		// Adds the beta, the output prob, and the transition
		// from the next state into the current beta
		probCurrentFrame[indexNode] = 
		    logMath.addAsLinear(probCurrentFrame[indexNode],
					probNextFrame[indexNextNode] +
					logTransitionProbability +
					outputProbs[indexNextNode]);
	    }
	    score[indexNode].setBeta(probCurrentFrame[indexNode]);
	}
    }


    /* Pseudo code:
    forward pass:
        token = maketoken(initialstate);
        List initialTokenlist = new List;
        newtokenlist.add(token);
    
        // Initial token is on a nonemitting state; no need to score;
        List newList = expandToEmittingStateList(initialTokenList){

        while (morefeatures){
           scoreTokenList(emittingTokenList, featurevector[timestamp]);
           pruneTokenList(emittingTokenList);
           List newList = expandToEmittingStateList(emittingTokenList){
           timestamp++;
        }
        // Some logic to expand to a final nonemitting state (how)?
        expandToNonEmittingStates(emittingTokenList);
    */

    /*
    private void forwardPass() {
        ActiveList activelist = new FastActiveList(createInitialToken());
	AcousticScorer acousticScorer = new ThreadedAcousticScorer();
	FeatureFrame featureFrame = frontEnd.getFeatureFrame(1, "");
	Pruner pruner = new SimplePruner();

        // Initialization code pushing initial state to emitting state here

        while ((featureFrame.getFeatures() != null)) {
            ActiveList nextActiveList = new FastActiveList();

            // At this point we have only emitting states. We score
            // and prune them
            ActiveList emittingStateList = new FastActiveList(); 
	                  // activelist.getEmittingStateList();
            acousticScorer.calculateScores(emittingStateList.getTokens());
	    // The pruner must clear up references to pruned objects
            emittingStateList = pruner.prune( emittingStateList);
            
            expandStateList(emittingStateList, nextActiveList); 

            while (nextActiveList.hasNonEmittingStates()){
		// exctractNonEmittingStateList will pull out the list
		// of nonemitting states completely from the
		// nextActiveList. At this point nextActiveList does
		// not have a list of nonemitting states and must
		// instantiate a new one.
                ActiveList nonEmittingStateList = 
		    nextActiveList.extractNonEmittingStateList();
                nonEmittingStateList = pruner.prune(nonEmittingStateList);
                expandStateList(nonEmittingStateList, nextActiveList); 
            }
            activeList = newActiveList;
        }
    }
    */

    /* Pseudo code
    backward pass:
       state = finaltoken.state.wholelistofeverythingthatcouldbefinal;
        while (moreTokensAtCurrentTime) { 
            Token token = nextToken();
            State state = token.state;
            state.gamma = state.logalpha + state.logbeta - logtotalprobability;
            SentenceHMM.updateState(state,state.gamma,vector[state.timestamp]);
            // state.update (state.gamma, vector[state.timestamp], updatefunction());
            while token.hasMoreIncomingEdges() {
                Edge transition = token.nextIncomingEdge();
                double logalpha = transition.source.alpha;
                double logbeta  = transition.destination.beta;
                double logtransition = transition.transitionprob;
                // transition.posterior = alpha*transition*beta / 
                //                           totalprobability;
                double localtransitionbetascore = logtransition + logbeta + 
                                              transition.destination.logscore;
                double transition.posterior = localtransitionbetascore + 
                                              logalpha - logtotalprobability;
                transition.updateaccumulator(transition.posterior);
                // transition.updateaccumulator(transition.posterior, updatefunction());
                SentenceHMM.updateTransition(transition, transitionstate,state.gamma);
                transition.source.beta = Logadd(transition.source.beta,
                                                localtransitionbetascore);
                                        
            }
        }
    */

    /*
    private void expandStateList(ActiveList stateList, 
                                 ActiveList nextActiveList) {
        while (stateList.hasMoreTokens()) {
            Token token = emittingStateList.getNextToken();

	    // First get list of links to possible future states
            List successorList = getSuccessors(token);
            while (successorList.hasMoreEntries()) {
                UtteranceGraphEdge edge = successorList.getNextEntry();

		// create a token for the future state, if its not
		// already in active list; The active list will check
		// for the key "edge.destination()" in both of its
		// lists
                if (nextActiveList.hasState(edge.destination())) {
                    Token newToken = 
			nextActiveList.getTokenForState(edge.destination());
		} else {
                    Token newToken = new Token(edge.destination());
		}

		// create a link between current state and future state
                TrainerLink newlink = new TrainerLink(edge, token, newToken);
                newlink.logScore = token.logScore + edge.transition.logprob();

		// add link to the appropriate lists for source and
		// destination tokens
                token.addOutGoingLink(newlink);

                newToken.addIncomingLink(newlink);
                newToken.alpha = logAdd(newToken.alpha, newlink.logScore);

                // At this point, we have placed a new token in the
                // successor state, and linked the token at the
                // current state to the token at the non-emitting
                // states.

		// Add token to appropriate active list
                nextActiveList.add(newToken);
            }
        }
    }
    */

    /*
    private void expandToEmittingStateList(List tokenList){
	List emittingTokenList = new List();
	do {
	    List nonEmittingTokenList = new List();
	    expandtokens(newtokenlist, emittingTokenList, 
			 nonemittingTokenList);
	    while (nonEmittingTokenList.length() != 0);
	    return emittingTokenList;
	}
    }
    */

    /*
    private void expandtokens(List tokens, List nonEmittingStateList, 
			      List EmittingStateList){
	while (moreTokens){
	    sucessorlist = SentenceHMM.gettransitions(nextToken());
	    while (moretransitions()){
		transition = successor;
		State destinationState = successor.state;
		newtoken = gettokenfromHash(destinationState, 
					    currenttimestamp);
		newtoken.logscore = Logadd(newtoken.logscore,
				   token.logscore + transition.logscore);
		// Add transition to newtoken predecessor list?
		// Add transition to token sucessor list
		// Should we define a token "arc" for this. ??
		if (state.isemitting)
		    EmittingStateList.add(newtoken);
		else
		    nonEmittingStateList.add(newtoken);
	    } 
	}
    }
    */

}
