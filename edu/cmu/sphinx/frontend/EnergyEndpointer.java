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
    "edu.cmu.sphinx.frontend.energyEndpointer.";

    /**
     * Controls whether to merge multiple speech segments within an
     * Utterance to one big speech segment, with the boundaries being
     * the start of the first speech segment, and the end of the
     * last speech segment. 
     */
    private boolean mergeSpeechSegments;

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

    // location values: below startLow, between low and high, above high
    private static final int BELOW_START_LOW = 1;
    private static final int BETWEEN_START_LOW_HIGH = 2;
    private static final int ABOVE_START_HIGH = 3;

    private CepstrumSource predecessor;  // where to pull Cepstra from
    private LinkedList inputBuffer;      // where to cache the read Cepstra

    /**
     * The last frame before speech went above startLow.
     */
    private Cepstrum lastFrameBelowStartLow;

    /**
     * The frame when speech stayed below endLow for endOffset frames.
     */
    private Cepstrum endOffsetFrame;

    /**
     * The last SPEECH_END Cepstrum inserted into the stream.
     */
    private Cepstrum lastSpeechEndFrame;

    private boolean inSpeech;       // are we in a speech region?
    private float lastEnergy;       // previous energy value
    private int location;           // which part of hill are we at?
    private int startLowFrames;     // # of contiguous frames energy < startLow
    private int startHighFrames;    // # of frames with energy > startHigh
    private int endLowFrames;       // # of frames with energy < endLow
    private int numSpeechSegments;  // # of speech segments in this utterance


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
        this.inputBuffer = new LinkedList();
        reset();
    }


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     *
     * @throws IOException if there is an error reading the parameters
     */
    private void initSphinxProperties() throws IOException {
        SphinxProperties properties = getSphinxProperties();

        mergeSpeechSegments = properties.getBoolean
            (PROP_PREFIX + "mergeSpeechSegments", false);

        startLow = properties.getFloat(PROP_PREFIX + "startLow", 0.0f);
        startHigh = properties.getFloat(PROP_PREFIX + "startHigh", 0.0f);
        endLow = properties.getFloat(PROP_PREFIX + "endLow", 0.0f);
        endHigh = properties.getFloat(PROP_PREFIX + "endHigh", 0.0f);

        startWindow = properties.getInt(PROP_PREFIX + "startWindow", 0);
        startOffset = properties.getInt(PROP_PREFIX + "startOffset", 0);
        endWindow = properties.getInt(PROP_PREFIX + "endWindow", 0);
        endOffset = properties.getInt(PROP_PREFIX + "endOffset", 0);
        maxDropout = properties.getInt(PROP_PREFIX + "maxDropout", 0);
        noSpeechTimeout = properties.getInt
            (PROP_PREFIX + "noSpeechTimeout", 0);
    }


    /**
     * Reset all the variables to their initial values.
     *
     * TODO: Currently, not all the variables are reset.
     */
    private void reset() {
        inSpeech = false;
        lastEnergy = 0;
        location = BELOW_START_LOW;
        startHighFrames = 0;
        endLowFrames = 0;
        numSpeechSegments = 0;
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
                inputBuffer.addFirst(cepstrum);
                // read ahead until we have reach the start of speech
                while (cepstrum.getEnergy() < startHigh ||
                       startHighFrames < startWindow) {
                    cepstrum = readCepstrum();
                    if (cepstrum != null) {
                        inputBuffer.addFirst(cepstrum);
                    }
                }
            } else if (signal != null && signal.equals(Signal.UTTERANCE_END)) {
                if (inSpeech) {
                    speechEnd();
                }
                inputBuffer.addFirst(cepstrum);

            } else {
                // add the new cepstrum to inputBuffer
                inputBuffer.addFirst(cepstrum);
            }
        }

        cepstrum = (Cepstrum) inputBuffer.removeLast();
        
        getTimer().stop();

        return cepstrum;
    }


    /**
     * Reads a Cepstrum from the predecessor, and use it to do endpointing.
     *
     * @return the Cepstrum read from the predecessor
     */
    private Cepstrum readCepstrum() throws IOException {
        Cepstrum cepstrum = predecessor.getCepstrum();

        if (cepstrum != null && cepstrum.hasContent()) {
            // Call a different method to handle the Cepstrum
            // depending on its energy level.
            if (cepstrum.getEnergy() < startLow || 
                cepstrum.getEnergy() < endLow) {
                processLowEnergyCepstrum(cepstrum);
            } else if (startLow < cepstrum.getEnergy() &&
                       cepstrum.getEnergy() <= startHigh) {
                processMediumEnergyCepstrum(cepstrum);
            } else if (cepstrum.getEnergy() > startHigh) {
                processHighEnergyCepstrum(cepstrum);
            }
        }
            
        return cepstrum;
    }


    /**
     * Process the given low energy Cepstrum.
     *
     * @param cepstrum the low energy Cepstrum to process
     */
    private void processLowEnergyCepstrum(Cepstrum cepstrum) {
        if (inSpeech) {
            // If we in a speech segment, but encounters a low energy
            // cepstrum, this can be the end of the speech segment.
            // This can be confirmed by (endLowFrames > endWindow).
            if (endLowFrames == endOffset) {
                endOffsetFrame = cepstrum;
            } else if (endLowFrames > endWindow) {
                speechEnd();
            }
            endLowFrames++;
        } else {
            startLowFrames++;
        }

        if (cepstrum.getEnergy() < startLow) {
            location = BELOW_START_LOW;
        }
    }


    /**
     * Process the given medium energy Cepstrum.
     *
     * @param cepstrum the medium energy Cepstrum to process
     */
    private void processMediumEnergyCepstrum(Cepstrum cepstrum) {
        if (location == BELOW_START_LOW) {
            if (lastFrameBelowStartLow != null) {
                // If we are below startLow, and lastFrameBelowStartLow
                // was not null, we just had a dropout (or spike).
                // If the dropout was longer than maxDropout,
                // then the lastFrameBelowStartLow should be reset.
                if (maxDropout < startLowFrames &&
                    inputBuffer.size() > 0) {
                    lastFrameBelowStartLow = (Cepstrum) inputBuffer.getFirst();
                }
            } else if (inputBuffer.size() > 0) {
                lastFrameBelowStartLow = (Cepstrum) inputBuffer.getFirst();
            }
        }
        startLowFrames = 0;
        location = BETWEEN_START_LOW_HIGH;
    }
   

    /**
     * Process the given high energy Cepstrum.
     *
     * @param cepstrum the high energy Cepstrum to process
     */
    private void processHighEnergyCepstrum(Cepstrum cepstrum) {
        if (location == BELOW_START_LOW) {
            if (inputBuffer.size() > 0) {
                lastFrameBelowStartLow = (Cepstrum) inputBuffer.getFirst();
            }
        }
        
        // if energy is greater than startHigh for 
        // more than startWindow frames
        if (startHighFrames > startWindow) {
            if (!inSpeech) {
                speechStart();
            } else {
                endLowFrames = 0;
                endOffsetFrame = null;
            }
        } else {
            startHighFrames++;
        }
        location = ABOVE_START_HIGH;
    }


    /**
     * What happens when an UTTERANCE_START is encountered.
     */
    private void utteranceStart() {
        reset();
        startLowFrames = 0;
        lastFrameBelowStartLow = null;
        endOffsetFrame = null;
    }


    /**
     * What happens when speech starts.
     */
    private void speechStart() {

        // insert a SPEECH_START either if this is the first speech
        // segment or, if we are not merging the speech segments
        if (numSpeechSegments == 0 || !mergeSpeechSegments) {
            insertSpeechStart();
        }

        // if we are merging the speech segments in an utterance
        // and there are already other speech segments in this utterance,
        // remove the last SPEECH_END Cepstrum
        if (mergeSpeechSegments && numSpeechSegments > 0) {
            removeLastSpeechEnd();
        }

        lastFrameBelowStartLow = null;
        inSpeech = true;
        endLowFrames = 0;
        endOffsetFrame = null;
        numSpeechSegments++;
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
        // "index" is where we should insert the SPEECH_START
        int index = inputBuffer.indexOf(lastFrameBelowStartLow);

        // Go back startOffset frames, but check if we have hit
        // an UTTERANCE_START, in which case we should stop going back.
        if (index > -1) {
            int i = 0;
            for (ListIterator iterator = inputBuffer.listIterator(index);
                 i < startOffset && iterator.hasNext(); i++) {
                Cepstrum cepstrum = (Cepstrum) iterator.next();
                if (cepstrum.getSignal().equals(Signal.UTTERANCE_START)) {
                    break;
                }
            }
            index += i;
        }

        if (index >= inputBuffer.size()) {
            index = inputBuffer.size();
        }
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
        lastSpeechEndFrame = new Cepstrum(Signal.SPEECH_END);
        inputBuffer.add(index, lastSpeechEndFrame);
    }


    /**
     * Removes the previous SPEECH_END Cepstrum.
     */
    private void removeLastSpeechEnd() {
        if (lastSpeechEndFrame != null) {
            inputBuffer.remove(lastSpeechEndFrame);
        }
    }
}
