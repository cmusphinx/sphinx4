/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Timer;

import java.io.IOException;

/**
 * Filters an input power spectrum through a mel filterbank. The
 * filters in the filterbank are placed in the frequency axis so as to
 * mimic the critical band, representing different perceptual effect
 * at different frequency bands. 
 */
public class MelFilterbank extends DataProcessor {

    /* Note to Philip: we already defined this property in
     * SpectrumAnalyzer.java, do we have to define it again here?
     */
    /**
     * The name of the SphinxProperty for the number of points
     * in the Fourier Transform, which is 512 by default.
     */
    public static final String PROP_NPOINT =
	"edu.cmu.sphinx.frontend.fft.NPoint";

    /**
     * The name of the Sphinx Property for the number of filters in
     * the filterbank.
     */
    public static final String PROP_NUMBER_FILTERS =
	"edu.cmu.sphinx.frontend.mel.numFilters";

    /**
     * The name of the Sphinx Property for the minimum frequency
     * covered by the filterbank.
     */
    public static final String PROP_MIN_FREQ = 
	"edu.cmu.sphinx.frontend.mel.minfreq";

    /**
     * The name of the Sphinx Property for the maximum frequency
     * covered by the filterbank.
     */
    public static final String PROP_MAX_FREQ = 
	"edu.cmu.sphinx.frontend.mel.maxfreq";

    private int sampleRate;

    private int NPoint;

    private int numberFilters;

    /**
     * Should these be float? It's not critical....
     */
    private double minFreq;

    private double maxFreq;

    private MelFilter[] filter;

    /**
     * Constructs a default MelFilterbank object with the given 
     * SphinxProperties context.
     *
     * @param context the context of the SphinxProperties to use
     */
    public MelFilterbank(String context) {
	initSphinxProperties(context);
	buildFilterbank(NPoint, numberFilters, minFreq, maxFreq);
        setTimer(Timer.getTimer(context, "MelFilterbank"));
    }

    /**
     * Reads the parameters needed from the static SphinxProperties object.
     *
     * @param context the context of the SphinxProperties used
     */
    private void initSphinxProperties(String context) {

        setSphinxProperties(context);
	SphinxProperties properties = getSphinxProperties();

        sampleRate = properties.getInt(FrontEnd.PROP_SAMPLE_RATE, 8000);

	NPoint = properties.getInt(PROP_NPOINT, 512);

	numberFilters = properties.getInt(PROP_NUMBER_FILTERS, 31);

	/**
	 * Oh, don't we all love legacy code with its inescrutable constants!
	 */
	minFreq = properties.getDouble(PROP_MIN_FREQ, 200);

	maxFreq = properties.getDouble(PROP_MAX_FREQ, 3500);
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
     * Build a mel filterbank with the parameters given.
     * Each filter will be shaped as a triangle. The triangles overlap
     * so that they cover the whole frequency range requested. The edges
     * of a given triangle will be by default at the center of the 
     * neighboring triangles.
     *
     * @param NPoint number of points in the power spectrum
     * @param numberFilters number of filters in the filterbank
     * @param minFreq lowest frequency in the range of interest
     * @param maxFreq highest frequency in the range of interest
     *
     */
    private void buildFilterbank(int NPoint, 
				 int numberFilters, 
				 double minFreq, 
				 double maxFreq) {
	double minFreqMel;
	double maxFreqMel;
	double deltaFreqMel;
	double leftEdge;
	double centerFreq;
	double centerFreqMel;
	double rightEdge;
	double rightEdgeMel;
	double initialFreqBin;
	double deltaFreq;

	this.filter = new MelFilter[numberFilters];

	minFreqMel = linToMelFreq(minFreq);
	maxFreqMel = linToMelFreq(maxFreq);
	deltaFreqMel = (maxFreqMel - minFreqMel) / (numberFilters + 1);

	/**
	 * In fact, the ratio should be between <code>sampleRate /
	 * 2</code> and <code>NPoint / 2</code> since the number of
	 * points in the power spectrum is half of the number of FFT
	 * points - the other half would be symmetrical for a real
	 * sequence -, and these points cover up to the Nyquist
	 * frequency, which is half of the sampling rate. The two
	 * "divide by 2" get canceled out.
	 */
	deltaFreq = (double)sampleRate / NPoint;
	/**
	 * Initialize edges and center freq. These variables will be
	 * updated so that the center frequency of a filter is the
	 * right edge of the filter to its left, and the left edge of
	 * the filter to its right.
	 */
	leftEdge = minFreq;
	centerFreqMel = minFreqMel + deltaFreqMel;
	centerFreq = melToLinFreq(centerFreqMel);
	for (int i = 0; i < numberFilters; i++) {
	    initialFreqBin = Math.round((leftEdge / deltaFreq)) * deltaFreq;
	    rightEdgeMel = centerFreqMel + deltaFreqMel;
	    rightEdge = melToLinFreq(rightEdgeMel);
	    /**
	     * Create the filter. The filter will need the frequencies
	     * in the linear domain.
	     */
	    this.filter[i] = new MelFilter();
	    this.filter[i].createMelFilter(leftEdge, centerFreq, rightEdge, 
					   initialFreqBin, deltaFreq, 
					   (sampleRate >> 1));
	    leftEdge = centerFreq;
	    centerFreq = rightEdge;
	    centerFreqMel = rightEdgeMel;
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
    private MelSpectrum process (Spectrum input) throws IllegalArgumentException {

	MelSpectrum outputMelSpectrum;

        getTimer().start();

	double[] in = input.getSpectrumData();

	if (in.length != (NPoint >> 1)) {
	    throw new IllegalArgumentException
               ("Window size is incorrect: in.length == " + in.length +
                 ", NPoint == " + NPoint);
	}

	double[] outputMelFilterbank = new double[numberFilters];

	/**
	 * Filter input power spectrum
	 */
	for (int i = 0; i < numberFilters; i++) {
	    outputMelFilterbank[i] = filter[i].filterOutput(in);
	}

        getTimer().stop();
	
        if (getDump()) {
            System.out.println(Util.dumpDoubleArray
                               (outputMelFilterbank, "MEL_SPECTRUM", 12, 5));
        }
	outputMelSpectrum = new MelSpectrum(outputMelFilterbank);
        return outputMelSpectrum;
    }


    /**
     * Reads the next Data object, which is the power spectrum of an
     * audio input frame. However, it can also be other Data objects
     * like a EndPointSignal.
     *
     * @return the next available Data object, returns null if no
     *     Data object is available
     */
    public Data read() throws IOException {

	Data input = getSource().read();
        
        if (input instanceof Spectrum) {

            input = process((Spectrum) input);
        }
      	return input;
    }	
}
