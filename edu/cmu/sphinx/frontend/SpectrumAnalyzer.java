/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.Complex;
import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Timer;

import java.io.IOException;

/**
 * Computes the FFT of an input sequence.
 */
public class SpectrumAnalyzer extends DataProcessor {

    /**
     * The name of the SphinxProperty for the number of points
     * in the Fourier Transform, which is 512 by default.
     */
    public static final String PROP_NUMBER_DFT_POINTS =
	"edu.cmu.sphinx.frontend.fft.numberDftPoints";

    private int windowSize;
    private int numberDftPoints;
    private int logBase2NumberDftPoints;

    private Complex[] weightFft;
    private Complex[] inputFrame;
    private Complex[] from;
    private Complex[] to;

    private Complex weightFftTimesFrom2;
    private Complex tempComplex;


    /**
     * Constructs a default Spectrum Analyzer with the given 
     * SphinxProperties context.
     *
     * @param context the context of the SphinxProperties to use
     */
    public SpectrumAnalyzer(String context) {
	initSphinxProperties(context);
	computeLogBase2(this.numberDftPoints);
	createWeightFft(numberDftPoints, false);
        initComplexArrays();
        weightFftTimesFrom2 = new Complex();
        setTimer(Timer.getTimer(context, "SpectrumAnalyzer"));
    }


    /**
     * Initialize all the Complex arrays that will be necessary for FFT.
     */
    private void initComplexArrays() {

        inputFrame = new Complex[numberDftPoints];
    	from = new Complex[numberDftPoints];
	to = new Complex[numberDftPoints];
        
        for (int i = 0; i < numberDftPoints; i++) {
            inputFrame[i] = new Complex();
            from[i] = new Complex();
            to[i] = new Complex();
        }
    }


    /**
     * Process data, creating the power spectrum from an input
     * audio frame.
     *
     * @param input input audio frame
     *
     * @return power spectrum
     *
     * @throws java.lang.IllegalArgumentException
     *
     */
    private Spectrum process (AudioFrame input) 
	throws IllegalArgumentException {
        getTimer().start();

	/**
	 * Create complex input sequence equivalent to the real
	 * input sequence.
	 * If the number of points is less than the window size,
	 * we incur in aliasing. If it's greater, we pad the input
	 * sequence with zeros.
	 */
	double[] in = input.getAudioSamples();

	if (in.length != windowSize) {
	    throw new IllegalArgumentException
               ("Window size is incorrect: in.length == " + in.length +
                 ", windowSize == " + windowSize);
	}

        if (numberDftPoints < windowSize) {
            int i = 0;
	    for (; i < numberDftPoints; i++) {
		inputFrame[i].set(in[i], 0.0f);
	    }
	    for (; i < windowSize; i++) {
                tempComplex.set(in[i], 0.0f);
		inputFrame[i % numberDftPoints].addComplex
		    (inputFrame[i % numberDftPoints], tempComplex);
	    }
	} else {
            int i = 0;
	    for (; i < windowSize; i++) {
		inputFrame[i].set(in[i], 0.0f);
	    }
	    for (; i < numberDftPoints; i++) {
		inputFrame[i].reset();
	    }
	}

	/**
	 * Create output sequence.
	 */
	double[] outputSpectrum = new double[numberDftPoints >> 1];

	/**
	 * Start Fast Fourier Transform recursion
	 */
	recurseFft(inputFrame, outputSpectrum, numberDftPoints, false);

	/**
	 * Return the power spectrum
	 */
	Spectrum output = new Spectrum(outputSpectrum);

        getTimer().stop();
	
        if (getDump()) {
            System.out.println(Util.dumpDoubleArray
                               (outputSpectrum, "SPEC_MAGNITUDE", 20, 10));
        }

        return output;
    }


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     *
     * @param context the context of the SphinxProperties used
     */
    private void initSphinxProperties(String context) {

        setSphinxProperties(context);
	SphinxProperties properties = getSphinxProperties();

        int sampleRate = properties.getInt(FrontEnd.PROP_SAMPLE_RATE, 8000);

        float windowSizeInMs = properties.getFloat
            (FrontEnd.PROP_WINDOW_SIZE_MS, 25.625F);

	windowSize = Util.getSamplesPerWindow(sampleRate, windowSizeInMs);

	/**
	 * Number of points in the FFT. By default, the value is 512,
	 * which means that we compute 512 values around a circle in the
	 * complex plane. Complex conjugate pairs will yield the same
	 * power, therefore the power produced by indices 256 through
	 * 511 are symmetrical with the ones between 1 and 254. Therefore,
	 * we need only return values between 0 and 255.
	 */
	numberDftPoints = properties.getInt(PROP_NUMBER_DFT_POINTS, 512);
    }


