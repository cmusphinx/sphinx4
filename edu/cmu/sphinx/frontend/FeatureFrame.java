/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

/**
 * Represents an array of Feature.
 */
public class FeatureFrame implements Data {

    private Feature[] features;

    /**
     * Constructs a FeatureFrame with the given Feature array
     *
     * @param features the Feature array
     */
    public FeatureFrame(Feature[] features) {
	this.features = features;
    }


    /**
     * Returns the feature data.
     *
     * @return the feature data
     */
    public Feature[] getData() {
	return features;
    }
}
