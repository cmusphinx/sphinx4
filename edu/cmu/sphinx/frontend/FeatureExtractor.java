/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;


/**
 * Extracts Features from a block of Cepstrum. This FeatureExtractor
 * expects Cepstrum that are MelFrequency cepstral coefficients (MFCC).
 * This FeatureExtractor takes in multiple Cepstrum(a) and outputs a 
 * FeatureFrame. This is a many-to-1 processor.
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
public class FeatureExtractor extends DataProcessor implements
FeatureSource {

    /**
     * The name of the SphinxProperty for the length of a Feature,
     * which is 39 by default.
     */
    public static final String PROP_FEATURE_LENGTH =
	"edu.cmu.sphinx.frontend.featureExtractor.featureLength";

    /**
     * The name of the SphinxProperty for the window of the
     * FeatureExtractor, which has a default value of 3.
     */
    public static final String PROP_FEATURE_WINDOW =
	"edu.cmu.sphinx.frontend.featureExtractor.windowSize";

    /**
     * The name of the SphinxProperty for the size of the circular
     * Cepstra buffer, which has a default value of 256.
     */
    public static final String PROP_CEP_BUFFER_SIZE =
    "edu.cmu.sphinx.frontend.featureExtractor.cepstraBufferSize";


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
    private PeekableQueue peekableQueue;
    private boolean segmentStart;
    private boolean segmentEnd;
    private int featureID;

    private CepstrumSource predecessor;
    private List outputQueue;


    /**
     * Implements an input queue that allows "peek"-ing as well as
     * actually reading.
     */
    private class PeekableQueue {
        
        private Vector queue = new Vector();

        /**
         * Constructs a default PeekableQueue.
         */
        public PeekableQueue() {
            queue = new Vector();
        };


        /**
         * Remove the next element in the queue. If there are no elements
         * in the queue, it will return getSource().read().
         */
        public Cepstrum removeNext() throws IOException {
            if (queue.size() > 0) {
                return (Cepstrum) queue.remove(0);
            } else {
                return predecessor.getCepstrum();
            }
        }

        /**
         * Peek the next element in the queue without actually removing it.
         */
        public Cepstrum peekNext() throws IOException {
            if (queue.size() > 0) {
                return (Cepstrum) queue.get(0);
            } else {
                Cepstrum next = predecessor.getCepstrum();
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
    public FeatureExtractor(String name, String context,
                            CepstrumSource predecessor) {
        super(name, context);
	initSphinxProperties();
        this.predecessor = predecessor;
	cepstraBuffer = new float[cepstraBufferSize][];
        peekableQueue = new PeekableQueue();
        outputQueue = new Vector();
        reset();
    }


    /**
     *
     */
    public void reset() {
        segmentStart = false;
        segmentEnd = false;
        featureID = 0;
    }


    /**
     * Returns the next valid feature ID, incrementing the ID
     * by one.
     *
     * @return the next valid feature ID
     */
    private int getNextFeatureID() {
        return featureID++;
    }


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     */
    private void initSphinxProperties() {
	SphinxProperties properties = getSphinxProperties();
	featureLength = properties.getInt(PROP_FEATURE_LENGTH, 39);
	window = properties.getInt(PROP_FEATURE_WINDOW, 3);
	cepstrumLength = properties.getInt(FrontEnd.PROP_CEPSTRUM_SIZE, 13);
        cepstraBufferSize = properties.getInt(PROP_CEP_BUFFER_SIZE, 256);
        cepstraBufferEdge = cepstraBufferSize - 8;
    }


    /**
     * Returns the next Feature object, which is a FeatureFrame
     * produced by this FeatureExtractor. It can also be
     * other Feature type objects, such as EndPointSignal.FRAME_START.
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

            if (segmentEnd) {
                segmentEnd = false;
                outputQueue.add(new Feature(Signal.SEGMENT_END,
                                            getNextFeatureID()));
            } else {

                Cepstrum input = (Cepstrum) peekableQueue.peekNext();
                
                if (input == null) {
                    return null;
                } else {
                    if (input.hasContent()) {
                        
                        int numberFeatures = readCepstra(featureBlockSize);
                        if (numberFeatures > 0) {
                            computeFeatures(numberFeatures);
                        }
                    } else if (input.getSignal().equals(Signal.SEGMENT_START)) {
                        
                        input = (Cepstrum) peekableQueue.removeNext();
                        segmentStart = true;
                        outputQueue.add(new Feature(input.getSignal(),
                                                    getNextFeatureID()));
                    }
                }
            }
        }
        if (outputQueue.size() > 0) {
            return (Feature) outputQueue.remove(0);
        } else {
            return null;
        }
    }	


    /**
     * Reads all the Cepstrum objects between the read
     * EndPointSignal.FRAME_START and the next EndPointSignal.FRAME_END,
     * and insert these Cepstrum into the cepstraBuffer.
     *
     * Returns the total number of features that should result from
     * the read Cepstra. This method is called after an
     * EndPointSignal.FRAME_START is read.
     *
     * @returns the number of features that should be computed using
     *    the Cepstrum read
     *
     * @throws java.io.IOException if there is an error reading Cepstrum
     */
    private int readCepstra(int numberCepstra) throws IOException {

        int residualVectors = 0;
        int cepstraRead = 0;

        // replicate the first cepstrum if segment start
        if (segmentStart) {
            Cepstrum firstCepstrum = (Cepstrum) peekableQueue.removeNext();
            residualVectors -= setStartCepstrum(firstCepstrum);
            segmentStart = false;
            cepstraRead++;
            featureID = 0;
        }

        boolean done = false;

        // read the cepstra
        while (!done && cepstraRead < numberCepstra) {
            Cepstrum cepstrum = peekableQueue.removeNext();
            if (cepstrum != null) {
                if (cepstrum.hasContent()) {
                    // just a cepstra
                    addCepstrumData(cepstrum.getCepstrumData());
                    cepstraRead++;
                } else if (cepstrum.hasSegmentEndSignal()) {
                    // end of segment cepstrum
                    segmentEnd = true;
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
     * number of frames in the cepstraBuffer.
     *
     * @param cepstrum the Cepstrum to replicate
     *
     * @return the number extra cepstra replicated
     */
    private int setStartCepstrum(Cepstrum cepstrum) {

        Arrays.fill(cepstraBuffer, 0, window+1, cepstrum.getCepstrumData());
        
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
     * Adds the given Cepstrum to the cepstraBuffer.
     */
    private void addCepstrumData(float[] cepstrumData) {
        cepstraBuffer[bufferPosition++] = cepstrumData;
        bufferPosition %= cepstraBufferSize;
    }


    /**
     * Replicate the last frame into the last window number of frames in
     * the cepstraBuffer.
     *
     * @return the number of replicated Cepstrum
     */
    private int replicateLastCepstrum() {

        int replicated = 0;

        if (bufferPosition > 0) {
         
            float[] last = this.cepstraBuffer[bufferPosition - 1];
            
            if (bufferPosition + window < cepstraBufferSize) {
                Arrays.fill(cepstraBuffer, bufferPosition, 
                            bufferPosition + window, last);
            } else {
                for (int i = 0; i < window; i++) {
                    addCepstrumData(last);
                }
            }
            replicated = window;
        }

        return replicated;
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

        assert(0 < totalFeatures && totalFeatures < cepstraBufferSize);

	// create the Features
	for (int i = 0; i < totalFeatures; i++) {
            Feature feature = computeNextFeature();
            if (feature != null) {
                outputQueue.add(feature);
                if (getDump()) {
                    System.out.println("FEATURE " + feature.toString());
                }
            }
	}

        getTimer().stop();
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

        if (jp3 > cepstraBufferEdge) {
            jf3 %= cepstraBufferSize;
            jf2 %= cepstraBufferSize;
            jf1 %= cepstraBufferSize;
            currentPosition %= cepstraBufferSize;
            jp1 %= cepstraBufferSize;
            jp2 %= cepstraBufferSize;
            jp3 %= cepstraBufferSize;
        }

        return (new Feature(feature, getNextFeatureID()));
    }
}

