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

import edu.cmu.sphinx.linguist.acoustic.AcousticModel;
import edu.cmu.sphinx.linguist.acoustic.AcousticModelFactory;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.trainer.TrainerAcousticModel;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.trainer.TrainerScore;
import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Utilities;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;


/**
 * This is a dummy implementation of a TrainManager.
 */
class SimpleTrainManager implements TrainManager {

    private Learner learner;
    private ControlFile controlFile;
    private SphinxProperties props;       // sphinx properties
    private TrainerAcousticModel[] models;

    private boolean dumpMemoryInfo;

    /**
     * A SphinxProperty name for the boolean property that controls
     * whether or not the recognizer will display detailed memory
     * information while it is running. The default value is
     * <code>true</code>.
     */
    public final static String PROP_DUMP_MEMORY_INFO = 
        PROP_PREFIX + "dumpMemoryInfo";


    /**
     * The default value for the property PROP_DUMP_MEMORY_INFO.
     */
    public final static boolean PROP_DUMP_MEMORY_INFO_DEFAULT = false;

    /** 
     * Constructor for the class.
     */
    public SimpleTrainManager(String context) {
	//	controlFile = new SimpleControlFile(context);
	controlFile = null;
    }

    /**
     * Initializes the TrainManager with the proper context.
     */
    public void initialize() {
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
	assert controlFile != null;
	for (controlFile.startUtteranceIterator();
	     controlFile.hasMoreUtterances(); ) {
	    Utterance utterance = controlFile.nextUtterance();
	    System.out.println(utterance);
	    for (utterance.startTranscriptIterator();
		 utterance.hasMoreTranscripts(); ) {
		System.out.println(utterance.nextTranscript());
	    }
	}
    }

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
    public void copyModels(String context) throws IOException {
	loadModels(context);
	saveModels(context);
    }

     /** 
      * Saves the acoustic models.
      *
      * @param context the context of this TrainManager
      *
      * @throws IOException if an error occurs while loading the data
      */
    public void saveModels(String context) throws IOException {
	if (1 == models.length) {
	    models[0].save(null);
	} else {
	    String name;
	    Collection modelList = AcousticModelFactory.getNames(props);
	    for (Iterator i = modelList.iterator(); i.hasNext();) {
		name = (String) i.next();
		try {
		    AcousticModel model = 
			AcousticModelFactory.getModel(props, name);
		    if (model instanceof TrainerAcousticModel) {
			TrainerAcousticModel tmodel =
			    (TrainerAcousticModel) model;
			tmodel.save(name);
		    }
		} catch (InstantiationException ie) {
		    ie.printStackTrace();
		    throw new IOException("InstantiationException occurred.");
		}
	    }
	}
    }

     /** 
      * Loads the acoustic models.
      *
      * @param context the context of this TrainManager
      */
    private void loadModels(String context) throws IOException {

        props = SphinxProperties.getSphinxProperties(context);
        dumpMemoryInfo = props.getBoolean(PROP_DUMP_MEMORY_INFO,
                                          PROP_DUMP_MEMORY_INFO_DEFAULT);
        
        dumpMemoryInfo("TrainManager start");
        
	models = getTrainerAcousticModels(context);
	for (int m = 0; m < models.length; m++) {
		models[m].load();
	}
        dumpMemoryInfo("acoustic model");

    }


     /** 
      * Initializes the acoustic models.
      *
      * @param context the context of this TrainManager
      */
    public void initializeModels(String context) throws IOException {
	TrainerScore score[];
        props = SphinxProperties.getSphinxProperties(context);
        dumpMemoryInfo = props.getBoolean(PROP_DUMP_MEMORY_INFO,
                                          PROP_DUMP_MEMORY_INFO_DEFAULT);
        
        dumpMemoryInfo("TrainManager start");
        
        models = getTrainerAcousticModels(context);
	for (int m = 0; m < models.length; m++) {

	    learner = new FlatInitializerLearner(props);
	    if (controlFile == null) {
		controlFile = new SimpleControlFile(context);
	    }
	    for (controlFile.startUtteranceIterator();
		 controlFile.hasMoreUtterances(); ) {
		Utterance utterance = controlFile.nextUtterance();
		learner.setUtterance(utterance);
		while ((score = learner.getScore()) != null) {
		    assert score.length == 1;
		    models[m].accumulate(0, score);
		}
	    }
	    // normalize() has a return value, but we can ignore it here.
	    float dummy = models[m].normalize();
	}
        dumpMemoryInfo("acoustic model");
    }


