/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Timer;

import java.io.IOException;
import java.util.Arrays;
import java.util.Vector;


/**
 * Extracts Features from a block of Cepstrum. This FeatureExtractor
 * expects Cepstrum that are MelFrequency cepstral coefficients (MFCC).
 * This FeatureExtractor takes in multiple Cepstrum(a) and outputs a 
 * FeatureFrame. This is a 1-to-1 processor.
 *
 * <p>The Sphinx properties that affect this processor are: <pre>
 * edu.cmu.sphinx.frontend.feature.length
 * edu.cmu.sphinx.frontend.feature.windowSize
 * </pre>
 *
 * @see Cepstrum
 * @see Feature
 * @see FeatureFrame
 */
public class FeatureExtractor extends DataProcessor {

    /**
     * The name of the SphinxProperty for the length of a Feature,
     * which is 39 by default.
     */
    public static final String PROP_FEATURE_LENGTH =
	"edu.cmu.sphinx.frontend.featureExtractor.featureLength";

    /**
     * The name of the SphinxProperty for the window of the FeatureExtractor,
     * which has a default value of 3.
     */
    public static final String PROP_FEATURE_WINDOW =
	"edu.cmu.sphinx.frontend.featureExtractor.windowSize";


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
    private boolean segmentStart;
    private boolean segmentEnd;


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
     *
     * @param context the context of the SphinxProperties to use
     */
    public FeatureExtractor(String context) {
	initSphinxProperties(context);
	cepstraBuffer = new float[LIVEBUFBLOCKSIZE][];
        setTimer(Timer.getTimer(context, "FeatureExtractor"));
        fcTimer = Timer.getTimer(context, "featComp");
        inputQueue = new InputQueue();
    }


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     */
    private void initSphinxProperties(String context) {
	setSphinxProperties(context);
	SphinxProperties properties = getSphinxProperties();
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
     *
     * @see Feature
     * @see FeatureFrame
     */
    public Data read() throws IOException {
        Data input = inputQueue.removeNext();
        
        if (input == null) {

            return null;
            
        } else {
            if (input instanceof EndPointSignal) {
                
                EndPointSignal signal = (EndPointSignal) input;

                if (signal.equals(EndPointSignal.FRAME_START)) {

                    Data featureFrame = process(readCepstra());
                    segmentStart = false;
                    segmentEnd = false;
                    return featureFrame;
 
                } else if (signal.equals(EndPointSignal.SEGMENT_START)) {
                    segmentStart = true;
                    return input;
                } else {
                    return input;
                }
            } else {
                return read();
            }
        }
    }	


    /**
     * Returns the total number of features that should result from
     * the read Cepstra. This method is called after an
     * EndPointSignal.FRAME_START is read. It will read all the
     * Cepstrum in between the read EndPointSignal.FRAME_START and
     * the next EndPointSignal.FRAME_END, and insert these Cepstrum
     * into the cepstraBuffer.
     *
     * @returns the number of features that should result from the
     * read Cepstrum
     *
     * @throws java.io.IOException if there is an error reading Cepstrum
     */
    private int readCepstra() throws IOException {

        int totalFeatures = 0;
        int residualVectors = 0;

        Data firstFrame = inputQueue.peekNext();

        if (firstFrame instanceof Cepstrum) {

            // replicate the first cepstrum if segment start
            if (segmentStart) {
                replicateFirstFrame((Cepstrum) inputQueue.removeNext());
                residualVectors -= window;
            }

            // read and copy all the middle Cepstrum
            totalFeatures += readMiddleCepstra();

            // is the next frame EndPointSignal.SEGMENT_END ?
            Data nextFrame = inputQueue.peekNext();

            if (nextFrame instanceof EndPointSignal) {
                
                EndPointSignal signal = (EndPointSignal) nextFrame;
                
                if (signal.equals(EndPointSignal.SEGMENT_END)) {
                    
                    // if it is, replicate the last cepstrum
                    replicateLastFrame();
                    residualVectors += window;
                }
            }
        }

        return totalFeatures + residualVectors;
    }


    /**
     * Reads all the cepstra until we hit the next EndPointSignal.
     * Returns the number of cepstra read. The last EndPointSignal is
     * removed from the inputQueue.
     *
     * @return the number of cepstra read
     *
     * @throws java.io.IOException if error reading the cepstra
     */
    private int readMiddleCepstra() throws IOException {
        
        int totalCepstra = 0;
        Data nextFrame = null;
        
        while (!((nextFrame = inputQueue.removeNext())
                 instanceof EndPointSignal)) {
            
            if (nextFrame instanceof Cepstrum) {
                Cepstrum cepstrum = (Cepstrum) nextFrame;
                cepstraBuffer[bufferPosition++] = cepstrum.getCepstrumData();
                totalCepstra++;
                
                if (bufferPosition == LIVEBUFBLOCKSIZE) {
                    bufferPosition = 0;
                }
            }
        }

        return totalCepstra;
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
    private void replicateLastFrame() {

        if (bufferPosition >= 0) {
         
            float[] last = this.cepstraBuffer[bufferPosition - 1];
            
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
    }


    /**
     * Converts the Cepstrum data in the cepstraBuffer into a FeatureFrame.
     *
     * @param the number of Features that will be produced
     *
     * @return a FeatureFrame
     */
    private FeatureFrame process(int totalFeatures) {

        getTimer().start();

        assert(0 < totalFeatures && totalFeatures < LIVEBUFBLOCKSIZE);

	// create the Features

        Feature[] features = new Feature[totalFeatures];

	for (int i = 0; i < totalFeatures; i++) {
            features[i] = computeNextFeature();
	}

        FeatureFrame featureFrame = new FeatureFrame(features);

        getTimer().stop();
        
        if (getDump()) {
            System.out.println(featureFrame.toString());
        }
        
        return featureFrame;
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

