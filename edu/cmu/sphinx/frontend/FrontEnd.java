/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Timer;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;


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
 * frontend.setAudioSource(...either a StreamAudioSource
 * or a BatchFileAudioSource...)
 *
 * frontend.run();
 * </pre>
 *
 * The processors will be executed in the order that they are added. The
 * first processor must take <b><code>AudioFrame</code></b> as
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
     * The name of the SphinxProperty for the size of a cepstrum, which is
     * 13 by default.
     */
    public static final String PROP_CEPSTRUM_SIZE =
    "edu.cmu.sphinx.frontend.cepstrumSize";
    

    private String context;
    private List processors = null;
    private DataSource audioFrameSource;
    private List queue;
    private Timer timer;
    

    /**
     * Constructs a default FrontEnd.
     */
    public FrontEnd(String context) {
        this.context = context;
	processors = new LinkedList();
        queue = new Vector();
        timer = Timer.getTimer(context, "FrontEnd");
    }


    /**
     * Adds the given processor to the list of processors.
     *
     * @param processor the DataProcessor to add
     */
    public void addProcessor(DataProcessor processor) {
	if (processors == null) {
	    processors = new LinkedList();
	}
	processors.add(processor);
    }


    /**
     * Returns all the processors.
     *
     * @return all the processors
     */
    public List getProcessors() {
        return processors;
    }


    /**
     * Links all the added processors together by calling
     * <code>setSource()</code> on each processor.
     */
    private void linkProcessors() {
	DataSource predecessor = null;
	ListIterator iterator = processors.listIterator();

	while (iterator.hasNext()) {
	    DataProcessor current = (DataProcessor) iterator.next();
	    current.setSource(predecessor);
	    predecessor = current;
	}
    }


    /**
     * Sets the source of audio input to this front-end.
     *
     * @param audioSource the source of audio data
     *
     * @see BatchFileAudioSource
     * @see StreamAudioSource
     */
    public void setAudioSource(DataSource audioSource) {
	this.audioFrameSource = audioSource;
    }


    /**
     * Returns the next Data object produced by the FrontEnd, which
     * can be a FeatureFrame, EndPointSignal.SEGMENT_START, or
     * EndPointSignal.SEGMENT_END signals produced by this FrontEnd.
     * Return null if no Data available
     *
     * @return a Data object, which is usually a FeatureFrame, or null
     *    if no Data available
     */
    public Data read() {
        synchronized (queue) {
            if (queue.size() == 0) {
                return null;
            } else {
                Object data = queue.remove(0);
                if (data == null) {
                    return null;
                } else {
                    return (Data) data;
                }
            }
        }
    }


    /**
     * Executes the FrontEnd. When this FrontEnd is used to create a
     * Thread, calling <code>Thread.start()</code> causes this method
     * to be executed in that Thread.
     */
    public void run() {

        linkProcessors();

	// set the data source of the first processor 
	DataProcessor first = (DataProcessor) processors.get(0);
	if (first != null) {
	    first.setSource(this.audioFrameSource);
	}

	DataProcessor last =
	    (DataProcessor) processors.get(processors.size() - 1);

        timer.start();

	if (last != null) {
            Data output = null;
            do {
                try {
                    output = last.read();
                    if (output != null) {
                        handleData(output);
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }                
            } while (output != null);
        }

        timer.stop();
    }


    /**
     * Handles an incoming Data object.
     */
    private void handleData(Data input) {

        if (input instanceof EndPointSignal) {
            EndPointSignal signal = (EndPointSignal) input;
            if (signal.equals(EndPointSignal.SEGMENT_START) ||
                signal.equals(EndPointSignal.SEGMENT_END)) {
                queue.add(input);
            }

        } else if (input instanceof FeatureFrame) {
            queue.add(input);
        }
    }
}
