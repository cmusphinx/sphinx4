/**
 * [[[copyright]]]
 */
package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Timer;

import java.io.IOException;
import java.util.Vector;
import java.util.Arrays;


/**
 * Slices up a AudioFrame into a number of overlapping
 * windows, and applies a Window function to each of them.
 * The number of resulting windows depends on the window
 * size and the window shift (commonly known as frame size and frame
 * shift in speech world). The Window will be applied to each such
 * window. Since the <code>read()</code> method will return a window,
 * and multiple windows are created for each AudioFrame, this
 * is a 1-to-many processor.
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
 *
 * @see AudioFrame
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
    private int frameSize;
    private DoubleBuffer overflowBuffer;


    /**
     * Constructs a default Windower with the specified context.
     *
     * @param context the context of the SphinxProperties this Windower uses
     */
    public Windower(String context) {
        super("Windower", context);
	initSphinxProperties();
	createWindow();
        outputQueue = new Vector();
        overflowBuffer = new DoubleBuffer(windowSize + frameSize);
    }


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     */
    private void initSphinxProperties() {

	SphinxProperties properties = getSphinxProperties();

        int sampleRate = properties.getInt
            (FrontEnd.PROP_SAMPLE_RATE, 8000);

        float windowSizeInMs = properties.getFloat
            (FrontEnd.PROP_WINDOW_SIZE_MS, 25.625F);

        float windowShiftInMs = properties.getFloat
            (FrontEnd.PROP_WINDOW_SHIFT_MS, 10.0F);

        windowSize = Util.getSamplesPerWindow(sampleRate, windowSizeInMs);
        windowShift = Util.getSamplesPerShift(sampleRate, windowShiftInMs);

        ALPHA = properties.getDouble(Windower.PROP_ALPHA, 0.46);

        int frameSizeInBytes = properties.getInt
	    (FrontEnd.PROP_BYTES_PER_AUDIO_FRAME, 4000);

        if (frameSizeInBytes % 2 == 1) {
            frameSizeInBytes++;
        }
        
        frameSize = frameSizeInBytes / 2;
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
     *
     * @throws java.io.IOException if there is an error reading
     * the Data objects
     *
     * @see AudioFrame
     */
    public Data read() throws IOException {

        Data output = getWindow();
        
        if (output == null) {
            output = getSource().read();

            getTimer().start();

            if (output != null && output instanceof AudioFrame) { 
                // process the AudioFrame, and output the data
                process((AudioFrame) output);
                output = getWindow();                
            } else if (output instanceof EndPointSignal) {
                // the end of segment
                EndPointSignal signal = (EndPointSignal) output;
                if (signal.equals(EndPointSignal.SEGMENT_END)) {
                    processSegmentEnd();
                    output = getWindow();
                    outputQueue.add(signal);
                }
            }

            getTimer().stop();
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

        // send a Signal indicating start of frame
        outputQueue.add(EndPointSignal.FRAME_START);

	double[] in = input.getAudioSamples();
        int length = in.length;

        // prepend overflow samples
        if (overflowBuffer.getOccupancy() > 0) {
            length = overflowBuffer.appendAll(in);
            in = overflowBuffer.getBuffer();
        }

        // apply Hamming window
        int residual = applyHammingWindow(in, length);

        // save elements that also belong to the next window
        overflowBuffer.reset();
        overflowBuffer.append(in, residual, length - residual);

        // send a Signal indicating end of frame
        outputQueue.add(EndPointSignal.FRAME_END);
    }


    /**
     * What happens when an EndPointSignal.SEGMENT_END signal is
     * received.
     */
    private void processSegmentEnd() {
        overflowBuffer.padWindow(windowSize);
        applyHammingWindow(overflowBuffer.getBuffer(), windowSize);
        overflowBuffer.reset();
    }


    /**
     * Applies the Hamming window to the given double array.
     * The windows are added to the output queue. Returns the index
     * of the first array element of next window that is not produced
     * because of insufficient data.
     *
     * @param in the double array to apply the Hamming window
     * @param length the number of elements in the array to apply the
     *     HammingWindow
     *
     * @return the index of the first array element of the next window
     */
    private int applyHammingWindow(double[] in, int length) {

        int windowCount = Util.getWindowCount
            (length, windowSize, windowShift);

        // create all the windows at once, not individually, saves time
        double[][] windows = new double[windowCount][windowSize];

        int windowStart = 0;

        for (int i = 0; i < windowCount; windowStart += windowShift, i++) {

            double[] myWindow = windows[i];
            
            // apply the Window function to the window of data            
            for (int w = 0, s = windowStart; w < windowSize; s++, w++) {
                myWindow[w] = in[s] * window[w];
            }
            
            // add the frame to the output queue
            outputQueue.add(new AudioFrame(myWindow));

            if (getDump()) {
                System.out.println
                    ("HAMMING_WINDOW " + Util.doubleArrayToString(myWindow));
            }
        }

        return windowStart;
    }
}

    
class DoubleBuffer {

    private double[] buffer;
    private int occupancy;

    /**
     * Constructs a DoubleBuffer of the given size.
     */
    public DoubleBuffer(int size) {
        buffer = new double[size];
        occupancy = 0;
    }

    /**
     * Returns the number of elements in this DoubleBuffer.
     *
     * @return the number of elements in this DoubleBuffer.
     */
    public int getOccupancy() {
        return occupancy;
    }

    /**
     * Returns the underlying double array used to store the data.
     *
     * @return the underlying double array
     */
    public double[] getBuffer() {
        return buffer;
    }

    /**
     * Appends all the elements in the given array to this DoubleBuffer.
     *
     * @param the array to copy from
     *
     * @return the resulting number of elements in this DoubleBuffer.
     */
    public int appendAll(double[] src) {
        return append(src, 0, src.length);
    }

    /**
     * Appends the specified elements in the given array to this DoubleBuffer.
     *
     * @param src the array to copy from
     * @param srcPos where in the source array to start from
     * @param length the number of elements to copy
     *
     * @return the resulting number of elements in this DoubleBuffer
     */
    public int append(double[] src, int srcPos, int length) {
        System.arraycopy(src, srcPos, buffer, occupancy, length);
        occupancy += length;
        return occupancy;
    }

    /**
     * If there are less than windowSize elements in this DoubleBuffer,
     * pad the up to windowSize elements with zero.
     *
     * @param windowSize the window size
     */
    public void padWindow(int windowSize) {
        if (occupancy < windowSize) {
            Arrays.fill(buffer, occupancy, windowSize, 0);
        }
    }

    /**
     * Sets the number of elements in this DoubleBuffer to zero, without
     * actually remove the elements.
     */
    public void reset() {
        occupancy = 0;
    }
}
