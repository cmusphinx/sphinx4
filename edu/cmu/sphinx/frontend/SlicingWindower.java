/**
 * [[[copyright]]]
 */
package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Timer;

import java.io.IOException;
import java.util.Vector;


/**
 * Slices up a DoubleAudioFrame into a number of overlapping
 * windows, and applies a Window function to each of them.
 * The number of resulting windows depends on the window
 * size and the window shift (commonly known as frame size and frame
 * shift in speech world). The Window will be applied to each such
 * window. Since the <code>read()</code> method will return a window,
 * and multiple windows are created for each DoubleAudioFrame, this
 * is a one-to-many processor.
 *
 * <p>For each input DoubleAudioFrame, calling
 * <code>SlicingWindower.read()</code> will return the following
 * series of <code>Data</code> objects: <pre>
 * FrameEndPointSignal DoubleAudioFrame ... DoubleAudioFrame
 * FrameEndPointSignal </pre>
 * Calling <code>isStart()</code> on the first
 * <code>FrameEndPointSignal</code> will return true, and calling
 * <code>isEnd()</code> on the last <code>FrameEndPointSignal</code>
 * will return true. The <code>DoubleAudioFrame(s)</code> are the
 * windowed data.
 *
 * <p> The applied Window, <i>W</i> of length <i>N</i> (usually the
 * window size) is given by the following:
 * <pre>
 * W(n) = (1-a) - (a * cos((2*Math.PI*n)/(N - 1))) </pre> where:
 * <br><b>a</b> is commonly known as the "alpha" value, it defaults to 0.46,
 * the value for the HammingWindow, which is commonly used.
 */
public class SlicingWindower extends PullingProcessor {

    /**
     * The name of the SphinxProperty for the alpha value of the Window.
     */
    public static final String PROP_ALPHA =
	"edu.cmu.sphinx.frontend.window.alpha";


    private double[] window;
    private int windowSize;
    private int windowShift;

    private double ALPHA;
    
    private Vector outputQueue;


    /**
     * Constructs a default SlicingWindower.
     */
    public SlicingWindower() {
	getSphinxProperties();
	createWindow();
        outputQueue = new Vector();
        setTimer(Timer.getTimer("", "SlicingWindower"));
    }


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     */
    private void getSphinxProperties() {
	// TODO : specify the context
	SphinxProperties properties = SphinxProperties.getSphinxProperties("");

        float sampleRate = properties.getFloat
            (FrontEnd.PROP_SAMPLE_RATE, 8000.0F);

        float windowSizeInMs = properties.getFloat
            (FrontEnd.PROP_WINDOW_SIZE_MS, 25.625F);

        float windowShiftInMs = properties.getFloat
            (FrontEnd.PROP_WINDOW_SHIFT_MS, 10.0F);

        windowSize = Util.getSamplesPerWindow(sampleRate, windowSizeInMs);
        windowShift = Util.getSamplesPerShift(sampleRate, windowShiftInMs);

        ALPHA = properties.getDouble(SlicingWindower.PROP_ALPHA, 0.46);
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
     * Reads the next Data object, which is usually a window of the input
     * DoubleAudioFrame, with the Window function applied to it.
     *
     * @return the next available Data object, returns null if no
     *     Data object is available
     */
    public Data read() throws IOException {

        Data output = getWindow();
        
        if (output == null) {
            output = getSource().read();

            if (output != null && output instanceof DoubleAudioFrame) { 
                // process the DoubleAudioFrame, and output the data
                process((DoubleAudioFrame) output);
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
     * Applies the Window to the given DoubleAudioFrame.
     * The audio samples are modified in place, and the original
     * DoubleAudioFrame is returned.
     *
     * @param input the input Data object
     *
     * @return the same DoubleAudioFrame but with Window applied
     */
    private void process(DoubleAudioFrame input) {
        
	double[] in = input.getData();

        int windowCount = Util.getWindowCount
            (in.length, windowSize, windowShift);

        // create all the windows at once, not individually, saves time
        double[][] windows = new double[windowCount][windowSize];

        // send a Signal indicating start of frame
        outputQueue.add(FrameEndPointSignal.getStartSignal());

        for (int windowStart = 0, i = 0; i < windowCount;
             windowStart += windowShift, i++) {

            getTimer().start();

            double[] myWindow = windows[i];
            
            // copy the frame into the window
            System.arraycopy(in, windowStart, myWindow, 0, windowSize);
            
            applyWindow(myWindow);

            getTimer().stop();

            // add the frame to the output queue.
            outputQueue.add(new DoubleAudioFrame(myWindow));

            if (getDump()) {
                Util.dumpDoubleArray(myWindow, "HAMMING_WINDOW");
            }
        }

        // send a Signal indicating end of frame
        outputQueue.add(FrameEndPointSignal.getEndSignal());
    }


    /**
     * Applies the Window to the given frame.
     *
     * @param frame the frame to apply the Window to
     */
    private void applyWindow(double[] frame) {
        for (int f = 0; f < frame.length; f++) {
            frame[f] *= window[f];
        }
    }
}
