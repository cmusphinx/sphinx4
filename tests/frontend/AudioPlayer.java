/**
 * [[[copyright]]]
 */

package tests.frontend;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;


/**
 * Plays audio data to the System's audio device.
 */
public class AudioPlayer {

    private AudioFormat defaultFormat = // default format is 8khz
    new AudioFormat(8000f, 16, 1, true, true);

    private SourceDataLine line;

    
    public AudioPlayer() {}


    /**
     * Plays the given byte array audio to the System's audio device.
     *
     * @param audio the audio data to play
     */
    public void play(byte[] audio) {
        openLine(defaultFormat);
        line.start();
        line.write(audio, 0, audio.length);
        line.drain();
        line.stop();
        line.close();
        line = null;
    }

    
    /**
     * Opens the audio
     *
     * @param format the format for the audio
     *
     * @throws UnsupportedOperationException if the line cannot be opened with
     *     the given format
     */
    private void openLine(AudioFormat format) {
        if (line != null) {
            line.close();
        }
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        try {
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
        } catch(LineUnavailableException lue) {
            lue.printStackTrace();
        }   
    }
}
