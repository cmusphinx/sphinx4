/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

/**
 * Represents a Cepstrum.
 */
public class Cepstrum implements Data {

    private float[] cepstrumData;


    /**
     * Constructs a Cepstrum with the given cepstrum data.
     */
    public Cepstrum(float[] cepstrumData) {
	this.cepstrumData = cepstrumData;
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
     *
     * @return a string representation of this Cepstrum
     */
    public String toString() {
        return ("MEL_CEPSTRUM   " + Util.floatArrayToString(cepstrumData));
    }
}
