/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

/**
 * Represents the power spectrum.
 */
public class Spectrum implements Data {

    private double[] spectraData;


    /**
     * Constructs a Spectrum with the given spectra data.
     */
    public Spectrum(double[] spectraData) {
	this.spectraData = spectraData;
    }


    /**
     * Returns the spectrum data.
     *
     * @return the spectrum data
     */
    public double[] getSpectrumData() {
	return spectraData;
    }
}
