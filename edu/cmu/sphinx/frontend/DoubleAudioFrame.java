/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

/**
 * Represents an audio data frame of type double.
 */
public class DoubleAudioFrame implements Data {

    private double[] audioSamples;


    /**
     * Constructs a DoubleAudioFrame with the given audio data.
     *
     * @param audioSamples the audio samples for this DoubleAudioFrame
     */
    public DoubleAudioFrame(double[] audioSamples) {
	this.audioSamples = audioSamples;
    }


    /**
     * Constructs a DoubleAudioFrame with the given number of samples.
     *
     * @param numberOfSamples the number of samples in this DoubleAudioFrame
     */
    public DoubleAudioFrame(int numberOfSamples) {
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