    /**
     * Make sure the number of points in the FFT is a power
     * of 2 by computing its log base 2 and checking for
     * remainders.
     *
     * @params numberDftPoints number of points in the FFT
     *
     * @throws java.lang.IllegalArgumentException
     */
    private void computeLogBase2(int numberDftPoints)
	throws IllegalArgumentException {
	this.logBase2NumberDftPoints = 0;
	for (int k = numberDftPoints; k > 1; k >>= 1, this.logBase2NumberDftPoints++) {
	    if (((k % 2) != 0) || (numberDftPoints < 0)) {
		throw new IllegalArgumentException("Not a power of 2: "
						   + numberDftPoints);
	    }
	}
    }

    /**
     * Initializes the <b>weightFft[]</b> vector.
     * <b>weightFft[k] = w ^ k</b>
     * where:
     * <b>w = exp(-2 * PI * i / N)</b>
     * <b>i</b> is a complex number such that <b>i * i = -1</b>
     * and <b>N</b> is the number of points in the FFT.
     * Since <b>w</b> is complex, this is the same as
     * <b>Re(weightFft[k]) = cos ( -2 * PI * k / N)</b>
     * <b>Im(weightFft[k]) = sin ( -2 * PI * k / N)</b>
     *
     * @param numberDftPoints number of points in the FFT
     * @param invert whether it's direct (false) or inverse (true) FFT
     *
     */
    private void createWeightFft(int numberDftPoints, boolean invert) {
	/**
	 * weightFFT will have numberDftPoints/2 complex elements.
	 */
        weightFft = new Complex[numberDftPoints >> 1];

	/**
	 * For the inverse FFT,
	 * w = 2 * PI / numberDftPoints;
	 */
	double w = -2 * Math.PI / numberDftPoints;
        if (invert) {
            w = -w;
        }
	
	for (int k = 0; k < (numberDftPoints / 2); k++) {
            weightFft[k] = new Complex(Math.cos (w * k), Math.sin (w * k));
	}
    }

    /**
     * Establish the recursion. The FFT computation will be 
     * computed by as a recursion. Each stage in the butterfly
     * will be fully computed during recursion. In fact, we use
     * the mechanism of recursion only because it's the simplest 
     * way of switching the "input" and "output" vectors. The 
     * output of a stage is the input to the next stage. The 
     * butterfly computes elements in place, but we still need to
     * switch the vectors. We could copy it (not very efficient...)
     * or, in C, switch the pointers. We can avoid the pointers by
     * using recursion.
     *
     * @param input input sequence
     * @param output output sequence
     * @param numberDftPoints number of points in the FFT
     * @param invert whether it's direct (false) or inverse (true) FFT
     *
     */

    private void recurseFft(Complex[] input,
		    double[] output,
		    int numberDftPoints,
		    boolean invert) {

	double divisor;
        
	/**
	 * The direct and inverse FFT are essentially the same
	 * algorithm, except for two difference: a scaling factor of
	 * "numberDftPoints" and the signal of the exponent in the
	 * weightFft vectors, defined in the method
	 * <code>createWeightFft</code>.
	 */

	if (!invert){
	    divisor = 1.0;
	} else {
	    divisor = (double) numberDftPoints;
	}

	/**
	 * Initialize the "from" and "to" variables.
	 */
	for (int i = 0; i < numberDftPoints; i++){
	    to[i].reset();
	    from[i].scaleComplex(input[i], divisor);
	}

	/**
	 * Repeat the recursion log2(numberDftPoints) times,
	 * i.e., we have log2(numberDftPoints) butterfly stages.
	 */
	butterflyStage(from, to, numberDftPoints, numberDftPoints >> 1);

	/**
	 * Compute energy ("float") for each frequency point
	 * from the fft ("complex")
	 */
	if ((this.logBase2NumberDftPoints & 1) == 0) {
	    for (int i = 0; i < (numberDftPoints >> 1); i++){
		output[i] = from[i].squaredMagnitudeComplex(); 
            }
	} else {
	    for (int i = 0; i < (numberDftPoints >> 1); i++){
		output[i] = to[i].squaredMagnitudeComplex();
            }
	}
	return;
    }


