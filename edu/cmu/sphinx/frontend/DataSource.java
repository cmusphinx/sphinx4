/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import java.io.IOException;


/**
 * A DataSource provides Data objects to be read by other objects.
 */
public interface DataSource {

    /**
     * Reads the next Data object from this DataSource.
     *
     * @return the next available Data object, returns null if no
     *     Data object is available
     */
    public Data read() throws IOException;
}
