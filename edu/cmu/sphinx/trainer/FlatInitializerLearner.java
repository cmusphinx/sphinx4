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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.FrontEndFactory;
import edu.cmu.sphinx.frontend.Signal;
import edu.cmu.sphinx.frontend.util.StreamCepstrumSource;
import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.trainer.TrainerAcousticModel;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.trainer.TrainerScore;
import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Utilities;

/**
 * Provides mechanisms for computing statistics given a set of states
 * and input data.
 */
public class FlatInitializerLearner implements Learner {


    private final static String PROP_PREFIX = 
	"edu.cmu.sphinx.trainer.";


    /**
     * The SphinxProperty name for the input data type.
     */
    public final static String PROP_INPUT_TYPE = PROP_PREFIX+"inputDataType";


    /**
     * The default value for the property PROP_INPUT_TYPE.
     */
    public final static String PROP_INPUT_TYPE_DEFAULT = "cepstrum";

    /**
     * The sphinx property for the front end class.
     */
    public final static String PROP_FRONT_END = PROP_PREFIX + "frontend";


    /**
     * The default value of PROP_FRONT_END.
     */
    public final static String PROP_FRONT_END_DEFAULT
        = "edu.cmu.sphinx.frontend.SimpleFrontEnd";


    private FrontEnd frontEnd;
    private DataProcessor dataSource;
    private String context;
    private String inputDataType;
    private SphinxProperties props;
    private Data curFeature;

    /**
     * Constructor for this learner.
     */
    public FlatInitializerLearner(SphinxProperties props)
	throws IOException {
	this.props = props;
	context = props.getContext();
	initialize();
    }

    /**
     * Initializes the Learner with the proper context and frontend.
     *
     * @throws IOException
     */
    private void initialize() throws IOException  {
	inputDataType = props.getString(PROP_INPUT_TYPE, 
                                        PROP_INPUT_TYPE_DEFAULT);
	if (inputDataType.equals("audio")) {
	    dataSource = new StreamDataSource();
	    dataSource.initialize("batchAudioSource", null, props, null);
	} else if (inputDataType.equals("cepstrum")) {
	    dataSource = new StreamCepstrumSource();
	    dataSource.initialize("batchCepstrumSource", null, props, null);
	} else {
	    throw new Error("Unsupported data type: " + inputDataType + "\n" +
			    "Only audio and cepstrum are supported\n");
	}

	frontEnd = getFrontEnd();
    }

    // Cut and paste from e.c.s.d.Recognizer.java
    /**
     * Initialize and return the frontend based on the given sphinx
     * properties.
     */
    protected FrontEnd getFrontEnd() {
        String path = null;
        try {
	    Collection names = FrontEndFactory.getNames(props);
	    assert names.size() == 1;
	    FrontEnd fe = null;
	    for (Iterator i = names.iterator(); i.hasNext(); ) {
		String name = (String) i.next();
		fe = FrontEndFactory.getFrontEnd(props, name);
	    }
	    return fe;
        } catch (InstantiationException ie) {
            throw new Error("IE: Can't create front end " + path, ie);
        }
    }

    /**
     * Sets the learner to use a utterance.
     *
     * @param utterance the utterance
     *
     * @throws IOException
     */
    public void setUtterance(Utterance utterance) throws IOException {
	String file = utterance.toString();

        InputStream is = new FileInputStream(file);

	inputDataType = props.getString(PROP_INPUT_TYPE, 
                                        PROP_INPUT_TYPE_DEFAULT);

        if (inputDataType.equals("audio")) {
            ((StreamDataSource) dataSource).setInputStream(is, file);
        } else if (inputDataType.equals("cepstrum")) {
            boolean bigEndian = Utilities.isCepstraFileBigEndian(file);
            ((StreamCepstrumSource) dataSource).setInputStream(is, bigEndian);
        }
    }

    /**
     * Returns a single frame of speech.
     *
     * @return a feature frame
     *
     * @throws IOException
     */
    private boolean getFeature() {
	try {
	    curFeature = frontEnd.getData();

            if (curFeature == null) {
                return false;
            }

	    if (curFeature instanceof DataStartSignal) {
                curFeature = frontEnd.getData();
                if (curFeature == null) {
                    return false;
                }
            }

	    if (curFeature instanceof DataEndSignal) {
		return false;
	    }

            if (curFeature instanceof Signal) {
                throw new Error("Can't score non-content feature");
            }

	} catch (DataProcessingException dpe) {
	    System.out.println("DataProcessingException " + dpe);
	    dpe.printStackTrace();
	    return false;
	}

	return true;
    }


    /**
     * Starts the Learner.
     */
    public void start(){
    }

    /**
     * Stops the Learner.
     */
    public void stop(){
    }

    /**
     * Initializes computation for current utterance and utterance graph.
     *
     * @param utterance the current utterance
     * @param graph the current utterance graph
     *
     * @throws IOException
     */
    public void initializeComputation(Utterance utterance, 
		      UtteranceGraph graph)  throws IOException {
	setUtterance(utterance);
	setGraph(graph);
    }

    /**
     * Implements the setGraph method. Since the flat initializer does
     * not need a graph, this method produces an error.
     *
     * @param graph the graph
     */
    public void setGraph(UtteranceGraph graph) {
	new Error("Flat initializer does not use a graph!");
    }

    /**
     * Gets the TrainerScore for the next frame
     *
     * @return the TrainerScore
     */
    public TrainerScore[] getScore() {
	// If getFeature() is true, curFeature contains a valid
	// Feature. If not, a problem or EOF was encountered.
	if (getFeature()) {
	    // Since it's flat initialization, the probability is
	    // neutral, and the senone means "all senones".
	    TrainerScore[] score = new TrainerScore[1];
	    score[0] = new TrainerScore(curFeature, 0.0f,
					TrainerAcousticModel.ALL_MODELS);
	    return score;
	} else {
	    return null;
	}
    }

}
