/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;


/**
 * Filters out the attenuation of audio data. Speech signals have an
 * attenuation of 20 dB/dec. Preemphasis flatten the signal to make it
 * less susceptible to finite precision effects later in the signal
 * processing. The Preemphasizer takes a ShortAudioFrame as input
 * and outputs a DoubleAudioFrame.
 */
public class Preemphasizer extends PullingProcessor {

    private static final String PROP_PREEMPHASIS_FACTOR =
	"edu.cmu.sphinx.frontend.preemphasisFactor";
    private static final String PROP_PREEMPHASIS_PRIOR =
	"edu.cmu.sphinx.frontend.preemphasisPrior";

    private float preemphasisFactor;
    private int windowSize;
    private int windowShift;
    // TODO: somehow get the prior from the frontend
    private short prior;


    /**
     * Constructs a default Preemphasizer.
     */
    public Preemphasizer() {
	getSphinxProperties();
    }


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     */
    private void getSphinxProperties() {
	// TODO : specify the context
	SphinxProperties properties = SphinxProperties.getSphinxProperties("");

	preemphasisFactor = properties.getFloat
	    (PROP_PREEMPHASIS_FACTOR, (float) 0.97);
	windowSize = properties.getInt(FrontEnd.PROP_WINDOW_SIZE, 205);
	windowShift = properties.getInt(FrontEnd.PROP_WINDOW_SHIFT, 80);
    }


    /**
     * Applies pre-emphasis filter to the given ShortAudioFrame.
     * If the preemphasis factor is zero, then the short input samples will
     * just be converted to double samples.
     *
     * @param input a ShortAudioFrame of audio data
     *
     * @return a DoubleAudioFrame of data with pre-emphasis filter applied
     */
    public Data process(Data input) {
	if (input instanceof SegmentEndPointSignal) {
	    SegmentEndPointSignal signal = (SegmentEndPointSignal) input;
	    return preemphasize((ShortAudioFrame) signal.getData());
	} else if (input instanceof ShortAudioFrame) {
	    return preemphasize((ShortAudioFrame) input);
	} else {
	    return input;
	}
    }
    
    
    private DoubleAudioFrame preemphasize(ShortAudioFrame audioDataFrame) {
	// NOTE:
	// It will not be necessary to allocate this extra double[]
	// if we started off with a double[]. In the pre-emphasis
	// for loop below, we can just start at the end of the array
	// to calculate the preemphasis in-place.

	short[] in = audioDataFrame.getData();
	double[] out = new double[in.length];

	if (preemphasisFactor != 0.0) {
	    // do preemphasis
	    out[0] = (double) in[0] - preemphasisFactor * (double) prior;
	    for (int i = 1; i < out.length; i++) {
		out[i] = (double) in[i] - preemphasisFactor * (double) in[i-1];
	    }
	} else {
	    // just convert sample from short to double
	    for (int i = 0; i < out.length; i++) {
		out[i] = (double) in[i];
	    }
	}

	// Util.dumpDoubleArray(out, "PREEMPHASIS");

	return (new DoubleAudioFrame(out));
    }
}
