/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

/**
 * Represents an audio data frame of type short.
 */
public class ShortAudioFrame implements Data {

    private short[] audioSamples;


    /**
     * Constructs a ShortAudioFrame with the given audio data.
     */
    public ShortAudioFrame(short[] audioSamples) {
	this.audioSamples = audioSamples;
    }


    /**
     * Returns the audio samples.
     *
     * @return the audio samples
     */
    public short[] getSamples() {
	return audioSamples;
    }
}
