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

import edu.cmu.sphinx.knowledge.acoustic.AcousticModel;
import edu.cmu.sphinx.knowledge.acoustic.TrainerAcousticModel;
import edu.cmu.sphinx.knowledge.acoustic.TrainerScore;
import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Utilities;

import java.io.IOException;
import java.util.List;
import java.util.Iterator;


/**
 * This is a dummy implementation of a TrainManager.
 */
public class SimpleTrainManager implements TrainManager {

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
	controlFile = new SimpleControlFile(context);
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
    protected void copyModels(String context) throws IOException {
	loadModels(context);
	saveModels(context);
    }

     /** 
      * Loads the acoustic models.
      *
      * @param context the context of this TrainManager
      *
      * @throws IOException if an error occurs while loading the data
      */
    protected void saveModels(String context) throws IOException {
	if (1 == models.length) {
	    models[0].save(null);
	} else {
	    String name;
	    List modelList = AcousticModel.getNames(context);
	    for (Iterator i = modelList.listIterator(); i.hasNext();) {
		name = (String) i.next();
		TrainerAcousticModel model = 
		    TrainerAcousticModel.getTrainerAcousticModel(name, 
								 context);
		model.save(name);
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
        
	models = getAcousticModels(context);
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
	TrainerScore score;
        props = SphinxProperties.getSphinxProperties(context);
        dumpMemoryInfo = props.getBoolean(PROP_DUMP_MEMORY_INFO,
                                          PROP_DUMP_MEMORY_INFO_DEFAULT);
        
        dumpMemoryInfo("TrainManager start");
        
        models = getAcousticModels(context);
	for (int m = 0; m < models.length; m++) {
	    models[m].initialize();

	    Learner learner = new FlatInitializerLearner(props);
	    for (controlFile.startUtteranceIterator();
		 controlFile.hasMoreUtterances(); ) {
		Utterance utterance = controlFile.nextUtterance();
		learner.setUtterance(utterance);
		while ((score = learner.getScore()) != null) {
		    models[m].accumulate(score);
		}
	    }
	    models[m].normalize();
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
    protected TrainerAcousticModel[] getAcousticModels(String context)
	throws IOException {
	List modelNames = AcousticModel.getNames(context);
	TrainerAcousticModel[] models;
	if (modelNames.size() == 0) {
	    models = new TrainerAcousticModel[1];
	    models[0] = TrainerAcousticModel.getTrainerAcousticModel(context);
	} else {
	    models = new TrainerAcousticModel[modelNames.size()];
	    int m = 0;
	    for (Iterator i = modelNames.iterator(); i.hasNext(); m++) {
		String modelName = (String) i.next();
		models[m] = 
		    TrainerAcousticModel.getTrainerAcousticModel(modelName, 
								 context);
	    }
	}
	return models;
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
