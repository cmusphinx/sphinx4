/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;


/**
 * Applies a HammingWindow to the given DoubleAudioFrame. The audio samples
 * are modified in place, and the original DoubleAudioFrame is returned.
 * The HammingWindow, <i>W</i> of length <i>N</i> (usually the window size)
 * is given by the following:
 * <pre>
 * W(n) = (1-a) - (a * cos((2*PI*n)/(N - 1))) </pre> where:
 * <br><b>a</b> is commonly known as the "alpha" value
 * <br><b>PI</b> is 3.14159265358979323846
 */
public class HammingWindower implements Processor {

    /**
     * The name of the SphinxProperty for the alpha value of the Hamming
     * Window.
     */
    public static final String PROP_ALPHA =
	"edu.cmu.sphinx.frontend.hammingWindow.alpha";

    private double[] window;
    private int windowSize;
    private static final double PI = 3.14159265358979323846;
    private static double ALPHA;
    private static double ONE_MINUS_ALPHA;


    /**
     * Constructs a default HammingWindower.
     */
    public HammingWindower() {
	getSphinxProperties();
	createWindow();
    }


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     */
    private void getSphinxProperties() {
	// TODO : specify the context
	SphinxProperties properties = SphinxProperties.getSphinxProperties("");
	windowSize = properties.getInt(FrontEnd.PROP_WINDOW_SIZE, 205);
	ALPHA = properties.getDouble(HammingWindower.PROP_ALPHA, 0.46);
	ONE_MINUS_ALPHA = (1 - ALPHA);
    }


    /**
     * Creates the HammingWindow.
     */
    private void createWindow() {
	this.window = new double[windowSize];
	if (windowSize > 1){
	    for (int i = 0; i < windowSize; i++) {
		window[i] = ONE_MINUS_ALPHA - ALPHA *
		    Math.cos(2 * PI * i / ((double) windowSize - 1.0));
	    }
	}
    }


    /**
     * Applies the HammingWindow to the given DoubleAudioFrame.
     * The audio samples are modified in place, and the original
     * DoubleAudioFrame is returned.
     *
     * @param input the input Data object
     *
     * @return the same DoubleAudioFrame but with HammingWindow applied
     */
    public Data process(Data input) {
	DoubleAudioFrame audioFrame = (DoubleAudioFrame) input;
	double[] in = audioFrame.getData();

	// System.out.println(in.length + " " + window.length);

	if (in.length > 1) {
	    for (int i = 0; i < in.length; i++) {
		in[i] *= window[i];
	    }
	}

	return audioFrame;
    }
}
