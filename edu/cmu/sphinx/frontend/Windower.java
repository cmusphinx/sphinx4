/**
 * [[[copyright]]]
 */
package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Timer;

import java.io.IOException;
import java.util.Vector;


/**
 * Slices up a AudioFrame into a number of overlapping
 * windows, and applies a Window function to each of them.
 * The number of resulting windows depends on the window
 * size and the window shift (commonly known as frame size and frame
 * shift in speech world). The Window will be applied to each such
 * window. Since the <code>read()</code> method will return a window,
 * and multiple windows are created for each AudioFrame, this
 * is a one-to-many processor.
 *
 * <p>For each input AudioFrame, calling
 * <code>Windower.read()</code> will return the following
 * series of <code>Data</code> objects: <pre>
 * EndPointSignal.FRAME_START AudioFrame ... AudioFrame
 * EndPointSignal.FRAME_END </pre>
 * The <code>AudioFrame(s)</code> are the windowed data.
 *
 * <p> The applied Window, <i>W</i> of length <i>N</i> (usually the
 * window size) is given by the following:
 * <pre>
 * W(n) = (1-a) - (a * cos((2*Math.PI*n)/(N - 1))) </pre> where:
 * <br><b>a</b> is commonly known as the "alpha" value, it defaults to 0.46,
 * the value for the HammingWindow, which is commonly used.
 *
 * <p>The Sphinx property that affects this processor is: <pre>
 * edu.cmu.sphinx.frontend.windower.alpha
 * </pre>
 */
public class Windower extends DataProcessor {

    /**
     * The name of the SphinxProperty for the alpha value of the Window,
     * which has a default value of 0.46 (double), which is the value for the
     * HammingWindow.
     */
    public static final String PROP_ALPHA =
	"edu.cmu.sphinx.frontend.windower.alpha";


    private double[] window;
    private int windowSize;
    private int windowShift;
    private double ALPHA;
    private Vector outputQueue;


    /**
     * Constructs a default Windower.
     */
    public Windower() {
	getSphinxProperties();
	createWindow();
        outputQueue = new Vector();
        setTimer(Timer.getTimer("", "Windower"));
    }


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     */
    private void getSphinxProperties() {
	// TODO : specify the context
	SphinxProperties properties = SphinxProperties.getSphinxProperties("");

        int sampleRate = properties.getInt
            (FrontEnd.PROP_SAMPLE_RATE, 8000);

        float windowSizeInMs = properties.getFloat
            (FrontEnd.PROP_WINDOW_SIZE_MS, 25.625F);

        float windowShiftInMs = properties.getFloat
            (FrontEnd.PROP_WINDOW_SHIFT_MS, 10.0F);

        windowSize = Util.getSamplesPerWindow(sampleRate, windowSizeInMs);
        windowShift = Util.getSamplesPerShift(sampleRate, windowShiftInMs);

        ALPHA = properties.getDouble(Windower.PROP_ALPHA, 0.46);
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
     * Returns the next Data object, which is usually a window of the input
     * AudioFrame, with the Window function applied to it.
     *
     * @return the next available Data object, returns null if no
     *     Data object is available
     */
    public Data read() throws IOException {

        Data output = getWindow();
        
        if (output == null) {
            output = getSource().read();

            if (output != null && output instanceof AudioFrame) { 
                // process the AudioFrame, and output the data
                process((AudioFrame) output);
                output = getWindow();                
            }
        }

        return output;
    }


    /**
     * Returns the next produced window in the output queue.
     *
     * @return the next window in the output queue, or null if none
     *    available
     */
    private Data getWindow() {
        if (outputQueue.size() > 0) {
            return (Data) outputQueue.remove(0);
        } else {
            return null;
        }
    }


    /**
     * Applies the Window to the given AudioFrame.
     * The audio samples are modified in place, and the original
     * AudioFrame is returned.
     *
     * @param input the input Data object
     *
     * @return the same AudioFrame but with Window applied
     */
    private void process(AudioFrame input) {
        
	double[] in = input.getAudioSamples();

        int windowCount = Util.getWindowCount
            (in.length, windowSize, windowShift);

        // create all the windows at once, not individually, saves time
        double[][] windows = new double[windowCount][windowSize];

        // send a Signal indicating start of frame
        outputQueue.add(EndPointSignal.SEGMENT_START);

        for (int windowStart = 0, i = 0; i < windowCount;
             windowStart += windowShift, i++) {

            getTimer().start();

            double[] myWindow = windows[i];
            
            // apply the Window function to the window of data            
            for (int w = 0, s = windowStart; w < windowSize; s++, w++) {
                myWindow[w] = in[s] * window[w];
            }
            
            getTimer().stop();

            // add the frame to the output queue
            outputQueue.add(new AudioFrame(myWindow));

            if (getDump()) {
                System.out.println
                    (Util.dumpDoubleArray(myWindow, "HAMMING_WINDOW"));
            }
        }

        // send a Signal indicating end of frame
        outputQueue.add(EndPointSignal.SEGMENT_END);
    }
}
