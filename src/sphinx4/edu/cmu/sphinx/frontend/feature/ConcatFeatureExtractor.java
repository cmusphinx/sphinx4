/*
 * Copyright 2002-2009 Carnegie Mellon University.  
 * Copyright 2009 PC-NG Inc.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */
package edu.cmu.sphinx.frontend.feature;

import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.util.props.*;

import java.util.*;

/**
 * This component concatenate the cepstrum from the sequence of frames according to the window size.
 * It's not supposed to give high accuracy alone, but combined with LDA transform it can give the same
 * or even better results than conventional delta and delta-delta coefficients. The idea is that
 * delta-delta computation is also a matrix multiplication thus using automatically generated
 * with LDA/MLLT matrix we can gain better results.
 * The model for this feature extractor should be trained with SphinxTrain with 1s_c feature type and
 * with cepwin option enabled. Don't forget to set the window size accordingly.
 */
public class ConcatFeatureExtractor extends BaseDataProcessor {

    /** The name of the SphinxProperty for the window of the DeltasFeatureExtractor. */
    @S4Integer(defaultValue = 3)
    public static final String PROP_FEATURE_WINDOW = "windowSize";
    
    private int cepstraBufferSize;
    private int bufferPosition;
    private int currentPosition;
    private int window;
    private DoubleData[] cepstraBuffer;
    private DataEndSignal dataEndSignal;
    private List<Data> outputQueue;


    /*
    * (non-Javadoc)
    *
    * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
    */
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        window = ps.getInt(PROP_FEATURE_WINDOW);
    }


    /*
    * (non-Javadoc)
    *
    * @see edu.cmu.sphinx.frontend.DataProcessor#initialize(edu.cmu.sphinx.frontend.CommonConfig)
    */
    public void initialize() {
        super.initialize();
        cepstraBufferSize = 256;
        cepstraBuffer = new DoubleData[cepstraBufferSize];
        outputQueue = new Vector<Data>();
        reset();
    }


    /** Resets the DeltasFeatureExtractor to be ready to read the next segment of data. */
    private void reset() {
        bufferPosition = 0;
        currentPosition = 0;
    }


    /**
     * Returns the next Data object produced by this DeltasFeatureExtractor.
     *
     * @return the next available Data object, returns null if no Data is available
     * @throws DataProcessingException if there is a data processing error
     */
    public Data getData() throws DataProcessingException {
        if (outputQueue.size() == 0) {
            Data input = getNextData();
            if (input != null) {
                if (input instanceof DoubleData) {
                    addCepstrum((DoubleData) input);
                    computeFeatures(1);
                } else if (input instanceof DataStartSignal) {
                    dataEndSignal = null;
                    outputQueue.add(input);
                    Data start = getNextData();
                    int n = processFirstCepstrum(start);
                    computeFeatures(n);
                    if (dataEndSignal != null) {
                        outputQueue.add(dataEndSignal);
                    }
                } else if (input instanceof DataEndSignal) {
                    // when the DataEndSignal is right at the boundary
                    int n = replicateLastCepstrum();
                    computeFeatures(n);
                    outputQueue.add(input);
                }
            }
        }
        if (outputQueue.size() > 0) {
            Data feature = outputQueue.remove(0);
            return feature;
        } else {
            return null;
        }
    }


    private Data getNextData() throws DataProcessingException {
        Data d = getPredecessor().getData();
        while (d != null && !(d instanceof DoubleData || d instanceof DataEndSignal || d instanceof DataStartSignal)) {
            outputQueue.add(d);
            d = getPredecessor().getData();
        }

        return d;
    }


    /**
     * Replicate the given cepstrum Data object into the first window+1 number of frames in the cepstraBuffer. This is
     * the first cepstrum in the segment.
     *
     * @param cepstrum the Data to replicate
     * @return the number of Features that can be computed
     */
    private int processFirstCepstrum(Data cepstrum)
            throws DataProcessingException {
        if (cepstrum instanceof DataEndSignal) {
            outputQueue.add(cepstrum);
            return 0;
        } else if (cepstrum instanceof DataStartSignal) {
            throw new Error("Too many UTTERANCE_START");
        } else {
            // At the start of an utterance, we replicate the first frame
            // into window+1 frames, and then read the next "window" number
            // of frames. This will allow us to compute the delta-
            // double-delta of the first frame.
            Arrays.fill(cepstraBuffer, 0, window + 1, cepstrum);
            bufferPosition = window + 1;
            bufferPosition %= cepstraBufferSize;
            currentPosition = window;
            currentPosition %= cepstraBufferSize;
            int numberFeatures = 1;
            dataEndSignal = null;
            for (int i = 0; i < window; i++) {
                Data next = getNextData();
                if (next != null) {
                    if (next instanceof DoubleData) {
                        // just a cepstra
                        addCepstrum((DoubleData) next);
                    } else if (next instanceof DataEndSignal) {
                        // end of segment cepstrum
                        dataEndSignal = (DataEndSignal) next;
                        replicateLastCepstrum();
                        numberFeatures += i;
                        break;
                    } else if (next instanceof DataStartSignal) {
                        throw new Error("Too many UTTERANCE_START");
                    }
                }
            }
            return numberFeatures;
        }
    }


    /**
     * Adds the given DoubleData object to the cepstraBuffer.
     *
     * @param cepstrum the DoubleData object to add
     */
    private void addCepstrum(DoubleData cepstrum) {
        cepstraBuffer[bufferPosition] = cepstrum;
        bufferPosition = (bufferPosition + 1) % cepstraBufferSize;
    }


    /**
     * Replicate the last frame into the last window number of frames in the cepstraBuffer.
     *
     * @return the number of replicated cepstrum
     */
    private int replicateLastCepstrum() {
        DoubleData last;
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
     * Converts the cepstrum data in the cepstraBuffer into a FeatureFrame.
     *
     * @param totalFeatures the number of Features that will be produced
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


    /** Computes the next Feature. */
    private void computeFeature() {
        Data feature = computeNextFeature();
        outputQueue.add(feature);
    }


    /**
     * Computes the next feature. Advances the pointers as well.
     *
     * @return the feature Data computed
     */
    private Data computeNextFeature() {
        DoubleData currentCepstrum = cepstraBuffer[currentPosition];
        float[] feature = new float[(window * 2 + 1) * currentCepstrum.getValues().length];
        int j = 0;
        for (int k = -window; k <= window; k++) {
        	int position = (currentPosition + k + cepstraBufferSize) % cepstraBufferSize;
        	double[] buffer = cepstraBuffer[position].getValues();
            for (int l = 0; l < buffer.length; l++) {
                feature[j++] = (float) buffer[l];
            }
        }
        currentPosition = (currentPosition + 1) % cepstraBufferSize ;

        return (new FloatData(feature,
                currentCepstrum.getSampleRate(),
                currentCepstrum.getCollectTime(),
                currentCepstrum.getFirstSampleNumber()));
    }
}
