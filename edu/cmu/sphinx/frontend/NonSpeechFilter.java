/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;
import java.io.IOException;
import java.util.*;


/**
 * Given a sequence of Cepstra, filters out the non-speech regions.
 * The sequence of Cepstra should have the speech and non-speech regions
 * marked out by the Signal.SPEECH_START and Signal.SPEECH_END, using
 * and endpointer.
 *
 * A sequence of Cepstra for an Utterance should look like:
 *
 * <pre>
 * UTTERANCE_START (non-speech Cepstra)
 * SPEECH_START (speech Cepstra) SPEECH_END (non-speech Cepstra)
 * UTTERANCE_END
 * </pre>
 * or
 * <pre>
 * UTTERANCE_START (non-speech Cepstra)
 * SPEECH_START (speech Cepstra) SPEECH_END (non-speech Cepstra)
 * SPEECH_START (speech Cepstra) SPEECH_END (non-speech Cepstra)
 * ...
 * UTTERANCE_END
 * </pre>
 * In the first case, where there is only one speech region, the
 * first non-speech region will be removed, and the SPEECH_START
 * Signal will be removed. The ending SPEECH_END and non-speech
 * region will be removed as well.
 *
 * <p>The second case is a little more complicated. If the SphinxProperty
 * <pre>
 * edu.cmu.sphinx.frontend.NonSpeechFilter.mergeSpeechSegments </pre>
 * is set to true (the default),
 * all the Cepstra from the first SPEECH_START to the last SPEECH_END
 * will be considered as one Utterance, and enclosed by a pair of
 * UTTERANCE_START and UTTERANCE_END. The first and last non-speech
 * regions, as well as all SPEECH_START and SPEECH_END,
 * will obviously be removed. This gives:
 * <pre>
 * UTTERANCE_START
 * (speech Cepstra) (non-speech Cepstra)
 * (speech Cepstra) (non-speech Cepstra)
 * ...
 * UTTERANCE_END
 * </pre>
 *
 * <p>On the other hand, if <code>mergeSpeechSegments</code> is set to
 * false, then each:
 * <pre>
 * SPEECH_START (speech Cepstra) SPEECH_END (non-speech Cepstra)
 * </pre>
 * will become:
 * <pre>
 * UTTERANCE_START (speech Cepstra) UTTERANCE_END
 * </pre>
 * that is, the SPEECH_START replaced by UTTERANCE_START, 
 * the SPEECH_END replaced by UTTERANCE_END, and the non-speech
 * region removed. Also, the first UTTERANCE_START and last
 * UTTERANCE_END in the original stream will be removed as well.
 * This will give:
 * <pre>
 * UTTERANCE_START (speech Cepstra) UTTERANCE_END
 * UTTERANCE_START (speech Cepstra) UTTERANCE_END
 * ...
 * </pre>
 */
public class NonSpeechFilter extends DataProcessor implements CepstrumSource {

    private static final String PROP_PREFIX = 
    FrontEnd.PROP_PREFIX + "NonSpeechFilter.";

    /**
     * Controls whether to merge multiple speech segments within an
     * Utterance to one big speech segment, with the boundaries being
     * the start of the first speech segment, and the end of the
     * last speech segment.
     */
    private boolean mergeSpeechSegments;

    private CepstrumSource predecessor;
    private List inputBuffer;


    /**
     * Constructs an NonSpeechFilter with the given name, context,
     * and CepstrumSource predecessor.
     *
     * @param name the name of this NonSpeechFilter
     * @param context the context of the SphinxProperties this
     *    NonSpeechFilter uses
     * @param predecessor the CepstrumSource where this NonSpeechFilter
     *    gets Cepstrum from
     */
    public NonSpeechFilter(String name, String context,
                           CepstrumSource predecessor) throws IOException {
        super(name, context);
        initSphinxProperties();
        this.predecessor = predecessor;
        this.inputBuffer = new LinkedList();
    }


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     *
     * @throws IOException if there is an error reading the parameters
     */
    private void initSphinxProperties() throws IOException {
        mergeSpeechSegments = getSphinxProperties().getBoolean
            (PROP_PREFIX + "mergeSpeechSegments", true);
    }

    
    /**
     * Sets the predeces
     */
    public void setPredecessor(CepstrumSource predecessor) {
        this.predecessor = predecessor;
    }


