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
public interface  Learner {

    /**
     * Starts the Learner.
     */
    public void start();

    /**
     * Stops the Learner.
     */
    public void stop();

    /**
     * Initializes computation for current SentenceHMM.
     *
     * @param sentenceHMM sentence HMM being processed
     */
    public void initializeComputation(SentenceHMM sentenceHMM);

    /**
     * Sets the learner to use a utterance.
     *
     * @param utterance the utterance
     *
     * @throws IOException
     */
    public void setUtterance(Utterance utterance) throws IOException;

    /**
     * Gets posterior probabilities for a given state.
     */
    public TrainerScore getScore();
}
