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
import java.util.Vector;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;


/**
 * Pre-processes the input audio into Features. 
 */
public class FrontEnd implements DataSource, Runnable {

    /**
     * The name of the SphinxProperty for window size (in samples).
     */
    public static final String PROP_WINDOW_SIZE =
	"edu.cmu.sphinx.frontend.windowSize";

    /**
     * The name of the SphinxProperty for window shift.
     */
    public static final String PROP_WINDOW_SHIFT =
	"edu.cmu.sphinx.frontend.windowShift";

    /**
     * The name of the SphinxProperty for the number of bytes per frame.
     */
    public static final String PROP_BYTES_PER_AUDIO_FRAME =
	"edu.cmu.sphinx.frontend.bytesPerAudioFrame";


    private InputMode inputMode;
    private int samplesPerAudioFrame;
    private List processors = null;
    private InputStream audioInputStream;
    private DataSource audioFrameSource;
    private String batchFile;
    private List queue;


    /**
     * Constructs a default FrontEnd.
     */
    public FrontEnd() {
	processors = new LinkedList();
        queue = new Vector();
    }


    /**
     * Adds the given processor to the list of processors.
     *
     * @param processorClass the name of the processor Class
     */
    public void addProcessor(PullingProcessor processor) {
	if (processors == null) {
	    processors = new LinkedList();
	}
	processors.add(processor);
    }


    /**
     * Links all the added processors together by calling
     * <code>setSource()</code> on each processor.
     */
    public void linkProcessors() {
	DataSource predecessor = null;
	ListIterator iterator = processors.listIterator();

	while (iterator.hasNext()) {
	    PullingProcessor current = (PullingProcessor) iterator.next();
	    current.setSource(predecessor);
	    predecessor = current;
	}
    }


    /**
     * Sets the source of audio input to this front-end.
     *
     * @param inputStream the source of audio input
     */
    public void setInputStream(InputStream inputStream) {
        this.inputMode = InputMode.STREAM;
	this.audioInputStream = inputStream;
	this.audioFrameSource = new DoubleAudioFrameSource(inputStream);
    }


    /**
     * Sets the input as a file that contains a list of audio files.
     *
     * @param batchFile a file that contains a list of audio files
     */
    public void setBatchFile(String batchFile) {
        this.inputMode = InputMode.BATCH;
        this.batchFile = batchFile;
    }


    /**
     * Returns a Feature produced by this FrontEnd.
     *
     * @return a Feature
     */
    public Data read() {
        Object data = queue.get(0);
        if (data != null) {
            return (Feature) data;
        } else {
            return null;
        }
    }


    /**
     * Starts the FrontEnd.
     */
    public void run() {

	// set the data source of the first processor 
	PullingProcessor first = (PullingProcessor) processors.get(0);
	if (first != null) {
	    first.setSource(this.audioFrameSource);
	}

	PullingProcessor last =
	    (PullingProcessor) processors.get(processors.size() - 1);
	if (last != null) {
	    try {
                Data output = null;
		do {
		    output = last.read();
                    
                    // add the features to the output queue
                    if (output != null) {
                        queue.add(output);
                    }
                    
		} while (output != null);
	    } catch (IOException ioe) {
		ioe.printStackTrace();
	    }
	}
    }
}


/**
 * Contains constants to indicate the input mode of the Frontend.
 */
class InputMode {


    private final String name;
    
    private InputMode(String name) {
        this.name = name;
    }
    
    public String toString() {
        return name;
    }

    /**
     * Indicates that the input is from an InputStream.
     */
    public static final InputMode STREAM = new InputMode("stream");
    
    /**
     * Indicates that the input is from a batch file.
     */
    public static final InputMode BATCH = new InputMode("batch");
}