    /**
     * Returns the next Cepstrum, which can be either Cepstrum with
     * data, or Cepstrum with an UTTERANCE_START or UTTERANCE_END.
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

            if (cepstrum.hasSignal(Signal.UTTERANCE_START)) {
                Cepstrum utteranceStart = cepstrum;

                // Read (and discard) all the Cepstrum from UTTERANCE_START
                // until we hit a SPEECH_START. The SPEECH_START is discarded.
                readUntil(Signal.SPEECH_START);
                cepstrum = utteranceStart;
                
            } else if (cepstrum.getSignal().equals(Signal.SPEECH_END)) {

                if (mergeSpeechSegments) {
                    // read (and discard) all the Cepstrum from SPEECH_END
                    // until we hit a UTTERANCE_END
                    List cepstrumList = readUntil(Signal.SPEECH_START,
                                                  Signal.UTTERANCE_END);
                    
                    Cepstrum last = (Cepstrum) cepstrumList.get
                        (cepstrumList.size() - 1);
                    
                    if (last != null) {
                        if (last.hasSignal(Signal.SPEECH_START)) {
                            // first remove the SPEECH_START, then add
                            // all the Cepstra to the inputBuffer
                            cepstrumList.remove(last);
                            inputBuffer.addAll(cepstrumList);
                            cepstrum = readCepstrum();
                            
                        } else if (last.hasSignal(Signal.UTTERANCE_END)) {
                            cepstrum = last;
                        }
                    }
                } else {
                    // instead of a SPEECH_END, return an UTTERANCE_END
                    cepstrum = new Cepstrum(Signal.UTTERANCE_END);
                    
                    // then read and discard everything up until
                    // a SPEECH_START or UTTERANCE_END
                    List cepstrumList = readUntil(Signal.SPEECH_START,
                                                  Signal.UTTERANCE_END);
                    Cepstrum last = (Cepstrum) cepstrumList.get
                        (cepstrumList.size() - 1);

                    if (last != null) {
                        // if it hit a SPEECH_START, put it back to the
                        // inputBuffer, so that it will be handled by the
                        // next call to getCepstrum()
                        if (last.hasSignal(Signal.SPEECH_START)) {
                            inputBuffer.add(last);
                        }
                    }
                }
            }
        }

        getTimer().stop();

        return cepstrum;
    }


    /**
     * Returns the next Cepstrum, either from the inputBuffer or the
     * predecessor.
     *
     * @return the next available Cepstrum
     */
    private Cepstrum readCepstrum() throws IOException {
        if (inputBuffer.size() > 0) {
            return (Cepstrum) inputBuffer.remove(0);
        } else {
            return predecessor.getCepstrum();
        }
    }


    /**
     * Remove from the end of this Queue all the Cepstrum,
     * until we hit a Cepstrum with the given Signal, which will
     * also be removed.
     *
     * @param signal where we should stop removing
     *
     * @return a list of all Cepstrum removed, including the Signal
     */
    private List readUntil(Signal signal) throws IOException {
        return readUntil(signal, null);
    }


    /**
     * Read until we hit a Cepstrum of the two given Signal types.
     *
     * @param signal1 the first Signal type
     * @param signal2 the second Signal type
     *
     * @return a list of all the Cepstrum read,
     *    including the last Cepstrum with the Signal
     */
    private List readUntil(Signal signal1, Signal signal2) throws
    IOException {
        List cepstrumList = new LinkedList();
        Cepstrum cepstrum = null;
        do {
            cepstrum = readCepstrum();
            if (cepstrum != null) {
                cepstrumList.add(cepstrum);
            }
        } while (cepstrum != null &&
                 !cepstrum.hasSignal(signal1) &&
                 !cepstrum.hasSignal(signal2));
        return cepstrumList;
    }
}
