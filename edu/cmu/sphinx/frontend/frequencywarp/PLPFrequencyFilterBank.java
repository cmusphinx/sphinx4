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

package edu.cmu.sphinx.frontend.frequencywarp;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;

/**
 * Filters an input power spectrum through a PLP filterbank. The
 * filters in the filterbank are placed in the frequency axis so as to
 * mimic the critical band, representing different perceptual effect
 * at different frequency bands. The filter outputs are also scaled
 * for equal loudness preemphasis. The filter shapes are defined by
 * the {@link PLPFilter} class. Like the {@link
 * MelFrequencyFilterBank}, this filter bank has characteristics
 * defined by the {@link #PROP_NUMBER_FILTERS number of filters}, the
 * {@link #PROP_MIN_FREQ minimum frequency}, and the {@link
 * #PROP_MAX_FREQ maximum frequency}. Unlike the
 * {@link MelFrequencyFilterBank}, the minimum and maximum frequencies here
 * refer to the <b>center</b> frequencies of the filters located at
 * the leftmost and rightmost positions, and not to the
 * edges. Therefore, this filter bank spans a frequency range that
 * goes beyond the limits suggested by the minimum and maximum
 * frequencies.
 *
 * @author <a href="mailto:rsingh@cs.cmu.edu">rsingh</a>
 * @version 1.0
 * @see PLPFilter
 */
public class PLPFrequencyFilterBank extends BaseDataProcessor {
    
    /**
     * The name of the Sphinx Property for the number of filters in
     * the filterbank.
     */
    public static final String PROP_NUMBER_FILTERS = "numFilters";


    /**
     * The default value of PROP_NUMBER_FILTERS.
     */
    public static final int PROP_NUMBER_FILTERS_DEFAULT = 32;


    /**
     * The name of the Sphinx Property for the center frequency
     * of the lowest filter in the filterbank.
     */
    public static final String PROP_MIN_FREQ = "minfreq";


    /**
     * The default value of PROP_MIN_FREQ.
     */
    public static final int PROP_MIN_FREQ_DEFAULT = 130;


    /**
     * The name of the Sphinx Property for the center frequency
     * of the highest filter in the filterbank.
     */
    public static final String PROP_MAX_FREQ = "maxfreq";


    /**
     * The default value of PROP_MAX_FREQ.
     */
    public static final int PROP_MAX_FREQ_DEFAULT = 3600;


