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
public class FrontEnd implements Runnable {

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


    private int samplesPerAudioFrame;
    private List processors = null;
    private InputStream audioInputStream;
    private DataSource audioFrameSource;


    /**
     * Constructs a default FrontEnd.
     */
    public FrontEnd() {
	processors = new LinkedList();
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
     * Sets the source of audio input to this front-end, and connects
     * it to the first processor.
     *
     * @param inputStream the source of audio input
     */
    public void setInputStream(InputStream inputStream) {
	this.audioInputStream = inputStream;
	this.audioFrameSource = new ShortAudioFrameSource(inputStream);
    }


    /**
     * Starts the FrontEnd.
     */
    public void run() {
	Data output;

	// set the data source of the first processor 
	PullingProcessor first = (PullingProcessor) processors.get(0);
	if (first != null) {
	    first.setSource(this.audioFrameSource);
	}

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


    /**
     * Test program for running the FrontEnd.
     */
    public static void main(String[] argv) {
	if (argv.length < 1) {
	    System.out.println("Usage: java FrontEnd <filename>");
	}
	FrontEnd frontEnd = new FrontEnd();
	frontEnd.addProcessor(new Preemphasizer());
	frontEnd.addProcessor(new CepstrumProducer());
	frontEnd.linkProcessors();

	try {
	    frontEnd.setInputStream(new FileInputStream(argv[0]));
	    frontEnd.run();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
