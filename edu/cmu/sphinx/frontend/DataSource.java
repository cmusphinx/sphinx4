/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import java.io.IOException;


/**
 * A DataSource produces Data objects to be read by other objects.
 */
public interface DataSource {

    /**
     * Reads the next Data object produced by this DataSource.
     *
     * @return the next available Data object, returns null if no
     *     Data object is available
     *
     * @throws java.io.IOException
     */
    public Data read() throws IOException;
}
