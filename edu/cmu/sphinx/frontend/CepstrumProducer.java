/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;
import java.io.IOException;
import java.util.List;
import java.util.ListIterator;


/**
 * Translates audio into speech cepstra. The Cepstra producer takes a
 * DoubleAudioFrame as input, and outputs a CepstraFrame (which is an
 * array of Cepstra).
 *
 * The audio is processed in windows (called a frame). We apply to each
 * audio frame a HammingWindow, followed by a Fast-Fourier Transform.
 * Each of these frames now become a Cepstrum. All these frames together
 * give a CepstrumFrame.
 */
public class CepstrumProducer extends PullingProcessor {

    public static final String PROP_CEPSTRUM_SIZE =
	"edu.cmu.sphinx.frontend.cepstrumSize";

    private int windowSize;
    private int windowShift;
    private int cepstrumSize;

    private Processor hammingWindower;
    private Processor fastFourierTransformer;

    // a DoubleAudioFrame window for producing cepstrum
    private DoubleAudioFrame window;


    /**
     * Constructs a default CepstrumProducer.
     */
    public CepstrumProducer() {

	getSphinxProperties();
	window = new DoubleAudioFrame(windowSize);

	hammingWindower = new HammingWindower();
	// fastFourierTransformer = new FastFourierTransformer();
    }


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     */
    private void getSphinxProperties() {
	// TODO : specify the context
	SphinxProperties properties = SphinxProperties.getSphinxProperties("");

	windowSize = properties.getInt(FrontEnd.PROP_WINDOW_SIZE, 205);
	windowShift = properties.getInt(FrontEnd.PROP_WINDOW_SHIFT, 80);
	cepstrumSize = properties.getInt(PROP_CEPSTRUM_SIZE, 13);
    }
	

    /**
     * Reads the next Data object, which is a CepstrumFrame
     * produced by this CepstrumProducer.
     *
     * @return the next available Data object, returns null if no
     *     Data object is available
     */
    public Data read() throws IOException {
	Data input = super.read();
	if (input instanceof DoubleAudioFrame) {
	    return process(input);
	} else {
	    return input;
	}
    }	


    /**
     * Converts the given input DoubleAudioFrame into a CepstrumFrame.
     *
     * @param input a DoubleAudioFrame of audio data
     *
     * @return a CepstrumFrame
     */
    public Data process(Data input) {
	
	if (!(input instanceof DoubleAudioFrame)) {
	    return input;
	} else {
	    DoubleAudioFrame audioDataFrame = (DoubleAudioFrame) input;
	    double[] samples = audioDataFrame.getData();
	    
	    int windowCount = Util.getWindowCount
		(samples.length, windowSize, windowShift);
	    Cepstrum[] cepstra = new Cepstrum[windowCount];
	    
	    for (int i = 0, windowStart = 0; i < windowCount;
		 i++, windowStart += windowShift) {
		
		// copy the frame into the window
		System.arraycopy(samples, windowStart,
				 window.getData(), 0, windowSize);
		
		// apply hamming window
		hammingWindower.process(window);
		Util.dumpDoubleArray(window.getData(), "HAMMING_WINDOW");
		
		// apply FFT
		// cepstra[i] = (Cepstrum) fastFourierTransformer.process(window);
	    }

	    return (new CepstrumFrame(cepstra));
	}
    }
}
