/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;


/**
 * Pre-processes the input audio into Features. 
 */
public class FrontEnd {

    public static final String PROP_WINDOW_SIZE =
	"edu.cmu.sphinx.frontend.windowSize";
    public static final String PROP_WINDOW_SHIFT =
	"edu.cmu.sphinx.frontend.windowShift";
    public static final String PROP_BYTES_PER_AUDIO_FRAME =
	"edu.cmu.sphinx.frontend.bytesPerAudioFrame";


    private int samplesPerAudioFrame;
    private List processors;
    private InputStream audioInputStream;
    private DataSource audioFrameSource;


    /**
     * Constructs a default FrontEnd.
     */
    public FrontEnd() {
	processors = new LinkedList();
	processors.add(new Preemphasizer());
	// processors.add(new CepstrumProducer());

	linkProcessors();
    }


    /**
     * Links all the processors together by calling <code>setSource()</code>
     * on each processor.
     */
    private void linkProcessors() {
	DataSource predecessor = null;
	ListIterator iterator = processors.listIterator();

	while (iterator.hasNext()) {
	    PullingProcessor current = (PullingProcessor) iterator.next();
	    current.setSource(predecessor);
	    predecessor = current;
	}
    }


    /**
     * Sets the source of audio input to this front-end, and connects
     * it to the first processor.
     *
     * @param inputStream the source of audio input
     */
    public void setInputStream(InputStream inputStream) {
	this.audioInputStream = inputStream;
	this.audioFrameSource = new ShortAudioFrameSource(inputStream);

	// set the data source of the first processor 
	PullingProcessor first = (PullingProcessor) processors.get(0);
	if (first != null) {
	    first.setSource(this.audioFrameSource);
	}
    }


    public void run() {
	Data output;
	PullingProcessor last =
	    (PullingProcessor) processors.get(processors.size() - 1);
	if (last != null) {
	    try {
		do {
		    // System.out.println("reading...");
		    output = last.read();
		    // System.out.println("...read");
		} while (output != null);
	    } catch (IOException ioe) {
		ioe.printStackTrace();
	    }
	}
    }


    private void read() {
	int i = 0;
	try {
	    while (audioFrameSource.read() != null) {
		System.out.println("Read " + (i++) + " frame(s)");
	    }
	} catch (IOException ioe) {
	    ioe.printStackTrace();
	}
    }


    /**
     * Test program for running the FrontEnd.
     */
    public static void main(String[] argv) {
	if (argv.length < 1) {
	    System.out.println("Usage: java FrontEnd <filename>");
	}
	FrontEnd frontEnd = new FrontEnd();
	try {
	    frontEnd.setInputStream(new FileInputStream(argv[0]));
	    frontEnd.run();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}


class ShortAudioFrameSource implements DataSource {

    private static final int SEGMENT_MAX_BYTES = 2000000;

    private InputStream audioStream;
    private AudioFormat audioFormat;
    private int frameSizeInBytes;
    private int windowSizeInBytes;
    private int windowShiftInBytes;
    private byte[] samplesBuffer;
    private int totalBytesRead;
    private byte[] overflowBuffer;
    private int overflowBytes;


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
	if (totalBytesRead == 0) {
	    ShortAudioFrame audioFrame = readNextFrame();
	    return SegmentEndPointSignal.createSegmentStartSignal(audioFrame);
	} else if (totalBytesRead + frameSizeInBytes >= SEGMENT_MAX_BYTES) {
	    ShortAudioFrame audioFrame = readNextFrame();
	    return SegmentEndPointSignal.createSegmentEndSignal(audioFrame);
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
	int totalInBuffer = 0;

	// if there are previous samples, pre-pend them to input speech samps
	if (overflowBytes > 0) {
	    System.arraycopy(overflowBuffer, 0, samplesBuffer, 0,
			     overflowBytes);
	    totalInBuffer += overflowBytes;
	    overflowBytes = 0;
	}

	// read one frame from the inputstream
	int read = 0;
	int totalRead = 0;
	do {
	    read = audioStream.read
		(samplesBuffer, totalInBuffer, frameSizeInBytes - totalRead);
	    if (read > 0) {
		totalRead += read;
		totalInBuffer += read;
	    }
	} while (read != -1 && totalRead < frameSizeInBytes);

	totalBytesRead += totalRead;

	// if no audio can be read, return null
	if (read == -1 && totalRead == 0) {
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
	    }

	    if (totalInBuffer % 2 == 1) {
		samplesBuffer[totalInBuffer++] = 0;
	    }

	    // convert the byte[] into a ShortAudioFrame
	    short[] audioFrame = Util.byteToShortArray
		(samplesBuffer, 0, totalInBuffer);

	    // Util.dumpShortArray(audioFrame, "FRAME_SOURCE");

	    return (new ShortAudioFrame(audioFrame));
	}
    }
}
