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
    private double prior;
    private int priorPosition;


    /**
     * Constructs a default Preemphasizer.
     *
     * @param the context of SphinxProperty this Preemphasizer uses
     */
    public Preemphasizer(String context) {
        super("Preemphasizer", context);
        initSphinxProperties();
    }


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     */
    private void initSphinxProperties() {
        SphinxProperties properties = getSphinxProperties();
	
        preemphasisFactor = properties.getFloat
	    (PROP_PREEMPHASIS_FACTOR, (float) 0.97);

        int sampleRate = properties.getInt(FrontEnd.PROP_SAMPLE_RATE, 8000);

        float windowSizeMs = properties.getFloat
            (FrontEnd.PROP_WINDOW_SIZE_MS, 25.625F);

        float windowShiftMs = properties.getFloat
            (FrontEnd.PROP_WINDOW_SHIFT_MS, 10.0F);
        
        int windowSize = Util.getSamplesPerWindow(sampleRate, windowSizeMs);
        int windowShift = Util.getSamplesPerShift(sampleRate, windowShiftMs);

        // prior position is the index from the end of the incoming AudioFrame
        // that is right before the AudioFrame
        priorPosition = windowSize - windowShift + 1;
    }


    /**
     * Returns the next Data object, which is usually a AudioFrame,
     * produced by this Preemphasizerm, though it can also be Data objects
     * like EndPoint.SEGMENT_START.
     *
     * @return the next available Data object, returns null if no
     *     Data object is available
     *
     * @throws java.io.IOException if there is an error reading
     * the Data objects
     *
     * @see AudioFrame
     */
    public Data read() throws IOException {

	Data input = getSource().read();
        
        if (input instanceof AudioFrame) {
            input = process((AudioFrame) input);
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
	
        // set the prior value for the next AudioFrame
        double nextPrior = prior;
        if (in.length > priorPosition) {
            nextPrior = in[in.length - priorPosition];
        }

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

        prior = nextPrior;
        
        getTimer().stop();

	if (getDump()) {
	    System.out.println("PREEMPHASIS " + input.toString());
	}

	return input;
    }
}
