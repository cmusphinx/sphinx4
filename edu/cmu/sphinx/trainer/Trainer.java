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
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.cmu.sphinx.util.SphinxProperties;


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
    private Set StageNames = new HashSet();

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
	addStage(Stage._00_INITIALIZATION);
	addStage(Stage._10_CI_TRAIN);
	addStage(Stage._20_UNTIED_CD_TRAIN);
	addStage(Stage._30_STATE_PRUNING);
	addStage(Stage._40_TIED_CD_TRAIN);
	addStage(Stage._90_CP_MODEL);
    }

    /**
     * Add Stage to a list of stages.
     *
     * @param stage the Stage to add
     */
    private void addStage(Stage stage) {
	StageList.add(stage);
	StageNames.add(stage.toString());
    }

    /**
     * Process this stage.
     * 
     * @param context this trainer's context
     */
    private void processStages(String context) {
	if (!(StageNames.contains(initialStage) && 
	    StageNames.contains(finalStage))) {
	    return;
	}
	for (Iterator iterator = StageList.iterator();
	     iterator.hasNext();) {
	    Stage stage = (Stage) iterator.next();
	    if (!isStageActive) {
		if (initialStage.equals(stage.toString())) {
		    isStageActive = true;
		}
	    }
	    if (isStageActive) {
		/*
		 * Not sure of an elegant way to do it. For each
		 * stage, it should call a different method.  Switch
		 * would be a good solution, but it works with int,
		 * and stage is of type Stage.
		 *
		 * run();
		*/
		try {
		    if (stage.equals(Stage._00_INITIALIZATION)) {
			System.out.println("00 - Initializing");
			trainManager.initializeModels(context);
			System.out.println("Saving");
			trainManager.saveModels(context);
		    } else if (stage.equals(Stage._10_CI_TRAIN)) {
			System.out.println("01 - CI train");
			trainManager.trainContextIndependentModels(context);
			System.out.println("Saving");
			trainManager.saveModels(context);
		    } else if (stage.equals(Stage._20_UNTIED_CD_TRAIN)) {
			System.out.println("02 - Untied CD train");
		    } else if (stage.equals(Stage._30_STATE_PRUNING)) {
			System.out.println("03 - State pruning");
		    } else if (stage.equals(Stage._40_TIED_CD_TRAIN)) {
			System.out.println("04 - Tied CD train");
		    } else if (stage.equals(Stage._90_CP_MODEL)) {
			System.out.println("Copying");
			trainManager.copyModels(context);
		    } else {
			assert false : "stage not implemented";
		    }
		} catch (IOException ioe) {
		    ioe.printStackTrace();
		    throw new Error("IOE: Can't finish trainer " + ioe, ioe);
		}

		if (finalStage.equals(stage.toString())) {
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

        String context = "trainer";
        String propertiesFile = argv[0];
        String pwd = System.getProperty("user.dir");

        try {
            SphinxProperties.initContext
                (context, new URL("file:///" + pwd +  "/"
                                   + propertiesFile));

            Trainer trainer = new Trainer(context);
	    trainer.processStages(context);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
