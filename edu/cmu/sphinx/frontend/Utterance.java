/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;


/**
 * Represents the complete audio data of an utterance.
 */
public class Utterance {

    private List audioBuffer;
    private int next = 0;

    private int sampleRate;
    private int windowShiftInBytes;
    private int windowSizeInBytes;


    /**
     * Constructs a default Utterance object.
     */
    public Utterance(String context) {
        audioBuffer = new Vector();

        // get the Sphinx properties
        SphinxProperties properties = 
            SphinxProperties.getSphinxProperties(context);
        sampleRate = properties.getInt(FrontEnd.PROP_SAMPLE_RATE, 8000);
        
        float windowSizeInMs = properties.getFloat
            (FrontEnd.PROP_WINDOW_SIZE_MS, 25.625F);
        float windowShiftInMs = properties.getFloat
            (FrontEnd.PROP_WINDOW_SHIFT_MS, 10.0F);
        
        windowSizeInBytes = Util.getSamplesPerWindow
            (sampleRate, windowSizeInMs) * 2;
        windowShiftInBytes = Util.getSamplesPerShift
            (sampleRate, windowShiftInMs) * 2;
    }


    /**
     * Adds an audio frame into this Utterance.
     */
    public void add(byte[] audio) {
        synchronized (audioBuffer) {
            audioBuffer.add(audio);
        }
    }


    /**
     * Returns the next audio frame.
     *
     * @return the next audio frame
     */
    public byte[] getNext() {
        synchronized (audioBuffer) {
            if (0 <= next && next < audioBuffer.size()) {
                return (byte[]) audioBuffer.get(next++);
            } else {
                return null;
            }
        }
    }
 

    /**
     * Returns the audio samples of the given feature.
     *
     * @return the audio samples
     */
    public byte[] getAudio(int frameNumber) {
        byte[] audio = new byte[windowSizeInBytes];

        // which byte in the Utterance does this feature start
        int startByte = frameNumber * windowShiftInBytes;
        int total = 0;

        for (Iterator i = audioBuffer.iterator(); i.hasNext(); ) {
            byte[] current = (byte[]) i.next();

            if (total + current.length > startByte) {
                // if the window starts in this audio frame
                int start = startByte - total;
                if (start + windowSizeInBytes <= current.length) {
                    // if the window is totally within this audio frame
                    System.arraycopy
                        (current, start, audio, 0, windowSizeInBytes);
                } else {
                    // if the window lies between this and the next audio frame
                    // copy the part that belongs to this frame
                    int firstLength = current.length - start;
                    System.arraycopy(current, start, audio, 0, firstLength);

                    // copy the part that belongs to the next frame
                    if (i.hasNext()) {
                        current = (byte[]) i.next();
                        System.arraycopy(current, 0, audio, firstLength,
                                         windowSizeInBytes - firstLength);
                    }
                }
                break;
            } else {
                total += current.length;
            }
        }

        return audio;
    }
}
