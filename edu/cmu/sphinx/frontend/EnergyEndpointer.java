/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;
import java.io.IOException;
import java.util.*;


/**
 * Implements an energy-based endpointer that is based on a simple
 * two event windowing algorithm.
 *
 *
 */

public class EnergyEndpointer extends DataProcessor implements CepstrumSource {

    private static final String PROP_PREFIX = 
    "edu.cmu.sphinx.frontend.endpointer.energy";


    /**
     * If energy is greater than startHigh for startWindow frames,
     * speech has started.
     */
    private int startWindow;

    /**
     * The number of frames before energy goes above startLow
     * that a SPEECH_START is inserted.
     */
    private int startOffset;

    /**
     * If energy is below endLow for endWindow frames, speech has ended.
     */
    private int endWindow;

    /**
     * The number of frames after energy goes below endLow that
     * a SPEECH_END is inserted.
     */
    private int endOffset;

    /**
     * maxDropout is the maximum number of frames with energy below
     * endLow before speech is considered ending.
     */
    private int maxDropout;
    
    /**
     * noSpeechTimeout is the number of non-speech frames before it is
     * considered as the end of utterance.
     */
    private int noSpeechTimeout;

    private float startLow;   // lower bound for the start window
    private float startHigh;  // upper bound for the start window
    
    private float endLow;     // lower bound for the end window
    private float endHigh;    // upper bound for the end window

    private static final int BELOW_START_LOW = 1;
    private static final int BETWEEN_START_LOW_HIGH = 2;
    private static final int ABOVE_START_HIGH = 3;

    private CepstrumSource predecessor;
    private List inputBuffer;
    private Cepstrum lastFrameBelowStartLow;
    private Cepstrum endOffsetFrame;
    private boolean inSpeech;
    private int location;
    private int startHighFrames;
    private int endLowFrames;


    /**
     * Constructs an EnergyEndpointer with the given name, context,
     * and CepstrumSource predecessor.
     *
     * @param name the name of this EnergyEndpointer
     * @param context the context of the SphinxProperties this
     *    EnergyEndpointer uses
     * @param predecessor the CepstrumSource where this EnergyEndpointer
     *    gets Cepstrum from
     */
    public EnergyEndpointer(String name, String context,
                            CepstrumSource predecessor) throws IOException {
        super(name, context);
        initSphinxProperties();
        this.predecessor = predecessor;
        this.inputBuffer = Collections.synchronizedList(new LinkedList());
    }


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     */
    private void initSphinxProperties() throws IOException {
        SphinxProperties properties = getSphinxProperties();

        startLow = properties.getFloat(PROP_PREFIX + ".startLow", 0.0f);
        startHigh = properties.getFloat(PROP_PREFIX + ".startHigh", 0.0f);
        endLow = properties.getFloat(PROP_PREFIX + ".endLow", 0.0f);
        endHigh = properties.getFloat(PROP_PREFIX + ".endHigh", 0.0f);

        startWindow = properties.getInt(PROP_PREFIX + ".startWindow", 0);
        startOffset = properties.getInt(PROP_PREFIX + ".startOffset", 0);
        endWindow = properties.getInt(PROP_PREFIX + ".endWindow", 0);
        endOffset = properties.getInt(PROP_PREFIX + ".endOffset", 0);
        maxDropout = properties.getInt(PROP_PREFIX + ".maxDropout", 0);
        noSpeechTimeout = properties.getInt
            (PROP_PREFIX + ".noSpeechTimeout", 0);
    }


    private void reset() {
        inSpeech = false;
        location = BELOW_START_LOW;
        startHighFrames = 0;
        endLowFrames = 0;
    }


