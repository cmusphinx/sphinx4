/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electronic Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.frontend.util.Util;

import edu.cmu.sphinx.util.SphinxProperties;

import java.io.IOException;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;


/**
 * Slices up an Audio into a number of overlapping
 * windows, and applies a Windowing function to each of them.
 * The number of resulting windows depends on the window
 * size and the window shift (commonly known as frame size and frame
 * shift in speech world). The Window will be applied to each such
 * window. Since the <code>getAudio()</code> method will return a window,
 * and multiple windows are created for each Audio, this
 * is a 1-to-many processor.
 *
 * <p>A window (which is an Audio object) is returned each time
 * <code>Windower.getAudio()</code> is called.
 *
 * <p> The applied Window, <i>W</i> of length <i>N</i> (usually the
 * window size) is given by the following:
 * <pre>
 * W(n) = (1-a) - (a * cos((2*Math.PI*n)/(N - 1))) </pre> where:
 * <br><b>a</b> is commonly known as the "alpha" value, it defaults to 0.46,
 * the value for the HammingWindow, which is commonly used.
 * </pre>
 *
 * @see Audio
 */
public class Windower extends DataProcessor implements AudioSource {


    private static final String PROP_PREFIX
        = "edu.cmu.sphinx.frontend.Windower.";


    /**
     * The SphinxProperty name for window size in milliseconds.
     */
    public static final String PROP_WINDOW_SIZE_MS
        = PROP_PREFIX + "windowSizeInMs";


    /**
     * The default value for PROP_WINDOW_SIZE_MS.
     */
    public static final float PROP_WINDOW_SIZE_MS_DEFAULT = 25.625f;


    /**
     * The SphinxProperty name for window shift in milliseconds,
     * which has a default value of 10F.
     */
    public static final String PROP_WINDOW_SHIFT_MS
        = PROP_PREFIX + "windowShiftInMs";


    /**
     * The default value for PROP_WINDOW_SHIFT_MS.
     */
    public static final float PROP_WINDOW_SHIFT_MS_DEFAULT = 10;

    
    /**
     * The name of the SphinxProperty for the alpha value of the Window,
     * which has a default value of 0.46 (double), which is the value for the
     * HammingWindow.
     */
    public static final String PROP_ALPHA = PROP_PREFIX + "alpha";


    /**
     * The default value for PROP_ALPHA.
     */
    public static final double PROP_ALPHA_DEFAULT = 0.46;


    private AudioSource predecessor;     // the previous processor
    private double[] hammingWindow;      // Hamming window
    private int windowSize;              // size of each window
    private int windowShift;             // the window size
    private List outputQueue;            // cache for output windows
    private DoubleBuffer overflowBuffer; // cache for overlapped audio regions
    private Utterance currentUtterance;  // the current Utterance
    private long currentCollectTime;
    private long currentFirstSampleNumber;


    /**
     * Constructs a default Windower with the specified context.
     *
     * @param context the context of the SphinxProperties this Windower uses
     *
     * @throws IOException if an I/O error occurs
     */
    public Windower(String name, String context, SphinxProperties props,
		    AudioSource predecessor) throws IOException {
        super(name, context, props);
	setProperties();
        this.predecessor = predecessor;
	createWindow();
        outputQueue = new LinkedList();
        overflowBuffer = new DoubleBuffer(windowSize);
    }


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     *
     * @param props the SphinxProperties object to read parameters from
     * @param the property prefix to use for properties in this
     *    SphinxProperties object
     */
    private void setProperties() {
	SphinxProperties props = getSphinxProperties();

        int sampleRate = props.getInt(FrontEnd.PROP_SAMPLE_RATE,
                                      FrontEnd.PROP_SAMPLE_RATE_DEFAULT);
        float windowSizeInMs = props.getFloat
            (PROP_WINDOW_SIZE_MS, PROP_WINDOW_SIZE_MS_DEFAULT);
        float windowShiftInMs = props.getFloat
            (PROP_WINDOW_SHIFT_MS, PROP_WINDOW_SHIFT_MS_DEFAULT);

        windowSize = Util.getSamplesPerWindow(sampleRate, windowSizeInMs);
        windowShift = Util.getSamplesPerShift(sampleRate, windowShiftInMs);
    }


    /**
     * Creates the Hamming Window.
     */
    private void createWindow() {
        double alpha = getSphinxProperties().getDouble
            (PROP_ALPHA, PROP_ALPHA_DEFAULT);
	this.hammingWindow = new double[windowSize];
	if (windowSize > 1){
	    double oneMinusAlpha = (1 - alpha);
	    for (int i = 0; i < windowSize; i++) {
		hammingWindow[i] = oneMinusAlpha - alpha *
		    Math.cos(2 * Math.PI * i / ((double) windowSize - 1.0));
	    }
	}
    }


