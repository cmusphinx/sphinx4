/*
 * Copyright 1999-2004 Carnegie Mellon University.  
 * Portions Copyright 2002-2004 Sun Microsystems, Inc.  
 * Portions Copyright 2002-2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.frontend.window;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.util.DataUtil;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;


/**
 * Slices up a Data object into a number of overlapping windows
 * (usually refered to as "frames" in the speech world). In order to
 * minimize the signal discontinuities at the boundaries of each
 * frame, we multiply each frame with a raised cosine windowing
 * function. Moreover, the system uses overlapping windows to capture
 * information that may occur at the window boundaries. These events
 * would not be well represented if the windows were juxtaposed.
 * <p> The number of resulting windows depends on the {@link
 * #PROP_WINDOW_SIZE_MS window size} and the {@link
 * #PROP_WINDOW_SHIFT_MS window shift} (commonly known as frame shift
 * in speech world). Figure 1 shows the relationship between the
 * original data stream, the window size, the window shift, and the
 * windows returned.
 * <p> <img src="doc-files/framing.jpg"> <br><b>Figure 1: Relationship
 * between original data, window size, window shift, and the windows
 * returned.</b>
 * <p> The raised cosine windowing function will be applied to each such
 * window. Since the {@link #getData()} method returns a
 * window, and multiple windows are created for each Data object, this
 * is a 1-to-many processor. Also note that the returned windows
 * should have the same number of data points as the windowing
 * function.
 * <p> The applied windowing function, <i>W(n)</i>, of length <i>N</i>
 * (the window size), is given by the following formula:
 * <pre>
 * W(n) = (1-a) - (a * cos((2 * Math.PI * n)/(N - 1)))
 * </pre>
 * where <b>a</b> is commonly known as the "alpha" value. This
 * variable can be set by the user using the property defined by
 * {@link #PROP_ALPHA}, which has a default value of {@link
 * #PROP_ALPHA_DEFAULT}. Please follow the links to the see the
 * constant field values. 
 * Some values of alpha receive special names, since they are used so
 * often. A value of 0.46 for the alpha results in a window named
 * Hamming window. A value of 0.5 results in the Hanning window. And a
 * value of 0 results in the Rectangular window. The default for this
 * system is the Hamming window, with alpha 0.46 (but check the value
 * in {@link #PROP_ALPHA_DEFAULT}!). Figure 2 below shows the Hamming
 * window function (a = 0.46), using our default window size of 25.625
 * ms and assuming a sample rate of 16kHz, thus yielding 410 samples
 * per window.
 * <p> <img src="doc-files/hamming-window.gif"> <br><b>Figure 2: The
 * Hamming window function.</b>
 *
 * @see Data
 */
public class RaisedCosineWindower extends BaseDataProcessor {
    /**
     * The SphinxProperty name for window size in milliseconds.
     */
    public static final String PROP_WINDOW_SIZE_MS = "windowSizeInMs";
    /**
     * The default value for PROP_WINDOW_SIZE_MS.
     */
    public static final float PROP_WINDOW_SIZE_MS_DEFAULT = 25.625f;
    /**
     * The SphinxProperty name for window shift in milliseconds, which has a
     * default value of 10F.
     */
    public static final String PROP_WINDOW_SHIFT_MS = "windowShiftInMs";
    /**
     * The default value for PROP_WINDOW_SHIFT_MS.
     */
    public static final float PROP_WINDOW_SHIFT_MS_DEFAULT = 10;
    /**
     * The name of the SphinxProperty for the alpha value of the Window, which
     * has a default value of 0.46 (double), which is the value for the
     * RaisedCosineWindow.
     */
    public static final String PROP_ALPHA = "alpha";
    

