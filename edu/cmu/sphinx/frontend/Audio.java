/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

/**
 * Represents an audio data frame of type double.
 */
public class Audio extends Data {

    private double[] audioSamples;


    /**
     * Constructs an Audio object with the given audio data.
     *
     * @param audioSamples the audio samples for this Audio
     */
    public Audio(double[] audioSamples) {
	this.audioSamples = audioSamples;
    }


    /**
     * Constructs an Audio object with the given audio data and Utterance.
     *
     * @param audioSamples the audio samples for this Audio
     * @param utterance the Utterance associated with this Audio
     */
    public Audio(double[] audioSamples, Utterance utterance) {
        super(utterance);
	this.audioSamples = audioSamples;
    }


    /**
     * Constructs an Audio object with the given Signal.
     *
     * @param signal the Signal this Audio object carries
     */
    public Audio(Signal signal) {
        super(signal);
    }

    
    /**
     * Returns the audio samples.
     *
     * @return the audio samples
     */
    public double[] getSamples() {
	return audioSamples;
    }


    /**
     * Returns a string representation of this Audio.
     * The format of the string is:
     * <pre>audioFrameLength data0 data1 ...</pre>
     *
     * @return a string representation of this Audio
     */
    public String toString() {
        return Util.doubleArrayToString(audioSamples);
    }
}
