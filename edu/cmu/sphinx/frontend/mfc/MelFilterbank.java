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


package edu.cmu.sphinx.frontend.mfc;

import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.Filterbank;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.Spectrum;
import edu.cmu.sphinx.frontend.SpectrumAnalyzer;
import edu.cmu.sphinx.frontend.SpectrumSource;

import edu.cmu.sphinx.util.SphinxProperties;

import java.io.IOException;

/**
 * Filters an input power spectrum through a mel filterbank. The
 * filters in the filterbank are placed in the frequency axis so as to
 * mimic the critical band, representing different perceptual effect
 * at different frequency bands. 
 */
public class MelFilterbank extends DataProcessor implements Filterbank {

    /**
     * The name of the Sphinx Property for the number of filters in
     * the filterbank.
     */
    public static final String PROP_NUMBER_FILTERS = FrontEnd.PROP_PREFIX +
	"mel.numFilters";


    /**
     * The name of the Sphinx Property for the minimum frequency
     * covered by the filterbank.
     */
    public static final String PROP_MIN_FREQ = FrontEnd.PROP_PREFIX +
	"mel.minfreq";


    /**
     * The name of the Sphinx Property for the maximum frequency
     * covered by the filterbank.
     */
    public static final String PROP_MAX_FREQ = FrontEnd.PROP_PREFIX +
	"mel.maxfreq";


    private int sampleRate;
    private int numberFftPoints;
    private int numberFilters;

    // Should these be float? It's not critical....

    private double minFreq;
    private double maxFreq;

    private MelFilter[] filter;

    private SpectrumSource predecessor;


    /**
     * Constructs a default MelFilterbank object.
     */
    public MelFilterbank() {
	super();
    }


    /**
     * Constructs a default MelFilterbank object with the given 
     * SphinxProperties context.
     *
     * @param name the name of this MelFilterbank
     * @param context the context of the SphinxProperties to use
     * @param props the SphinxProperties object to read properties from
     * @param predecessor the SpectrumSource to obtain Spectrum(a) from,
     *     which is usually a SpectrumAnalyzer (does FFT)
     *
     * @throws IOException if an I/O error occurs
     */
    public MelFilterbank(String name, String context, SphinxProperties props,
                         SpectrumSource predecessor) throws IOException {
	this();
	initialize(name, context, props, predecessor);
    }


    /**
     * Initializes this MelFilterbank with the given name, context and
     * predecessor.
     *
     * @param name the name of this MelFilterbank
     * @param context the context of this MelFilterbank
     * @param props the SphinxProperties object to read properties from
     * @param predecessor the predecessor of this MelFilterbank
     */
    public void initialize(String name, String context,
			   SphinxProperties props,
			   SpectrumSource predecessor) throws IOException {
        super.initialize(name, context, props);
	setProperties();
        this.predecessor = predecessor;
	buildFilterbank(numberFftPoints, numberFilters, minFreq, maxFreq);
    }


    /**
     * Reads the properties.
     */
    public void setProperties() {
	SphinxProperties props = getSphinxProperties();

        sampleRate = props.getInt(FrontEnd.PROP_SAMPLE_RATE, 16000);
        minFreq = props.getDouble(PROP_MIN_FREQ, 130);
        maxFreq = props.getDouble(PROP_MAX_FREQ, 6800);
        numberFilters = props.getInt(PROP_NUMBER_FILTERS, 40);
        numberFftPoints = props.getInt
	    (SpectrumAnalyzer.PROP_NUMBER_FFT_POINTS, 512);
    }


    /**
     * Compute mel frequency from linear frequency.
     *
     * Since we don't have <code>log10()</code>, we have to compute it
     * using natural log: <b>log10(x) = ln(x) / ln(10)</b>
     *
     * @param inputFreq the input frequency in linear scale
     *
     * @return the frequency in a mel scale
     *
     */
    private double linToMelFreq(double inputFreq) {

	return (2595.0 * (Math.log(1.0 + inputFreq / 700.0) / Math.log(10.0)));
    }


    /**
     * Compute linear frequency from mel frequency.
     *
     * @param inputFreq the input frequency in mel scale
     *
     * @return the frequency in a linear scale
     *
     */
    private double melToLinFreq(double inputFreq) {

	return (700.0 * (Math.pow(10.0, (inputFreq / 2595.0)) - 1.0));
    }


    /**
     * Sets the given frequency to the nearest frequency bin from the
     * FFT.  The FFT can be thought of as a sampling of the actual
     * spectrum of a signal. We use this function to find the sampling
     * point of the spectrum that is closest to the given frequency.
     *
     * @param inFreq the input frequency
     * @param stepFreq the distance between frequency bins
     *
     * @return the closest frequency bin
     *
     * @throws IllegalArgumentException
     */
    private double setToNearestFrequencyBin(double inFreq, double stepFreq)
	throws IllegalArgumentException {
	if (stepFreq == 0) {
	    throw new IllegalArgumentException("stepFreq is zero");
	}
	return stepFreq * Math.round (inFreq / stepFreq);
    }


