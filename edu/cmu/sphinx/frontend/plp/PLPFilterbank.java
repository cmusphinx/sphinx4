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


package edu.cmu.sphinx.frontend.plp;

import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.Filterbank;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.Spectrum;
import edu.cmu.sphinx.frontend.SpectrumAnalyzer;
import edu.cmu.sphinx.frontend.SpectrumSource;

import edu.cmu.sphinx.util.SphinxProperties;

import java.io.IOException;

/**
 * Filters an input power spectrum through a PLP filterbank. The
 * filters in the filterbank are placed in the frequency axis so as to
 * mimic the critical band, representing different perceptual effect
 * at different frequency bands.
 * The filter outputs are also scaled for equal loudness preemphasis.
 *
 * Created: Tue Dec 24 17:56:39 2002
 *
 * @author <a href="mailto:rsingh@cs.cmu.edu">rsingh</a>
 * @version 1.0
 */
public class PLPFilterbank extends DataProcessor implements Filterbank {


    /**
     * The name of the Sphinx Property for the number of filters in
     * the filterbank.
     */
    public static final String PROP_NUMBER_FILTERS = FrontEnd.PROP_PREFIX +
	"plp.numFilters";


    /**
     * The name of the Sphinx Property for the minimum frequency
     * covered by the filterbank.
     */
    public static final String PROP_MIN_FREQ = FrontEnd.PROP_PREFIX +
	"plp.minfreq";


    /**
     * The name of the Sphinx Property for the maximum frequency
     * covered by the filterbank.
     */
    public static final String PROP_MAX_FREQ = FrontEnd.PROP_PREFIX +
	"plp.maxfreq";


    private int sampleRate;
    private int numberFftPoints;
    private int numberFilters;

    private double minFreq;
    private double maxFreq;

    private PLPFilter[] criticalBandFilter;
    private double[] equalLoudnessScaling;

    private SpectrumSource predecessor;


    /**
     * Constructs a default PLPFilterbank.
     */
    public PLPFilterbank() {
	super();
    }


    /**
     * Initializes this PLPFilterbank object with the given 
     * SphinxProperties context.
     * Constructs both the Bark frequency critical band filters
     * and the array of equal loudness scaling factor.
     *
     * @param name the name of this PLPFilterbank
     * @param context the context of the SphinxProperties to use
     * @param props the SphinxProperties to read properties from
     * @param predecessor the SpectrumSource to obtain Spectrum(a) from,
     *     which is usually a SpectrumAnalyzer (does FFT)
     *
     * @throws IOException if an I/O error occurs
     */
    public void initialize(String name, String context, SphinxProperties props,
			   SpectrumSource predecessor) throws IOException {
        super.initialize(name, context, props);
	setProperties();
        this.predecessor = predecessor;
	buildCriticalBandFilterbank();
	buildEqualLoudnessScalingFactors();
    }


    /**
     * Reads the parameters.
     */
    private void setProperties() {
	SphinxProperties props = getSphinxProperties();
        sampleRate = props.getInt(FrontEnd.PROP_SAMPLE_RATE, 16000);
        minFreq = props.getDouble(PROP_MIN_FREQ, 130);
        maxFreq = props.getDouble(PROP_MAX_FREQ, 6800);
        numberFilters = props.getInt(PROP_NUMBER_FILTERS, 40);
        numberFftPoints = props.getInt
	    (SpectrumAnalyzer.PROP_NUMBER_FFT_POINTS, 512);
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
     * @param input input Power Spectrum
     *
     * @return PLP power spectrum
     *
     * @throws java.lang.IllegalArgumentException
     *
     */
    private Spectrum process (Spectrum input) throws
        IllegalArgumentException {

	double[] in = input.getSpectrumData();

	if (in.length != ((numberFftPoints >> 1) + 1)) {
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

	Spectrum outputPLPSpectrum = new Spectrum
            (outputPLPSpectralArray, input.getUtterance());

        if (getDump()) {
            System.out.println("PLP_SPECTRUM   " +
                               outputPLPSpectrum.toString());
        }

        return outputPLPSpectrum;
    }


    /**
     * Reads the next PLPSpectrum object, which is the power spectrum of an
     * audio input frame. However, it can also be other PLPSpectrum objects
     * like a EndPointSignal.
     *
     * @return the next available PLPSpectrum object, returns null if no
     *     PLPSpectrum object is available
     *
     * @throws java.io.IOException if there is an error reading
     * the PLPSpectrum objects
     */
    public Spectrum getSpectrum() throws IOException {

	Spectrum input = predecessor.getSpectrum();
        
        getTimer().start();

        if (input != null) {
            if (input.hasContent()) {
                input = process(input);
            }
        }

        getTimer().stop();
	
      	return input;
    }	
}
