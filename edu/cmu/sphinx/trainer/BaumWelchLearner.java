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

/**
 * Provides mechanisms for computing statistics given a set of states
 * and input data.
 */
public class BaumWelchLearner implements Learner {

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
     * Initializes computation for current SentenceHMM.
     *
     * @param sentenceHMM sentence HMM being processed
     */
    public void initializeComputation(SentenceHMM sentenceHMM){
    }

    /**
     * Sets the learner to use a utterance.
     *
     * @param utterance the utterance
     *
     * @throws IOException
     */
    public void setUtterance(Utterance utterance) throws IOException {
    }

    /**
     * Gets posterior probabilities for a given state.
     *
     * @param stateID state ID number, relative to the sentence HMM
     */
    public TrainerScore getScore(){
        return null;
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
