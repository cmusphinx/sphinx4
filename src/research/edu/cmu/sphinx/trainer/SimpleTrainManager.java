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
import edu.cmu.sphinx.linguist.acoustic.tiedstate.trainer.TrainerAcousticModel;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.trainer.TrainerScore;
import edu.cmu.sphinx.util.Utilities;
import edu.cmu.sphinx.util.props.*;

import java.io.IOException;
import java.util.List;


/** This is a dummy implementation of a TrainManager. */
class SimpleTrainManager implements TrainManager {

    private ControlFile controlFile;
    private TrainerAcousticModel[] models;

    private boolean dumpMemoryInfo;

    @S4Component(type = Learner.class)
    public static final String LEARNER = "learner";
    private Learner learner;

    @S4Component(type = Learner.class)
    public static final String INIT_LEARNER = "initLearner";
    private Learner initLearner;

    @S4ComponentList(type = AcousticModel.class)
    public static final String AM_COLLECTION = "learner";
    private List<? extends AcousticModel> acousticModels;

    /**
     * The property for the boolean property that controls whether or not the recognizer will display detailed
     * memory information while it is running. The default value is <code>true</code>.
     */
    @S4Boolean(defaultValue = false)
    public final static String DUMP_MEMORY_INFO = PROP_PREFIX + "dumpMemoryInfo";

    private int maxIteration;
    private float minimumImprovement;


    public void newProperties(PropertySheet ps) throws PropertyException {
        dumpMemoryInfo = ps.getBoolean(DUMP_MEMORY_INFO);
        learner = (Learner) ps.getComponent(LEARNER);
        initLearner = (Learner) ps.getComponent(INIT_LEARNER);

        minimumImprovement = ps.getFloat(PROP_MINIMUM_IMPROVEMENT);
        maxIteration = ps.getInt(PROP_MAXIMUM_ITERATION);

        acousticModels = ps.getComponentList(AM_COLLECTION, AcousticModel.class);
    }


    public void initialize() {
    }


    /** Starts the TrainManager. */
    public void start() {
    }


    /** Stops the TrainManager. */
    public void stop() {
    }


    /** Do the train. */
    public void train() {
        assert controlFile != null;
        for (controlFile.startUtteranceIterator();
             controlFile.hasMoreUtterances();) {
            Utterance utterance = controlFile.nextUtterance();
            System.out.println(utterance);
            for (utterance.startTranscriptIterator();
                 utterance.hasMoreTranscripts();) {
                System.out.println(utterance.nextTranscript());
            }
        }
    }


    /**
     * Copy the model.
     * <p/>
     * This method copies to model set, possibly to a new location and new format. This is useful if one wants to
     * convert from binary to ascii and vice versa, or from a directory structure to a JAR file. If only one model is
     * used, then name can be null.
     *
     * @param context this TrainManager's context
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
     * @throws IOException if an error occurs while loading the data
     */
    public void saveModels(String context) throws IOException {
        if (1 == models.length) {
            models[0].save(null);
        } else {
            for (AcousticModel model : acousticModels) {
                if (model instanceof TrainerAcousticModel) {
                    TrainerAcousticModel tmodel =
                            (TrainerAcousticModel) model;
                    tmodel.save(model.getName());
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
        dumpMemoryInfo("TrainManager start");

        models = getTrainerAcousticModels();
        for (TrainerAcousticModel model : models) {
            model.load();
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

        dumpMemoryInfo("TrainManager start");

        models = getTrainerAcousticModels();
        for (TrainerAcousticModel model : models) {

            if (controlFile == null) {
                controlFile = ConfigurationManager.getInstance(SimpleControlFile.class);
            }
            for (controlFile.startUtteranceIterator();
                 controlFile.hasMoreUtterances();) {
                Utterance utterance = controlFile.nextUtterance();
                initLearner.setUtterance(utterance);
                while ((score = learner.getScore()) != null) {
                    assert score.length == 1;
                    model.accumulate(0, score);
                }
            }

            // normalize() has a return value, but we can ignore it here.
            model.normalize();
        }
        dumpMemoryInfo("acoustic model");
    }


    /**
     * Gets an array of models.
     *
     * @return the AcousticModel(s) used by this Recognizer, not initialized
     */
    protected TrainerAcousticModel[] getTrainerAcousticModels()
            throws IOException {
        return models;
    }


    /**
     * Trains context independent models. If the initialization stage was skipped, it loads models from files,
     * automatically.
     *
     * @param context the context of this train manager.
     * @throws IOException
     */
    public void trainContextIndependentModels(String context)
            throws IOException {
        UtteranceGraph uttGraph;
        TrainerScore[] score;
        TrainerScore[] nextScore;

        // If initialization was performed, then learner should not be
        // null. Otherwise, we need to load the models.
        if (learner == null) {
            loadModels(context);
        }


        dumpMemoryInfo("TrainManager start");

        assert models != null;
        models = getTrainerAcousticModels();
        for (TrainerAcousticModel model : models) {
            float logLikelihood;
            float lastLogLikelihood = Float.MAX_VALUE;
            float relativeImprovement = 100.0f;
            if (controlFile == null) {
                controlFile = new SimpleControlFile();
            }
            for (int iteration = 0;
                 (iteration < maxIteration) &&
                     (relativeImprovement > minimumImprovement);
                 iteration++) {
                System.out.println("Iteration: " + iteration);
                model.resetBuffers();
                for (controlFile.startUtteranceIterator();
                     controlFile.hasMoreUtterances();) {
                    Utterance utterance = controlFile.nextUtterance();
                    uttGraph =
                        new UtteranceHMMGraph(context, utterance, model);
                    learner.setUtterance(utterance);
                    learner.setGraph(uttGraph);
                    nextScore = null;
                    while ((score = learner.getScore()) != null) {
                        for (int i = 0; i < score.length; i++) {
                            if (i > 0) {
                                model.accumulate(i, score, nextScore);
                            } else {
                                model.accumulate(i, score);
                            }
                        }
                        nextScore = score;
                    }
                    model.updateLogLikelihood();
                }
                logLikelihood = model.normalize();
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