    /**
     * Returns the next Cepstrum, which can be either Cepstrum with
     * data, or Cepstrum with a SPEECH_START or SPEECH_END signal.
     *
     * @return the next Cepstrum, or null if no Cepstrum is available
     *
     * @throws java.io.IOException if there is error reading the
     *    Cepstrum object
     *
     * @see Cepstrum
     */
    public Cepstrum getCepstrum() throws IOException {
        
        Cepstrum cepstrum = readCepstrum();

        getTimer().start();

        if (cepstrum != null) {

            Signal signal = cepstrum.getSignal();
            
            if (signal != null && signal.equals(Signal.UTTERANCE_START)) {
                utteranceStart();
                inputBuffer.add(0, cepstrum);
                // read ahead startOffset number of Cepstrum
                while (cepstrum.getEnergy() < startHigh ||
                       startHighFrames < startWindow) {
                    cepstrum = readCepstrum();
                    if (cepstrum != null) {
                        inputBuffer.add(0, cepstrum);
                    }
                }
            } else if (signal != null && signal.equals(Signal.UTTERANCE_END)) {
                if (inSpeech) {
                    speechEnd();
                }
                inputBuffer.add(0, cepstrum);

            } else {

                // add the new cepstrum to inputBuffer
                inputBuffer.add(0, cepstrum);
            }
        }

        if (inputBuffer.size() > 0) {
            cepstrum = (Cepstrum) inputBuffer.remove(inputBuffer.size()-1);
        }
        
        getTimer().stop();

        return cepstrum;
    }
    

    private Cepstrum readCepstrum() throws IOException {
        Cepstrum cepstrum = predecessor.getCepstrum();
        if (cepstrum != null && cepstrum.hasContent()) {
            
            if (cepstrum.getEnergy() < endLow) {
                if (inSpeech) {
                    if (endLowFrames == endOffset) {
                        endOffsetFrame = cepstrum;
                    } else if (endLowFrames > endWindow) {
                        speechEnd();
                    }
                    endLowFrames++;
                }
                location = BELOW_START_LOW;

            } else if (startLow < cepstrum.getEnergy() &&
                       cepstrum.getEnergy() <= startHigh) {
                if (location == BELOW_START_LOW) {
                    if (inputBuffer.size() > 0) {
                        lastFrameBelowStartLow = (Cepstrum) inputBuffer.get(0);
                    }
                }
                location = BETWEEN_START_LOW_HIGH;

            } else if (cepstrum.getEnergy() > startHigh) {

                if (location == BELOW_START_LOW) {
                    if (inputBuffer.size() > 0) {
                        lastFrameBelowStartLow = (Cepstrum) inputBuffer.get(0);
                    }
                }

                // if energy is greater than startHigh for 
                // more than startWindow frames
                if (startHighFrames > startWindow) {
                    if (!inSpeech) {
                        insertSpeechStart();
                        inSpeech = true;
                    } else {
                        endLowFrames = 0;
                        endOffsetFrame = null;
                    }
                } else {
                    startHighFrames++;
                }
                location = ABOVE_START_HIGH;
            }
        }
        return cepstrum;
    }

    /**
     * What happens when an UTTERANCE_START is encountered.
     */
    private void utteranceStart() {
        inSpeech = false;
        startHighFrames = 0;
        endLowFrames = 0;
        lastFrameBelowStartLow = null;
        endOffsetFrame = null;
    }


    /**
     * What should happen when speech ends.
     */
    private void speechEnd() {
        insertSpeechEnd();
        inSpeech = false;
        endLowFrames = 0;
        startHighFrames = 0;
    }

    
    /**
     * Inserts a SPEECH_START at the appropriate place.
     */
    private void insertSpeechStart() {
        int index = inputBuffer.indexOf(lastFrameBelowStartLow);
        lastFrameBelowStartLow = null;
        if (index < 0) {
            System.out.println("Cannot find lastFrameBelowStartLow");
            index = 0;
        }
        inputBuffer.add(index, (new Cepstrum(Signal.SPEECH_START)));
    }

    
    /**
     * Inserts a SPEECH_END at the appropriate place.
     */
    private void insertSpeechEnd() {
        int index = inputBuffer.indexOf(endOffsetFrame);
        endOffsetFrame = null;
        if (index < 0) {
            index = 0;
        }
        inputBuffer.add(index, (new Cepstrum(Signal.SPEECH_END)));
    }
}
