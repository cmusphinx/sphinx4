/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

/**
 * Represents an audio data frame of type double.
 */
public class AudioFrame implements Data {

    private double[] audioSamples;


    /**
     * Constructs a AudioFrame with the given audio data.
     *
     * @param audioSamples the audio samples for this AudioFrame
     */
    public AudioFrame(double[] audioSamples) {
	this.audioSamples = audioSamples;
    }


    /**
     * Constructs a AudioFrame with the given number of samples.
     *
     * @param numberOfSamples the number of samples in this AudioFrame
     */
    public AudioFrame(int numberOfSamples) {
	audioSamples = new double[numberOfSamples];
    }


    /**
     * Returns the audio samples.
     *
     * @return the audio samples
     */
    public double[] getAudioSamples() {
	return audioSamples;
    }
}
