
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


package edu.cmu.sphinx.frontend.feature;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.DoubleData;

import edu.cmu.sphinx.util.SphinxProperties;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;


/**
 * Extracts Features from a block of Data. This FeatureExtractor
 * expects Data that are MelFrequency cepstral coefficients (MFCC).
 * This FeatureExtractor takes in multiple Data(a) and outputs a 
 * FeatureFrame. This is a 1-to-1 processor.
 *
 * <p>The Sphinx properties that affect this processor are: <pre>
 * edu.cmu.sphinx.frontend.FeatureExtractor.windowSize
 * edu.cmu.sphinx.frontend.FeatureExtractor.cepstraBufferSize
 * </pre>
 */
public class S3FeatureExtractor extends BaseDataProcessor {

    private static final String PROP_PREFIX
        = "edu.cmu.sphinx.frontend.feature.S3FeatureExtractor.";


    /**
     * The name of the SphinxProperty for the window size of the
     * S3FeatureExtractor, which has a default value of 3.
     */
    public static final String PROP_FEATURE_WINDOW 
        = PROP_PREFIX + "windowSize";


    /**
     * The default value of PROP_FEATURE_WINDOW.
     */
    public static final int PROP_FEATURE_WINDOW_DEFAULT = 3;


    private DoubleData[] cepstraBuffer;

    private int cepstraBufferSize;
    private int cepstraBufferEdge;
    private int featureBlockSize = 25;
    private int bufferPosition;
    private int currentPosition;
    private int window;
    private int jp1, jp2, jp3, jf1, jf2, jf3;

    private boolean segmentStart;
    private List outputQueue;
    private Data lastFeature;


    /**
     * Initializes this S3FeatureExtractor.
     *
     * @param name        the name of this S3FeatureExtractor
     * @param frontEnd    the front end this S3FeatureExtractor belongs to
     * @param props       the SphinxProperties to use
     * @param predecessor the predecessor to obtain Data objects from
     */
    public void initialize(String name, String frontEnd,
                           SphinxProperties props, DataProcessor predecessor) {
	super.initialize(name, frontEnd, props, predecessor);
        cepstraBufferSize = 256;
        setProperties(props);
        cepstraBuffer = new DoubleData[cepstraBufferSize];
        outputQueue = new Vector();
        reset();
    }


    /**
     * Resets the S3FeatureExtractor to be ready to read the next segment
     * of data. 
     */
    private void reset() {
        segmentStart = true;
        bufferPosition = 0;
        currentPosition = 0;
    }


    /**
     * Reads the parameters needed from the the static SphinxProperties object.
     */
    private void setProperties(SphinxProperties props) {
	window = props.getInt(getFullPropertyName(PROP_FEATURE_WINDOW), 
                              PROP_FEATURE_WINDOW_DEFAULT);
        cepstraBufferEdge = cepstraBufferSize - (window * 2 + 2);
    }


    /**
     * Returns the next feature Data object produced by this
     * S3FeatureExtractor.
     *
     * @return the next available Data object, returns null if no
     *         Data is available
     *
     * @throws DataProcessingException if a data processing error occurs
     */
    public Data getData() throws DataProcessingException {
        if (outputQueue.size() == 0) {
	    Data input = getPredecessor().getData();
            if (input == null) {
		return null;
	    } else {
		if (input instanceof DoubleData) {
		    addDoubleData((DoubleData) input);
		    if (segmentStart) {
                        // we must have at least (window * 2 + 1)
			// cepstra in order to recognize anything
			if (readFirstCepstra() < (window * 2)) {
			    return null;
			}
			segmentStart = false;
			Data firstFeature = computeNextFeature();
			outputQueue.add(firstFeature);
			replicateOutputFeature((DoubleData) firstFeature);
		    } else {
			outputQueue.add(computeNextFeature());
		    }
		} else if (input instanceof DataStartSignal) {
		    segmentStart = true;
		    outputQueue.add(input);		    
		} else if (input instanceof DataEndSignal) {
		    // when the DataEndSignal is right at the boundary
		    if (lastFeature instanceof DoubleData) {
                        replicateOutputFeature((DoubleData) lastFeature);
                    }
		    outputQueue.add(input);
		}
            }
        }
        if (outputQueue.size() > 0) {
	    lastFeature = (Data) outputQueue.remove(0);
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
    private int readFirstCepstra() throws DataProcessingException {

	int readCepstra = 0;

	// read the first window cepstra
	for (; readCepstra < window; readCepstra++) {
	    Data cepstrum = getPredecessor().getData();
	    addDoubleData((DoubleData) cepstrum);
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
	    Data cepstrum = getPredecessor().getData();
	    addDoubleData((DoubleData) cepstrum);
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
    private void replicateOutputFeature(DoubleData feature) {
	for (int i = 0; i < window; i++) {
	    DoubleData clonedFeature = (DoubleData) feature.clone();
            outputQueue.add(clonedFeature);
	}
    }


    /**
     * Adds the given Data to the cepstraBuffer.
     */
    private void addDoubleData(DoubleData cepstrum) {
        cepstraBuffer[bufferPosition++] = cepstrum;
        bufferPosition %= cepstraBufferSize;
    }


    /**
     * Computes the next feature. Advances the pointers as well.
     */
    private Data computeNextFeature() {

        DoubleData currentData = cepstraBuffer[currentPosition++];

	double[] mfc3f = cepstraBuffer[jf3++].getValues();
	double[] mfc2f = cepstraBuffer[jf2++].getValues();
	double[] mfc1f = cepstraBuffer[jf1++].getValues();
        double[] current = currentData.getValues();
	double[] mfc1p = cepstraBuffer[jp1++].getValues();
	double[] mfc2p = cepstraBuffer[jp2++].getValues();
	double[] mfc3p = cepstraBuffer[jp3++].getValues();

        double[] feature = new double[current.length * 3];
	
	// CEP; copy all the cepstrum data
	int j = current.length;
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

        return (new DoubleData(feature,
                               currentData.getCollectTime(),
                               currentData.getFirstSampleNumber()));
    }
}