    /**
     * Build a mel filterbank with the parameters given.
     * Each filter will be shaped as a triangle. The triangles overlap
     * so that they cover the whole frequency range requested. The edges
     * of a given triangle will be by default at the center of the 
     * neighboring triangles.
     *
     * @param numberFftPoints number of points in the power spectrum
     * @param numberFilters number of filters in the filterbank
     * @param minFreq lowest frequency in the range of interest
     * @param maxFreq highest frequency in the range of interest
     *
     * @throws IllegalArgumentException
     */
    private void buildFilterbank(int numberFftPoints, 
				 int numberFilters, 
				 double minFreq, 
				 double maxFreq) 
	throws IllegalArgumentException {
	double minFreqMel;
	double maxFreqMel;
	double deltaFreqMel;
	double[] leftEdge = new double[numberFilters];
	double[] centerFreq = new double[numberFilters];
	double[] rightEdge = new double[numberFilters];
	double nextEdgeMel;
	double nextEdge;
	double initialFreqBin;
	double deltaFreq;

	this.filter = new MelFilter[numberFilters];

	/**
	 * In fact, the ratio should be between <code>sampleRate /
	 * 2</code> and <code>numberFftPoints / 2</code> since the number of
	 * points in the power spectrum is half of the number of FFT
	 * points - the other half would be symmetrical for a real
	 * sequence -, and these points cover up to the Nyquist
	 * frequency, which is half of the sampling rate. The two
	 * "divide by 2" get canceled out.
	 */
	if (numberFftPoints == 0) {
	    throw new IllegalArgumentException("Number of FFT points is zero");
	}
	deltaFreq = (double)sampleRate / numberFftPoints;
	/**
	 * Initialize edges and center freq. These variables will be
	 * updated so that the center frequency of a filter is the
	 * right edge of the filter to its left, and the left edge of
	 * the filter to its right.
	 */

	if (numberFilters < 1) {
	    throw new IllegalArgumentException("Number of filters illegal: "
					       + numberFilters);
	}
	minFreqMel = linToMelFreq(minFreq);
	maxFreqMel = linToMelFreq(maxFreq);
	deltaFreqMel = (maxFreqMel - minFreqMel) / (numberFilters + 1);

	leftEdge[0] = setToNearestFrequencyBin(minFreq, deltaFreq);
	nextEdgeMel = minFreqMel;
	for (int i = 0; i < numberFilters; i++) {
	    nextEdgeMel += deltaFreqMel;
	    nextEdge = melToLinFreq(nextEdgeMel);
	    centerFreq[i] = setToNearestFrequencyBin(nextEdge, deltaFreq);
	    if (i > 0) {
		rightEdge[i - 1] = centerFreq[i];
	    }
	    if (i < numberFilters - 1) {
		leftEdge[i + 1] = centerFreq[i];
	    }
	}
	nextEdgeMel = nextEdgeMel + deltaFreqMel;
	nextEdge = melToLinFreq(nextEdgeMel);
	rightEdge[numberFilters - 1] = 
	    setToNearestFrequencyBin(nextEdge, deltaFreq);

	for (int i = 0; i < numberFilters; i++) {
	    initialFreqBin = setToNearestFrequencyBin(leftEdge[i], deltaFreq);
	    if (initialFreqBin < leftEdge[i]) {
		initialFreqBin += deltaFreq;
	    }
	    this.filter[i] = new MelFilter(leftEdge[i], centerFreq[i], 
					   rightEdge[i], initialFreqBin, 
					   deltaFreq);
	}
    }


    /**
     * Process data, creating the power spectrum from an input
     * audio frame.
     *
     * @param input input Power Spectrum
     *
     * @return power spectrum
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
                 ", numberFftPoints == " + ((numberFftPoints>>1)+1));
	}

	double[] outputMelFilterbank = new double[numberFilters];

	/**
	 * Filter input power spectrum
	 */
	for (int i = 0; i < numberFilters; i++) {
	    outputMelFilterbank[i] = filter[i].filterOutput(in);
	}

	Spectrum outputMelSpectrum = new Spectrum
            (outputMelFilterbank, input.getUtterance());

        if (getDump()) {
            System.out.println("MEL_SPECTRUM   " +
                               outputMelSpectrum.toString());
        }

        return outputMelSpectrum;
    }


    /**
     * Reads the next MelSpectrum object, which is the power spectrum of an
     * audio input frame. However, it can also be other MelSpectrum objects
     * like a EndPointSignal.
     *
     * @return the next available MelSpectrum object, returns null if no
     *     MelSpectrum object is available
     *
     * @throws java.io.IOException if there is an error reading
     * the MelSpectrum objects
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
