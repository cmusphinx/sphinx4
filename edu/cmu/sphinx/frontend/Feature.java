/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

/**
 * Represents a single Feature. A Feature is simply an array of numbers,
 * usually of length 39 and of type float.
 */
public class Feature extends Data {

    private float[] featureData = null;
    private int ID;


    /**
     * Constructs a Feature with the given feature data.
     *
     * @param featureData the feature data points
     * @param ID the ID of this Feature with respect to the current
     *    speech segment.
     */
    public Feature(float[] featureData, int ID) {
        super(Signal.CONTENT);
	this.featureData = featureData;
        this.ID = ID;
    }


    /**
     * Constructs a Feature with the given featureData, ID, and utterance
     *
     * @param featureData the feature data
     * @param ID the Id of this Feature with respect to the current
     *    speech segment
     * @param utterance the Utterance associated with this Feature
     */
    public Feature(float[] featureData, int ID, Utterance utterance) {
        super(utterance);
        this.featureData = featureData;
        this.ID = ID;
    }


    /**
     * Constructs a Feature with the given Signal.
     *
     * @param signal the Signal this Feature carries
     * @param ID the ID of this Feature with respect to the current
     *    speech segment.
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
     * Returns the audio data that corresponds to this Feature.
     * Note that this method only returns that particular window of
     * audio data that this Feature corresponds to, not the audio data
     * of the entire utterance.
     *
     * <p>The audio data might not be available, because the
     * <code>edu.cmu.sphinx.frontend.keepAudioReference</code>
     * SphinxProperty is set to false. In that case, this method
     * return null.
     *
     * @return the audio data that corresponds to this Feature, or null
     *    if the audio data is not available.
     */
    public byte[] getAudio() {
        if (getUtterance() == null) {
            return null;
        } else {
            return getUtterance().getAudio(getID());
        }
    }


    /**
     * Returns a String representation of this Feature.
     * The format of the string is:
     * <pre>featureLength data0 data1 ...</pre>
     *
     * @return the String representation
     */
    public String toString() {
        if (featureData != null) {
            return ID + " " + Util.floatArrayToString(featureData);
        } else {
            return getSignal().toString();
        }
    }
}
