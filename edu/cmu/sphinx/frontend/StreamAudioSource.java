/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;


/**
 * A StreamAudioSource converts data from an InputStream into
 * AudioFrame(s). One would obtain the AudioFrames using
 * the <code>read()</code> method.
 * The Sphinx properties that affect this StreamAudioSource are:
 * <pre>
 * edu.cmu.sphinx.frontend.bytesPerAudioFrame
 * edu.cmu.sphinx.frontend.sampleRate
 * edu.cmu.sphinx.frontend.windowSizeInMs
 * edu.cmu.sphinx.frontend.windowShiftInMs
 * </pre>The audio samples that do not fit into the current frame
 * will be used in the next frame (which is obtained by the next call to
 * <code>read()</code>).
 *
 * @see BatchFileAudioSource
 */
public class StreamAudioSource extends DataProcessor {

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
    private int windowSizeInBytes;
    private int windowShiftInBytes;

    /**
     * The buffer that contains the samples. "totalInBuffer" indicates
     * the number of bytes in the samplesBuffer so far.
     */
    private ByteBuffer samplesBuffer;
    private ByteBuffer overflowBuffer;

    private boolean streamEndReached = false;
    private boolean segmentStarted = false;


    /**
     * Constructs a StreamAudioSource with the given InputStream.
     *
     * @param audioStream the InputStream where audio data comes from
     */
    public StreamAudioSource(String context, InputStream audioStream) {
        super("StreamAudioSource", context);
	initSphinxProperties();
	this.audioStream = audioStream;
	samplesBuffer = new ByteBuffer(frameSizeInBytes * 2);
        overflowBuffer = new ByteBuffer(frameSizeInBytes);
        reset();
    }


    /**
     * Resets this StreamAudioSource into a state ready to read the
     * current InputStream.
     */
    public void reset() {
        samplesBuffer.reset();
        overflowBuffer.reset();
    }


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     */
    private void initSphinxProperties() {
	// TODO : specify the context
	SphinxProperties properties = getSphinxProperties();

        int sampleRate = properties.getInt(FrontEnd.PROP_SAMPLE_RATE, 8000);

        float windowSizeInMs = properties.getFloat
            (FrontEnd.PROP_WINDOW_SIZE_MS, 25.625F);

        float windowShiftInMs = properties.getFloat
            (FrontEnd.PROP_WINDOW_SHIFT_MS, 10.0F);
	
        windowSizeInBytes = Util.getSamplesPerWindow
            (sampleRate, windowSizeInMs) * 2;

        windowShiftInBytes = Util.getSamplesPerShift
            (sampleRate, windowShiftInMs) * 2;
	
        frameSizeInBytes = properties.getInt
	    (FrontEnd.PROP_BYTES_PER_AUDIO_FRAME, 4000);

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

        if (!streamEndReached) {
            
            if (!segmentStarted) {
                
                segmentStarted = true;
                output = EndPointSignal.SEGMENT_START;
                
            } else if (samplesBuffer.getTotalBytesRead() >= segmentMaxBytes) {
                
                segmentStarted = false;
                output = EndPointSignal.SEGMENT_END;
                
            } else {
                Data nextFrame = readNextFrame();
                streamEndReached = (nextFrame == null);
                
                if (streamEndReached) {
                    audioStream.close();
                    output = EndPointSignal.SEGMENT_END;
                } else {
                    output = nextFrame;
                }
            }
        }

        getTimer().stop();

        return output;
    }


    /**
     * Returns the next AudioFrame from the input stream, or null if
     * there is none available
     *
     * @return a AudioFrame
     *
     * @throws java.io.IOException
     */
    private AudioFrame readNextFrame() throws IOException {

	// if there are previous samples, pre-pend them to input speech samps
	// read one frame from the inputstream
        samplesBuffer.reset();
        samplesBuffer.transferAllFrom(overflowBuffer);
        overflowBuffer.reset();
	samplesBuffer.readFrom(audioStream, frameSizeInBytes);
        
	// if nothing in the samplesBuffer, return null
	if (samplesBuffer.getTotalInBuffer() == 0) {
	    return null;
	}
	
	// if read bytes do not fill a frame, copy them to overflow buffer
	// after the previously stored overlap samples
	if (samplesBuffer.getTotalInBuffer() < windowSizeInBytes) {
            overflowBuffer.transferAllFrom(samplesBuffer);
	    return null;

	} else {
	    int occupiedElements = Util.getOccupiedElements
		(samplesBuffer.getTotalInBuffer(), windowSizeInBytes,
                 windowShiftInBytes);

	    if (occupiedElements < samplesBuffer.getTotalInBuffer()) {

		// assign samples which don't fill an entire frame to 
		// overflow buffer for use on next pass
		int offset = occupiedElements - 
                    (windowSizeInBytes - windowShiftInBytes);
                int residue = samplesBuffer.getTotalInBuffer() - offset;

                overflowBuffer.reset();
                overflowBuffer.transferFrom(samplesBuffer, offset,
                                            residue);
		samplesBuffer.setTotalInBuffer(occupiedElements);
	    }

	    // convert the ByteBuffer into a AudioFrame
	    AudioFrame audioFrame = samplesBuffer.toAudioFrame();
            
	    if (getDump()) {
		System.out.println("FRAME_SOURCE " + audioFrame.toString());
	    }

	    return audioFrame;
	}
    }
}


