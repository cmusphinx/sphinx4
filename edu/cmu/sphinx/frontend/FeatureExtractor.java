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
 * This FeatureExtractor takes in a CepstrumFrame and outputs a 
 * FeatureFrame. This is a 1-to-1 processor.
 *
 * <p>The Sphinx properties that affect this processor are: <pre>
 * edu.cmu.sphinx.frontend.feature.length
 * edu.cmu.sphinx.frontend.feature.windowSize
 * </pre>
 */
public class FeatureExtractor extends DataProcessor {

    /**
     * The name of the SphinxProperty for the length of a Feature,
     * which is 39 by default.
     */
    public static final String PROP_FEATURE_LENGTH =
	"edu.cmu.sphinx.frontend.feature.length";

    /**
     * The name of the SphinxProperty for the window of the FeatureExtractor,
     * which has a default value of 3.
     */
    public static final String PROP_FEATURE_WINDOW =
	"edu.cmu.sphinx.frontend.feature.windowSize";


    private static final int LIVEBUFBLOCKSIZE = 256;
    private static final int BUFFER_EDGE = LIVEBUFBLOCKSIZE - 8;
    
    private int featureLength;
    private int cepstrumLength;
    private float[][] cepstraBuffer;
    private int bufferPosition;
    private int currentPosition;
    private int window;
    private int jp1, jp2, jp3, jf1, jf2, jf3;
    private InputQueue inputQueue;
    private Timer fcTimer;


    /**
     * Implements an input queue that allows "peek"-ing as well as
     * actually reading.
     */
    private class InputQueue {
        private Vector queue = new Vector();

        public InputQueue() {
            queue = new Vector();
        };


        /**
         * Remove the next element in the queue. If there are no elements
         * in the queue, it will return getSource().read().
         */
        public Data removeNext() throws IOException {
            if (queue.size() > 0) {
                return (Data) queue.remove(0);
            } else {
                return getSource().read();
            }
        }

        /**
         * Peek the next element in the queue without actually removing it.
         */
        public Data peekNext() throws IOException {
            if (queue.size() > 0) {
                return (Data) queue.get(0);
            } else {
                Data next = getSource().read();
                queue.add(next);
                return next;
            }
        }
    }
            

    /**
     * Constructs a default FeatureExtractor.
     */
    public FeatureExtractor() {
	getSphinxProperties();
	cepstraBuffer = new float[LIVEBUFBLOCKSIZE][];
        setTimer(Timer.getTimer("", "FeatureExtractor"));
        fcTimer = Timer.getTimer("", "featComp");
        inputQueue = new InputQueue();
    }


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     */
    private void getSphinxProperties() {
	// TODO : specify the context
	SphinxProperties properties = SphinxProperties.getSphinxProperties("");
	featureLength = properties.getInt(PROP_FEATURE_LENGTH, 39);
	window = properties.getInt(PROP_FEATURE_WINDOW, 3);
	cepstrumLength = properties.getInt(FrontEnd.PROP_CEPSTRUM_SIZE, 13);
    }


    /**
     * Returns the next Data object, which is a FeatureFrame
     * produced by this FeatureExtractor.
     *
     * @return the next available Data object, returns null if no
     *     Data object is available
     */
    public Data read() throws IOException {
        Data input = inputQueue.removeNext();
        
        if (input == null) {
            
            return null;
            
        } else {
            
            boolean segmentStart = false;
            
            if (input == EndPointSignal.SEGMENT_START) {
                // it must be a SegmentStartSignal
                segmentStart = true;
                input = inputQueue.removeNext();
            }
            
            Data nextFrame = inputQueue.peekNext();
            
            boolean segmentEnd = (nextFrame == EndPointSignal.SEGMENT_END);
            
            // absorb the SegmentEndSignal
            if (segmentEnd) {
                inputQueue.removeNext();
            }
            
            return process((CepstrumFrame) input, segmentStart, segmentEnd);
        }
    }	



    /**
     * Converts the given input CepstrumFrame into a FeatureFrame.
     *
     * @param input a CepstrumFrame
     *
     * @return a FeatureFrame
     */
    private FeatureFrame process(CepstrumFrame cepstrumFrame,
                                 boolean startSegment, boolean endSegment) {

        getTimer().start();

	Cepstrum[] cepstra = cepstrumFrame.getCepstra();
	assert(cepstra.length < LIVEBUFBLOCKSIZE);

	int residualVectors = 0;

	if (startSegment) {
            replicateFirstFrame(cepstra[0]);
	    residualVectors -= window;
	}

	// copy (the reference of) all the input cepstrum to our cepstraBuffer
	for (int i = 0; i < cepstra.length; i++) {
	    this.cepstraBuffer[bufferPosition++] =
                cepstra[i].getCepstrumData();
            if (bufferPosition == LIVEBUFBLOCKSIZE) {
                bufferPosition = 0;
            }
	}

	if (endSegment) {
            replicateLastFrame(cepstra);
	    residualVectors += window;
	}


	// create the Features
        fcTimer.start();

        int totalFeatures = cepstra.length + residualVectors;
        Feature[] features = new Feature[totalFeatures];

	for (int i = 0; i < totalFeatures; i++) {
            
            features[i] = computeNextFeature();
            
            if (getDump()) {
                System.out.println(Util.dumpFloatArray
                                   (features[i].getFeatureData(), "FEATURE"));
            }
	}

        fcTimer.stop();

        getTimer().stop();

        return (new FeatureFrame(features));
    }


    /**
     * Replicate the given cepstrum into the last window number of frames in
     * the cepstraBuffer.
     */
    private void replicateFirstFrame(Cepstrum cepstrum) {
        Arrays.fill(cepstraBuffer, 0, window, cepstrum.getCepstrumData());
        
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
    }


    /**
     * Replicate the last frame into the last window number of frames in
     * the cepstraBuffer.
     */
    private void replicateLastFrame(Cepstrum[] cepstra) {
        float[] last;
        if (cepstra.length > 0) {
            last = cepstra[cepstra.length-1].getCepstrumData();
        } else {
            last = this.cepstraBuffer[bufferPosition - 1];
        }
        
        if (bufferPosition + window < LIVEBUFBLOCKSIZE) {
            Arrays.fill(cepstraBuffer, bufferPosition, 
                        bufferPosition + window, last);
        } else {
            for (int i = 0; i < window; i++) {
                this.cepstraBuffer[bufferPosition++] = last;
                bufferPosition %= LIVEBUFBLOCKSIZE;
            }
        }
            
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

        return (new Feature(feature));
    }
}

