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

import edu.cmu.sphinx.util.SphinxProperties;

import java.io.IOException;


/**
 * Apply Cepstral Mean Normalization (CMN) to a Cepstrum.
 * It subtracts the mean of all the input so far
 * from the cepstrum. The Sphinx properties that affect this processor
 * are: <pre>
 * edu.cmu.sphinx.frontend.cmn.initialCepstralMean
 * edu.cmu.sphinx.frontend.cmn.windowSize
 * edu.cmu.sphinx.frontend.cmn.shiftWindow </pre>
 *
 * <p>The mean of all the input cepstrum so far is not recalculated
 * for each cepstrum. This mean is recalculated after
 * <code>edu.cmu.sphinx.frontend.cmn.shiftWindow</code> cepstra.
 * This mean is calculated by dividing the sum of all input cepstrum so
 * far by the number of input cepstrum. After obtaining the mean,
 * the sum is exponentially by multiplying it by the ratio: <pre>
 * cmnWindow/(cmnWindow + number of frames since the last recalculation)
 * </pre>
 *
 * <p>This is a 1-to-1 processor.
 *
 * @see Cepstrum
 */
public class LiveCMN extends DataProcessor implements CepstrumSource {


    /**
     * The name of the SphinxProperty for the initial cepstral mean,
     * which has a default value of 12.0F.
     * This is a front-end dependent magic number.
     */
    private static final String PROP_INITIAL_MEAN = 
    "cmn.live.initialCepstralMean";


    /**
     * The name of the SphinxProperty for the CMN window size,
     * which has a default value of 500.
     */
    private static final String PROP_CMN_WINDOW = 
    "cmn.live.windowSize";


    /**
     * The name of the SphinxProperty for the CMN shifting window,
     * which has a default value of 800.
     * The shifting window specifies how many cepstrum after which
     * we re-calculate the cepstral mean.
     */
    private static final String PROP_CMN_SHIFT_WINDOW =
    "cmn.live.shiftWindow";


    private float[] currentMean;   // array of current means
    private float[] sum;           // array of current sums
    private float initialMean;     // initial mean, magic number
    private int numberFrame;       // total number of input Cepstrum
    private int cmnShiftWindow;    // # of Cepstrum to recalculate mean
    private int cmnWindow;
    private int cepstrumLength;    // length of a Cepstrum
    private CepstrumSource predecessor;


    /**
     * Constructs a default LiveCMN with the given
     * SphinxProperties context.
     *
     * @param name the name of this LiveCMN
     * @param context the context of the SphinxProperties to use
     * @param predecessor the CepstrumSource from which this normalizer
     *    obtains Cepstrum to normalize
     *
     * @throws IOException if an I/O error occurs
     */
    public LiveCMN(String name, String context, CepstrumSource predecessor)
        throws IOException {
        super(name, context);
        initSphinxProperties();
	initMeansSums();
        this.predecessor = predecessor;
    }


    /**
     * Sets the predecessor (i.e., the previous processor)
     * of this LiveCMN processor.
     *
     * @param predecessor the predecessor
     */
    public void setPredecessor(CepstrumSource predecessor) {
        this.predecessor = predecessor;
    }


    /**
     * Initializes the currentMean and sum arrays.
     */
    private void initMeansSums() {
	currentMean = new float[cepstrumLength];
	currentMean[0] = initialMean;
	sum = new float[cepstrumLength];
    }


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     *
     * @throws IOException if an I/O error occurs
     */
    private void initSphinxProperties() throws IOException {
	initialMean = getFloatAcousticProperty(PROP_INITIAL_MEAN, 12.0f);
	cmnWindow = getIntAcousticProperty(PROP_CMN_WINDOW, 500);
	cmnShiftWindow = getIntAcousticProperty(PROP_CMN_SHIFT_WINDOW, 800);

	cepstrumLength = getSphinxProperties().getInt
            (FrontEnd.PROP_CEPSTRUM_SIZE, 13);
    }
	

    /**
     * Returns the next Cepstrum object, which is a normalized Cepstrum
     * produced by this class. However, it can also be a Cepstrum object
     * carrying a Signal.UTTERANCE_END signal.
     *
     * @return the next available Cepstrum object, returns null if no
     *     Cepstrum object is available
     *
     * @throws java.io.IOException if there is an error reading
     * the Cepstrum objects
     */
    public Cepstrum getCepstrum() throws IOException {
	
        Cepstrum input = predecessor.getCepstrum();

        getTimer().start();

        if (input != null) {
            if (input.hasContent()) {
                normalize(input);
            } else if (input.hasSignal(Signal.UTTERANCE_END)) {
                updateMeanSumBuffers();
            }
        }

        getTimer().stop();

        return input;
    }
    

    /**
     * Normalizes the given Cepstrum with using the currentMean array.
     * Updates the sum array with the given Cepstrum.
     *
     * @param input a Cepstrum
     */
    private void normalize(Cepstrum cepstrumObject) {

        float[] cepstrum = cepstrumObject.getCepstrumData();
        for (int j = 0; j < cepstrum.length; j++) {
            sum[j] += cepstrum[j];
            cepstrum[j] -= currentMean[j];
        }

        numberFrame++;

        if (numberFrame > cmnShiftWindow) {
            updateMeanSumBuffers();
        }

        if (getDump()) {
            System.out.println("CMN_CEPSTRUM " + cepstrumObject.toString());
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
            float sf = (float) (1.0/numberFrame);
            
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
     * @param multipler the amount to multiply by
     */
    private static final void multiplyArray(float[] array,
                                            float multiplier) {
        for (int i = 0; i < array.length; i++) {
            array[i] *= multiplier;
        }
    }
}
