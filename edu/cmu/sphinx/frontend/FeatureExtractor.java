/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Timer;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;


/**
 * Extracts Features from a block of Cepstrum. This FeatureExtractor
 * expects Cepstrum that are MelFrequency cepstral coefficients (MFCC).
 * This FeatureExtractor takes in a CepstrumFrame and outputs Features.
 */
public class FeatureExtractor extends PullingProcessor {

    /**
     * The name of the SphinxProperty for the length of a Feature.
     */
    public static final String PROP_FEATURE_LENGTH =
	"edu.cmu.sphinx.frontend.featureLength";

    /**
     * The name of the SphinxProperty for the window of the FeatureExtractor.
     */
    public static final String PROP_FEATURE_WINDOW =
	"edu.cmu.sphinx.frontend.featureWindow";


    private static final int LIVEBUFBLOCKSIZE = 256;
    private static final int BUFFER_EDGE = LIVEBUFBLOCKSIZE - 8;

    private int featureLength;
    private int cepstrumLength;
    private float[][] cepstraBuffer;
    private int bufferPosition;
    private int currentPosition;
    private int window;
    private int jp1, jp2, jp3, jf1, jf2, jf3;
    private List outputQueue;


    /**
     * Constructs a default FeatureExtractor.
     */
    public FeatureExtractor() {
	getSphinxProperties();
	cepstraBuffer = new float[LIVEBUFBLOCKSIZE][];
        setTimer(Timer.getTimer("", "FeatureExtractor"));
        outputQueue = new Vector();
    }


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     */
    private void getSphinxProperties() {
	// TODO : specify the context
	SphinxProperties properties = SphinxProperties.getSphinxProperties("");
	featureLength = properties.getInt(PROP_FEATURE_LENGTH, 39);
	window = properties.getInt(PROP_FEATURE_WINDOW, 3);
	cepstrumLength = properties.getInt
	    (CepstrumProducer.PROP_CEPSTRUM_SIZE, 13);
    }
	

    /**
     * Reads the next Data object, which is a Feature
     * produced by this FeatureExtractor.
     *
     * @return the next available Data object, returns null if no
     *     Data object is available
     */
    public Data read() throws IOException {
        Feature feature = getFeature();
        if (feature != null) {
            return feature;
        } else {
            Data input = getSource().read();
            
            if (input == null) {
                return null;
            } else {
                if (input instanceof SegmentEndPointSignal ||
                    input instanceof CepstrumFrame) {
                    process(input);
                    return getFeature();
                } else {
                    return read();
                }
            }
        }
    }	


    /**
     * Returns the next Feature in the outputQueue.
     *
     * @return the next Feature in the outputQueue
     */
    private Feature getFeature() {
        synchronized (outputQueue) {
            if (outputQueue.size() > 0) {
                float[] featureData = (float[]) outputQueue.remove(0);
                return (new Feature(featureData));
            } else {
                return null;
            }
        }
    }
    

