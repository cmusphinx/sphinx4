/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import java.io.IOException;


/**
 * A FeatureFrameSource produces FeatureFrames.
 */
public interface FeatureFrameSource {

    /**
     * Returns the next FeatureFrame object produced by this FeatureFrameSource.
     *
     * @return the next available FeatureFrame object, returns null if no
     *     FeatureFrame object is available
     *
     * @throws java.io.IOException
     */
    public FeatureFrame getFeatureFrame() throws IOException;
}
