/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;


/**
 * A StreamAudioSource converts data from an InputStream into
 * Audio(s). One would obtain the Audios using
 * the <code>read()</code> method.
 *
 * The size of each Audio returned is specified by:
 * <pre>
 * edu.cmu.sphinx.frontend.bytesPerAudio
 * </pre>
 *
 * @see BatchFileAudioSource
 */
public class StreamAudioSource extends DataProcessor implements AudioSource {

    private InputStream audioStream;

    private int frameSizeInBytes;
    private boolean keepAudioReference;
    
    private boolean streamEndReached = false;
    private boolean utteranceEndSent = false;
    private boolean utteranceStarted = false;


    /**
     * Constructs a StreamAudioSource with the given InputStream.
     *
     * @param name the name of this StreamAudioSource
     * @param context the context of this StreamAudioSource
     * @param audioStream the InputStream where audio data comes from
     */
    public StreamAudioSource(String name, String context,
                             InputStream audioStream) {
        super(name, context);
	initSphinxProperties();
        setInputStream(audioStream);
    }


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     */
    private void initSphinxProperties() {
        keepAudioReference = getSphinxProperties().getBoolean
            (FrontEnd.PROP_KEEP_AUDIO_REFERENCE, true);

        frameSizeInBytes = getSphinxProperties().getInt
	    (FrontEnd.PROP_BYTES_PER_AUDIO_FRAME, 4000);

        if (frameSizeInBytes % 2 == 1) {
            frameSizeInBytes++;
        }
    }


    /**
     * Sets the InputStream from which this StreamAudioSource reads.
     *
     * @param inputStream the InputStream from which audio data comes
     */
    public void setInputStream(InputStream inputStream) {
	this.audioStream = inputStream;
        streamEndReached = false;
        utteranceEndSent = false;
        utteranceStarted = false;
    }

    
    /**
     * Reads and returns the next Audio from the InputStream of
     * StreamAudioSource, return null if no data is read and end of file
     * is reached.
     *
     * @return the next Audio or <code>null</code> if none is
     *     available
     *
     * @throws java.io.IOException
     */
    public Audio getAudio() throws IOException {

        getTimer().start();

        Audio output = null;

        if (streamEndReached) {
            if (!utteranceEndSent) {
                output = new Audio(Signal.UTTERANCE_END);
                utteranceEndSent = true;
            }
        } else {
            if (!utteranceStarted) {
                utteranceStarted = true;
                output = new Audio(Signal.UTTERANCE_START);
                
            } else {
                output = readNextFrame();
            }
        }

        getTimer().stop();

        return output;
    }


    /**
     * Returns the next Audio from the input stream, or null if
     * there is none available
     *
     * @return a Audio or null
     *
     * @throws java.io.IOException
     */
    private Audio readNextFrame() throws IOException {

        // read one frame's worth of bytes
	int read = 0;
	int totalRead = 0;
        final int bytesToRead = frameSizeInBytes;
        byte[] samplesBuffer = new byte[frameSizeInBytes];

	do {
	    read = audioStream.read
		(samplesBuffer, totalRead, bytesToRead - totalRead);
	    if (read > 0) {
		totalRead += read;
	    }
	} while (read != -1 && totalRead < bytesToRead);

        if (totalRead <= 0) {
            streamEndReached = true;
            audioStream.close();
            return null;
        }

        // shrink incomplete frames
        if (totalRead < bytesToRead) {
            totalRead = (totalRead % 2 == 0) ? totalRead + 2 : totalRead + 3;
            streamEndReached = true;
            audioStream.close();
        }

        // turn it into an Audio
        Audio audio = new Audio
            (Util.byteToDoubleArray(samplesBuffer, 0, totalRead));
        
        if (getDump()) {
            System.out.println("FRAME_SOURCE " + audio.toString());
        }
        
        return audio;
    }
}





