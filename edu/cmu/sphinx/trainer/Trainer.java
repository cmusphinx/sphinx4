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

import java.io.IOException;

import java.net.URL;


/**
 * Trains models given a set of audio files.
 *
 * At this point, a very simple file that helps us debug the code.
 */
public class Trainer {

    private final static String PROP_PREFIX = 
	"edu.cmu.sphinx.trainer.Trainer.";

    /**
     * The SphinxProperty name for how many files to skip.
     */
    public final static String PROP_WHICH_BATCH = PROP_PREFIX + "whichBatch";
    public final static String PROP_TOTAL_BATCHES 
	= PROP_PREFIX + "totalBatches";

    private String context;
    private int whichBatch;
    private int totalBatches;
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
	whichBatch = props.getInt(PROP_WHICH_BATCH, 0);
	totalBatches = props.getInt(PROP_TOTAL_BATCHES, 1);

	trainManager = new SimpleTrainManager(context);
    }

    /**
     * Prints debugging info.
     */
    private void printAll() {
	trainManager.train();
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
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
