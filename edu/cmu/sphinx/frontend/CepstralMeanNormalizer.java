/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;
import java.io.IOException;
import java.util.List;
import java.util.ListIterator;


/**
 * Apply Cepstral Mean Normalization (CMN) to the set of input MFC frames. 
 * It subtracts the mean of the input from each frame.
 */
public class CepstralMeanNormalizer extends PullingProcessor {


    /**
     * The name of the SphinxProperty for the initial cepstral mean.
     * This is a front-end dependent magic number.
     */
    public static final String PROP_INITIAL_MEAN =
	"edu.cmu.sphinx.frontend.initialCepstralMean";

    /**
     * The name of the SphinxProperty for the CMN window.
     */
    public static final String PROP_CMN_WINDOW =
	"edu.cmu.sphinx.frontend.cmnWindow";

    /**
     * The name of the SphinxProperty for the CMN shifting window.
     * The shifting window specifies how many frames after which
     * the window is shifted.
     */
    public static final String PROP_CMN_SHIFT_WINDOW =
	"edu.cmu.sphinx.frontend.cmnShiftWindow";


    private float initialMean;
    private int cepstrumLength;
    private int numberFrame;
    private float[] currentMean;
    private float[] sum;
    private int cmnShiftWindow;
    private int cmnWindow;
    private float sf;


    /**
     * Constructs a default CepstralMeanNormalizer.
     */
    public CepstralMeanNormalizer() {
	getSphinxProperties();
	init();
    }


    /**
     * Initializes the currentMean and sum arrays.
     */
    private void init() {
	currentMean = new float[cepstrumLength];
	currentMean[0] = initialMean;
	sum = new float[cepstrumLength];
    }


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     */
    private void getSphinxProperties() {
	// TODO : specify the context
	SphinxProperties properties = SphinxProperties.getSphinxProperties("");
	initialMean = properties.getFloat(PROP_INITIAL_MEAN, 12.0f);
	cepstrumLength = properties.getInt
	    (CepstrumProducer.PROP_CEPSTRUM_SIZE, 13);
	cmnWindow = properties.getInt(PROP_CMN_WINDOW, 500);
	cmnShiftWindow = properties.getInt(PROP_CMN_SHIFT_WINDOW, 800);
    }
	

    /**
     * Reads the next Data object, which is a normalized CepstrumFrame
     * produced by this class.
     *
     * @return the next available Data object, returns null if no
     *     Data object is available
     */
    public Data read() throws IOException {
	Data input = getSource().read();
	if (input instanceof SegmentEndPointSignal ||
	    input instanceof CepstrumFrame) {
	    return process(input);
	} else {
	    return input;
	}
    }	


    /**
     * Normalizes the given CepstrumFrame, which is an array of Cepstrum.
     * Returns the same CepstrumFrame, but with its cepstra normalized.
     * It is assumed that all Cepstrum in the CepstrumFrame are of the same
     * length.
     *
     * @param input a CepstrumFrame
     *
     * @return a normalized CepstrumFrame
     */
    public Data process(Data input) {
	
	CepstrumFrame cepstrumFrame;
	SegmentEndPointSignal signal = null;

	if (input instanceof SegmentEndPointSignal) {
	    signal = (SegmentEndPointSignal) input;
	    cepstrumFrame = (CepstrumFrame) signal.getData();
	} else {
	    cepstrumFrame = (CepstrumFrame) input;
	}

	Cepstrum[] cepstra = cepstrumFrame.getData();

	if (cepstra.length <= 0) {
	    return cepstrumFrame;
	} else {

	    normalize(cepstra);

	    // Shift buffers down if we have more than cmnShiftWindow frames
	    if (numberFrame > cmnShiftWindow) {
		updateMeanSumBuffers();
	    }

	    // if this is the end of the segment, shift the buffers
	    if (input instanceof SegmentEndPointSignal && signal.isEnd()) {
		updateMeanSumBuffers();
	    }
	}
	
	return input;
    }


    /**
     * Normalize the given array of Cepstrum.
     *
     * @param cepstra the array of Cepstrum to normalize
     */
    private void normalize(Cepstrum[] cepstra) {
	// do the mean normalization
	for (int i = 0; i < cepstra.length; i++) {
	    float[] cepstrum = cepstra[i].getData();
	    for (int j = 0; j < cepstrum.length; j++) {
		sum[j] += cepstrum[j];
		cepstrum[j] -= currentMean[j];
	    }
	    ++numberFrame;
	}
    }


    /**
     * Updates the currentMean buffer with the values in the sum buffer.
     * Then decay the sum buffer exponentially, i.e., divide the sum
     * with numberFrames.
     */
    private void updateMeanSumBuffers() {
	// update the currentMean buffer with the sum buffer
	sf = (float) (1.0/numberFrame);
	for (int i = 0; i < cepstrumLength; i++) {
	    currentMean[i] = sum[i] * sf;
	}

	// decay the sum buffer exponentially
	if (numberFrame >= cmnShiftWindow) {
	    sf = cmnWindow * sf;
	    for (int i = 0; i < cepstrumLength; i++) {
		sum[i] *= sf;
	    }
	    numberFrame = cmnWindow;
	}
    }
}
