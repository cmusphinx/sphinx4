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
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.DoubleData;

import edu.cmu.sphinx.util.IDGenerator;
import edu.cmu.sphinx.util.SphinxProperties;

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
 * edu.cmu.sphinx.frontend.FeatureExtractor.windowSize
 * edu.cmu.sphinx.frontend.FeatureExtractor.cepstraBufferSize
 * </pre>
 *
 * @see Cepstrum
 * @see Feature
 */
public class DeltasFeatureExtractor extends BaseDataProcessor {

    private static final String PROP_PREFIX
        = "edu.cmu.sphinx.frontend.feature.DeltasFeatureExtractor.";


    /**
     * The name of the SphinxProperty for the window of the
     * DeltasFeatureExtractor.
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
    private int bufferPosition;
    private int currentPosition;
    private int featureBlockSize = 25;
    private int window;
    private int jp1, jp2, jp3, jf1, jf2, jf3;

    private Data dataEndSignal;

    private boolean segmentStart;
    private boolean segmentEnd;

    private List outputQueue;


    /**
     * Constructs a default DeltasFeatureExtractor.
     *
     * @param name        the name of this DeltasFeatureExtractor
     * @param frontEnd    the front end this DeltasFeatureExtractor belongs
     * @param props       the SphinxProperties to use
     * @param predecessor the CepstrumSource to get Cepstrum from
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
     * Resets the DeltasFeatureExtractor to be ready to read the next segment
     * of data. 
     */
    private void reset() {
        segmentStart = true;
        segmentEnd = false;
        bufferPosition = 0;
        currentPosition = 0;
    }


    /**
     * Reads the parameters needed from the the static SphinxProperties object.
     *
     * @param props the SphinxProperties to read properties from
     */
    private void setProperties(SphinxProperties props) {
	window = props.getInt(getFullPropertyName(PROP_FEATURE_WINDOW),
                              PROP_FEATURE_WINDOW_DEFAULT);
        cepstraBufferEdge = cepstraBufferSize - (window * 2 + 2);
    }


    /**
     * Returns the next Data object produced by this DeltasFeatureExtractor.
     * The output Data objects are the delta and double delta of the 
     * input Data objects.
     *
     * @return the next available Data object, returns null if no
     *         Data object is available
     *
     * @throws DataProcessingException if a data processing error occurs
     */
    public Data getData() throws DataProcessingException {

        if (outputQueue.size() == 0) {

            if (segmentEnd) {
                segmentEnd = false;
                outputQueue.add(dataEndSignal);
                dataEndSignal = null;
            } else {
                Data input = getPredecessor().getData();
                if (input == null) {
                    return null;
                } else {
                    if (input instanceof DoubleData) {
                        // "featureBlockSize-1" since first Cepstrum
                        // already read
                        int numberFeatures = 
                            readCepstra(featureBlockSize - 1, input);
                        if (numberFeatures > 0) {
                            computeFeatures(numberFeatures);
                        }
                    } else if (input instanceof DataStartSignal) {
                        segmentStart = true;
                        outputQueue.add(input);
                    } else if (input instanceof DataEndSignal) {
			// when the UTTERANCE_END is right at the boundary
			segmentEnd = false;
			outputQueue.add(input);
		    }
                }
            }
        }
        if (outputQueue.size() > 0) {
	    Data feature = (Data) outputQueue.remove(0);
	    // System.out.println(feature);
            return feature;
        } else {
            return null;
        }
    }

    /**
     * Reads the given number of Data and insert them into the cepstraBuffer.
     *
     * Returns the total number of features that should result from
     * the read Data.
     *
     * @param numberCepstra the number of Data objects to read
     * @param firstCepstrum the first Data object
     *
     * @return the number of features that should be computed using
     *         the Data read
     *
     * @throws DataProcessingException if a data processing error occurs
     */
    private int readCepstra(int numberCepstra, Data firstCepstrum) throws
	DataProcessingException {

        int residualVectors = 0;
        int cepstraRead = 0;

        // replicate the first cepstrum of a segment
        if (segmentStart) {
            residualVectors -= setStartCepstrum(firstCepstrum);
            segmentStart = false;
            cepstraRead++;
        } else if (firstCepstrum instanceof DoubleData) {
            addCepstrum((DoubleData) firstCepstrum);
            cepstraRead++;
        }

        boolean done = false;

        // read the cepstra
        while (!done && cepstraRead < numberCepstra) {
            Data cepstrum = getPredecessor().getData();
            if (cepstrum != null) {
                if (cepstrum instanceof DoubleData) {
                    // just a cepstra
                    addCepstrum((DoubleData) cepstrum);
                    cepstraRead++;
                } else if (cepstrum instanceof DataEndSignal) {
                    // end of segment cepstrum
                    segmentEnd = true;
                    dataEndSignal = cepstrum;
                    residualVectors += replicateLastCepstrum();
                    done = true;
                    break;
                }
            } else {
                done = true;
                break;
            }
        }
                    
        return (cepstraRead + residualVectors);
    }


    /**
     * Replicate the given cepstrum into the first window+1
     * number of frames in the cepstraBuffer. This is the first cepstrum
     * in the segment.
     *
     * @param cepstrum the cepstrum to replicate
     *
     * @return the number extra cepstra replicated
     */
    private int setStartCepstrum(Data cepstrum) {

        Arrays.fill(cepstraBuffer, 0, window+1, cepstrum);
        
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

        return window;
    }


    /**
     * Adds the given DoubleData to the cepstraBuffer.
     *
     * @param cepstrum the DoubleData object to add
     */
    private void addCepstrum(DoubleData cepstrum) {
        cepstraBuffer[bufferPosition++] = cepstrum;
        bufferPosition %= cepstraBufferSize;
    }


    /**
     * Replicate the last frame into the last window number of frames in
     * the cepstraBuffer.
     *
     * @return the number of replicated cepstral Data objects
     */
    private int replicateLastCepstrum() {

        int replicated = 0;

        if (bufferPosition > 0) {
            DoubleData last = this.cepstraBuffer[bufferPosition - 1];
            
            if (bufferPosition + window < cepstraBufferSize) {
                Arrays.fill(cepstraBuffer, bufferPosition, 
                            bufferPosition + window, last);
            } else {
                for (int i = 0; i < window; i++) {
                    addCepstrum(last);
                }
            }
            replicated = window;
        }

        return replicated;
    }


    /**
     * Converts the cepstral data in the cepstraBuffer into a FeatureFrame.
     *
     * @param totalFeatures the number of features (Data objects) to produce
     */
    private void computeFeatures(int totalFeatures) {

        getTimer().start();

        assert(0 < totalFeatures && totalFeatures < cepstraBufferSize);

	// create the Features
	for (int i = 0; i < totalFeatures; i++) {
            Data feature = computeNextFeature();
            if (feature != null) {
                outputQueue.add(feature);
            }
	}

        getTimer().stop();
    }

    /**
     * Computes the next feature. Advances the pointers as well.
     *
     * @return the feature Data object computed
     */
    private Data computeNextFeature() {

        DoubleData currentCepstrum = cepstraBuffer[currentPosition++];

	double[] mfc3f = cepstraBuffer[jf3++].getValues();
	double[] mfc2f = cepstraBuffer[jf2++].getValues();
	double[] mfc1f = cepstraBuffer[jf1++].getValues();
        double[] current = currentCepstrum.getValues();
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

        return (new DoubleData(feature, currentCepstrum.getCollectTime(),
                               currentCepstrum.getFirstSampleNumber()));
    }
}

