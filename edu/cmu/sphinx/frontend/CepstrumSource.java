/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import java.io.IOException;


/**
 * A CepstrumSource produces Cepstrum(a).
 */
public interface CepstrumSource {

    /**
     * Returns the next Cepstrum object produced by this CepstrumSource.
     *
     * @return the next available Cepstrum object, returns null if no
     *     Cepstrum object is available
     *
     * @throws java.io.IOException
     */
    public Cepstrum getCepstrum() throws IOException;
}
