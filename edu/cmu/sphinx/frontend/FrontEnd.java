/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Timer;
import java.io.IOException;


/**
 * Pre-processes the audio into Features. The FrontEnd consists
 * of a series of processors. The FrontEnd connects all the processors
 * by the input and output points.
 * 
 * <p>The input to the FrontEnd is an AudioSource. For an audio stream,
 * the <code>StreamAudioSource</code> should be used. The 
 * input can also be a file containing a list of audio files, in which case
 * the <code>BatchFileAudioSource</code> can be used.
 *
 * <p>The output of the FrontEnd are Features, so a <code>FeatureSource</code>
 * is needed.
 * A typical sequence of calls to initialize the FrontEnd is:
 * <pre>
 * FrontEnd fe = new FrontEnd("frontend", context);
 * fe.setAudioSource(new StreamAudioSource(...));
 * 
 * // create the processors
 * Preemphasizer preemphasizer = new Preemphasizer
 *     ("Preemphasizer", context, fe.getAudioSource());
 * Windower windower = new Windower
 *     ("HammingWindow", context, preemphasizer);
 * SpectrumAnalyzer spectrumAnalyzer = new SpectrumAnalyzer
 *     ("FFT", context, windower);
 * MelFilterbank melFilterbank = new MelFilterbank
 *     ("MelFilter", context, spectrumAnalyzer);
 * MelCepstrumProducer melCepstrum = new MelCepstrumProducer
 *     ("MelCepstrum", context, melFilterbank);
 * CepstralMeanNormalizer cmn = new CepstralMeanNormalizer
 *     ("CMN", context, melCepstrum);
 *
 * // FeatureExtractor implements FeatureSource
 * fe.setFeatureSource(new FeatureExtractor(name, context, cmn));
 * </pre>
 *
 * <p>To obtain <code>Feature</code>(s) from the FrontEnd, you would use:
 * <pre>
 * fe.getFeatureFrame(10);
 * </pre>
 * which will return a <code>FeatureFrame</code> with 10 <code>Feature</code>s
 * in it. If there are only 5 more features before the end of segment,
 *
 * @see AudioSource
 * @see CepstralMeanNormalizer
 * @see Feature
 * @see FeatureExtractor
 * @see FeatureFrame
 * @see FeatureSource
 * @see MelCepstrumProducer
 * @see MelFilterbank
 * @see SpectrumAnalyzer
 * @see Windower
 */
public class FrontEnd extends DataProcessor {

    /**
     * The name of the SphinxProperty for sample rate in Hertz (i.e.,
     * number of times per second), which has a default value of 8000.
     */
    public static final String PROP_SAMPLE_RATE =
	"edu.cmu.sphinx.frontend.sampleRate";

    /**
     * The name of the SphinxProperty for the number of bytes per frame,
     * which has a default value of 4000.
     */
    public static final String PROP_BYTES_PER_AUDIO_FRAME =
	"edu.cmu.sphinx.frontend.bytesPerAudioFrame";

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
     * The name of the SphinxProperty for the size of a cepstrum, which is
     * 13 by default.
     */
    public static final String PROP_CEPSTRUM_SIZE =
    "edu.cmu.sphinx.frontend.cepstrumSize";
    

    private AudioSource audioSource;
    private FeatureSource featureSource;
    

    /**
     * Constructs a default FrontEnd.
     *
     * @param name the name of this FrontEnd
     * @param context the context of this FrontEnd
     */
    public FrontEnd(String name, String context) {
        super(name, context);
    }


    /**
     * Returns the AudioSource of this FrontEnd. The AudioSource of
     * the front end is where it gets its audio data.
     *
     * @return the AudioSource
     */
    public AudioSource getAudioSource() {
        return audioSource;
    }


    /**
     * Sets the AudioSource of this FrontEnd.  The AudioSource of
     * the front end is where it gets its audio data
     *
     * @param audioSource the AudioSource
     */
    public void setAudioSource(AudioSource audioSource) {
        this.audioSource = audioSource;
    }


    /**
     * Returns the FeatureSource of this FrontEnd.
     *
     * @return the FeatureSource of this FrontEnd
     */
    public FeatureSource getFeatureSource() {
        return featureSource;
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
}
