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
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;


/**
 * Applies a logarithm and then a Discrete Cosine Transform (DCT) to the input
 * data. The input data is normally the mel spectrum. It has been proven that,
 * for a sequence of real numbers, the discrete cosine transform is equivalent
 * to the discrete Fourier transform. Therefore, this class corresponds to the
 * last stage of converting a signal to cepstra, defined as the inverse Fourier
 * transform of the logarithm of the Fourier transform of a signal. The
 * property {@link #PROP_CEPSTRUM_LENGTH}refers to the dimensionality of the
 * coefficients that are actually returned, defaulting to
 * {@link #PROP_CEPSTRUM_LENGTH_DEFAULT}. When the input is mel-spectrum, the
 * vector returned is the MFCC (Mel-Frequency Cepstral Coefficient) vector,
 * where the 0-th element is the energy value.
 */
public class DiscreteCosineTransform extends BaseDataProcessor {
    /**
     * The name of the Sphinx Property for the number of filters in the
     * filterbank.
     */
    public static final String PROP_NUMBER_FILTERS = "numFilters";
    /**
     * The default value for PROP_NUMBER_FILTERS.
     */
    public static final int PROP_NUMBER_FILTERS_DEFAULT = 40;
    
    /**
     * The name of the sphinx property for the size of the ceptrum
     */
    public static final String PROP_CEPSTRUM_LENGTH = "cepstrumLength";
    
    /**
     * The default value for PROP_CEPSTRUM_LENGTH
     */
    public static final int PROP_CEPSTRUM_LENGTH_DEFAULT = 13;
    
    
    private int cepstrumSize; // size of a Cepstrum
    private int numberMelFilters; // number of mel-filters
    private double[][] melcosine;

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String,
     *      edu.cmu.sphinx.util.props.Registry)
     */
    public void register(String name, Registry registry)
            throws PropertyException {
        super.register(name, registry);
        registry.register(PROP_NUMBER_FILTERS, PropertyType.INT);
        registry.register(PROP_CEPSTRUM_LENGTH, PropertyType.INT);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        numberMelFilters = ps.getInt(PROP_NUMBER_FILTERS,
                PROP_NUMBER_FILTERS_DEFAULT);
        cepstrumSize = ps.getInt(PROP_CEPSTRUM_LENGTH, PROP_CEPSTRUM_LENGTH_DEFAULT);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.frontend.DataProcessor#initialize(edu.cmu.sphinx.frontend.CommonConfig)
     */
    public void initialize() {
        super.initialize();
    }   


    /**
     * Compute the MelCosine filter bank.
     */
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
     * Returns the next DoubleData object, which is the mel cepstrum of the
     * input frame. Signals are returned unmodified.
     * 
     * @return the next available DoubleData melcepstrum, or Signal object, or
     *         null if no Data is available
     * 
     * @throws DataProcessingException
     *                 if a data processing error occurred
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
     * @param input
     *                a MelSpectrum frame
     * 
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
        // create the cepstrum by apply the melcosine filter
        double[] cepstrum = applyMelCosine(melspectrum);
        DoubleData output = new DoubleData(cepstrum, input.getSampleRate(),
                input.getCollectTime(),
                input.getFirstSampleNumber());
        return output;
    }

    /**
     * Apply the MelCosine filter to the given melspectrum.
     * 
     * @param melspectrum
     *                the MelSpectrum data
     * 
     * @return MelCepstrum data produced by apply the MelCosine filter to the
     *         MelSpectrum data
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
}