    /**
     * Converts the given input CepstrumFrame into a Features. The
     * Features are cached in the outputQueue.
     *
     * @param input a CepstrumFrame
     */
    private void process(Data input) {

        getTimer().start();
	
	CepstrumFrame cepstrumFrame = null;
	SegmentEndPointSignal signal = null;
	boolean startSegment = false, endSegment = false;

        if (input instanceof CepstrumFrame) {
            cepstrumFrame = (CepstrumFrame) input;
	} else if (input instanceof SegmentEndPointSignal) {
	    signal = (SegmentEndPointSignal) input;
	    startSegment = signal.isStart();
	    endSegment = signal.isEnd();
	    cepstrumFrame = (CepstrumFrame) signal.getData();
	} else {
            return;
        }

	Cepstrum[] cepstra = cepstrumFrame.getData();
	assert(cepstra.length < LIVEBUFBLOCKSIZE);

	int residualVectors = 0;

	if (startSegment) {
	    // Replicate first frame into the first win frames
	    // in the cepstraBuffer
	    for (int i = 0; i < window; i++) {
		this.cepstraBuffer[i] = cepstra[0].getData();
	    }

	    bufferPosition = window;
	    bufferPosition %= LIVEBUFBLOCKSIZE;
	    currentPosition = bufferPosition;

	    jp1 = currentPosition - 1;
	    jp2 = currentPosition - 2;
	    jp3 = currentPosition - 3;
	    jf1 = currentPosition + 1;
	    jf2 = currentPosition + 2;
	    jf3 = currentPosition + 3;

            if (jp3 > BUFFER_EDGE) {
                jf3 %= LIVEBUFBLOCKSIZE;
                jf2 %= LIVEBUFBLOCKSIZE;
                jf1 %= LIVEBUFBLOCKSIZE;
                jp1 %= LIVEBUFBLOCKSIZE;
                jp2 %= LIVEBUFBLOCKSIZE;
                jp3 %= LIVEBUFBLOCKSIZE;
            }

	    residualVectors -= window;
	}

	// copy (the reference of) all the input cepstrum to our cepstraBuffer
	for (int i = 0; i < cepstra.length; i++) {
	    this.cepstraBuffer[bufferPosition++] = cepstra[i].getData();
            if (bufferPosition == LIVEBUFBLOCKSIZE) {
                bufferPosition = 0;
            }
	}

	if (endSegment) {
	    // Replicate the last frame into the last win frames in
	    // the cepstraBuffer
            float[] last;
	    if (cepstra.length > 0) {
                last = cepstra[cepstra.length-1].getData();
            } else {
                last = this.cepstraBuffer[bufferPosition - 1];
            }

            for (int i = 0; i < window; i++) {
                this.cepstraBuffer[bufferPosition++] = last;
                bufferPosition %= LIVEBUFBLOCKSIZE;
            }

	    residualVectors += window;
	}

	// create the Features

        int totalFeatures = cepstra.length + residualVectors;
        
	for (int i = 0; i < totalFeatures; i++) {
	    float[] feature = computeNextFeature();
            outputQueue.add(feature);
            if (getDump()) {
                Util.dumpFloatArray(feature, "FEATURE");
            }
	}

        getTimer().stop();
    }


    /**
     * Computes the next feature. Advances the pointers as well.
     */
    private float[] computeNextFeature() {
	float[] mfc3f = cepstraBuffer[jf3++];
	float[] mfc2f = cepstraBuffer[jf2++];
	float[] mfc1f = cepstraBuffer[jf1++];
        float[] current = cepstraBuffer[currentPosition++];
	float[] mfc1p = cepstraBuffer[jp1++];
	float[] mfc2p = cepstraBuffer[jp2++];
	float[] mfc3p = cepstraBuffer[jp3++];
	
	float[] feature = new float[featureLength];
	
	// CEP; copy all the cepstrum data except for the first one
	int j = cepstrumLength - 1;
	System.arraycopy(current, 1, feature, 0, j);
	
	// DCEP: mfc[2] - mfc[-2]
	for (int k = 1; k < mfc2f.length; k++) {
	    feature[j++] = (mfc2f[k] - mfc2p[k]);
	}
	
  	// POW: C0, DC0, D2C0
	feature[j++] = current[0];
	feature[j++] = mfc2f[0] - mfc2p[0];
	
	// D2CEP: (mfc[3] - mfc[-1]) - (mfc[1] - mfc[-3])
	for (int k = 0; k < mfc3f.length; k++) {
	    feature[j++] = (mfc3f[k] - mfc1p[k]) - (mfc1f[k] - mfc3p[k]);
	}

        if (jp3 > BUFFER_EDGE) {
            jf3 %= LIVEBUFBLOCKSIZE;
            jf2 %= LIVEBUFBLOCKSIZE;
            jf1 %= LIVEBUFBLOCKSIZE;
            currentPosition %= LIVEBUFBLOCKSIZE;
            jp1 %= LIVEBUFBLOCKSIZE;
            jp2 %= LIVEBUFBLOCKSIZE;
            jp3 %= LIVEBUFBLOCKSIZE;
        }

        return feature;
    }
}
