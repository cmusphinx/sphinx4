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
 * FeatureFrame. This is a 1-to-1 processor.
 *
 * <p>The Sphinx properties that affect this processor are: <pre>
 * edu.cmu.sphinx.frontend.featureExtractor.featureLength
 * edu.cmu.sphinx.frontend.featureExtractor.windowSize
 * edu.cmu.sphinx.frontend.featureExtractor.cepstraBufferSize
 * </pre>
 *
 * @see Cepstrum
 * @see Feature
 */
public class FeatureExtractor extends DataProcessor implements
FeatureSource {

    /**
     * The name of the SphinxProperty for the length of a Feature,
     * which is 39 by default.
     */
    public static final String PROP_FEATURE_LENGTH =
    FrontEnd.PROP_PREFIX + "featureExtractor.featureLength";
    
    /**
     * The name of the SphinxProperty for the window of the
     * FeatureExtractor, which has a default value of 3.
     */
    private static final String PROP_FEATURE_WINDOW =
    FrontEnd.PROP_PREFIX + "featureExtractor.windowSize";
    
    /**
     * The name of the SphinxProperty for the size of the circular
     * Cepstra buffer, which has a default value of 256.
     */
    private static final String PROP_CEP_BUFFER_SIZE =
    FrontEnd.PROP_PREFIX + "featureExtractor.cepstraBufferSize";


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
    private boolean segmentStart;
    private boolean segmentEnd;
    private IDGenerator featureID;

    private CepstrumSource predecessor;
    private List outputQueue;

    private Utterance currentUtterance;


    /**
     * Constructs a default FeatureExtractor.
     *
     * @param name the name of this FeatureExtractor
     * @param context the context of the SphinxProperties to use
     * @param predecessor the CepstrumSource to get Cepstrum from
     */
    public FeatureExtractor(String name, String context,
                            CepstrumSource predecessor) {
        super(name, context);
	initSphinxProperties();
        this.predecessor = predecessor;
	cepstraBuffer = new float[cepstraBufferSize][];
        outputQueue = new Vector();
        featureID = new IDGenerator();
        reset();
    }


    /**
     * Resets the FeatureExtractor to be ready to read the next segment
     * of data. 
     */
    private void reset() {
        segmentStart = true;
        segmentEnd = false;
        featureID.reset();
        bufferPosition = 0;
        currentPosition = 0;
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
     * Returns the next Feature object produced by this FeatureExtractor.
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
                outputQueue.add(new Feature(Signal.UTTERANCE_END,
                                            IDGenerator.NON_ID));

            } else {
                Cepstrum input = predecessor.getCepstrum();
                
                if (input == null) {
                    return null;
                } else {

                    if (input.hasContent()) {
                        // "featureBlockSize-1" since first Cepstrum
                        // already read
                        int numberFeatures = readCepstra(featureBlockSize - 1,
                                                         input);
                        if (numberFeatures > 0) {
                            computeFeatures(numberFeatures);
                        }
                    } else if (input.getSignal().equals
                               (Signal.UTTERANCE_START)) {
                        segmentStart = true;
                        outputQueue.add
                            (new Feature(Signal.UTTERANCE_START,
                                         IDGenerator.NON_ID));
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
     * Reads the given number of Cepstrum
     * and insert these Cepstrum into the cepstraBuffer.
     *
     * Returns the total number of features that should result from
     * the read Cepstra.
     *
     * @return the number of features that should be computed using
     *    the Cepstrum read
     *
     * @throws java.io.IOException if there is an error reading Cepstrum
     */
    private int readCepstra(int numberCepstra, Cepstrum firstCepstrum) throws
    IOException {

        int residualVectors = 0;
        int cepstraRead = 0;

        // replicate the first cepstrum of a segment
        if (segmentStart) {
            residualVectors -= setStartCepstrum(firstCepstrum);
            segmentStart = false;
            cepstraRead++;
            featureID.reset();
        } else if (firstCepstrum.hasContent()) {
            addCepstrumData(firstCepstrum.getCepstrumData());
            cepstraRead++;
        }

        boolean done = false;

        // read the cepstra
        while (!done && cepstraRead < numberCepstra) {
            Cepstrum cepstrum = predecessor.getCepstrum();
            if (cepstrum != null) {
                if (cepstrum.hasContent()) {
                    // just a cepstra
                    addCepstrumData(cepstrum.getCepstrumData());
                    cepstraRead++;
                } else if (cepstrum.hasUtteranceEndSignal()) {
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
     * number of frames in the cepstraBuffer. This is the first cepstrum
     * in the segment.
     *
     * @param cepstrum the Cepstrum to replicate
     *
     * @return the number extra cepstra replicated
     */
    private int setStartCepstrum(Cepstrum cepstrum) {

        currentUtterance = cepstrum.getUtterance();

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

        return (new Feature(feature, featureID.getNextID(), currentUtterance));
    }
}


/**
 * An ID generator that gives out positive integer IDs and
 * checks for overflow.
 */ 
class IDGenerator {

    /**
     * Indicates a non-ID value, which is -1.
     */
    public static int NON_ID = -1;

    private int id = 0;
    
    /**
     * Returns the next valid ID, checks for integer overflow.
     *
     * @return the next valid ID, goes back to zero if overflow
     */
    public int getNextID() {
        if (id == Integer.MAX_VALUE) {
            return (id = 0);
        } else {
            return id++;
        }
    }

    /**
     * Resets the ID to zero.
     */
    public void reset() {
        id = 0;
    }
}

