
/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;


/**
 * Applies a Window to the given DoubleAudioFrame. The audio samples
 * are modified in place, and the original DoubleAudioFrame is returned.
 * The Window, <i>W</i> of length <i>N</i> (usually the window size)
 * is given by the following:
 * <pre>
 * W(n) = (1-a) - (a * cos((2*PI*n)/(N - 1))) </pre> where:
 * <br><b>a</b> is commonly known as the "alpha" value, it defaults to 0.46,
 * the value for the HammingWindow, which is commonly used.
 * <br><b>PI</b> is 3.14159265358979323846
 */
public class Windower implements Processor {

    /**
     * The name of the SphinxProperty for the alpha value of the Window.
     */
    public static final String PROP_ALPHA =
	"edu.cmu.sphinx.frontend.window.alpha";

    /**
     * The name of the SphinxProperty which indicates if the preemphasized
     * ShortAudioFrames should be dumped. The default value of this
     * SphinxProperty is false.
     */
    public static final String PROP_DUMP =
	"edu.cmu.sphinx.frontend.window.dump";

    private double[] window;
    private int windowSize;
    private static final double PI = 3.14159265358979323846;
    private double ALPHA;
    private boolean dump;


    /**
     * Constructs a default Windower.
     */
    public Windower() {
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
	ALPHA = properties.getDouble(Windower.PROP_ALPHA, 0.46);
	dump = properties.getBoolean(PROP_DUMP, false);
    }


    /**
     * Creates the Window.
     */
    private void createWindow() {
	this.window = new double[windowSize];
	if (windowSize > 1){
	    double oneMinusAlpha = (1 - ALPHA);
	    for (int i = 0; i < windowSize; i++) {
		window[i] = oneMinusAlpha - ALPHA *
		    Math.cos(2 * Math.PI * i / ((double) windowSize - 1.0));
	    }
	}
    }


    /**
     * Applies the Window to the given DoubleAudioFrame.
     * The audio samples are modified in place, and the original
     * DoubleAudioFrame is returned.
     *
     * @param input the input Data object
     *
     * @return the same DoubleAudioFrame but with Window applied
     */
    public Data process(Data input) {
	DoubleAudioFrame audioFrame = (DoubleAudioFrame) input;
	double[] in = audioFrame.getData();

	if (in.length > 1) {
	    for (int i = 0; i < in.length; i++) {
		in[i] *= window[i];
	    }
	}

	if (dump) {
	    Util.dumpDoubleArray(in, "HAMMING_WINDOW");
	}

	return audioFrame;
    }
}
