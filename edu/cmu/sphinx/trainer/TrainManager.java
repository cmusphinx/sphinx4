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
     * The minimum relative improvement of the log likelihood
     * associated with the training data.
     */
    public final static String
	PROP_MINIMUM_IMPROVEMENT = PROP_PREFIX + "minimumImprovement";

    /**
     * Default value for minimumImprovement.
     */
    public final static float PROP_MINIMUM_IMPROVEMENT_DEFAULT = 0.2f;

    /**
     * The maximum number of iterations.
     */
    public final static String PROP_MAXIMUM_ITERATION =
	PROP_PREFIX + "maximumIteration";

    /**
     * The default value for maximumIteration.
     */
    public final static int PROP_MAXIMUM_ITERATION_DEFAULT = 15;

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

    /**
     * Initialize models.
     */
    public void initialize();
}
