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
 * <UTTERANCE_START> (non-speech Cepstra) <SPEECH_START> (speech Cepstra)
 * <SPEECH_END> (non-speech Cepstra) <UTTERANCE_END>
 * </pre>
 */
public class NonSpeechFilter extends DataProcessor implements CepstrumSource {

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
        this.predecessor = predecessor;
        this.inputBuffer = new LinkedList();
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
            if (cepstrum.getSignal().equals(Signal.UTTERANCE_START)) {
                Cepstrum utteranceStart = cepstrum;

                // Read (and discard) all the Cepstrum from UTTERANCE_START
                // until we hit a SPEECH_START. The SPEECH_START is discarded.
                readUntil(Signal.SPEECH_START);
                cepstrum = utteranceStart;
                
            } else if (cepstrum.getSignal().equals(Signal.SPEECH_END)) {

                // read (and discard) all the Cepstrum from SPEECH_END
                // until we hit a UTTERANCE_END
                List cepstrumList = readUntil(Signal.SPEECH_START,
                                              Signal.UTTERANCE_END);

                Cepstrum lastCepstrum = (Cepstrum) cepstrumList.get
                    (cepstrumList.size() - 1);

                if (lastCepstrum != null) {
                    if (lastCepstrum.getSignal().equals(Signal.SPEECH_START)) {
                        // first remove the SPEECH_START, then add
                        // all the Cepstra to the inputBuffer
                        cepstrumList.remove(lastCepstrum);
                        inputBuffer.addAll(cepstrumList);
                        cepstrum = readCepstrum();

                    } else if (lastCepstrum.getSignal().equals
                               (Signal.UTTERANCE_END)) {
                        cepstrum = lastCepstrum;
                    }
                }
            }
        }

        getTimer().stop();

        return cepstrum;
    }


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
     * @return the last Cepstrum removed
     */
    private Cepstrum readUntil(Signal signal) throws IOException {
        Cepstrum cepstrum = null;
        do {
            cepstrum = readCepstrum();
        } while (cepstrum != null && !cepstrum.getSignal().equals(signal));
        return cepstrum;
    }

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
                 !cepstrum.getSignal().equals(signal1) &&
                 !cepstrum.getSignal().equals(signal2));
        return cepstrumList;
    }
}
