/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

/**
 * Represents an array of Features.
 */
public class FeatureFrame implements Data {

    private Feature[] features;

    /**
     * Constructs a FeatureFrame with the given array of Features
     *
     * @param features the Feature array
     */
    public FeatureFrame(Feature[] features) {
	this.features = features;
    }


    /**
     * Returns the array of Features
     *
     * @return the array of Features
     */
    public Feature[] getFeatures() {
	return features;
    }
}
