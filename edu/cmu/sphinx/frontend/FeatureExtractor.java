/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Timer;

import java.io.IOException;
import java.util.List;
import java.util.ListIterator;


/**
 * Extracts Features from a block of Cepstrum. This FeatureExtractor
 * expects Cepstrum that are MelFrequency cepstral coefficients (MFCC).
 * This FeatureExtractor takes in a CepstrumFrame and outputs a FeatureFrame.
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

    private int featureLength;
    private int cepstrumLength;
    private float[][] cepstraBuffer;
    private int bufferPosition;
    private int currentPosition;
    private int window;
    private int jp1, jp2, jp3, jf1, jf2, jf3;


    /**
     * Constructs a default FeatureExtractor.
     */
    public FeatureExtractor() {
	getSphinxProperties();
	cepstraBuffer = new float[LIVEBUFBLOCKSIZE][];
        setTimer(Timer.getTimer("", "FeatureExtractor"));
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
     * Reads the next Data object, which is a FeatureFrame
     * produced by this FeatureExtractor.
     *
     * @return the next available Data object, returns null if no
     *     Data object is available
     */
    public Data read() throws IOException {
	Data input = getSource().read();
        Data output = input;

        getTimer().start();

	if (input instanceof SegmentEndPointSignal ||
	    input instanceof CepstrumFrame) {
	    output = process(input);
	}

        getTimer().stop();

        return output;
    }	


    /**
     * Converts the given input CepstrumFrame into a FeatureFrame.
     *
     * @param input a CepstrumFrame
     *
     * @return a FeatureFrame
     */
    private Data process(Data input) {
	
	CepstrumFrame cepstrumFrame;
	SegmentEndPointSignal signal = null;
	boolean startSegment = false, endSegment = false;

	if (input instanceof SegmentEndPointSignal) {
	    signal = (SegmentEndPointSignal) input;
	    startSegment = signal.isStart();
	    endSegment = signal.isEnd();
	    cepstrumFrame = (CepstrumFrame) signal.getData();
	} else {
	    cepstrumFrame = (CepstrumFrame) input;
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
	    jp1 %= LIVEBUFBLOCKSIZE;
	    jp2 = currentPosition - 2;
	    jp2 %= LIVEBUFBLOCKSIZE;
	    jp3 = currentPosition - 3;
	    jp3 %= LIVEBUFBLOCKSIZE;
	    jf1 = currentPosition + 1;
	    jf1 %= LIVEBUFBLOCKSIZE;
	    jf2 = currentPosition + 2;
	    jf2 %= LIVEBUFBLOCKSIZE;
	    jf3 = currentPosition + 3;
	    jf3 %= LIVEBUFBLOCKSIZE;
	    residualVectors -= window;
	}

	// copy (the reference of) all the input cepstrum to our cepstraBuffer 
	for (int i = 0; i < cepstra.length; i++) {
	    this.cepstraBuffer[bufferPosition++] = cepstra[i].getData();
	    bufferPosition %= LIVEBUFBLOCKSIZE;
	}

	if (endSegment) {
	    // Replicate the last frame into the last win frames in
	    // the cepstraBuffer
	    if (cepstra.length > 0) {
		for (int i = 0; i < window; i++) {
		    this.cepstraBuffer[bufferPosition++] =
			cepstra[cepstra.length-1].getData();
		    bufferPosition %= LIVEBUFBLOCKSIZE;
		}
	    } else {
		int tPosition = bufferPosition - 1;
		for (int i = 0; i < window; i++) {
		    this.cepstraBuffer[bufferPosition++] =
			this.cepstraBuffer[tPosition];
		    bufferPosition %= LIVEBUFBLOCKSIZE;
		}
	    }
	    residualVectors += window;
	}

	// create the Features
	int totalFeatures = cepstra.length + residualVectors;
	Feature[] features = new Feature[totalFeatures];

	for (int i = 0; i < totalFeatures; i++) {
	    features[i] = computeNextFeature();
	}

	FeatureFrame featureFrame = new FeatureFrame(features);

	if (getDump()) {
	    Util.dumpFeatureFrame(featureFrame);
	}

	if (input instanceof SegmentEndPointSignal) {
	    signal.setData(featureFrame);
	    return signal;
	} else {
	    return featureFrame;
	}
    }


    /**
     * Computes the next feature. Advances the pointers as well.
     */
    private Feature computeNextFeature() {
	float[] mfc3f = this.cepstraBuffer[jf3++];
	float[] mfc2f = this.cepstraBuffer[jf2++];
	float[] mfc1f = this.cepstraBuffer[jf1++];
	float[] mfc1p = this.cepstraBuffer[jp1++];
	float[] mfc2p = this.cepstraBuffer[jp2++];
	float[] mfc3p = this.cepstraBuffer[jp3++];
	
	float[] feature = new float[featureLength];
	
	// CEP; copy all the cepstrum data except for the first one
	int j = cepstrumLength - 1;
	System.arraycopy
	    (this.cepstraBuffer[currentPosition], 1, feature, 0, j);
	
	// DCEP: mfc[2] - mfc[-2]
	for (int k = 1; k < cepstrumLength; k++) {
	    feature[j++] = (mfc2f[k] - mfc2p[k]);
	}
	
	// POW: C0, DC0, D2C0
	feature[j++] = this.cepstraBuffer[currentPosition][0];
	feature[j++] = mfc2f[0] - mfc2p[0];
	feature[j++] = (mfc3f[0] - mfc1p[0]) - (mfc1f[0] - mfc3p[0]);
	
	// D2CEP: (mfc[3] - mfc[-1]) - (mfc[1] - mfc[-3])
	for (int k = 1; k < cepstrumLength; k++) {
	    feature[j++] = (mfc3f[k] - mfc1p[k]) - (mfc1f[k] - mfc3p[k]);
	}

	jf3 %= LIVEBUFBLOCKSIZE;
	jf2 %= LIVEBUFBLOCKSIZE;
	jf1 %= LIVEBUFBLOCKSIZE;
	jp1 %= LIVEBUFBLOCKSIZE;
	jp2 %= LIVEBUFBLOCKSIZE;
	jp3 %= LIVEBUFBLOCKSIZE;
	currentPosition++;
	currentPosition %= LIVEBUFBLOCKSIZE;

	return (new Feature(feature));
    }
}
