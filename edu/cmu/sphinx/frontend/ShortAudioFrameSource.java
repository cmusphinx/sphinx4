/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;
import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;


public class ShortAudioFrameSource implements DataSource {

    private static final int SEGMENT_MAX_BYTES = 2000000;

    private InputStream audioStream;
    private AudioFormat audioFormat;
    private int frameSizeInBytes;
    private int windowSizeInBytes;
    private int windowShiftInBytes;
    private byte[] samplesBuffer;
    private int totalInBuffer;
    private int totalBytesRead;
    private byte[] overflowBuffer;
    private int overflowBytes;
    private Queue queue;


    /**
     * Constructs an AudioSource with the given InputStream and frame size.
     * Frame size refers to the number of audio samples in each frame.
     *
     * @param audioStream the InputStream where audio data comes from
     * @param audioFormat the audio format of the input audioStream
     * @param frameSize the number of audio samples in each output
     *     AudioFrame
     */
    public ShortAudioFrameSource(InputStream audioStream) {
	getSphinxProperties();

	this.audioStream = audioStream;
	this.samplesBuffer = new byte[frameSizeInBytes * 2];
	this.totalBytesRead = 0;
	this.overflowBuffer = new byte[frameSizeInBytes];
	this.overflowBytes = 0;
	this.queue = new Queue();
    }


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     */
    private void getSphinxProperties() {
	// TODO : specify the context
	SphinxProperties properties = SphinxProperties.getSphinxProperties("");
	windowSizeInBytes = properties.getInt
	    (FrontEnd.PROP_WINDOW_SIZE, 205) * 2;
	windowShiftInBytes = properties.getInt
	    (FrontEnd.PROP_WINDOW_SHIFT, 80) * 2;
	frameSizeInBytes = properties.getInt
	    (FrontEnd.PROP_BYTES_PER_AUDIO_FRAME, 4000);
    }


    /**
     * Returns the InputStream from which this ShortAudioFrameSource reads.
     *
     * @return the InputStream from which audio data comes
     */
    public InputStream getInputStream() {
	return audioStream;
    }

    
    /**
     * Reads and returns the next ShortAudioFrame from the InputStream of
     * AudioFrameSource, return null if no data is read and end of file
     * is reached.
     *
     * @return the next ShortAudioFrame or <code>null</code> if none is
     *     available
     */
    public Data read() throws IOException {
	if (!queue.isEmpty()) {
	    return queue.pop();
	} else if (totalBytesRead == 0) {
	    queue.push(readNextFrame());
	    return SegmentEndPointSignal.createSegmentStartSignal();
	} else if (totalBytesRead + frameSizeInBytes >= SEGMENT_MAX_BYTES) {
	    queue.push(readNextFrame());
	    return SegmentEndPointSignal.createSegmentEndSignal();
	} else {
	    return readNextFrame();
	}
    }


    /**
     * Reads the next ShortAudioFrame from the input stream.
     *
     * @return a ShortAudioFrame
     */
    private ShortAudioFrame readNextFrame() throws IOException {

	totalInBuffer = 0;

	// if there are previous samples, pre-pend them to input speech samps
	if (overflowBytes > 0) {
	    System.arraycopy(overflowBuffer, 0, samplesBuffer, 0,
			     overflowBytes);
	    totalInBuffer += overflowBytes;
	    overflowBytes = 0;
	}

	// read one frame from the inputstream
	readInputStream(frameSizeInBytes);

	// if nothing in the samplesBuffer, return null
	if (totalInBuffer == 0) {
	    return null;
	}
	
	// reset the totalBytesRead if necessary
	if (totalBytesRead >= SEGMENT_MAX_BYTES) {
	    totalBytesRead = 0;
	}
	
	// if read bytes do not fill a frame, copy them to overflow buffer
	// after the previously stored overlap samples

	if (totalInBuffer < windowSizeInBytes) {
	    System.arraycopy(samplesBuffer, 0, overflowBuffer, overflowBytes,
			     totalInBuffer);
	    overflowBytes += totalInBuffer;
	    return null;
	} else {
	    int occupiedElements = Util.getOccupiedElements
		(totalInBuffer, windowSizeInBytes, windowShiftInBytes);

	    // System.out.println("totalInBuffer: " + totalInBuffer/2);
	    // System.out.println("occupiedElements: " + occupiedElements/2);

	    if (occupiedElements < totalInBuffer) {

		// assign samples which don't fill an entire frame to 
		// overflow buffer for use on next pass
		
		int offset = windowShiftInBytes * Util.getWindowCount
		    (totalInBuffer, windowSizeInBytes, windowShiftInBytes);
		// System.out.println("offset: " + offset/2);
		overflowBytes = totalInBuffer - offset;

		System.arraycopy
		    (samplesBuffer, offset, overflowBuffer, 0, overflowBytes);
		totalInBuffer = occupiedElements;

		// TODO: set "prior" for the next read
		short prior = Util.bytesToShort(samplesBuffer, offset - 2);
		queue.push(new PreemphasisPriorSignal(prior));
	    }

	    if (totalInBuffer % 2 == 1) {
		samplesBuffer[totalInBuffer++] = 0;
	    }

	    // convert the byte[] into a ShortAudioFrame
	    short[] audioFrame = Util.byteToShortArray
		(samplesBuffer, 0, totalInBuffer);

	    Util.dumpShortArray(audioFrame, "FRAME_SOURCE");

	    return (new ShortAudioFrame(audioFrame));
	}
    }


    /**
     * Reads the specified number of bytes from the InputStream, and
     * return the number of bytes read.
     *
     * @param numberOfBytes the number of bytes to read
     *
     * @return the number of bytes read
     */
    private int readInputStream(int numberOfBytes) throws IOException {
	int read = 0;
	int totalRead = 0;
	do {
	    read = audioStream.read
		(samplesBuffer, totalInBuffer, numberOfBytes - totalRead);
	    if (read > 0) {
		totalRead += read;
		totalInBuffer += read;
	    }
	} while (read != -1 && totalRead < numberOfBytes);
	
	totalBytesRead += totalRead;

	return totalRead;
    }
}

class Queue {

    private Data queue = null;

    public void push(Data data) {
	queue = data;
    }

    public Data pop() {
	Data temp = queue;
	queue = null;
	return temp;
    }

    public boolean isEmpty() {
	return (queue == null);
    }
}
