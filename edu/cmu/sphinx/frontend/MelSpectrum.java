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


    /**
     * Returns a string representation of this MelSpectrum.
     * The format of the string is:
     * <pre>melspectrumLength data0 data1 ...</pre>
     *
     * @return a string representation of this MelSpectrum
     */
    public String toString() {
        return Util.doubleArrayToString(melSpectralData);
    }
}
