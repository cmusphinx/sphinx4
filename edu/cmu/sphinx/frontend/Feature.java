/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

/**
 * Represents a single Feature. A Feature is simply an array of numbers,
 * usually of length 39 and of type float.
 */
public class Feature extends Data {

    private float[] featureData;
    private int ID;


    /**
     * Constructs a Feature with the given feature data.
     *
     * @param featureData the feature data points
     */
    public Feature(float[] featureData, int ID) {
        super(Signal.CONTENT);
	this.featureData = featureData;
        this.ID = ID;
    }


    /**
     * Constructs a Feature with the given Signal.
     *
     * @param signal the Signal this Feature carries
     */
    public Feature(Signal signal, int ID) {
        super(signal);
        this.ID = ID;
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
     * Returns the ID of this Feature.
     *
     * @return the ID
     */
    public int getID() {
        return ID;
    }


    /**
     * Returns a String representation of this Feature.
     * The format of the string is:
     * <pre>featureLength data0 data1 ...</pre>
     *
     * @return the String representation
     */
    public String toString() {
        return Util.floatArrayToString(featureData);
    }
}
