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

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.knowledge.acoustic.TrainerScore;

import java.io.IOException;

import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.FeatureFrame;
import edu.cmu.sphinx.frontend.Feature;
import edu.cmu.sphinx.frontend.Signal;
import edu.cmu.sphinx.frontend.util.StreamAudioSource;
import edu.cmu.sphinx.frontend.util.StreamCepstrumSource;
import edu.cmu.sphinx.frontend.DataSource;

import edu.cmu.sphinx.knowledge.acoustic.TrainerAcousticModel;
import edu.cmu.sphinx.knowledge.acoustic.TrainerScore;

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

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
    private Feature curFeature;

    /**
     * Constructor for this learner.
     */
    public BaumWelchLearner(SphinxProperties props)
	throws IOException {
	this.props = props;
	context = props.getContext();
	initialize();
    }

    /**
     * Initializes the Learner with the proper context and frontend.
     *
     * @param utterance the current utterance
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
            fe.initialize("FrontEnd", context, dataSource);
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
     * @throw IOException
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
            System.out.println("FlatInitializerLearner: FeatureFrame is null");
            return false;
        }
        if (ff.getFeatures() == null) {
            System.out.println
                ("FlatInitializerLearner: no features in FeatureFrame");
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
     * Implements the setGraph method. Since the flat initializer does
     * not need a graph, this method produces an error.
     *
     * @param graph the graph
     */
    public void setGraph(UtteranceGraph graph) {
	new Error("Flat initializer does not use a graph!");
    }

    /**
     * Gets the TrainerScore for the next frame
     *
     * @return the TrainerScore
     */
    public TrainerScore getScore() {
	// If getFeature() is true, curFeature contains a valid
	// Feature. If not, a problem or EOF was encountered.
	if (getFeature()) {
	    // Since it's flat initialization, the probability is
	    // neutral, and the senone means "all senones".
	    TrainerScore score = new TrainerScore(curFeature, 0.0f, 
				  TrainerAcousticModel.ALL_MODELS);
	    return score;
	} else {
	    return null;
	}
    }


    //    private void forwardPass() {
    //        ActiveList activelist = new FastActiveList(createInitialToken());
    //	AcousticScorer acousticScorer = new ThreadedAcousticScorer();
    //	FeatureFrame featureFrame = frontEnd.getFeatureFrame(1, "");
    //	Pruner pruner = new SimplePruner();
    //
    //        /* Initialization code pushing initial state to emitting state here */
    //
    //        while ((featureFrame.getFeatures() != null)) {
    //            ActiveList nextActiveList = new FastActiveList();
    //
    //            /* At this point we have only emitting states. We score and
    //             * prune them 
    //             */
    //            ActiveList emittingStateList = new FastActiveList(); // activelist.getEmittingStateList();
    //            acousticScorer.calculateScores(emittingStateList.getTokens());
    //            // The pruner must clear up references to pruned objects
    //            emittingStateList = pruner.prune( emittingStateList);
    //            
    //            expandStateList(emittingStateList, nextActiveList); 
    //
    //            while (nextActiveList.hasNonEmittingStates()){
    //                // exctractNonEmittingStateList will pull out the list of
    //                // nonemitting states completely from the nextActiveList.
    //                // At this point nextActiveList does not have a list of
    //                // nonemitting states and must instantiate a new one.
    //                ActiveList nonEmittingStateList = 
    //                               nextActiveList.extractNonEmittingStateList();
    //                nonEmittingStateList = pruner.prune(nonEmittingStateList);
    //                expandStateList(nonEmittingStateList, nextActiveList); 
    //            }
    //            activeList = newActiveList;
    //        }
    //    }
    //
    //
    //    private void expandStateList(ActiveList stateList, 
    //                                 ActiveList nextActiveList) {
    //        while (stateList.hasMoreTokens()) {
    //            Token token = emittingStateList.getNextToken();
    //
    //            // First get list of links to possible future states
    //            List successorList = getSuccessors(token);
    //            while (successorList.hasMoreEntries()) {
    //                UtteranceGraphEdge edge = successorList.getNextEntry();
    //
    //                //create a token for the future state, if its not already
    //                //in active list;
    //                //The active list will check for the key "edge.destination()"
    //                //in both of its lists
    //                if (nextActiveList.hasState(edge.destination())) {
    //                    Token newToken = 
    //                          nextActiveList.getTokenForState(edge.destination());
    //		} else {
    //                    Token newToken = new Token(edge.destination());
    //		}
    //
    //                //create a link between current state and future state
    //                TrainerLink newlink = new TrainerLink(edge, token, newToken);
    //                newlink.logScore = token.logScore + edge.transition.logprob();
    //
    //                //add link to the appropriate lists for source and
    //                //destination tokens
    //                token.addOutGoingLink(newlink);
    //
    //                newToken.addIncomingLink(newlink);
    //                newToken.alpha = logAdd(newToken.alpha, newlink.logScore);
    //
    //                /* At this point, we have placed a new token in the
    //                 * successor state, and linked the token at the
    //                 * current state to the token at the non-emitting states.
    //                 *
    //                 * Add token to appropriate active list
    //                 */
    //                nextActiveList.add(newToken);
    //            }
    //        }
    //    }


/*
[
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

]


expandToEmittingStateList(List tokenList){
   List emittingTokenList = new List();
   do {
        List nonEmittingTokenList = new List();
        expandtokens(newtokenlist, emittingTokenList, nonemittingTokenList);
   while (nonEmittingTokenList.length() != 0);
   return emittingTokenList;
}


expandtokens(List tokens, List nonEmittingStateList, List EmittingStateList){
   while (moreTokens){
       sucessorlist = SentenceHMM.gettransitions(nextToken());
       while (moretransitions()){
            transition = successor;
            State destinationState = successor.state;
            newtoken = gettokenfromHash(destinationState, currenttimestamp);
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
