/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

/**
 * Represents the mel scale spectrum.
 */
public class MelSpectrum implements Data {

    private double[] melSpectralData;


    /**
     * Constructs a Spectrum with the given spectra data.
     *
     * @param melSpectralData a vector containing a frame's mel spectrum
     */
    public MelSpectrum(double[] melSpectralData) {
	this.melSpectralData = melSpectralData;
    }


    /**
     * Returns the mel spectrum data.
     *
     * @return the mel spectrum data
     */
    public double[] getMelSpectrumData() {
	return melSpectralData;
    }
}
