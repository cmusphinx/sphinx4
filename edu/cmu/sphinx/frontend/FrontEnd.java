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
 * Pre-processes the input audio into Features. The FrontEnd is composed
 * of a series of processors that are added by the <pre>addProcessor()</pre>
 * method. The input to the FrontEnd can be an InputStream (which contains
 * audio data), in which case
 * the method <pre>setInputStream()</pre> will be used. Alternatively, the
 * input can also be a file containing a list of audio files, in which case
 * the method <pre>setBatchFile()</pre> will be used. A typical sequence
 * of method calls to use the FrontEnd is: <pre>
 * FrontEnd frontend = new FrontEnd();
 * frontend.addProcessor(...a processor...);
 * // add other processors
 * 
 * frontend.setInputStream(...InputStream created from a file...);
 * OR
 * frontend.setBatchFile(...a batch file containing a list of audio files...);
 *
 * frontend.run();
 * </pre>
 *
 * The processors will be executed in the order that they are added. The
 * first processor must take <b><code>DoubleAudioFrame</code></b> as
 * input, and the last processor must output <b><code>Features</code></b>. 
 */
public class FrontEnd implements DataSource, Runnable {


    /**
     * The name of the SphinxProperty for sample rate in Hertz (i.e.,
     * number of times per second), which has a default value of 8000.
     */
    public static final String PROP_SAMPLE_RATE =
	"edu.cmu.sphinx.frontend.sampleRate";

    /**
     * The name of the SphinxProperty for window size in milliseconds,
     * which has a default value of 25.625F.
     */
    public static final String PROP_WINDOW_SIZE_MS =
	"edu.cmu.sphinx.frontend.windowSizeInMs";

    /**
     * The name of the SphinxProperty for window shift in milliseconds,
     * which has a default value of 10F.
     */
    public static final String PROP_WINDOW_SHIFT_MS =
	"edu.cmu.sphinx.frontend.windowShiftInMs";

    /**
     * The name of the SphinxProperty for the number of bytes per frame,
     * which has a default value of 4000.
     */
    public static final String PROP_BYTES_PER_AUDIO_FRAME =
	"edu.cmu.sphinx.frontend.bytesPerAudioFrame";

    /**
     * The name of the SphinxProperty for the number of points in the FFT.
     */
    public static final String PROP_FFT_NPOINT =
	"edu.cmu.sphinx.frontend.fftNPoint";


    private int samplesPerAudioFrame;
    private List processors = null;
    private DataSource audioFrameSource;
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
     * Sets the source of audio input to this front-end.
     *
     * @param inputStream the source of audio input
     */
    public void setInputStream(InputStream inputStream) {
	this.audioFrameSource = new StreamAudioSource(inputStream);
    }


    /**
     * Sets the input as a file that contains a list of audio files.
     *
     * @param batchFile a file that contains a list of audio files
     *
     * @throws IOException if there is a file I/O problem
     */
    public void setBatchFile(String batchFile) throws IOException {
        this.audioFrameSource = new BatchFileAudioSource(batchFile);
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

        linkProcessors();

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
                    
                    // add the Features in the FeatureFrame to the output queue
                    if (output != null && output instanceof FeatureFrame) {
                        float[][] features = ((FeatureFrame) output).getData();
                        for (int i = 0; i < features.length; i++) {
                            Feature feature = new Feature(features[i]);
                            queue.add(feature);
                        }
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
