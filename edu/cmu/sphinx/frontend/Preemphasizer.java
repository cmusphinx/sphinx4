/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;


/**
 * Filters out the attenuation of audio data. Speech signals have an
 * attenuation of 20 dB/dec. Preemphasis flatten the signal to make it
 * less susceptible to finite precision effects later in the signal
 * processing. The Preemphasizer takes as input an ShortAudioFrame
 * and outputs a DoubleAudioFrame.
 *
 */
public class Preemphasizer implements Processor {

    private static final String PROP_PREEMPHASIS_FACTOR =
	"edu.cmu.sphinx.frontend.preemphasisFactor";
    private static final String PROP_PREEMPHASIS_PRIOR =
	"edu.cmu.sphinx.frontend.preemphasisPrior";

    private double preemphasisFactor = 0.0f;
    private double prior;


    /**
     * Constructs a default Preemphasizer with the given Pre-emphasis Factor
     * value.
     */
    public Preemphasizer() {}


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     */
    private void getSphinxProperties() {
	// TODO : specify the context
	SphinxProperties properties = SphinxProperties.getSphinxProperties("");

	preemphasisFactor = properties.getDouble
	    (PROP_PREEMPHASIS_FACTOR, 0.97);
	// TODO : specify the prior value
	// preemphasisFactor = properties.getDouble(PROP_PREEMPHASIS_PRIOR);
    }


    /**
     * Applies pre-emphasis filter to the given DataFrame.
     *
     * @param dataFrame the ShortAudioFrame to apply pre-emphasis filter.
     *
     * @return the audio data with pre-emphasis filter applied
     */
    public Data process(Data input) {

	ShortAudioFrame audioDataFrame = (ShortAudioFrame) input;

	// NOTE:
	// It will not be necessary to allocate this extra double[]
	// if we started off with a double[]. In the pre-emphasis
	// for loop below, we can just start at the end of the array
	// to calculate the preemphasis in-place.

	short[] in = audioDataFrame.getSamples();
	double[] out = new double[in.length];

	if (preemphasisFactor != 0.0) {
	    // do preemphasis
	    out[0] = (double) in[0] - preemphasisFactor * prior;
	    for (int i = 1; i < out.length; i++) {
		out[i] = (double) in[i] - preemphasisFactor * in[i-1];
	    }
	} else {
	    // just convert sample from short to double
	    for (int i = 0; i < out.length; i++) {
		out[i] = (double) in[i];
	    }
	}

	return (new DoubleAudioFrame(out));
    }
}