/**
 * A byte buffer that provides the functionality needed by the
 * StreamAudioSource.
 */
class ByteBuffer {

    private byte[] buffer;
    private int totalInBuffer;
    private int totalBytesRead;

    /**
     * Constructs a ByteBuffer of the given size.
     *
     * @param size size of the buffer
     */
    public ByteBuffer(int size) {
        buffer = new byte[size];
    }

    /**
     * Returns the actual byte array.
     *
     * @return the actual byte array
     */
    public byte[] getBuffer() {
        return buffer;
    }

    /**
     * Returns the total number of bytes in the buffer.
     *
     * @return the total number of bytes in the buffer
     */
    public int getTotalInBuffer() {
        return totalInBuffer;
    }

    /**
     * Sets the number of bytes consider relevant in this buffer.
     *
     * @param the number of relevant bytes
     */
    public void setTotalInBuffer(int numberBytes) {
        this.totalInBuffer = numberBytes;
    }

    /**
     * Returns the total number of bytes read as a result of the
     * <code>readFrom()</code> method.
     *
     * @return the total number of bytes read
     */
    public int getTotalBytesRead() {
        return totalBytesRead;
    }
    
    /**
     * Sets the number of bytes in the buffer to zero, and sets
     * the total number of bytes read from an InputStream to zero.
     */
    public void reset() {
        totalInBuffer = 0;
        totalBytesRead = 0;
    }

    /**
     * Reads the given number of bytes from the given InputStream.
     *
     * @return the actual number of bytes read
     *
     * @throws java.io.IOException if there is an error reading from
     * the InputStream
     */
    public int readFrom(InputStream inputStream, int bytesToRead) throws IOException {
	int read = 0;
	int totalRead = 0;
	do {
	    read = inputStream.read
		(buffer, totalInBuffer, bytesToRead - totalRead);
	    if (read > 0) {
		totalRead += read;
		totalInBuffer += read;
	    }
	} while (read != -1 && totalRead < bytesToRead);

        totalBytesRead += totalRead;

	return totalRead;
    }

    /**
     * Transfer the given number of bytes from the given ByteBuffer,
     * starting at the given position.
     *
     * @param src the ByteBuffer to transfer bytes from
     * @param srcPos where to start from the source ByteBuffer
     * @param number of bytes to transfer
     */
    public void transferFrom(ByteBuffer src, int srcPos, int length) {
        System.arraycopy(src.buffer, srcPos, buffer, totalInBuffer, length);
        totalInBuffer += length;
    }

    /**
     * Transfer all the bytes in the given ByteBuffer to this ByteBuffer.
     *
     * @param src the ByteBuffer to transfer bytes from
     */
    public void transferAllFrom(ByteBuffer src) {
        transferFrom(src, 0, src.totalInBuffer);
    }

    /**
     * If this ByteBuffer has an odd number of bytes, pad an extra
     * zero byte to make it even.
     */
    private void padOddBytes() {
        if (totalInBuffer % 2 == 1) {
            buffer[totalInBuffer++] = 0;
        }
    }

    /**
     * Returns this ByteBuffer as a double array.
     *
     * @return a double array
     */
    public double[] toDoubleArray() {
        padOddBytes();
        return Util.byteToDoubleArray(buffer, 0, totalInBuffer);
    }

    /**
     * Returns this ByteBuffer as an AudioFrame object.
     *
     * @return an AudioFrame
     */
    public AudioFrame toAudioFrame() {
        return (new AudioFrame(toDoubleArray()));
    }
}
