
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


package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.IDGenerator;
import edu.cmu.sphinx.util.SphinxProperties;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;


/**
 * Extracts Features from a block of Cepstrum. This FeatureExtractor
 * expects Cepstrum that are MelFrequency cepstral coefficients (MFCC).
 * This FeatureExtractor takes in multiple Cepstrum(a) and outputs a 
 * FeatureFrame. This is a 1-to-1 processor.
 *
 * <p>The Sphinx properties that affect this processor are: <pre>
 * edu.cmu.sphinx.frontend.featureExtractor.featureLength
 * edu.cmu.sphinx.frontend.featureExtractor.windowSize
 * edu.cmu.sphinx.frontend.featureExtractor.cepstraBufferSize
 * </pre>
 *
 * @see Cepstrum
 * @see Feature
 */
public class S3FeatureExtractor extends DataProcessor 
    implements FeatureExtractor {

    /**
     * The name of the SphinxProperty for the window of the
     * S3FeatureExtractor, which has a default value of 3.
     */
    private static final String PROP_FEATURE_WINDOW =
	FeatureExtractor.PROP_PREFIX + "windowSize";
    
    /**
     * The name of the SphinxProperty for the feature type to compute.
     */
    private static final String PROP_FEATURE_TYPE =
	FeatureExtractor.PROP_PREFIX + "featureType";

    /**
     * The name of the SphinxProperty for the size of the circular
     * Cepstra buffer, which has a default value of 256.
     */
    private static final String PROP_CEP_BUFFER_SIZE =
	FeatureExtractor.PROP_PREFIX + "cepstraBufferSize";

    private int featureBlockSize = 25;
    private int featureLength;
    private int cepstrumLength;

    private int cepstraBufferSize;
    private int cepstraBufferEdge;
    private float[][] cepstraBuffer;

    private int bufferPosition;
    private int currentPosition;
    private int window;
    private int jp1, jp2, jp3, jf1, jf2, jf3;
    private boolean segmentStart;
    private IDGenerator featureID;

    private CepstrumSource predecessor;
    private List outputQueue;
    private Feature lastFeature;

    private Utterance currentUtterance;


    /**
     * Constructs a default S3FeatureExtractor.
     */
    public S3FeatureExtractor() {}


    /**
     * Constructs a default S3FeatureExtractor.
     *
     * @param name the name of this S3FeatureExtractor
     * @param context the context of interest
     * @param props the SphinxProperties to use
     * @param predecessor the CepstrumSource to get Cepstrum from
     */
    public S3FeatureExtractor(String name, String context, 
				  SphinxProperties props,
				  CepstrumSource predecessor) {
	initialize(name, context, props, predecessor);
    }

    
    /**
     * Initializes this S3FeatureExtractor.
     *
     * @param name the name of this S3FeatureExtractor
     * @param context the context of interest
     * @param props the SphinxProperties to use
     * @param predecessor the CepstrumSource to get Cepstrum from
     */
    public void initialize(String name, String context, SphinxProperties props,
			   CepstrumSource predecessor) {
	super.initialize(name, context, props);
        setProperties();
        this.predecessor = predecessor;
        cepstraBuffer = new float[cepstraBufferSize][];
        outputQueue = new Vector();
        featureID = new IDGenerator();
        reset();
    }


    /**
     * Resets the S3FeatureExtractor to be ready to read the next segment
     * of data. 
     */
    private void reset() {
        segmentStart = true;
        featureID.reset();
        bufferPosition = 0;
        currentPosition = 0;
    }


    /**
     * Reads the parameters needed from the the static SphinxProperties object.
     */
    private void setProperties() {
	SphinxProperties properties = getSphinxProperties();
	featureLength = properties.getInt(PROP_FEATURE_LENGTH, 39);
	window = properties.getInt(PROP_FEATURE_WINDOW, 3);
	cepstrumLength = properties.getInt
	    (FrontEnd.PROP_PREFIX + FrontEnd.PROP_CEPSTRUM_SIZE, 13);
        cepstraBufferSize = properties.getInt(PROP_CEP_BUFFER_SIZE, 256);
        cepstraBufferEdge = cepstraBufferSize - 8;
    }


    /**
     * Returns the next Feature object produced by this S3FeatureExtractor.
     *
     * @return the next available Feature object, returns null if no
     *     Feature object is available
     *
     * @throws java.io.IOException if there is an error reading
     * the Feature objects
     *
     * @see Feature
     * @see FeatureFrame
     */
    public Feature getFeature() throws IOException {
        if (outputQueue.size() == 0) {
	    Cepstrum input = predecessor.getCepstrum();
	    
	    if (input == null) {
		return null;
	    } else {
		if (input.hasContent()) {
		    addCepstrumData(input.getCepstrumData());
		    if (segmentStart) {
			currentUtterance = input.getUtterance();
			// we must have at least (window * 2 + 1)
			// cepstra in order to recognize anything
			if (readFirstCepstra() < (window * 2)) {
			    return null;
			}
			segmentStart = false;
			Feature firstFeature = computeNextFeature();
			outputQueue.add(firstFeature);
			replicateOutputFeature(firstFeature);
		    } else {
			outputQueue.add(computeNextFeature());
		    }
		    
		} else if (input.hasSignal(Signal.UTTERANCE_START)) {
		    segmentStart = true;
		    outputQueue.add(new Feature(Signal.UTTERANCE_START,
						IDGenerator.NON_ID));
		    
		} else if (input.hasSignal(Signal.UTTERANCE_END)) {
		    // when the UTTERANCE_END is right at the boundary
		    replicateOutputFeature(lastFeature);
		    outputQueue.add(new Feature(Signal.UTTERANCE_END,
						IDGenerator.NON_ID));
		}
            }
        }
        if (outputQueue.size() > 0) {
	    lastFeature = (Feature) outputQueue.remove(0);
	    return lastFeature;
        } else {
            return null;
        }
    }


    /**
     * Reads the second to (window*2 + 1) cepstra of this Utterance.
     * Also sets the currentPosition currently.
     *
     * @return the number of cepstra read
     */
    private int readFirstCepstra() throws IOException {

	int readCepstra = 0;

	// read the first window cepstra
	for (; readCepstra < window; readCepstra++) {
	    Cepstrum cepstrum = predecessor.getCepstrum();
	    addCepstrumData(cepstrum.getCepstrumData());
	}

        bufferPosition = window + 1;
        bufferPosition %= cepstraBufferSize;
        currentPosition = window;
        currentPosition %= cepstraBufferSize;
        
        jp1 = currentPosition - 1;
        jp2 = currentPosition - 2;
        jp3 = currentPosition - 3;
        jf1 = currentPosition + 1;
        jf2 = currentPosition + 2;
        jf3 = currentPosition + 3;
        
        if (jp3 > cepstraBufferEdge) {
            jf3 %= cepstraBufferSize;
            jf2 %= cepstraBufferSize;
            jf1 %= cepstraBufferSize;
            jp1 %= cepstraBufferSize;
            jp2 %= cepstraBufferSize;
            jp3 %= cepstraBufferSize;
        }

	// read the next window number of cepstra
	for (; readCepstra < (window * 2); readCepstra++) {
	    Cepstrum cepstrum = predecessor.getCepstrum();
	    addCepstrumData(cepstrum.getCepstrumData());
	}

	// if this utterance has too few cepstra, throw an 
	// IllegalStateException
	if (readCepstra < (window * 2)) {
	    throw new IllegalStateException
		("Utterance has less than " + (window * 2 + 1) + " cepstra");
	}

	return readCepstra;
    }


    /**
     * Replicates the given feature, and add them to the outputQueue.
     */
    private void replicateOutputFeature(Feature feature) {
	for (int i = 0; i < window; i++) {
	    Feature clonedFeature = (Feature) feature.clone();
	    clonedFeature.setID(featureID.getNextID());
	    outputQueue.add(clonedFeature);
	}
    }


    /**
     * Adds the given Cepstrum to the cepstraBuffer.
     */
    private void addCepstrumData(float[] cepstrumData) {
        cepstraBuffer[bufferPosition++] = cepstrumData;
        bufferPosition %= cepstraBufferSize;
    }


    /**
     * Computes the next feature. Advances the pointers as well.
     */
    private Feature computeNextFeature() {

        float[] feature = new float[featureLength];

	float[] mfc3f = cepstraBuffer[jf3++];
	float[] mfc2f = cepstraBuffer[jf2++];
	float[] mfc1f = cepstraBuffer[jf1++];
        float[] current = cepstraBuffer[currentPosition++];
	float[] mfc1p = cepstraBuffer[jp1++];
	float[] mfc2p = cepstraBuffer[jp2++];
	float[] mfc3p = cepstraBuffer[jp3++];
	
	// CEP; copy all the cepstrum data
	int j = cepstrumLength;
	System.arraycopy(current, 0, feature, 0, j);
	
	// DCEP: mfc[2] - mfc[-2]
	for (int k = 0; k < mfc2f.length; k++) {
	    feature[j++] = (mfc2f[k] - mfc2p[k]);
	}
	
	// D2CEP: (mfc[3] - mfc[-1]) - (mfc[1] - mfc[-3])
	for (int k = 0; k < mfc3f.length; k++) {
	    feature[j++] = (mfc3f[k] - mfc1p[k]) - (mfc1f[k] - mfc3p[k]);
	}

        if (jp3 > cepstraBufferEdge) {
            jf3 %= cepstraBufferSize;
            jf2 %= cepstraBufferSize;
            jf1 %= cepstraBufferSize;
            currentPosition %= cepstraBufferSize;
            jp1 %= cepstraBufferSize;
            jp2 %= cepstraBufferSize;
            jp3 %= cepstraBufferSize;
        }

        return (new Feature(feature, featureID.getNextID(), currentUtterance));
    }
}

