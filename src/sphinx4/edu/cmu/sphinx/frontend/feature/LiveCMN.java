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

import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Double;
import edu.cmu.sphinx.util.props.S4Integer;

/**
 * Subtracts the mean of all the input so far from the Data objects. Unlike the {@link BatchCMN}, it does not read in
 * the entire stream of Data objects before it calculates the mean. It estimates the mean from already seen data and
 * subtracts the mean from the Data objects on the fly. Therefore, there is no delay introduced by LiveCMN.
 * <p/>
 * The Sphinx properties that affect this processor are defined by the fields {@link #PROP_INITIAL_MEAN}, {@link
 * #PROP_CMN_WINDOW}, and {@link #PROP_CMN_SHIFT_WINDOW}. Please follow the link "Constant Field Values" below to see
 * the actual name of the Sphinx properties.
 * <p/>
 * <p>The mean of all the input cepstrum so far is not reestimated for each cepstrum. This mean is recalculated after
 * every {@link #PROP_CMN_SHIFT_WINDOW} cepstra. This mean is estimated by dividing the sum of all input cepstrum so
 * far. After obtaining the mean, the sum is exponentially decayed by multiplying it by the ratio:
 * <pre>
 * cmnWindow/(cmnWindow + number of frames since the last recalculation)
 * </pre>
 * <p/>
 * <p>This is a 1-to-1 processor.
 *
 * @see BatchCMN
 */
public class LiveCMN extends BaseDataProcessor {


    /** The property for the initial cepstral mean. This is a front-end dependent magic number. */
    @S4Double(defaultValue = 12.0)
    public static final String PROP_INITIAL_MEAN = "initialMean";
    private double initialMean;     // initial mean, magic number

    /** The property for the live CMN window size. */
    @S4Integer(defaultValue = 100)
    public static final String PROP_CMN_WINDOW = "cmnWindow";
    private int cmnWindow;

    /**
     * The property for the CMN shifting window. The shifting window specifies how many cepstrum after
     * which we re-calculate the cepstral mean.
     */
    @S4Integer(defaultValue = 160)
    public static final String PROP_CMN_SHIFT_WINDOW = "shiftWindow";
    private int cmnShiftWindow;     // # of Cepstrum to recalculate mean


    private double[] currentMean;   // array of current means
    private double[] sum;           // array of current sums
    private int numberFrame;        // total number of input Cepstrum


    public LiveCMN(double initialMean, int cmnWindow, int cmnShiftWindow) {
        initLogger();
        this.initialMean = initialMean;
        this.cmnWindow = cmnWindow;
        this.cmnShiftWindow = cmnShiftWindow;
    }

    public LiveCMN() {

    }

    @Override
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        initialMean = ps.getDouble(PROP_INITIAL_MEAN);
        cmnWindow = ps.getInt(PROP_CMN_WINDOW);
        cmnShiftWindow = ps.getInt(PROP_CMN_SHIFT_WINDOW);
    }


    /** Initializes this LiveCMN. */
    @Override
    public void initialize() {
        super.initialize();
    }


    /**
     * Initializes the currentMean and sum arrays with the given cepstrum length.
     *
     * @param cepstrumLength the length of the cepstrum
     */
    private void initMeansSums(int cepstrumLength) {
        currentMean = new double[cepstrumLength];
        currentMean[0] = initialMean;

//         hack until we've fixed the NonSpeechDataFilter
        if (sum == null)
            sum = new double[cepstrumLength];
    }


    /**
     * Returns the next Data object, which is a normalized Data produced by this class. Signals are returned
     * unmodified.
     *
     * @return the next available Data object, returns null if no Data object is available
     * @throws DataProcessingException if there is a data processing error
     */
    @Override
    public Data getData() throws DataProcessingException {

        Data input = getPredecessor().getData();

        if (input instanceof DataStartSignal) {
            sum = null;
            numberFrame = 0;
        }

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
     * Normalizes the given Data with using the currentMean array. Updates the sum array with the given Data.
     *
     * @param cepstrumObject the Data object to normalize
     */
    private void normalize(DoubleData cepstrumObject) {

        double[] cepstrum = cepstrumObject.getValues();

        if (cepstrum.length != sum.length) {
            throw new Error("Data length (" + cepstrum.length +
                    ") not equal sum array length (" +
                    sum.length + ')');
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
     * Updates the currentMean buffer with the values in the sum buffer. Then decay the sum buffer exponentially, i.e.,
     * divide the sum with numberFrames.
     */
    private void updateMeanSumBuffers() {

        if (numberFrame > 0) {

            // update the currentMean buffer with the sum buffer
            double sf = 1.0 / numberFrame;

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
     * @param array      the array to multiply
     * @param multiplier the amount to multiply by
     */
    private static void multiplyArray(double[] array, double multiplier) {
        for (int i = 0; i < array.length; i++) {
            array[i] *= multiplier;
        }
    }
}
