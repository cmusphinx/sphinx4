/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Timer;

import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

/**
 * Pre-processes the audio into Features. The FrontEnd consists
 * of a series of processors. The FrontEnd connects all the processors
 * by the input and output points.
 * 
 * <p>The input to the FrontEnd is an audio data. For an audio stream,
 * the <code>StreamAudioSource</code> should be used. The 
 * input can also be a file containing a list of audio files, in which case
 * the <code>BatchFileAudioSource</code> can be used.
 *
 * <p>The output of the FrontEnd are Features.
 * To obtain <code>Feature</code>(s) from the FrontEnd, you would use:
 * <pre>
 * fe.getFeatureFrame(10);
 * </pre>
 * which will return a <code>FeatureFrame</code> with 10 <code>Feature</code>s
 * in it. If there are only 5 more features before the end of utterance,
 *
 * This FrontEnd contains all the "standard" frontend processors like
 * Preemphasizer, HammingWindower, SpectrumAnalyzer, ..., etc.
 *
 * @see AudioSource
 * @see BatchCMN
 * @see Feature
 * @see FeatureExtractor
 * @see FeatureFrame
 * @see FeatureSource
 * @see LiveCMN
 * @see MelCepstrumProducer
 * @see MelFilterbank
 * @see SpectrumAnalyzer
 * @see Windower
 */
public class FrontEnd extends DataProcessor {

    private static Map frontends = new HashMap(); 

    
    /**
     * The name of the SphinxProperty for sample rate in Hertz (i.e.,
     * number of times per second), which has a default value of 8000.
     */
    public static final String PROP_SAMPLE_RATE = "sampleRate";


    /**
     * The prefix for all Frontend SphinxProperties names.
     * Its value is currently <code>"edu.cmu.sphinx.frontend."</code>.
     */
    public static final String PROP_PREFIX = "edu.cmu.sphinx.frontend.";


    /**
     * The name of the SphinxProperty that specifies whether the
     * FrontEnd should use the properties from the AcousticModel.
     */
    public static final String PROP_USE_ACOUSTIC_MODEL_PROPS =
    PROP_PREFIX + "useAcousticModelProperties";

    
    /**
     * The name of the SphinxProperty for the number of bits per
     * sample.
     */
    public static final String PROP_BITS_PER_SAMPLE = PROP_PREFIX +
    "bitsPerSample";


    /**
     * The name of the SphinxProperty for the number of bytes per frame,
     * which has a default value of 4000.
     */
    public static final String PROP_BYTES_PER_AUDIO_FRAME = 
    PROP_PREFIX + ".bytesPerAudioFrame";


    /**
     * The name of the SphinxProperty for window size in milliseconds,
     * which has a default value of 25.625F.
     */
    public static final String PROP_WINDOW_SIZE_MS = 
    PROP_PREFIX + "windowSizeInMs";


    /**
     * The name of the SphinxProperty for window shift in milliseconds,
     * which has a default value of 10F.
     */
    public static final String PROP_WINDOW_SHIFT_MS =
    PROP_PREFIX + "windowShiftInMs";


    /**
     * The name of the SphinxProperty for the size of a cepstrum, which is
     * 13 by default.
     */
    public static final String PROP_CEPSTRUM_SIZE = 
    PROP_PREFIX + "cepstrumSize";


    /**
     * The name of the SphinxProperty that indicates whether Features
     * should retain a reference to the original raw audio bytes. The
     * default value is true.
     */
    public static final String PROP_KEEP_AUDIO_REFERENCE =
    PROP_PREFIX + "keepAudioReference";


    /**
     * The name of the SphinxProperty that decides whether we do live
     * mode CMN or batch mode CMN.
     */
    public static final String PROP_LIVE_CMN = PROP_PREFIX + "cmn.live";
    
    
    private AudioSource audioSource;
    private FeatureSource featureSource;
    private boolean liveCMN = false;
    

    /**
     * Constructs a default FrontEnd.
     *
     * @param name the name of this FrontEnd
     * @param context the context of this FrontEnd
     */
    public FrontEnd(String name, String context, AudioSource audioSource)
        throws IOException {

        super(name, context);
        
        frontends.put(context, this);

        liveCMN = getSphinxProperties().getBoolean(PROP_LIVE_CMN, false);

        // initialize all the frontend processors

        Preemphasizer preemphasizer = new Preemphasizer
            ("Preemphasizer", context, audioSource);

        Windower windower = new Windower
            ("HammingWindow", context, preemphasizer);

        SpectrumAnalyzer spectrumAnalyzer = new SpectrumAnalyzer
            ("FFT", context, windower);

        MelFilterbank melFilterbank = new MelFilterbank
            ("MelFilter", context, spectrumAnalyzer);

        MelCepstrumProducer melCepstrum = new MelCepstrumProducer
            ("MelCepstrum", context, melFilterbank);

        CepstrumSource cmn = null;

        if (liveCMN) {
            cmn = new LiveCMN("LiveCMN", context, melCepstrum);
        } else {
            cmn = new BatchCMN("BatchCMN", context, melCepstrum);
        }

        FeatureExtractor extractor = new FeatureExtractor
            ("FeatureExtractor", context, cmn);

        setAudioSource(audioSource);
        setFeatureSource(extractor);
    }


    /**
     * Returns the FrontEnd with the given context, or null if there is no
     * FrontEnd with that context.
     *
     * @return the FrontEnd with the given context, or null if there
     *    is no FrontEnd with that context
     */
    public static FrontEnd getFrontEnd(String context) {
        Object frontend = frontends.get(context);
        if (frontend != null) {
            return (FrontEnd) frontend;
        } else {
            return null;
        }
    }


    /**
     * Returns the AudioSource of this FrontEnd. The AudioSource of
     * the front end is where it gets its audio data.
     *
     * @return the AudioSource
     */
    private AudioSource getAudioSource() {
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
    private FeatureSource getFeatureSource() {
        return featureSource;
    }


    /**
     * Sets the FeatureSource of this FrontEnd.
     *
     * @param featureSource the FeatureSource
     */
    private void setFeatureSource(FeatureSource featureSource) {
        this.featureSource = featureSource;
    }


    /**
     * Returns the next N number of Features produced by this FrontEnd
     * as a single FeatureFrame.
     * The number of Features return maybe less than N, in which
     * case the last Feature will contain a Signal.UTTERANCE_END signal.
     * Consequently, the size of the FeatureFrame will be less than N.
     *
     * @param numberFeatures the number of Features to return
     *
     * @return the next N number of Features in a FeatureFrame, or null
     *    if no more FeatureFrames available
     *
     * @see FeatureFrame
     */
    public FeatureFrame getFeatureFrame(int numberFeatures) throws
    IOException {

        getTimer().start();

        FeatureFrame featureFrame = null;
        Feature[] features = new Feature[numberFeatures];
        Feature feature = null;

        int i = 0;
        do {
            feature = featureSource.getFeature();
            if (feature != null) {
                features[i++] = feature;
                if (feature.hasUtteranceEndSignal()) {
                    break;
                }
            } else {
                break;
            }
        } while (i < numberFeatures);

        // if there are some features
        if (i > 0) {

            // if we hit the end of utterance before getting the
            // desired number of features, shrink the array
            if (i < numberFeatures) {
                Feature[] lessFeatures = new Feature[i];
                for (i = 0; i < lessFeatures.length; i++) {
                    lessFeatures[i] = features[i];
                }
                features = lessFeatures;
            }
            
            featureFrame = new FeatureFrame(features);
        }
        
        getTimer().stop();

        return featureFrame;
    }

}
