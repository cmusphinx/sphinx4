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
 * AudioFrame(s). One would obtain the AudioFrames using
 * the <code>read()</code> method.
 *
 * The size of each AudioFrame returned is specified by:
 * <pre>
 * edu.cmu.sphinx.frontend.bytesPerAudioFrame
 * </pre>
 * The maximum size of a segment of speech is given by:
 * <pre>
 * edu.cmu.sphinx.frontend.segmentMaxBytes
 * </pre>
 *
 * @see BatchFileAudioSource
 */
public class StreamAudioSource extends DataProcessor {

    private InputStream audioStream;

    private int segmentMaxBytes;
    private int frameSizeInBytes;

    /**
     * The buffer that contains the samples. "totalInBuffer" indicates
     * the number of bytes in the samplesBuffer so far.
     */
    private byte[] samplesBuffer;
    private int totalBytesRead;
    
    private boolean streamEndReached = false;
    private boolean segmentEndSent = false;
    private boolean segmentStarted = false;


    /**
     * Constructs a StreamAudioSource with the given InputStream.
     *
     * @param audioStream the InputStream where audio data comes from
     */
    public StreamAudioSource(String context, InputStream audioStream) {
        super("StreamAudioSource", context);
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

        segmentMaxBytes = properties.getInt
            (FrontEnd.PROP_SEGMENT_MAX_BYTES, 2000000);
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
     * Reads and returns the next AudioFrame from the InputStream of
     * StreamAudioSource, return null if no data is read and end of file
     * is reached.
     *
     * @return the next AudioFrame or <code>null</code> if none is
     *     available
     *
     * @throws java.io.IOException
     */
    public Data read() throws IOException {

        getTimer().start();

        Data output = null;

        if (streamEndReached) {
     
            if (!segmentEndSent) {
                output = EndPointSignal.SEGMENT_END;
                segmentEndSent = true;
            }
        } else {
            if (!segmentStarted) {
                
                segmentStarted = true;
                output = EndPointSignal.SEGMENT_START;
                
            } else if (totalBytesRead >= segmentMaxBytes) {
                
                segmentStarted = false;
                output = EndPointSignal.SEGMENT_END;
                segmentEndSent = true;
                
            } else {
                output = readNextFrame();
            }
        }

        getTimer().stop();

        return output;
    }


    /**
     * Returns the next AudioFrame from the input stream, or null if
     * there is none available
     *
     * @return a AudioFrame or null
     *
     * @throws java.io.IOException
     */
    private AudioFrame readNextFrame() throws IOException {

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

        // turn it into an AudioFrame
        AudioFrame audioFrame = new AudioFrame
            (Util.byteToDoubleArray(finalBuffer, 0, finalBuffer.length));
        
        if (getDump()) {
            System.out.println("FRAME_SOURCE " + audioFrame.toString());
        }
        
        return audioFrame;
    }
}





