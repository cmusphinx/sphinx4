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

}
