/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

/**
 * Represents a single Feature. A Feature is simply an array of numbers,
 * usually of length 39 and of type float.
 */
public class Feature implements Data {

    private float[] featureData;


    /**
     * Constructs a Feature with the given feature data.
     *
     * @param featureData the feature data points
     */
    public Feature(float[] featureData) {
	this.featureData = featureData;
    }


    /**
     * Returns the feature data.
     *
     * @return the feature data
     */
    public float[] getFeatureData() {
	return featureData;
    }


    /**
     * Returns a String representation of this Feature.
     *
     * @returns the String representation
     */
    public String toString() {
        return Util.dumpFloatArray(featureData, "FEATURE");
    }
}
