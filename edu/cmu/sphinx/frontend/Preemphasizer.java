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
 * processing. The Preemphasizer takes a DoubleAudioFrame as input
 * and outputs the same DoubleAudioFrame, but with preemphasis applied.
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
 */
public class Preemphasizer extends PullingProcessor {

    /**
     * The name of the SphinxProperty which indicates if the preemphasized
     * DoubleAudioFrames should be dumped. The default value of this
     * SphinxProperty is false.
     */
    public static final String PROP_DUMP =
	"edu.cmu.sphinx.frontend.preemphasis.dump";

    /**
     * The name of the SphinxProperty for preemphasis factor/alpha.
     */
    public static final String PROP_PREEMPHASIS_FACTOR =
	"edu.cmu.sphinx.frontend.preemphasis.factor";
    

    private float preemphasisFactor;
    private int windowSize;
    private int windowShift;
    // TODO: somehow get the prior from the frontend
    private double prior;


    /**
     * Constructs a default Preemphasizer.
     */
    public Preemphasizer() {
	getSphinxProperties();
        setTimer(Timer.getTimer("", "Preemphasizer"));
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
     * Reads the next Data object, which is usually a DoubleAudioFrame,
     * produced by this Preemphasizerm, though it can also be Data objects
     * like SegmentEndPointSignal.
     *
     * @return the next available Data object, returns null if no
     *     Data object is available
     */
    public Data read() throws IOException {

	Data input = getSource().read();
        
        if (input instanceof DoubleAudioFrame) {

            input = process((DoubleAudioFrame) input);

	} else if (input instanceof PreemphasisPriorSignal) {

	    PreemphasisPriorSignal signal = (PreemphasisPriorSignal) input;
	    prior = (double) signal.getPrior();
	    input = read();
	}

        return input;
    }	


    /**
     * Applies pre-emphasis filter to the given DoubleAudioFrame.
     *
     * @param input a DoubleAudioFrame of audio data
     *
     * @return a DoubleAudioFrame of data with pre-emphasis filter applied
     */
    private Data process(DoubleAudioFrame input) {

        getTimer().start();

	double[] in = input.getData();
	
	if (in.length > 1 && preemphasisFactor != 0.0) {

	    // do preemphasis
            double current;
            double previous = in[0];
            
	    in[0] = previous - preemphasisFactor * prior;

	    for (int i = 1; i < in.length; i++) {
                current = (double) in[i];
		in[i] = current - preemphasisFactor * previous;
                previous = current;
	    }

	}

        getTimer().stop();

	if (getDump()) {
	    Util.dumpDoubleArray(in, "PREEMPHASIS");
	}

	return input;
    }
}
