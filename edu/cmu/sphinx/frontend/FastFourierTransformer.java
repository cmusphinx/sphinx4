/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;


/**
 * Computes the FFT of an input sequence.
 * This is a code under construction. Lots of things need to
 * be revised. Don't use it as is!!! As yet.
 * <br><b>PI</b> is 3.14159265358979323846
 */
public class FastFourierTransformer implements Processor {

    /**
     * The name of the SphinxProperty for the alpha value of the Hamming
     * Window.
     */
    public static final String PROP_NPOINT =
	"edu.cmu.sphinx.frontend.fastFourierTransform.Npoint";

    private static final double PI = 3.14159265358979323846;
    private static int Npoint;


    /**
     * Constructs a default FastFourierTransformer.
     */
    public FastFourierTransformer(float input, float output) {
	log2N = computeLog2N(Npoint);
	createWk(Npoint);
	recurseFFT(input, output, Npoint, false);
    }


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     */
    private void getSphinxProperties() {
	// TODO : specify the context
	SphinxProperties properties = SphinxProperties.getSphinxProperties("");
	windowSize = properties.getInt(FrontEnd.PROP_WINDOW_SIZE, 205);
	Npoint = properties.getDouble(HammingWindower.Npoint, 256);
    }


    /**
     * Make sure the number of points in the FFT is a power
     * of 2 by computing its log base 2 and checking for
     * remainders.
     */
    private int computeLog2N(int N) {
	for (int k = N, log2N = 0; k > 1; k >> 1, log2N++) {
	    if (((k % 2) != 0) || (N < 0)) {
		/* Break horribly.
		 */
	    }
	}
	return log2N;
    }

    /**
     * Initializes the Wk[] vector.
     * Wk[k] = w ^ k
     * where:
     * w = exp(-2 * PI * i / N)
     * i is a complex number such that i * i = -1
     * and N is the number of points in the FFT.
     * Since w is complex, this is the same as
     * Re(Wk[k]) = cos ( -2 * PI * k / N)
     * Im(Wk[k]) = sin ( -2 * PI * k / N)
     */
    private void createWk(int N, boolean invert) {
	/**
	 * Wk will have N/2 complex elements.
	 */
	this.Wk = new complex[N>>1];
	/**
	 * For the inverse FFT,
	 * w = 2 * PI / N;
	 */
	w = -2 * PI / N;
	if (invert == true){
	    w = -w;
	}
	for (int k = 0; k < N / 2; k++) {
	    Wk[k].re = cos (w * k);
	    Wk[k].im = sin (w * k);
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
     */

    void recurseFFT(complex input[], // input sequence
		    complex output[], // output sequence
		    int Npoint, // number of points in the FFT
		    boolean invert) // whether it's a direct or inverse FFT
    {

	double from[Npoint];
	double to[Npoint];
	double divisor;

	/**
	 * The direct and inverse FFT are essentially the same
	 * algorithm, except for two difference: a scaling factor
	 * of "Npoint" and the signal of the exponent in the Wk
	 * vectors, defined in the method "createWk".
	 */

	if (invert == true){
	    divisor = 1.0;
	}
	else {
	    divisor = Npoint;
	}
	/**
	 * Initialize the "from" variable.
	 */
	for (int i = 0; i < Npoint; i++){
	    from[i] = input[i] / divisor;
	}
	/**
	 * Repeat the recursion log2(Npoint) times,
	 * i.e., we have log2(Npoint) butterfly stages.
	 */
	FastFourierTransform.Butterfly(from, to, Npoint, Npoint >> 1);
	/**
	 * Compute energy ("float") for each frequency point
	 * from the fft ("complex")
	 */
	if (log2N & 1 == 0){
	    for (i = 0; i < Npoint; i++){
		output[i] = SqrMag(to[i]);
	    }
	}
	else {
	    for (i = 0; i < Npoint; i++){
		output[i] = SqrMag(from[i]);
	    }
	}
	return;
    }

    void Butterfly(complex from[], complex to[], int Npoint, int currentStage)
    {
	/**
	 * We repeat Butterfly for logs(Npoint stages, by calling the
	 * recursion with the argument "currentStage" divided by 2
	 * at each call, and checking if it's still > 0.
	 */
	if (currentStage > 0){
	    /* do stuff */

	    /**
	     * This call'd better be the last call in this block, so when
	     * it returns we go straight into the return line below.
	     */
	    FastFourierTransform.Butterfly(to, // switch the "from" and ....
					   from, // .... "to" variables
					   Npoint, // Npoint remains the same
					   currentStage >> 1); // divide by 2
	}
	return;
    }


}
