/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

/**
 * Represents a Cepstrum.
 */
public class Cepstrum implements Data {

    private float[] cepstraData;


    /**
     * Constructs a Cepstrum with the given cepstra data.
     */
    public Cepstrum(float[] cepstraData) {
	this.cepstraData = cepstraData;
    }


    /**
     * Returns the cepstrum data.
     *
     * @return the cepstrum data
     */
    public float[] getData() {
	return cepstraData;
    }
}
