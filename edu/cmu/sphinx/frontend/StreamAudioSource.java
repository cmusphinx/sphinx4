/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;
import java.io.IOException;
import java.io.InputStream;


/**
 * A StreamAudioSource converts data from an InputStream into
 * Audio(s). One would obtain the Audios using
 * the <code>read()</code> method.
 *
 * The size of each Audio returned is specified by:
 * <pre>
 * edu.cmu.sphinx.frontend.bytesPerAudio
 * </pre>
 * The maximum size of a segment of speech is given by:
 * <pre>
 * edu.cmu.sphinx.frontend.streamAudioSource.segmentMaxBytes
 * </pre>
 *
 * @see BatchFileAudioSource
 */
public class StreamAudioSource extends DataProcessor implements AudioSource {

    /**
     * The name of the SphinxProperty which specifies the maximum
     * number of bytes in a segment of speech.
     * The default value is 2,000,000.
     */
    public static final String PROP_SEGMENT_MAX_BYTES =
    "edu.cmu.sphinx.frontend.streamAudioSource.segmentMaxBytes";
    

    private InputStream audioStream;

    private int segmentMaxBytes;
    private int frameSizeInBytes;

    private byte[] samplesBuffer;
    private int totalBytesRead;
    
    private boolean streamEndReached = false;
    private boolean segmentEndSent = false;
    private boolean segmentStarted = false;


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
	samplesBuffer = new byte[frameSizeInBytes];
    }


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     */
    private void initSphinxProperties() {
	SphinxProperties properties = getSphinxProperties();

        frameSizeInBytes = properties.getInt
	    (FrontEnd.PROP_BYTES_PER_AUDIO_FRAME, 4000);

        if (frameSizeInBytes % 2 == 1) {
            frameSizeInBytes++;
        }

        segmentMaxBytes = properties.getInt(PROP_SEGMENT_MAX_BYTES, 2000000);
    }


    /**
     * Sets the InputStream from which this StreamAudioSource reads.
     *
     * @param inputStream the InputStream from which audio data comes
     */
    public void setInputStream(InputStream inputStream) {
	this.audioStream = inputStream;
        streamEndReached = false;
        segmentEndSent = false;
        segmentStarted = false;
        totalBytesRead = 0;
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
            if (!segmentEndSent) {
                output = new Audio(Signal.SEGMENT_END);
                segmentEndSent = true;
            }
        } else {
            if (!segmentStarted) {
                
                segmentStarted = true;
                output = new Audio(Signal.SEGMENT_START);
                
            } else if (totalBytesRead >= segmentMaxBytes) {
                
                segmentStarted = false;
                output = new Audio(Signal.SEGMENT_END);
                segmentEndSent = true;
                
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

        this.totalBytesRead += totalRead;

        byte[] finalBuffer = samplesBuffer;

        // shrink smaller frames
        if (totalRead < bytesToRead) {
            int finalBufferSize = (totalRead % 2 == 0) ? totalRead + 2:
                totalRead + 3;
            finalBuffer = new byte[finalBufferSize];
            System.arraycopy(samplesBuffer, 0, finalBuffer, 0, totalRead);
            streamEndReached = true;
            audioStream.close();
        }

        // turn it into an Audio
        Audio audio = new Audio
            (Util.byteToDoubleArray(finalBuffer, 0, finalBuffer.length));
        
        if (getDump()) {
            System.out.println("FRAME_SOURCE " + audio.toString());
        }
        
        return audio;
    }
}





