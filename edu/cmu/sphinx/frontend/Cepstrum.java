/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

/**
 * Represents a Cepstrum.
 */
public class Cepstrum extends Data {

    private float[] cepstrumData;


    /**
     * Constructs a Cepstrum with the given cepstrum data.
     */
    public Cepstrum(float[] cepstrumData) {
        super(Signal.CONTENT);
	this.cepstrumData = cepstrumData;
    }


    /**
     * Constructs a Cepstrum with the given Signal.
     *
     * @param signal the Signal this Cepstrum carries
     */
    public Cepstrum(Signal signal) {
        super(signal);
    }


    /**
     * Returns the cepstrum data.
     *
     * @return the cepstrum data
     */
    public float[] getCepstrumData() {
	return cepstrumData;
    }


    /**
     * Returns a string representation of this Cepstrum.
     * The format of the string is:
     * <pre>cepstrumLength data0 data1 ...</pre>
     *
     * @return a string representation of this Cepstrum
     */
    public String toString() {
        return (Util.floatArrayToString(cepstrumData));
    }
}
