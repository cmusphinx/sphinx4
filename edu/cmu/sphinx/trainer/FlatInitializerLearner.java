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

import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.FeatureFrame;
import edu.cmu.sphinx.frontend.Feature;
import edu.cmu.sphinx.frontend.Signal;
import edu.cmu.sphinx.frontend.util.StreamAudioSource;
import edu.cmu.sphinx.frontend.util.StreamCepstrumSource;
import edu.cmu.sphinx.frontend.DataSource;

import edu.cmu.sphinx.knowledge.acoustic.TrainerAcousticModel;
import edu.cmu.sphinx.knowledge.acoustic.TrainerScore;

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

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
    private DataSource dataSource;
    private String context;
    private String inputDataType;
    private SphinxProperties props;
    private Feature curFeature;

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
     * @param utterance the current utterance
     *
     * @throws IOException
     */
    private void initialize() throws IOException  {

	inputDataType = props.getString(PROP_INPUT_TYPE, 
                                        PROP_INPUT_TYPE_DEFAULT);

	if (inputDataType.equals("audio")) {
	    dataSource = new StreamAudioSource
		("batchAudioSource", context, null, null);
	} else if (inputDataType.equals("cepstrum")) {
	    dataSource = new StreamCepstrumSource
		("batchCepstrumSource", context);
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
            path = props.getString(PROP_FRONT_END, PROP_FRONT_END_DEFAULT);
            FrontEnd fe = (FrontEnd)Class.forName(path).newInstance();
            fe.initialize("FrontEnd", context, dataSource);
            return fe;
        } catch (ClassNotFoundException fe) {
            throw new Error("CNFE:Can't create front end " + path, fe);
        } catch (InstantiationException ie) {
            throw new Error("IE: Can't create front end " + path, ie);
        } catch (IllegalAccessException iea) {
            throw new Error("IEA: Can't create front end " + path, iea);
        } catch (IOException ioe) {
            throw new Error("IOE: Can't create front end " + path + " "
                    + ioe, ioe);
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
            ((StreamAudioSource) dataSource).setInputStream(is, file);
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
     * @throw IOException
     */
    private boolean getFeature() {
	FeatureFrame ff;

	try {
	    ff = frontEnd.getFeatureFrame(1, null);

            if (!hasFeatures(ff)) {
                return false;
            }

	    curFeature = ff.getFeatures()[0];

	    if (curFeature.getSignal() == Signal.UTTERANCE_START) {
                ff = frontEnd.getFeatureFrame(1, null);
                if (!hasFeatures(ff)) {
                    return false;
                }
                curFeature = ff.getFeatures()[0];
            }

	    if (curFeature.getSignal() == Signal.UTTERANCE_END) {
		return false;
	    }

            if (!curFeature.hasContent()) {
                throw new Error("Can't score non-content feature");
            }

	} catch (IOException ioe) {
	    System.out.println("IO Exception " + ioe);
	    ioe.printStackTrace();
	    return false;
	}

	return true;
    }

    /**
     * Checks to see if a FeatureFrame is null or if there are Features in it.
     *
     * @param ff the FeatureFrame to check
     *
     * @return false if the given FeatureFrame is null or if there
     * are no Features in the FeatureFrame; true otherwise.
     */
    private boolean hasFeatures(FeatureFrame ff) {
        if (ff == null) {
            System.out.println("FlatInitializerLearner: FeatureFrame is null");
            return false;
        }
        if (ff.getFeatures() == null) {
            System.out.println
                ("FlatInitializerLearner: no features in FeatureFrame");
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
     * Initializes computation for current SentenceHMM.
     *
     * @param sentenceHMM sentence HMM being processed
     */
    public void initializeComputation(SentenceHMM sentenceHMM) {
    }

    /**
     * Gets the TrainerScore for the next frame
     *
     * @return the TrainerScore
     */
    public TrainerScore getScore() {
	// If getFeature() is true, curFeature contains a valid
	// Feature. If not, a problem or EOF was encountered.
	if (getFeature()) {
	    // Since it's flat initialization, the probability is
	    // neutral, and the senone means "all senones".
	    TrainerScore score = new TrainerScore(curFeature, 0.0f, 
				  TrainerAcousticModel.ALL_MODELS);
	    return score;
	} else {
	    return null;
	}
    }

}
