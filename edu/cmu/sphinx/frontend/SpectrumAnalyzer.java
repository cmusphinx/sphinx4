/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Complex;

import java.io.IOException;

/**
 * Computes the FFT of an input sequence.
 * This is a code under construction. Lots of things need to
 * be revised. Don't use it as is!!! As yet.
 */
public class SpectrumAnalyzer extends DataProcessor {

    /**
     * The name of the SphinxProperty for the number of points
     * in the Fourier Transform.
     */
    public static final String PROP_NPOINT =
	"edu.cmu.sphinx.frontend.fastFourierTransform.NPoint";

    private int windowSize;
    private int NPoint;
    private int log2N;
    private Complex[] Wk;

    /**
     * Constructs a default Spectrum Analyzer.
     */
    public void SpectrumAnalyzer() {

	getSphinxProperties();
	computeLog2N();
	createWk(NPoint, false);

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
    private Spectrum process (DoubleAudioFrame input) 
	throws IllegalArgumentException {

	/**
	 * Create complex input sequence equivalent to the real
	 * input sequence.
	 * If the number of points is less than the window size,
	 * we incur in aliasing. If it's greater, we pad the input
	 * sequence with zeros.
	 */
	double[] in = input.getData();
	if (in.length != windowSize) {
	    throw new IllegalArgumentException("Window size is incorrect");
	}
	Complex[] inputSeq = new Complex[NPoint];
	if (NPoint < windowSize) {
	    for (int i = 0; i < NPoint; i++) {
		inputSeq[i]= new Complex((double) in[i]);
	    }
	    for (int i = NPoint; i < windowSize; i++) {
		inputSeq[i % NPoint].addComplex(inputSeq[i % NPoint],
						new Complex((double) in[i]));
	    }
	} else {
	    for (int i = 0; i < windowSize; i++) {
		inputSeq[i] = new Complex((double) in[i]);
	    }
	    for (int i = windowSize; i < NPoint; i++) {
		inputSeq[i] = new Complex();
	    }
	}

	/**
	 * Create output sequence.
	 */
	double[] outputSpectrum = new double[NPoint];

	/**
	 * Start Fast Fourier Transform recursion
	 */
	recurseFFT(inputSeq, outputSpectrum, NPoint, false);

	/**
	 * Return the power spectrum
	 */
	Spectrum output = new Spectrum(outputSpectrum);
	return output;
    }


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     */
    private void getSphinxProperties() {

	SphinxProperties properties = SphinxProperties.getSphinxProperties("");

        int sampleRate = properties.getInt(FrontEnd.PROP_SAMPLE_RATE, 8000);

        float windowSizeInMs = properties.getFloat
            (FrontEnd.PROP_WINDOW_SIZE_MS, 25.625F);

        windowSize = Util.getSamplesPerWindow(sampleRate, windowSizeInMs);

	NPoint = properties.getInt(FrontEnd.PROP_FFT_NPOINT, 256);
    }


    /**
     * Make sure the number of points in the FFT is a power
     * of 2 by computing its log base 2 and checking for
     * remainders.
     *
     * @throws java.lang.IllegalArgumentException
     *
     */
    private void computeLog2N() throws IllegalArgumentException {
	for (int k = NPoint, log2N = 0; k > 1; k >>= 1, log2N++) {
	    if (((k % 2) != 0) || (NPoint < 0)) {
		throw new IllegalArgumentException("Not a power of 2");
	    }
	}
    }

    /**
     * Initializes the <b>Wk[]</b> vector.
     * <b>Wk[k] = w ^ k</b>
     * where:
     * <b>w = exp(-2 * PI * i / N)</b>
     * <b>i</b> is a complex number such that <b>i * i = -1</b>
     * and <b>N</b> is the number of points in the FFT.
     * Since <b>w</b> is complex, this is the same as
     * <b>Re(Wk[k]) = cos ( -2 * PI * k / N)</b>
     * <b>Im(Wk[k]) = sin ( -2 * PI * k / N)</b>
     *
     * @param N number of points in the FFT
     * @param invert whether it's direct (false) or inverse (true) FFT
     *
     */
    private void createWk(int N, boolean invert) {
	/**
	 * Wk will have N/2 complex elements.
	 */
	Complex[] Wk = new Complex[N>>1];
	/**
	 * For the inverse FFT,
	 * w = 2 * PI / N;
	 */
	double w = -2 * Math.PI / N;
	if (invert == true){
	    w = -w;
	}
	for (int k = 0; k < N / 2; k++) {
	    Wk[k] = new Complex(Math.cos (w * k), Math.sin (w * k));
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
     * @param NPoint number of points in the FFT
     * @param invert whether it's direct (false) or inverse (true) FFT
     *
     */

    private void recurseFFT(Complex[] input,
		    double[] output,
		    int NPoint,
		    boolean invert) {

	Complex[] from = new Complex[NPoint];
	Complex[] to = new Complex[NPoint];
	double divisor;

	/**
	 * The direct and inverse FFT are essentially the same
	 * algorithm, except for two difference: a scaling factor
	 * of "NPoint" and the signal of the exponent in the Wk
	 * vectors, defined in the method <code>createWk</code>.
	 */

	if (invert == false){
	    divisor = 1.0f;
	} else {
	    divisor = (double) NPoint;
	}
	/**
	 * Initialize the "from" and "to" variables.
	 */
	for (int i = 0; i < NPoint; i++){
	    to[i] = new Complex();
	    from[i] = new Complex();
	    from[i].scaleComplex(input[i], divisor);
	}
	/**
	 * Repeat the recursion log2(NPoint) times,
	 * i.e., we have log2(NPoint) butterfly stages.
	 */
	butterflyStage(from, to, NPoint, NPoint >> 1);
	/**
	 * Compute energy ("float") for each frequency point
	 * from the fft ("complex")
	 */
	if ((log2N & 1) == 0){
	    for (int i = 0; i < NPoint; i++){
		output[i] = to[i].squaredMagnitudeComplex(); 
	    }
	} else {
	    for (int i = 0; i < NPoint; i++){
		output[i] = from[i].squaredMagnitudeComplex();
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
     * We repeat <code>butterflyStage</code> for <b>log_2(NPoint)</b>
     * stages, by calling the recursion with the argument
     * <code>currentDistance</code> divided by 2 at each call, and
     * checking if it's still > 0.
     *
     * @param from the input sequence at each stage
     * @param to the output sequence
     * @param NPoint the total number of points
     * @param currentDistance the "distance" between elements in the butterfly
     */
    private void butterflyStage(Complex[] from, 
			   Complex[] to, 
			   int NPoint, 
			   int currentDistance)
    {
	int ndx1From;
	int ndx2From;
	int ndx1To;
	int ndx2To;
	int ndxWk = 0;
	Complex wkTimesF2 = new Complex();

	if (currentDistance > 0){
	    for (int s = 0; s < currentDistance; s++) {
		ndx1From = s;
		ndx2From = s + currentDistance;
		ndx1To = s;
		ndx2To = s + (NPoint >> 1);
		while (ndxWk < (NPoint >> 1)) {
		    /**
		     * <b>wkTimesF2 = Wk[k] * from[ndx2From]</b>
		     */
		    wkTimesF2.multiplyComplex(Wk[ndxWk], from[ndx2From]);
		    /**
		     * <b>to[ndx1To] = from[ndx1From] + wkTimesF2</b>
		     */
		    to[ndx1To].addComplex(from[ndx1From], wkTimesF2);
		    /**
		     * <b>to[ndx2To] = from[ndx1From] - wkTimesF2</b>
		     */
		    to[ndx2To].subtractComplex(from[ndx1From], wkTimesF2);
		    ndx1From += (2 * currentDistance);
		    ndx2From += (2 * currentDistance);
		    ndx1To += currentDistance;
		    ndx2To += currentDistance;
		    ndxWk += currentDistance;
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
	    butterflyStage(to, from, NPoint, currentDistance >> 1);
	}
	return;
    }

    /**
     * Reads the next Data object, which is the power spectrum of the
     * input frame. However, it can also be other Data objects
     * like a EndPointSignal.
     *
     * @return the next available Data object, returns null if no
     *     Data object is available
     */
    public Data read() throws IOException {

	Data input = getSource().read();
        
        if (input instanceof DoubleAudioFrame) {

            input = process((DoubleAudioFrame) input);

        } else if (input instanceof EndPointSignal) {
	    // At this point - or in the call immediatelly preceding
	    // this -, we should have created a cepstrum frame with
	    // whatever data came last, even if we had less than
	    // windowSize of data.
	}
	return input;
    }	


}
