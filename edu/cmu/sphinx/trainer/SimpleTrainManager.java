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


/**
 * This is a dummy implementation of a TrainManager.
 */
public class SimpleTrainManager implements TrainManager {

    /** 
     * Constructor for the class.
     */
    public SimpleTrainManager(String context) {
    }

    /**
     * Initializes the TrainManager with the proper context.
     *
     * @param context the context to use
     * @param learner the Learner to use
     * @param sentenceHMM the SentenceHMM to use
     */
    public void initialize(String context, Learner learner, 
			   SentenceHMM sentenceHMM) {
    }

    /**
     * Starts the TrainManager.
     */
    public void start() {
    }

    /**
     * Stops the TrainManager.
     */
    public void stop() {
    }

    /**
     * Do the train.
     */
    public void train() {
    }
}
