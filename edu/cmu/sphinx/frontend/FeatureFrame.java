/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

/**
 * Represents an array of Features as a two-dimensional float array.
 */
public class FeatureFrame implements Data {

    private float[][] features;

    /**
     * Constructs a FeatureFrame with the given 2D float array
     *
     * @param features the Feature array
     */
    public FeatureFrame(float[][] features) {
	this.features = features;
    }


    /**
     * Returns the feature frame data.
     *
     * @return the feature frame data
     */
    public float[][] getData() {
	return features;
    }
}
