/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import java.io.IOException;


/**
 * A SpectrumSource produces Spectrum objects to be read by other objects.
 */
public interface SpectrumSource {

    /**
     * Returns the next Spectrum object produced by this SpectrumSource.
     *
     * @return the next available Spectrum object, returns null if no
     *     Spectrum object is available
     *
     * @throws java.io.IOException
     */
    public Spectrum getSpectrum() throws IOException;
}
