/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */


package edu.cmu.sphinx.frontend.feature;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;


/**
 * Subtracts the mean of all the input so far from the Data objects.
 * Unlike the {@link BatchCMN}, it does not read in the entire stream of Data
 * objects before it calculates the mean. It estimates the mean from
 * already seen data and subtracts the mean from the Data objects on
 * the fly. Therefore, there is no delay introduced by LiveCMN.
 * 
 * The Sphinx properties that affect this processor are defined by the
 * fields {@link #PROP_INITIAL_MEAN}, {@link #PROP_CMN_WINDOW}, and
 * {@link #PROP_CMN_SHIFT_WINDOW}. Please follow the link "Constant
 * Field Values" below to see the actual name of the
 * Sphinx properties.
 *
 * <p>The mean of all the input cepstrum so far is not reestimated
 * for each cepstrum. This mean is recalculated after every
 * {@link #PROP_CMN_SHIFT_WINDOW} cepstra.
 * This mean is estimated by dividing the sum of all input cepstrum so
 * far. After obtaining the mean, the sum is exponentially decayed by
 * multiplying it by the ratio:
 * <pre>
 * cmnWindow/(cmnWindow + number of frames since the last recalculation)
 * </pre>
 *
 * <p>This is a 1-to-1 processor.
 *
 * @see BatchCMN
 */
public class LiveCMN extends BaseDataProcessor {


    /**
     * The name of the SphinxProperty for the initial cepstral mean.
     * This is a front-end dependent magic number.
     */
    public static final String PROP_INITIAL_MEAN
        = "initialMean";


    /**
     * The default value for PROP_INITIAL_MEAN.
     */
    public static final float PROP_INITIAL_MEAN_DEFAULT = 12.0f;
    
    
    /**
     * The name of the SphinxProperty for the live CMN window size.
     */
    public static final String PROP_CMN_WINDOW = "cmnWindow";


    /**
     * The default value for PROP_CMN_WINDOW.
     */
    public static final int PROP_CMN_WINDOW_DEFAULT = 100;
    

    /**
     * The name of the SphinxProperty for the CMN shifting window.
     * The shifting window specifies how many cepstrum after which
     * we re-calculate the cepstral mean.
     */
    public static final String PROP_CMN_SHIFT_WINDOW
        = "shiftWindow";


    /**
     * The default value of PROP_CMN_SHIFT_WINDOW.
     */
    public static final int PROP_CMN_SHIFT_WINDOW_DEFAULT = 160;
 

    private double[] currentMean;   // array of current means
    private double[] sum;           // array of current sums
    private double initialMean;     // initial mean, magic number
    private int numberFrame;        // total number of input Cepstrum
    private int cmnShiftWindow;     // # of Cepstrum to recalculate mean
    private int cmnWindow;

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String,
     *      edu.cmu.sphinx.util.props.Registry)
     */
    public void register(String name, Registry registry)
            throws PropertyException {
        super.register(name, registry);
	registry.register(PROP_INITIAL_MEAN, PropertyType.DOUBLE);
	registry.register(PROP_CMN_WINDOW, PropertyType.INT);
	registry.register(PROP_CMN_SHIFT_WINDOW, PropertyType.INT);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
	initialMean = ps.getDouble (PROP_INITIAL_MEAN, PROP_INITIAL_MEAN_DEFAULT);
	cmnWindow = ps.getInt (PROP_CMN_WINDOW, PROP_CMN_WINDOW_DEFAULT);
	cmnShiftWindow = ps.getInt (PROP_CMN_SHIFT_WINDOW, PROP_CMN_SHIFT_WINDOW_DEFAULT);
    }

    /**
     * Initializes this LiveCMN.
     */
    public void initialize() {
        super.initialize();
    }


    /**
     * Initializes the currentMean and sum arrays with the given cepstrum
     * length.
     *
     * @param cepstrumLength the length of the cepstrum
     */
    private void initMeansSums(int cepstrumLength) {
	currentMean = new double[cepstrumLength];
	currentMean[0] = initialMean;
	sum = new double[cepstrumLength];
    }


    /**
     * Returns the next Data object, which is a normalized Data
     * produced by this class. Signals are returned unmodified.
     *
     * @return the next available Data object, returns null if no
     *         Data object is available
     *
     * @throws DataProcessingException if there is a data processing error
     */
    public Data getData() throws DataProcessingException {
	
        Data input = getPredecessor().getData();

        getTimer().start();

        if (input != null) {
            if (input instanceof DoubleData) {
		DoubleData data = (DoubleData) input;
                if (sum == null) {
                    initMeansSums(data.getValues().length);
                }
                normalize(data);
            } else if (input instanceof DataEndSignal) {
                updateMeanSumBuffers();
            }
        }

        getTimer().stop();

        return input;
    }
    

    /**
     * Normalizes the given Data with using the currentMean array.
     * Updates the sum array with the given Data.
     *
     * @param cepstrumObject the Data object to normalize
     */
    private void normalize(DoubleData cepstrumObject) {

        double[] cepstrum = cepstrumObject.getValues();

        if (cepstrum.length != sum.length) {
            throw new Error("Data length (" + cepstrum.length +
                            ") not equal sum array length (" + 
                            sum.length + ")");
        }

        for (int j = 0; j < cepstrum.length; j++) {
            sum[j] += cepstrum[j];
            cepstrum[j] -= currentMean[j];
        }

        numberFrame++;

        if (numberFrame > cmnShiftWindow) {
            updateMeanSumBuffers();
        }
    }


    /**
     * Updates the currentMean buffer with the values in the sum buffer.
     * Then decay the sum buffer exponentially, i.e., divide the sum
     * with numberFrames.
     */
    private void updateMeanSumBuffers() {

        if (numberFrame > 0) {

            // update the currentMean buffer with the sum buffer
            double sf = (double) (1.0/numberFrame);
            
            System.arraycopy(sum, 0, currentMean, 0, sum.length);
            
            multiplyArray(currentMean, sf);
            
            // decay the sum buffer exponentially
            if (numberFrame >= cmnShiftWindow) {
                multiplyArray(sum, (sf * cmnWindow));
                numberFrame = cmnWindow;
            }
        }
    }


    /**
     * Multiplies each element of the given array by the multiplier.
     *
     * @param array the array to multiply
     * @param multiplier the amount to multiply by
     */
    private static final void multiplyArray(double[] array,
                                            double multiplier) {
        for (int i = 0; i < array.length; i++) {
            array[i] *= multiplier;
        }
    }
}
