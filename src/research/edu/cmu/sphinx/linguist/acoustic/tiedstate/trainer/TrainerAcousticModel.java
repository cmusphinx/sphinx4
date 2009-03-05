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

package edu.cmu.sphinx.linguist.acoustic.tiedstate.trainer;

import edu.cmu.sphinx.linguist.acoustic.tiedstate.Loader;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Saver;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.TiedStateAcousticModel;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Sphinx3Loader;
import edu.cmu.sphinx.util.TimerPool;
import edu.cmu.sphinx.util.props.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Logger;

/** Represents the generic interface to the Acoustic Model for sphinx4 */
public class TrainerAcousticModel extends TiedStateAcousticModel {

    /** Prefix for acoustic model SphinxProperties. */
    public final static String PROP_PREFIX = "edu.cmu.sphinx.linguist.acoustic.";


    @S4Component(type = Loader.class)
    public static final String LOADER = "loader";
    private Loader loader;

    @S4Component(type = Saver.class)
    public static final String SAVER = "saver";
    private Saver saver;


    @S4Double(defaultValue = Sphinx3Loader.PROP_VARIANCE_FLOOR_DEFAULT)
    public final static String PROP_VARIANCE_FLOOR = "varianceFloor";

    /** Mixture component score floor. */
    @S4Double(defaultValue = Sphinx3Loader.PROP_MC_FLOOR_DEFAULT)
    public final static String PROP_MC_FLOOR = "MixtureComponentScoreFloor";

    /** Mixture weight floor. */
    @S4Double(defaultValue = Sphinx3Loader.PROP_MW_FLOOR_DEFAULT)
    public final static String PROP_MW_FLOOR = "mixtureWeightFloor";


    /** The default value of PROP_LOCATION_SAVE. */
    public final static String PROP_LOCATION_SAVE_DEFAULT = ".";


    /**
     * The save format for the acoustic model data. Current supported formats are:
     * <p/>
     * sphinx3.ascii sphinx3.binary sphinx4.ascii sphinx4.binary
     */
    @S4String(defaultValue = "sphinx3.binary")
    public final static String PROP_FORMAT_SAVE = PROP_PREFIX + "format.save";


    /** Flag indicating all models should be operated on. */
    public final static int ALL_MODELS = -1;

    /** The logger for this class */
    private Logger logger;

    /** The pool manager */
    private HMMPoolManager hmmPoolManager;
    public String saveFormat;


    @Override
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        logger = ps.getLogger();

        loader = (Loader) ps.getComponent(LOADER);
        saver = (Saver) ps.getComponent(SAVER);

        hmmPoolManager = new HMMPoolManager(loader);
        loadTimer = TimerPool.getTimer(TIMER_LOAD);
        saveFormat = ps.getString(PROP_FORMAT_SAVE);

        logInfo();
    }


    /**
     * Saves the acoustic model with a given name and format
     *
     * @param name the name of the acoustic model
     * @throws IOException           if the model could not be loaded
     * @throws FileNotFoundException if the model does not exist
     */
    public void save(String name) throws IOException {
        saver.save(name, true);
        logger.info("saved models with " + saver);
    }


    /**
     * Loads the acoustic models. This has to be explicitly requested in this class.
     *
     * @throws IOException           if the model could not be loaded
     * @throws FileNotFoundException if the model does not exist
     */
    public void load() throws IOException, FileNotFoundException {
        loadTimer.start();
//        super.load();
        loadTimer.stop();
        logInfo();
        hmmPoolManager = new HMMPoolManager(loader);
    }


    /** Resets the buffers. */
    public void resetBuffers() {
        // Resets the buffers and associated variables.
        hmmPoolManager.resetBuffers();
    }


    /**
     * Accumulate the current TrainerScore into the buffers.
     *
     * @param index            the current index into the TrainerScore vector
     * @param trainerScore     the TrainerScore in the current frame
     * @param nextTrainerScore the TrainerScore in the next frame
     */
    public void accumulate(int index, TrainerScore[] trainerScore,
                           TrainerScore[] nextTrainerScore) {
        hmmPoolManager.accumulate(index, trainerScore, nextTrainerScore);
    }


    /**
     * Accumulate the current TrainerScore into the buffers.
     *
     * @param index        the current index into the TrainerScore vector
     * @param trainerScore the TrainerScore
     */
    public void accumulate(int index, TrainerScore[] trainerScore) {
        hmmPoolManager.accumulate(index, trainerScore);
    }


    /** Update the log likelihood. This should be called at the end of each utterance. */
    public void updateLogLikelihood() {
        hmmPoolManager.updateLogLikelihood();
    }


    /**
     * Normalize the buffers and update the models.
     *
     * @return the log likelihood for the whole training set
     */
    public float normalize() {
        float logLikelihood = hmmPoolManager.normalize();
        hmmPoolManager.update();
        return logLikelihood;
    }


}

