/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

/**
 * Represents an array of Features.
 */
public class FeatureFrame implements Data {

    private Feature[] features = null;

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


    /**
     * Returns a String representation of this FeatureFrame.
     *
     * @returns the String representation
     */
    public String toString() {
        String result = "FEATURE_FRAME ";
        if (features != null) {
            result += features.length;
            for (int i = 0; i < features.length; i++) {
                result += ("\n" + features[i].toString());
            }
        } else {
            result += "0";
        }
        return result;
    }
}
