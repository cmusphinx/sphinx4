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
package edu.cmu.sphinx.frontend.transform;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.util.props.*;

/**
 * Applies a logarithm and then a Discrete Cosine Transform (DCT) to the input data. The input data is normally the mel
 * spectrum. It has been proven that, for a sequence of real numbers, the discrete cosine transform is equivalent to the
 * discrete Fourier transform. Therefore, this class corresponds to the last stage of converting a signal to cepstra,
 * defined as the inverse Fourier transform of the logarithm of the Fourier transform of a signal. The property {@link
 * #PROP_CEPSTRUM_LENGTH}refers to the dimensionality of the coefficients that are actually returned, defaulting to
 * {@link #PROP_CEPSTRUM_LENGTH_DEFAULT}. When the input is mel-spectrum, the vector returned is the MFCC (Mel-Frequency
 * Cepstral Coefficient) vector, where the 0-th element is the energy value.
 */
@SuppressWarnings({"UnnecessaryLocalVariable"})
public class DiscreteCosineTransform extends BaseDataProcessor {

    /** The name of the Sphinx Property for the number of filters in the filterbank. */
    @S4Integer(defaultValue = 40)
    public static final String PROP_NUMBER_FILTERS = "numberFilters";
    /** The default value for PROP_NUMBER_FILTERS. */
    public static final int PROP_NUMBER_FILTERS_DEFAULT = 40;

    /** The name of the sphinx property for the size of the ceptrum */
    @S4Integer(defaultValue = 13)
    public static final String PROP_CEPSTRUM_LENGTH = "cepstrumLength";

    /** The default value for PROP_CEPSTRUM_LENGTH */
    public static final int PROP_CEPSTRUM_LENGTH_DEFAULT = 13;

    /** The name of the sphinx property for the size of the ceptrum */
    @S4Boolean(defaultValue = false)
    public static final String PROP_DCT2 = "useDCT2";

    /** The default value for PROP_DCT2 */
    public static final boolean PROP_DCT2_DEFAULT = false;

    private int cepstrumSize; // size of a Cepstrum
    private int numberMelFilters; // number of mel-filters
    private double[][] melcosine;
    private boolean useDCT2;

    /*
    * (non-Javadoc)
    *
    * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
    */
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        numberMelFilters = ps.getInt(PROP_NUMBER_FILTERS
        );
        cepstrumSize = ps.getInt(PROP_CEPSTRUM_LENGTH);
        useDCT2 = ps.getBoolean(PROP_DCT2);
    }


    /*
    * (non-Javadoc)
    *
    * @see edu.cmu.sphinx.frontend.DataProcessor#initialize(edu.cmu.sphinx.frontend.CommonConfig)
    */
    public void initialize() {
        super.initialize();
    }


    /** Compute the MelCosine filter bank. */
    private void computeMelCosine() {
        melcosine = new double[cepstrumSize][numberMelFilters];
        double period = (double) 2 * numberMelFilters;
        for (int i = 0; i < cepstrumSize; i++) {
            double frequency = 2 * Math.PI * i / period;
            for (int j = 0; j < numberMelFilters; j++) {
                melcosine[i][j] = Math.cos(frequency * (j + 0.5));
            }
        }
    }


    /**
     * Returns the next DoubleData object, which is the mel cepstrum of the input frame. Signals are returned
     * unmodified.
     *
     * @return the next available DoubleData melcepstrum, or Signal object, or null if no Data is available
     * @throws DataProcessingException if a data processing error occurred
     */
    public Data getData() throws DataProcessingException {
        Data input = getPredecessor().getData(); // get the spectrum
        getTimer().start();
        if (input != null && input instanceof DoubleData) {
            input = process((DoubleData) input);
        }
        getTimer().stop();
        return input;
    }


    /**
     * Process data, creating the mel cepstrum from an input spectrum frame.
     *
     * @param input a MelSpectrum frame
     * @return a mel Cepstrum frame
     */
    private DoubleData process(DoubleData input)
            throws IllegalArgumentException {
        double[] melspectrum = input.getValues();

        if (melcosine == null) {
            numberMelFilters = melspectrum.length;
            computeMelCosine();

        } else if (melspectrum.length != numberMelFilters) {
            throw new IllegalArgumentException
                    ("MelSpectrum size is incorrect: melspectrum.length == " +
                            melspectrum.length + ", numberMelFilters == " +
                            numberMelFilters);
        }
        // first compute the log of the spectrum
        for (int i = 0; i < melspectrum.length; ++i) {
            if (melspectrum[i] > 0) {
                melspectrum[i] = Math.log(melspectrum[i]);
            } else {
                // in case melspectrum[i] isn't greater than 0
                // instead of trying to compute a log we just
                // assign a very small number
                melspectrum[i] = -1.0e+5;
            }
        }
	
	double[] cepstrum;
	
        // create the cepstrum by apply the melcosine filter
	if (useDCT2) {
            cepstrum = applyMelCosine2(melspectrum);
	} else {
	    cepstrum = applyMelCosine(melspectrum);
	}
        DoubleData output = new DoubleData(cepstrum, input.getSampleRate(),
                input.getCollectTime(),
                input.getFirstSampleNumber());
        return output;
    }


    /**
     * Apply the MelCosine filter to the given melspectrum.
     *
     * @param melspectrum the MelSpectrum data
     * @return MelCepstrum data produced by apply the MelCosine filter to the MelSpectrum data
     */
    private double[] applyMelCosine(double[] melspectrum) {
        // create the cepstrum
        double[] cepstrum = new double[cepstrumSize];
        double period = (double) numberMelFilters;
        double beta = 0.5;
        // apply the melcosine filter
        for (int i = 0; i < cepstrum.length; i++) {
            if (numberMelFilters > 0) {
                double[] melcosine_i = melcosine[i];
                int j = 0;
                cepstrum[i] += (beta * melspectrum[j] * melcosine_i[j]);
                for (j = 1; j < numberMelFilters; j++) {
                    cepstrum[i] += (melspectrum[j] * melcosine_i[j]);
                }
                cepstrum[i] /= period;
            }
        }
        return cepstrum;
    }
    
        /**
     * Apply the optimized MelCosine filter used in pocketsphinx to the given melspectrum.
     *
     * @param melspectrum the MelSpectrum data
     * @return MelCepstrum data produced by apply the MelCosine filter to the MelSpectrum data
     */
    private double[] applyMelCosine2 (double[] melspectrum) {
        // create the cepstrum
        double[] cepstrum = new double[cepstrumSize];
        double sqrt_inv_n = Math.sqrt(1.0 / numberMelFilters);
        double sqrt_inv_2n = Math.sqrt(2.0 / numberMelFilters);

	cepstrum[0] = melspectrum [0];
	for (int j = 1; j < numberMelFilters; j++) {
	    cepstrum[0] += melspectrum[j];
	}

	cepstrum[0] *= sqrt_inv_n;

        if (numberMelFilters <= 0) {
		return cepstrum;
	}
	
        for (int i = 1; i < cepstrum.length; i++) {
            double[] melcosine_i = melcosine[i];
            int j = 0;
            cepstrum[i] = 0;
            for (j = 0; j < numberMelFilters; j++) {
                    cepstrum[i] += (melspectrum[j] * melcosine_i[j]);
            }
            cepstrum[i] *= sqrt_inv_2n;
        }
        return cepstrum;
    }
}
