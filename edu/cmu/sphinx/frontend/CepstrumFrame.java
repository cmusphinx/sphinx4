/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

/**
 * Represents an array of Cepstrum.
 */
public class CepstrumFrame implements Data {

    private Cepstrum[] cepstra;

    /**
     * Constructs a CepstrumFrame with the given Cepstrum array
     *
     * @param cepstra the Cepstrum array
     */
    public CepstrumFrame(Cepstrum[] cepstra) {
	this.cepstra = cepstra;
    }


    /**
     * Returns the cepstrum data.
     *
     * @return the cepstrum data
     */
    public Cepstrum[] getData() {
	return cepstra;
    }
}
