/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Timer;
import java.io.IOException;


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
 *
 * @see FeatureFrame 
 */
public class FrontEnd extends DataProcessor implements Runnable {

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
     * The name of the SphinxProperty which specifies the maximum
     * number of bytes in a segment of speech.
     * The default value is 2,000,000.
     */
    public static final String PROP_SEGMENT_MAX_BYTES =
    "edu.cmu.sphinx.frontend.segmentMaxBytes";
    
    /**
     * The name of the SphinxProperty for the size of a cepstrum, which is
     * 13 by default.
     */
    public static final String PROP_CEPSTRUM_SIZE =
    "edu.cmu.sphinx.frontend.cepstrumSize";
    

    private AudioSource audioSource;
    private FeatureSource featureSource;
    

    /**
     * Constructs a default FrontEnd.
     */
    public FrontEnd(String name, String context) {
        super(name, context);
    }


    public AudioSource getAudioSource() {
        return audioSource;
    }


    public void setAudioSource(AudioSource audioSource) {
        this.audioSource = audioSource;
    }


    /**
     * Sets the FeatureSource of this FrontEnd.
     *
     * @param featureSource the FeatureSource
     */
    public void setFeatureSource(FeatureSource featureSource) {
        this.featureSource = featureSource;
    }


    /**
     * Returns the next N number of Features produced by this FrontEnd
     * as a single FeatureFrame.
     * The number of Features return maybe less than N, in which
     * case the last Feature will contain a Signal.SEGMENT_END signal.
     * However, the size of the FeatureFrame will still be N.
     *
     * @param numberFeatures the number of Features to return
     *
     * @return the next N number of Features in a FeatureFrame
     *
     * @see FeatureFrame
     */
    public FeatureFrame getFeatureFrame(int numberFeatures) throws
    IOException {

        Feature[] features = new Feature[numberFeatures];
        FeatureFrame featureFrame = new FeatureFrame(features);
        Feature feature = null;

        int i = 0;
        do {
            feature = featureSource.getFeature();
            features[i++] = feature;
            if (feature.hasSegmentEndSignal()) {
                break;
            }
        } while (i < numberFeatures);
        
        return featureFrame;
    }


    /**
     * Executes the FrontEnd. When this FrontEnd is used to create a
     * Thread, calling <code>Thread.start()</code> causes this method
     * to be executed in that Thread.
     */
    public void run() {

        getTimer().start();

        Feature feature = null;
        
        try {
            do {
                feature = featureSource.getFeature();
            } while (feature != null);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        getTimer().stop();
    }
}
