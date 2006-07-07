/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.trainer;

import java.io.IOException;

/**
 * Manages inputs and outputs to the other trainer classes.
 */
interface TrainManager{

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
     * Initialize the TrainManager.
     */
    public void initialize();

     /** 
      * Saves the acoustic models.
      *
      * @param context the context of this TrainManager
      *
      * @throws IOException if an error occurs while loading the data
      */
    void saveModels(String context) throws IOException;

    /** 
     * Copy the model.
     *
     * This method copies to model set, possibly to a new location and
     * new format. This is useful if one wants to convert from binary
     * to ascii and vice versa, or from a directory structure to a JAR
     * file. If only one model is used, then name can be null.
     *
     * @param context this TrainManager's context
     *
     * @throws IOException if an error occurs while loading the data
     */
    void copyModels(String context) throws IOException;

     /** 
      * Initializes the acoustic models.
      *
      * @param context the context of this TrainManager
      */
    void initializeModels(String context) throws IOException;

    /**
     * Trains context independent models. If the initialization stage
     * was skipped, it loads models from files, automatically.
     *
     * @param context the context of this train manager.
     *
     * @throws IOException
     */
    void trainContextIndependentModels(String context) throws IOException;
}