    /**
     * Returns the next Audio object, which is usually a window of the input
     * Audio, with the Window function applied to it.
     *
     * @return the next available Audio object, returns null if no
     *     Audio object is available
     *
     * @throws java.io.IOException if there is an error reading
     * the Audio objects
     *
     * @see Audio
     */
    public Audio getAudio() throws IOException {
        
        if (outputQueue.size() == 0) {
            Audio input = predecessor.getAudio();
            
            getTimer().start();

            if (input != null) {
                if (input.hasContent()) {
                    // process the Audio, and output the windows
                    process(input);
                } else {
                    if (input.hasSignal(Signal.UTTERANCE_START)) {
                        currentFirstSampleNumber 
                            = input.getFirstSampleNumber();
                    } else if (input.hasSignal(Signal.UTTERANCE_END)) {
                        // end of utterance handling
                        processUtteranceEnd();
                    }
                    outputQueue.add(input);
                }
            }

            getTimer().stop();
        }

        if (outputQueue.size() > 0) {
            Audio output = (Audio) outputQueue.remove(0);
            if (output.hasContent()) {
                assert output.getSamples().length == windowSize;
            }
            return output;
        } else {
            return null;
        }
    }

    /**
     * Applies the Windowing to the given Audio. The resulting windows
     * are cached in the outputQueue.
     *
     * @param input the input Audio object
     */
    private void process(Audio input) throws IOException {

        currentUtterance = input.getUtterance();
        currentCollectTime = input.getCollectTime();
        
        if (input.getFirstSampleNumber() == 0) {
            currentFirstSampleNumber = 0;
        }

	double[] in = input.getSamples();
        int length = overflowBuffer.getOccupancy() + in.length;

        List audioList = new LinkedList();
        audioList.add(input);

        Audio utteranceEnd = null;
        
        // read in more Audio if we have under one window's length of data
        while (length < windowSize) {
            Audio next = predecessor.getAudio();
            if (next.hasContent()) {
                audioList.add(next);
                length += next.getSamples().length;
            } else if (next.hasSignal(Signal.UTTERANCE_END)) {
                utteranceEnd = next;
                break;
            }
        }

	double[] allSamples = in;

        // prepend overflow samples
        if (length != in.length) {

            allSamples = new double[length];
            int start = 0;
	    
	    // copy overflow samples to allSamples buffer
	    System.arraycopy(overflowBuffer.getBuffer(), 0,
			     allSamples, start, overflowBuffer.getOccupancy());
            start = overflowBuffer.getOccupancy();

	    // copy input samples to allSamples buffer
            for (Iterator i = audioList.iterator(); i.hasNext(); ) {
                Audio next = (Audio) i.next();
                double[] samples = next.getSamples();
                System.arraycopy(samples, 0, allSamples, start, 
                                 samples.length);
                start += samples.length;
            }
	}

        // apply Hamming window
        int residual = applyHammingWindow(allSamples, length);

        // save elements that also belong to the next window
        overflowBuffer.reset();
	if (length - residual > 0) {
	    overflowBuffer.append(allSamples, residual, length - residual);
	}
        if (utteranceEnd != null) {
            // end of utterance handling
            processUtteranceEnd();
            outputQueue.add(utteranceEnd);
        }
    }


    /**
     * What happens when an EndPointSignal.UTTERANCE_END signal is
     * received. Basically pads up to a window of the overflow buffer
     * with zeros, and then apply the Hamming window to it.
     */
    private void processUtteranceEnd() {
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
     * @param in the audio data to apply window and the Hamming window
     * @param length the number of elements in the array to apply the
     *     HammingWindow
     *
     * @return the index of the first array element of the next window
     */
    private int applyHammingWindow(double[] in, int length) {

        int windowCount;

	// if no windows can be created but there is some data,
	// pad it with zeros
	if (length < windowSize) {
	    double[] padded = new double[windowSize];
	    Arrays.fill(padded, 0);
	    System.arraycopy(in, 0, padded, 0, length);
	    in = padded;
	    windowCount = 1;
	} else {
	    windowCount = getWindowCount(length, windowSize, windowShift);
	}

        // create all the windows at once, not individually, saves time
        double[][] windows = new double[windowCount][windowSize];

        int windowStart = 0;

        for (int i = 0; i < windowCount; windowStart += windowShift, i++) {

            double[] myWindow = windows[i];
            
            // apply the Hamming Window function to the window of data
            for (int w = 0, s = windowStart; w < windowSize; s++, w++) {
                myWindow[w] = in[s] * hammingWindow[w];
            }
            
            // add the frame to the output queue
            outputQueue.add(new Audio
                            (myWindow, currentUtterance, 
                             currentCollectTime, currentFirstSampleNumber));
            currentFirstSampleNumber += windowShift;

            if (getDump()) {
                System.out.println
                    ("HAMMING_WINDOW " + Util.doubleArrayToString(myWindow));
            }
        }

        return windowStart;
    }


    /**
     * Returns the number of windows in the given array, given the windowSize
     * and windowShift.
     *
     * @param arraySize the size of the array
     * @param windowSize the window size
     * @param windowShift the window shift
     *
     * @return the number of windows
     */
    private static int getWindowCount(int arraySize, int windowSize,
                                      int windowShift) {
        if (arraySize < windowSize) {
            return 0;
        } else {
            int windowCount = 1;
            for (int windowEnd = windowSize;
                 windowEnd + windowShift <= arraySize;
                 windowEnd += windowShift) {
                windowCount++;
            }
            return windowCount;
        }
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
        if (occupancy + length > buffer.length) {
            length = buffer.length - occupancy;
	    throw new Error("Windower: overflow-buffer: attempting to fill " +
			    "buffer beyond its capacity.");
	}
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
