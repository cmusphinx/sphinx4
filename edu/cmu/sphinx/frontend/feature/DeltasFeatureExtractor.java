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
package edu.cmu.sphinx.frontend.feature;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.FloatData;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;

/**
 * Computes the delta and double delta of input cepstrum (or plp or ...). The
 * delta is the first order derivative and the double delta (a.k.a. delta
 * delta) is the second order derivative of the original cepstrum. They help
 * model the speech signal dynamics. The output data is a {@link FloatData}
 * object with a float array of size three times the original cepstrum, formed
 * by the concatenation of cepstra, delta cepstra, and double delta cepstra.
 * The output is the feature vector used by the decoder. Figure 1 shows the
 * arrangement of the output feature data array:
 * <p>
 * <img src="doc-files/feature.jpg"> <br>
 * <b>Figure 1: Layout of the returned features. </b>
 * <p>
 * Suppose that the original cepstrum has a length of N, the first N elements
 * of the feature are just the original cepstrum, the second N elements are the
 * delta of the cepstrum, and the last N elements are the double delta of the
 * cepstrum.
 * <p>
 * Figure 2 below shows pictorially the computation of the delta and double
 * delta of a cepstrum vector, using the last 3 cepstra and the next 3 cepstra.
 * <img src="doc-files/deltas.jpg"> <br>
 * <b>Figure 2: Delta and double delta vector computation. </b>
 * <p>
 * Refering to Figure 2, the delta is computed by subtracting the cepstrum that
 * is two frames behind of the current cepstrum from the cepstrum that is two
 * frames ahead of the current cepstrum. The computation of the double delta is
 * similar. It is computed by subtracting the delta cepstrum one time frame
 * behind from the delta cepstrum one time frame ahead. Replacing delta cepstra
 * with cepstra, this works out to a formula involving the cepstra that are one
 * and three behind and after the current cepstrum.
 */
public class DeltasFeatureExtractor extends BaseDataProcessor {
    /**
     * The name of the SphinxProperty for the window of the
     * DeltasFeatureExtractor.
     */
    public static final String PROP_FEATURE_WINDOW = "windowSize";
    /**
     * The default value of PROP_FEATURE_WINDOW.
     */
    public static final int PROP_FEATURE_WINDOW_DEFAULT = 3;
    private int cepstraBufferSize;
    private int cepstraBufferEdge;
    private int bufferPosition;
    private int currentPosition;
    private int window;
    private int jp1, jp2, jp3, jf1, jf2, jf3;
    private DoubleData[] cepstraBuffer;
    private DataEndSignal dataEndSignal;
    private List outputQueue;

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String,
     *      edu.cmu.sphinx.util.props.Registry)
     */
    public void register(String name, Registry registry)
            throws PropertyException {
        super.register(name, registry);
        registry.register(PROP_FEATURE_WINDOW, PropertyType.INT);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        window = ps.getInt(PROP_FEATURE_WINDOW, PROP_FEATURE_WINDOW_DEFAULT);
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
        cepstraBufferEdge = cepstraBufferSize - (window * 2 + 2);
        outputQueue = new Vector();
        reset();
    }

    /**
     * Resets the DeltasFeatureExtractor to be ready to read the next segment
     * of data.
     */
    private void reset() {
        bufferPosition = 0;
        currentPosition = 0;
    }



    /**
     * Returns the next Data object produced by this DeltasFeatureExtractor.
     * 
     * @return the next available Data object, returns null if no Data is
     *         available
     * 
     * @throws DataProcessingException
     *                 if there is a data processing error
     */
    public Data getData() throws DataProcessingException {
        if (outputQueue.size() == 0) {
            Data input = getPredecessor().getData();
            if (input != null) {
                if (input instanceof DoubleData) {
                    addCepstrum((DoubleData) input);
                    computeFeatures(1);
                } else if (input instanceof DataStartSignal) {
                    dataEndSignal = null;
                    outputQueue.add(input);
                    Data start = getPredecessor().getData();
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
            Data feature = (Data) outputQueue.remove(0);
            return feature;
        } else {
            return null;
        }
    }

    /**
     * Replicate the given cepstrum Data object into the first window+1 number
     * of frames in the cepstraBuffer. This is the first cepstrum in the
     * segment.
     * 
     * @param cepstrum
     *                the Data to replicate
     * 
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
                Data next = getPredecessor().getData();
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
     * Adds the given DoubleData object to the cepstraBuffer.
     * 
     * @param cepstrum
     *                the DoubleData object to add
     */
    private void addCepstrum(DoubleData cepstrum) {
        cepstraBuffer[bufferPosition++] = cepstrum;
        bufferPosition %= cepstraBufferSize;
    }

    /**
     * Replicate the last frame into the last window number of frames in the
     * cepstraBuffer.
     * 
     * @return the number of replicated Cepstrum
     */
    private int replicateLastCepstrum() {
        DoubleData last = null;
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
     * @param totalFeatures
     *                the number of Features that will be produced
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
        Data feature = computeNextFeature();
        outputQueue.add(feature);
    }

    /**
     * Computes the next feature. Advances the pointers as well.
     * 
     * @return the feature Data computed
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
        float[] feature = new float[current.length * 3];
        // CEP; copy all the cepstrum data
        int j = 0;
        for (int k = 0; k < current.length; k++) {
            feature[j++] = (float) current[k];
        }
        // System.arraycopy(current, 0, feature, 0, j);
        // DCEP: mfc[2] - mfc[-2]
        for (int k = 0; k < mfc2f.length; k++) {
            feature[j++] = (float) (mfc2f[k] - mfc2p[k]);
        }
        // D2CEP: (mfc[3] - mfc[-1]) - (mfc[1] - mfc[-3])
        for (int k = 0; k < mfc3f.length; k++) {
            feature[j++] = (float) ((mfc3f[k] - mfc1p[k]) - (mfc1f[k] - mfc3p[k]));
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

        return (new FloatData(feature, 
			      currentCepstrum.getSampleRate(),
			      currentCepstrum.getCollectTime(),
                              currentCepstrum.getFirstSampleNumber()));
    }
}
