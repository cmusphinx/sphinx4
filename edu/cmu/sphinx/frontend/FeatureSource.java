/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import java.io.IOException;


/**
 * A FeatureSource produces Feature objects to be read by other objects.
 */
public interface FeatureSource {

    /**
     * Returns the next Feature object produced by this FeatureSource.
     *
     * @return the next available Feature object, returns null if no
     *     Feature object is available
     *
     * @throws java.io.IOException
     */
    public Feature getFeature() throws IOException;
}