    /**
     * The default value for PROP_ALPHA.
     */
    public static final double PROP_ALPHA_DEFAULT = 0.46;
    private double alpha; // the window alpha value
    private double[] cosineWindow; // the raised consine window
    private int windowSize; // size of each window
    private int windowShift; // the window size
    private List outputQueue; // cache for output windows
    private DoubleBuffer overflowBuffer; // cache for overlapped audio regions
    private long currentCollectTime;
    private long currentFirstSampleNumber;
    private float windowSizeInMs;
    private float windowShiftInMs;
    private int sampleRate = 0;

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String,
     *      edu.cmu.sphinx.util.props.Registry)
     */
    public void register(String name, Registry registry)
            throws PropertyException {
        super.register(name, registry);
        registry.register(PROP_ALPHA, PropertyType.DOUBLE);
        registry.register(PROP_WINDOW_SIZE_MS, PropertyType.FLOAT);
        registry.register(PROP_WINDOW_SHIFT_MS, PropertyType.FLOAT);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        alpha = ps.getDouble(PROP_ALPHA, PROP_ALPHA_DEFAULT);
        windowSizeInMs = ps.getFloat(PROP_WINDOW_SIZE_MS,
                PROP_WINDOW_SIZE_MS_DEFAULT);
        windowShiftInMs = ps.getFloat(PROP_WINDOW_SHIFT_MS,
                PROP_WINDOW_SHIFT_MS_DEFAULT);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.frontend.DataProcessor#initialize(edu.cmu.sphinx.frontend.CommonConfig)
     */
    public void initialize() {
        super.initialize();

        // createWindow();
        outputQueue = new LinkedList();
    }
    

    /**
     * Creates the Hamming Window.
     */
    private void createWindow() {
        int windowSize =
            DataUtil.getSamplesPerWindow(sampleRate, windowSizeInMs);
        
        windowShift = DataUtil.getSamplesPerShift(sampleRate, windowShiftInMs);

       	this.cosineWindow = new double[windowSize];

	if (cosineWindow.length > 1){
	    double oneMinusAlpha = (1 - alpha);
	    for (int i = 0; i < cosineWindow.length; i++) {
		cosineWindow[i] = oneMinusAlpha - alpha *
		    Math.cos(2 * Math.PI * i / 
                             ((double) cosineWindow.length - 1.0));
	    }
	}

        overflowBuffer = new DoubleBuffer(windowSize);
    }


    /**
     * Returns the next Data object, which is usually a window of the input
     * Data, with the windowing function applied to it.
     *
     * @return the next available Data object, returns null if no
     *     Data object is available
     *
     * @throws DataProcessingException if a data processing error occurred
     *
     * @see Data
     */
    public Data getData() throws DataProcessingException {
        
        if (outputQueue.size() == 0) {
            Data input = getPredecessor().getData();
            
            getTimer().start();

            if (input != null) {
                if (input instanceof DoubleData) {
                    DoubleData data = (DoubleData) input;
                    if (currentFirstSampleNumber == -1) {
                        currentFirstSampleNumber = data.getFirstSampleNumber();
                    }
                    if (cosineWindow == null ||
                        sampleRate != data.getSampleRate()) {
                        sampleRate = data.getSampleRate();
                        createWindow();
                    }
                    // process the Data, and output the windows
                    process(data);
                } else {
                    if (input instanceof DataStartSignal) {
                        // reset the current first sample number
                        currentFirstSampleNumber = -1; 
                    } else if (input instanceof DataEndSignal) {
                        // end of utterance handling
                        processUtteranceEnd();
                    }
                    outputQueue.add(input);
                }
            }

            getTimer().stop();
        }

        if (outputQueue.size() > 0) {
            Data output = (Data) outputQueue.remove(0);
            if (output instanceof DoubleData) {
                assert ((DoubleData) output).getValues().length == 
                    cosineWindow.length;
            }
            return output;
        } else {
            return null;
        }
    }

    /**
     * Applies the Windowing to the given Data. The resulting windows
     * are cached in the outputQueue.
     *
     * @param input the input Data object
     *
     * @throws DataProcessingException if a data processing error occurs
     */
    private void process(DoubleData input) throws DataProcessingException {

        currentCollectTime = input.getCollectTime();
        
	double[] in = input.getValues();
        int length = overflowBuffer.getOccupancy() + in.length;

        List dataList = new LinkedList();
        dataList.add(input);
        
        Data utteranceEnd = null;
        
        // read in more Data if we have under one window's length of data
        while (length < cosineWindow.length) {
            Data next = getPredecessor().getData();
            if (next instanceof DoubleData) {
                dataList.add(next);
                length += ((DoubleData) next).getValues().length;
            } else if (next instanceof DataEndSignal) {
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
            for (Iterator i = dataList.iterator(); i.hasNext(); ) {
                DoubleData next = (DoubleData) i.next();
                double[] samples = next.getValues();
                System.arraycopy(samples, 0, allSamples, start, 
                                 samples.length);
                start += samples.length;
            }
	}
        
        // apply Hamming window
        int residual = applyRaisedCosineWindow(allSamples, length);
        
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
     * What happens when an DataEndSignal is
     * received. Basically pads up to a window of the overflow buffer
     * with zeros, and then apply the Hamming window to it.
     */
    private void processUtteranceEnd() {
        overflowBuffer.padWindow(cosineWindow.length);
        applyRaisedCosineWindow
            (overflowBuffer.getBuffer(), cosineWindow.length);
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
     *     RaisedCosineWindow
     *
     * @return the index of the first array element of the next window
     */
    private int applyRaisedCosineWindow(double[] in, int length) {

        int windowCount;

	// if no windows can be created but there is some data,
	// pad it with zeros
	if (length < cosineWindow.length) {
	    double[] padded = new double[cosineWindow.length];
            Arrays.fill(padded, 0);
	    System.arraycopy(in, 0, padded, 0, length);
	    in = padded;
	    windowCount = 1;
	} else {
	    windowCount = getWindowCount
                (length, cosineWindow.length, windowShift);
	}

        // create all the windows at once, not individually, saves time
        double[][] windows = new double[windowCount][cosineWindow.length];

        int windowStart = 0;

        for (int i = 0; i < windowCount; windowStart += windowShift, i++) {

            double[] myWindow = windows[i];
            
            // apply the Hamming Window function to the window of data
            for (int w = 0, s = windowStart; w < myWindow.length; s++, w++) {
                myWindow[w] = in[s] * cosineWindow[w];
            }
            
            // add the frame to the output queue
            outputQueue.add(new DoubleData
                            (myWindow, sampleRate, 
                             currentCollectTime, currentFirstSampleNumber));
            currentFirstSampleNumber += windowShift;
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
    DoubleBuffer(int size) {
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
     * @param src the array to copy from
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
	    throw new Error("RaisedCosineWindower: " + 
                            "overflow-buffer: attempting to fill " +
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
