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
 * edu.cmu.sphinx.frontend.FeatureExtractor.featureLength
 * edu.cmu.sphinx.frontend.FeatureExtractor.windowSize
 * edu.cmu.sphinx.frontend.FeatureExtractor.cepstraBufferSize
 * </pre>
 *
 * @see Cepstrum
 * @see Feature
 */
public class LiveFeatureExtractor extends DataProcessor
    implements FeatureExtractor {


    /**
     * The name of the SphinxProperty for the window of the
     * LiveFeatureExtractor.
     */
    public static final String PROP_FEATURE_WINDOW =
	FeatureExtractor.PROP_PREFIX + "windowSize";


    /**
     * The default value of PROP_FEATURE_WINDOW.
     */
    public static final int PROP_FEATURE_WINDOW_DEFAULT = 3;
    

    /**
     * The name of the SphinxProperty for the size of the circular
     * Cepstra buffer.
     */
    public static final String PROP_CEPSTRUM_BUFFER_SIZE =
	FeatureExtractor.PROP_PREFIX + "cepstraBufferSize";


    /**
     * The default value of PROP_CEPSTRUM_BUFFER_SIZE.
     */
    public static final int PROP_CEPSTRUM_BUFFER_SIZE_DEFAULT = 256;


    private int featureLength;
    private int cepstrumLength;

    private int cepstraBufferSize;
    private int cepstraBufferEdge;
    private Cepstrum[] cepstraBuffer;

    private int bufferPosition;
    private int currentPosition;
    private int window;
    private int jp1, jp2, jp3, jf1, jf2, jf3;
    private IDGenerator featureID;

    private long utteranceEndTime;
    private long utteranceEndSample;

    private CepstrumSource predecessor;
    private List outputQueue;

    private Utterance currentUtterance;


    /**
     * Constructs a default LiveFeatureExtractor.
     */
    public LiveFeatureExtractor() {}


    /**
     * Constructs a default LiveFeatureExtractor.
     *
     * @param name the name of this LiveFeatureExtractor
     * @param context the context of interest
     * @param props the SphinxProperties to use
     * @param predecessor the CepstrumSource to get Cepstrum from
     */
    public LiveFeatureExtractor(String name, String context, 
				  SphinxProperties props,
				  CepstrumSource predecessor) {
	initialize(name, context, props, predecessor);
    }

    
    /**
     * Initializes this LiveFeatureExtractor.
     *
     * @param name the name of this LiveFeatureExtractor
     * @param context the context of interest
     * @param props the SphinxProperties to use
     * @param predecessor the CepstrumSource to get Cepstrum from
     */
    public void initialize(String name, String context, SphinxProperties props,
			   CepstrumSource predecessor) {
	super.initialize(name, context, props);
        setProperties();
        this.predecessor = predecessor;
        cepstraBuffer = new Cepstrum[cepstraBufferSize];
        outputQueue = new Vector();
        featureID = new IDGenerator();
        reset();
        System.out.println("Using LiveFeatureExtractor");
    }


    /**
     * Resets the LiveFeatureExtractor to be ready to read the next segment
     * of data. 
     */
    private void reset() {
        featureID.reset();
        bufferPosition = 0;
        currentPosition = 0;
    }


    /**
     * Reads the parameters needed from the the static SphinxProperties object.
     */
    private void setProperties() {
	SphinxProperties properties = getSphinxProperties();
	featureLength = properties.getInt(PROP_FEATURE_LENGTH, 
                                          PROP_FEATURE_LENGTH_DEFAULT);
	window = properties.getInt(PROP_FEATURE_WINDOW,
                                   PROP_FEATURE_WINDOW_DEFAULT);
	cepstrumLength = properties.getInt
	    (FrontEnd.PROP_CEPSTRUM_SIZE, FrontEnd.PROP_CEPSTRUM_SIZE_DEFAULT);
        cepstraBufferSize = properties.getInt
            (PROP_CEPSTRUM_BUFFER_SIZE, PROP_CEPSTRUM_BUFFER_SIZE_DEFAULT);
        cepstraBufferEdge = cepstraBufferSize - (window * 2 + 2);
    }


    /**
     * Returns the next Feature object produced by this LiveFeatureExtractor.
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
            if (input != null) {
                if (input.hasContent()) {
                    addCepstrum(input);
                    computeFeatures(1);
                } else if (input.hasSignal(Signal.UTTERANCE_START)) {
                    utteranceEndTime = -1;
                    outputQueue.add
                        (new Feature
                         (Signal.UTTERANCE_START, IDGenerator.NON_ID,
                          input.getCollectTime(),
                          input.getFirstSampleNumber()));

                    featureID.reset();
                    Cepstrum start = predecessor.getCepstrum();
                    int n = processFirstCepstrum(start);
                    computeFeatures(n);
                    if (utteranceEndTime >= 0) {
                        outputQueue.add
                            (getUtteranceEndFeature(utteranceEndTime,
                                                    utteranceEndSample));
                    }
                } else if (input.hasSignal(Signal.UTTERANCE_END)) {
                    // when the UTTERANCE_END is right at the boundary
                    int n = replicateLastCepstrum();
                    computeFeatures(n);
                    outputQueue.add
                        (getUtteranceEndFeature(input.getCollectTime(),
                                                input.getFirstSampleNumber()));
                }
            }
        }
        if (outputQueue.size() > 0) {
	    Feature feature = (Feature) outputQueue.remove(0);
            return feature;
        } else {
            return null;
        }
    }

    /**
     * Returns a new Feature with the UTTERANCE_END Signal.
     */
    private Feature getUtteranceEndFeature(long collectTime,
                                           long firstSampleNumber) {
        return (new Feature
                (Signal.UTTERANCE_END, IDGenerator.NON_ID, collectTime,
                 firstSampleNumber));
    }

    /**
     * Replicate the given cepstrum into the first window+1
     * number of frames in the cepstraBuffer. This is the first cepstrum
     * in the segment.
     *
     * @param cepstrum the Cepstrum to replicate
     *
     * @return the number Features that can be computed
     */
    private int processFirstCepstrum(Cepstrum cepstrum) throws IOException {

        if (cepstrum.hasSignal(Signal.UTTERANCE_END)) {
            outputQueue.add(getUtteranceEndFeature
                            (cepstrum.getCollectTime(),
                             cepstrum.getFirstSampleNumber()));
            return 0;
        } else if (cepstrum.hasSignal(Signal.UTTERANCE_START)) {
            throw new Error("Too many UTTERANCE_START");
        } else {
            // At the start of an utterance, we replicate the first frame
            // into window+1 frames, and then read the next "window" number
            // of frames. This will allow us to compute the delta-
            // double-delta of the first frame.

            currentUtterance = cepstrum.getUtterance();

            Arrays.fill(cepstraBuffer, 0, window+1, cepstrum);
        
            bufferPosition = window + 1;
            bufferPosition %= cepstraBufferSize;
            currentPosition = window;
            currentPosition %= cepstraBufferSize;

            int numberFeatures = 1;
            utteranceEndTime = -1;
            
            for (int i = 0; i < window; i++) {
                Cepstrum next = predecessor.getCepstrum();
                if (next != null) {
                    if (next.hasContent()) {
                        // just a cepstra
                        addCepstrum(next);
                    } else if (next.hasSignal(Signal.UTTERANCE_END)) {
                        // end of segment cepstrum
                        utteranceEndTime = next.getCollectTime();
                        utteranceEndSample = next.getFirstSampleNumber();
                        replicateLastCepstrum();
                        numberFeatures += i;
                        break;
                    } else if (next.hasSignal(Signal.UTTERANCE_START)) {
                        throw new Error("Too many UTTERANCE_START");
                    }
                }
            }
            
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
            
            return numberFeatures;
        }
    }

    /**
     * Adds the given Cepstrum to the cepstraBuffer.
     */
    private void addCepstrum(Cepstrum cepstrum) {
        cepstraBuffer[bufferPosition++] = cepstrum;
        bufferPosition %= cepstraBufferSize;
    }


    /**
     * Replicate the last frame into the last window number of frames in
     * the cepstraBuffer.
     *
     * @return the number of replicated Cepstrum
     */
    private int replicateLastCepstrum() {

        Cepstrum last = null;

        if (bufferPosition > 0) {
            last = this.cepstraBuffer[bufferPosition - 1];
        } else if (bufferPosition == 0) {
            last = cepstraBuffer[cepstraBuffer.length - 1];
        } else {
            throw new Error("BufferPosition < 0");
        }
        
        for (int i = 0; i < window; i++) {
            addCepstrum(last);
        }

        return window;
    }


    /**
     * Converts the Cepstrum data in the cepstraBuffer into a FeatureFrame.
     *
     * @param the number of Features that will be produced
     *
     * @return a FeatureFrame
     */
    private void computeFeatures(int totalFeatures) {
        getTimer().start();
        if (totalFeatures == 1) {
            computeFeature();
        } else {
            // create the Features
            for (int i = 0; i < totalFeatures; i++) {
                computeFeature();
            }
        }
        getTimer().stop();
    }

    /**
     * Computes the next Feature.
     */
    private void computeFeature() {
        Feature feature = computeNextFeature();
        outputQueue.add(feature);
        if (getDump()) {
            System.out.println("FEATURE " + feature.toString());
        }
    }

    /**
     * Computes the next feature. Advances the pointers as well.
     */
    private Feature computeNextFeature() {

        Cepstrum currentCepstrum = cepstraBuffer[currentPosition++];

        float[] feature = new float[featureLength];

	float[] mfc3f = cepstraBuffer[jf3++].getCepstrumData();
	float[] mfc2f = cepstraBuffer[jf2++].getCepstrumData();
	float[] mfc1f = cepstraBuffer[jf1++].getCepstrumData();
        float[] current = currentCepstrum.getCepstrumData();
	float[] mfc1p = cepstraBuffer[jp1++].getCepstrumData();
	float[] mfc2p = cepstraBuffer[jp2++].getCepstrumData();
	float[] mfc3p = cepstraBuffer[jp3++].getCepstrumData();
	
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

        return (new Feature(feature, featureID.getNextID(), currentUtterance, 
                            currentCepstrum.getCollectTime(),
                            currentCepstrum.getFirstSampleNumber()));
    }
}