    /**
     * Gets an array of models.
     *
     * @param context the context of interest
     *
     * @return the AcousticModel(s) used by this Recognizer, not initialized
     */
    protected TrainerAcousticModel[] getTrainerAcousticModels(String context)
	throws IOException {
        SphinxProperties props = SphinxProperties.getSphinxProperties(context);
        List modelList = new ArrayList();
	Collection modelNames = AcousticModelFactory.getNames(props);

        for (Iterator i = modelNames.iterator(); i.hasNext();) {
            String modelName = (String) i.next();
	    try {
		AcousticModel model =
		    AcousticModelFactory.getModel(props, modelName);
		if (model instanceof TrainerAcousticModel) {
		    modelList.add(model);
		}
	    } catch (InstantiationException ie) {
		ie.printStackTrace();
		throw new IOException("InstantiationException occurred.");
	    }
        }
	return (TrainerAcousticModel[]) 
            modelList.toArray(new TrainerAcousticModel[modelList.size()]);
    }

    /**
     * Trains context independent models. If the initialization stage
     * was skipped, it loads models from files, automatically.
     *
     * @param context the context of this train manager.
     *
     * @throws IOException
     */
    public void trainContextIndependentModels(String context) 
	throws IOException {
	UtteranceGraph uttGraph;
	TranscriptGraph transcriptGraph;
	TrainerScore[] score;
	TrainerScore[] nextScore;
	float minimumImprovement;
	int maxIteration;

	// If initialization was performed, then learner should not be
	// null. Otherwise, we need to load the models.
	if (learner == null) {
	    loadModels(context);
	}
        props = SphinxProperties.getSphinxProperties(context);
        dumpMemoryInfo = props.getBoolean(PROP_DUMP_MEMORY_INFO,
                                          PROP_DUMP_MEMORY_INFO_DEFAULT);
        
	minimumImprovement = props.getFloat(PROP_MINIMUM_IMPROVEMENT,
					    PROP_MINIMUM_IMPROVEMENT_DEFAULT);
	maxIteration = props.getInt(PROP_MAXIMUM_ITERATION,
				    PROP_MAXIMUM_ITERATION_DEFAULT);

        dumpMemoryInfo("TrainManager start");

        assert models != null;
        models = getTrainerAcousticModels(context);
	for (int m = 0; m < models.length; m++) {
	    float logLikelihood;
	    float lastLogLikelihood = Float.MAX_VALUE;
	    float relativeImprovement = 100.0f;
	    learner = new BaumWelchLearner(props);
	    if (controlFile == null) {
		controlFile = new SimpleControlFile(context);
	    }
	    for (int iteration = 0; 
		 (iteration < maxIteration) && 
		     (relativeImprovement > minimumImprovement);
		 iteration++) {
		System.out.println("Iteration: " + iteration);
		models[m].resetBuffers();
		for (controlFile.startUtteranceIterator();
		     controlFile.hasMoreUtterances(); ) {
		    Utterance utterance = controlFile.nextUtterance();
		    uttGraph = 
			new UtteranceHMMGraph(context, utterance, models[m]);
		    learner.setUtterance(utterance);
		    learner.setGraph(uttGraph);
		    nextScore = null;
		    while ((score = learner.getScore()) != null) {
			for (int i = 0; i < score.length; i++) {
			    if (i > 0) {
				models[m].accumulate(i, score, nextScore);
			    } else {
				models[m].accumulate(i, score);
			    }
			}
			nextScore = score;
		    }
		    models[m].updateLogLikelihood();
		}
		logLikelihood = models[m].normalize();
		System.out.println("Loglikelihood: " + logLikelihood);
		saveModels(context);
		if (iteration > 0) {
		    if (lastLogLikelihood != 0) {
			relativeImprovement = 
			    (logLikelihood - lastLogLikelihood) /
			    lastLogLikelihood * 100.0f;
		    } else if (lastLogLikelihood == logLikelihood) {
			relativeImprovement = 0;
		    }
		    System.out.println("Finished iteration: " + iteration + 
				       " - Improvement: " + 
				       relativeImprovement);
		}
		lastLogLikelihood = logLikelihood;
	    }
	}
    }


    /**
     * Conditional dumps out memory information
     *
     * @param what an additional info string
     */
    private void dumpMemoryInfo(String what) {
        if (dumpMemoryInfo) {
            Utilities.dumpMemoryInfo(what);
        }
    }


}
