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
 * Manages inputs and outputs to the other trainer classes.
 */
public interface TrainManager{

    /**
     * Prefix for SphinxProperties in this file.
     */
    public final static String PROP_PREFIX = 
	"edu.cmu.sphinx.trainer.Trainer.";

    /**
     * Initializes the TrainManager with the proper context.
     *
     * @param context the context to use
     * @param learner the Learner to use
     */
    public void initialize(String context, Learner learner);

    /**
     * Starts the TrainManager.
     */
    public void start();

    /**
     * Stops the TrainManager.
     */
    public void stop();

    /**
     * Do the train.
     */
    public void train();

}
