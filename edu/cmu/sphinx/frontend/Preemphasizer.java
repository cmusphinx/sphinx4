/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Timer;

import java.io.IOException;


/**
 * Filters out the attenuation of audio data. Speech signals have an
 * attenuation of 20 dB/dec. Preemphasis flatten the signal to make it
 * less susceptible to finite precision effects later in the signal
 * processing. The Preemphasizer takes a AudioFrame as input
 * and outputs the same AudioFrame, but with preemphasis applied.
 *
 * The SphinxProperties of this Preemphasizer are: <pre>
 * edu.cmu.sphinx.frontend.preemphasis.dump
 * edu.cmu.sphinx.frontend.preemphasis.factor
 * </pre>
 * This Preemphasizer also processes the PreemphasisPriorSignal signal.
 * This type of signal contain the value of the previous sample, which
 * is used in applying preemphasis.
 *
 * Other Data objects are passed along unchanged through this Preemphasizer.
 *
 * @see AudioFrame
 */
public class Preemphasizer extends DataProcessor {


    /**
     * The name of the SphinxProperty for preemphasis factor/alpha, which
     * has a default value of 0.97F.
     */
    public static final String PROP_PREEMPHASIS_FACTOR =
	"edu.cmu.sphinx.frontend.preemphasis.factor";
    

    private float preemphasisFactor;
    // TODO: somehow get the prior from the frontend
    private double prior;


    /**
     * Constructs a default Preemphasizer.
     *
     * @param the context of SphinxProperty this Preemphasizer uses
     */
    public Preemphasizer(String context) {
        initSphinxProperties(context);
        setTimer(Timer.getTimer(context, "Preemphasizer"));
    }


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     */
    private void initSphinxProperties(String context) {
	setSphinxProperties(context);
        SphinxProperties properties = getSphinxProperties();
	preemphasisFactor = properties.getFloat
	    (PROP_PREEMPHASIS_FACTOR, (float) 0.97);
    }


    /**
     * Returns the next Data object, which is usually a AudioFrame,
     * produced by this Preemphasizerm, though it can also be Data objects
     * like EndPoint.SEGMENT_START.
     *
     * @return the next available Data object, returns null if no
     *     Data object is available
     *
     * @see AudioFrame
     */
    public Data read() throws IOException {

	Data input = getSource().read();
        
        if (input instanceof AudioFrame) {

            input = process((AudioFrame) input);

	} else if (input instanceof PreemphasisPriorSignal) {
	    PreemphasisPriorSignal signal = (PreemphasisPriorSignal) input;
	    prior = (double) signal.getPrior();
	    input = read();
	}

        return input;
    }	


    /**
     * Applies pre-emphasis filter to the given AudioFrame.
     *
     * @param input a AudioFrame of audio data
     *
     * @return a AudioFrame of data with pre-emphasis filter applied
     */
    private Data process(AudioFrame input) {

        getTimer().start();

	double[] in = input.getAudioSamples();
	
	if (in.length > 1 && preemphasisFactor != 0.0) {

	    // do preemphasis
            double current;
            double previous = in[0];
            
	    in[0] = previous - preemphasisFactor * prior;

	    for (int i = 1; i < in.length; i++) {
                current = in[i];
		in[i] = current - preemphasisFactor * previous;
                previous = current;
	    }
	}

        getTimer().stop();

	if (getDump()) {
	    System.out.println(Util.dumpDoubleArray(in, "PREEMPHASIS"));
	}

	return input;
    }
}
