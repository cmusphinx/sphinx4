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


import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.knowledge.acoustic.*;


/**
 * Provides mechanisms for computing statistics given a set of states
 * and input data.
 */
public abstract class BaumWelchLearner implements Learner {

    private FrontEnd frontEnd;

    /**
     * Initializes the Learner with the proper context and frontend.
     *
     * @param context the context to use
     * @param frontend the FrontEnd to use
     */
    public void initialize(String context, FrontEnd frontend){
	this.frontEnd = frontend;
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
     * Initializes computation for current SentenceHMM.
     *
     * @param sentenceHMM sentence HMM being processed
     */
    public void initializeComputation(SentenceHMM sentenceHMM){
    }

    /**
     * Gets posterior probabilities for a given state.
     *
     * @param stateID state ID number, relative to the sentence HMM
     */
    public double getScore(int stateID){
	return 0;
    }
    /*
// Cha chaaaang..

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
		// 			  totalprobability;
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