    private int sampleRate;
    private int numberFftPoints;
    private int numberFilters;
    private double minFreq;
    private double maxFreq;
    private PLPFilter[] criticalBandFilter;
    private double[] equalLoudnessScaling;

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String,
     *      edu.cmu.sphinx.util.props.Registry)
     */
    public void register(String name, Registry registry)
            throws PropertyException {
        super.register(name, registry);
        registry.register(PROP_MIN_FREQ, PropertyType.DOUBLE);
        registry.register(PROP_MAX_FREQ, PropertyType.DOUBLE);
        registry.register(PROP_NUMBER_FILTERS, PropertyType.INT);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        minFreq = ps.getDouble (PROP_MIN_FREQ, PROP_MIN_FREQ_DEFAULT);
        maxFreq = ps.getDouble (PROP_MAX_FREQ, PROP_MAX_FREQ_DEFAULT);
        numberFilters = ps.getInt (PROP_NUMBER_FILTERS, PROP_NUMBER_FILTERS_DEFAULT);
    }

    /**
     * Initializes this PLPFrequencyFilterBank object 
     */
    public void initialize() {
        super.initialize();
    }

    /**
     * Build a PLP filterbank with the parameters given.
     * The center frequencies of the PLP filters will be
     * uniformly spaced between the minimum and maximum
     * analysis frequencies on the Bark scale.
     * on the Bark scale.
     *
     * @throws IllegalArgumentException
     */
    private void buildCriticalBandFilterbank() throws IllegalArgumentException{
	double minBarkFreq;
	double maxBarkFreq;
	double deltaBarkFreq;
	double nyquistFreq;
	double centerFreq;
	int numberDFTPoints = (numberFftPoints>>1) + 1;
	double[] DFTFrequencies;

	/* This is the same class of warper called by PLPFilter.java */
	FrequencyWarper bark = new FrequencyWarper();

	this.criticalBandFilter = new PLPFilter[numberFilters];

	if (numberFftPoints == 0) {
	    throw new IllegalArgumentException("Number of FFT points is zero");
	}
	if (numberFilters < 1) {
	    throw new IllegalArgumentException("Number of filters illegal: "
					       + numberFilters);
	}

	DFTFrequencies = new double[numberDFTPoints];
	nyquistFreq = sampleRate/2;
	for (int i=0; i<numberDFTPoints; i++){
	    DFTFrequencies[i] = (double)i*nyquistFreq/
                                        (double)(numberDFTPoints-1);
	}

	/**
	 * Find center frequencies of filters in the Bark scale
	 * translate to linear frequency and create PLP filters
	 * with these center frequencies.
	 *
	 * Note that minFreq and maxFreq specify the CENTER FREQUENCIES
	 * of the lowest and highest PLP filters
	 */


	minBarkFreq = bark.hertzToBark(minFreq);
	maxBarkFreq = bark.hertzToBark(maxFreq);

        if (numberFilters < 1) {
            throw new IllegalArgumentException("Number of filters illegal: "
		                                               + numberFilters);
	}
	deltaBarkFreq = (maxBarkFreq - minBarkFreq)/(numberFilters+1);

	for (int i = 0; i < numberFilters; i++) {
	    centerFreq = bark.barkToHertz(minBarkFreq + i*deltaBarkFreq);
	    criticalBandFilter[i] = new PLPFilter(DFTFrequencies,centerFreq);
	}
    }

    /**
     * This function return the equal loudness preemphasis factor
     * at any frequency. The preemphasis function is given by
     *
     * E(w) = (w^2+56.8e6)*w^4/((w^2+6.3e6)^2(w^2+0.38e9)(w^6+9.58e26))
     *
     * where w is frequency in radians/second
     */
    private double loudnessScalingFunction(double freq){
	double freqsquared = freq*freq;
	double freqfourth = freqsquared*freqsquared;
	double freqsixth = freqfourth*freqsquared;

	double numerator = (freqsquared + 56.8e6) * freqfourth;
	double denominator = Math.pow((freqsquared + 6.3e6),2) * 
	                              (freqsquared + 0.38e9) *
	                              (freqsixth + 9.58e26);

	return numerator/denominator;
    }

    /**
     * Create an array of equal loudness preemphasis scaling terms
     * for all the filters
     */
    private void buildEqualLoudnessScalingFactors(){
	double centerFreq;

	equalLoudnessScaling = new double[numberFilters];
	for (int i = 0; i < numberFilters; i++){
	    centerFreq = criticalBandFilter[i].centerFreqInHz * 2.0 * Math.PI;
	    equalLoudnessScaling[i] = loudnessScalingFunction(centerFreq);
	}
    }

    /**
     * Process data, creating the power spectrum from an input
     * audio frame.
     *
     * @param input input power spectrum
     *
     * @return PLP power spectrum
     *
     * @throws java.lang.IllegalArgumentException
     *
     */
    private DoubleData process(DoubleData input) throws
        IllegalArgumentException {

	double[] in = input.getValues();
        
        if (criticalBandFilter == null || 
            sampleRate != input.getSampleRate()) {
            numberFftPoints = (in.length - 1) * 2;
            sampleRate = input.getSampleRate();
            buildCriticalBandFilterbank();
            buildEqualLoudnessScalingFactors();

        } else if (in.length != ((numberFftPoints >> 1) + 1)) {
	    throw new IllegalArgumentException
               ("Window size is incorrect: in.length == " + in.length +
                 ", numberFftPoints == " + ((numberFftPoints >> 1) + 1));
	}

	double[] outputPLPSpectralArray = new double[numberFilters];

	/**
	 * Filter input power spectrum
	 */
	for (int i = 0; i < numberFilters; i++) {
	    // First compute critical band filter output
	    outputPLPSpectralArray[i] = criticalBandFilter[i].filterOutput(in);
	    // Then scale it for equal loudness preemphasis
	    outputPLPSpectralArray[i] *= equalLoudnessScaling[i];
	}

	DoubleData output = new DoubleData
            (outputPLPSpectralArray, input.getSampleRate(),
             input.getCollectTime(), input.getFirstSampleNumber());

        return output;
    }


    /**
     * Reads the next Data object, which is the power spectrum of an
     * audio input frame. However, it can also be other Data objects
     * like a Signal, which is returned unmodified.
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
                input = process((DoubleData) input);
            }
        }

        getTimer().stop();
	
      	return input;
    }	
}
