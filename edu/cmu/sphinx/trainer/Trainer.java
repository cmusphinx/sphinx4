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
import edu.cmu.sphinx.util.Timer;
import edu.cmu.sphinx.util.Utilities;

import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;

import java.io.IOException;

import java.net.URL;


/**
 * Trains models given a set of audio files.
 *
 * At this point, a very simple file that helps us debug the code.
 */
public class Trainer {

    /**
     * Prefix for SphinxProperties in this file.
     */
    public final static String PROP_PREFIX = "edu.cmu.sphinx.trainer.Trainer.";

    /**
     * The SphinxProperty name for the initial trainer stage to be processed.
     */
    public final static String PROP_INITIAL_STAGE = PROP_PREFIX + 
	"initialStage";

    /**
     * Default initial stage.
     */
    public final static String PROP_INITIAL_STAGE_DEFAULT = 
	"_00_INITIALIZATION";

    /**
     * The SphinxProperty name for the final trainer stage to be processed.
     */
    public final static String PROP_FINAL_STAGE = PROP_PREFIX + 
	"finalStage";

    /**
     * Default final stage.
     */
    public final static String PROP_FINAL_STAGE_DEFAULT = 
	"_40_TIED_CD_TRAIN";


    private String context;
    private String initialStage;
    private String finalStage;
    private boolean isStageActive = false;
    private List StageList = new LinkedList();
    private TrainManager trainManager;


    /**
     * Constructs a Trainer.
     *
     * @param context the context of this trainer.
     */
    public Trainer(String context) {
	this.context = context;
	SphinxProperties props = SphinxProperties.getSphinxProperties(context);
	init(props);
    }

    /**
     * Constructs a Trainer.
     *
     * @param props the sphinx properties to use
     */
    public Trainer(SphinxProperties props) {
	init(props);
    }

    /**
     * Common intialization code
     *
     * @param props the sphinx properties
     */
    private void init(SphinxProperties props) {
        context = props.getContext();
	trainManager = new SimpleTrainManager(context);
	initialStage = props.getString(PROP_INITIAL_STAGE, 
				       PROP_INITIAL_STAGE_DEFAULT);
	finalStage = props.getString(PROP_FINAL_STAGE, 
				       PROP_FINAL_STAGE_DEFAULT);
	StageList.add(Stage._00_INITIALIZATION);
	StageList.add(Stage._10_CI_TRAIN);
	StageList.add(Stage._20_UNTIED_CD_TRAIN);
	StageList.add(Stage._30_STATE_PRUNING);
	StageList.add(Stage._40_TIED_CD_TRAIN);
    }

    /**
     * Prints debugging info.
     */
    private void printAll() {
	trainManager.train();
    }

    /**
     * Process this stage.
     * 
     * @param stage the stage to process.
     */
    private void processStages() {
	if (!(StageList.contains(initialStage) && 
	    StageList.contains(finalStage))) {
	    return;
	}
	for (Iterator iterator = StageList.iterator();
	     iterator.hasNext();) {
	    Stage stage = (Stage) iterator.next();
	    if (!isStageActive) {
		if (initialStage.equals(stage)) {
		    isStageActive = true;
		}
	    }
	    if (isStageActive) {
		/*
		 * Not sure of an elegant way to do it. For each
		 * stage, it should call a different method.
		 *
		 * run();
		*/
		if (finalStage.equals(stage)) {
		    isStageActive = false;
		}
	    }
	}
    }

    /**
     * Main method of this Trainer.
     *
     * @param argv argv[0] : SphinxProperties file
     */
    public static void main(String[] argv) {

        if (argv.length < 1) {
            System.out.println
                ("Usage: Trainer propertiesFile");
            System.exit(1);
        }

        String context = "batch";
        String propertiesFile = argv[0];
        String pwd = System.getProperty("user.dir");

        try {
            SphinxProperties.initContext
                (context, new URL("file://" + pwd +  "/"
                                   + propertiesFile));

            Trainer trainer = new Trainer(context);
	    trainer.printAll();
	    trainer.processStages();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