    /**
     * Compute one stage in the FFT butterfly. The name "butterfly"
     * appears because this method computes elements in pairs, and
     * a flowgraph of the computation (output "0" comes from input "0"
     * and "1" and output "1" comes from input "0" and "1") resembles a
     * butterfly.
     *
     * We repeat <code>butterflyStage</code> for
     * <b>log_2(numberDftPoints)</b> stages, by calling the recursion
     * with the argument <code>currentDistance</code> divided by 2 at
     * each call, and checking if it's still > 0.
     *
     * @param from the input sequence at each stage
     * @param to the output sequence
     * @param numberDftPoints the total number of points
     * @param currentDistance the "distance" between elements in the butterfly
     */
    private void butterflyStage(Complex[] from, 
				Complex[] to, 
				int numberDftPoints,
				int currentDistance)
    {
	int ndx1From;
	int ndx2From;
	int ndx1To;
	int ndx2To;
	int ndxWeightFft;

	if (currentDistance > 0) {

            int twiceCurrentDistance = 2 * currentDistance;

	    for (int s = 0; s < currentDistance; s++) {
		ndx1From = s;
		ndx2From = s + currentDistance;
		ndx1To = s;
		ndx2To = s + (numberDftPoints >> 1);
		ndxWeightFft = 0;
		while (ndxWeightFft < (numberDftPoints >> 1)) {
		    /**
		     * <b>weightFftTimesFrom2 = weightFft[k]   </b>
		     * <b>                      *from[ndx2From]</b>
		     */
		    weightFftTimesFrom2.multiplyComplex
			(weightFft[ndxWeightFft], from[ndx2From]);
		    /**
		     * <b>to[ndx1To] = from[ndx1From]       </b>
		     * <b>             + weightFftTimesFrom2</b>
		     */
		    to[ndx1To].addComplex
			(from[ndx1From], weightFftTimesFrom2);
		    /**
		     * <b>to[ndx2To] = from[ndx1From]       </b>
		     * <b>             - weightFftTimesFrom2</b>
		     */
		    to[ndx2To].subtractComplex
			(from[ndx1From], weightFftTimesFrom2);
		    ndx1From += twiceCurrentDistance;
		    ndx2From += twiceCurrentDistance;
		    ndx1To += currentDistance;
		    ndx2To += currentDistance;
		    ndxWeightFft += currentDistance;
		}
	    }

	    /**
	     * This call'd better be the last call in this block, so when
	     * it returns we go straight into the return line below.
	     *
	     * We switch the <i>to</i> and <i>from</i> variables,
	     * the total number of points remains the same, and
	     * the <i>currentDistance</i> is divided by 2.
	     */
	    butterflyStage(to, from, numberDftPoints, (currentDistance >> 1));
	}
	return;
    }

    /**
     * Reads the next Data object, which is an audio frame from which
     * we'll compute the power spectrum. However, it can also be other
     * Data objects like a EndPointSignal.
     *
     * @return the next available Data object, returns null if no
     *     Data object is available
     */
    public Data read() throws IOException {

	Data input = getSource().read();
        
        if (input instanceof AudioFrame) {

            input = process((AudioFrame) input);
        }
        
        // At this point - or in the call immediatelly preceding
        // this -, we should have created a cepstrum frame with
        // whatever data came last, even if we had less than
        // windowSize of data.
        
	return input;
    }	
}
