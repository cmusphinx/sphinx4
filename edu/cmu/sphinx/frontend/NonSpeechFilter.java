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
        
        Cepstrum cepstrum = predecessor.getCepstrum();

        getTimer().start();

        if (cepstrum != null) {
            if (cepstrum.getSignal().equals(Signal.UTTERANCE_START)) {
                Cepstrum utteranceStart = cepstrum;
                // remove all the Cepstrum from UTTERANCE_START
                // until we hit a SPEECH_START
                readUtil(Signal.SPEECH_START);
                cepstrum = utteranceStart;
                
            } else if (cepstrum.getSignal().equals(Signal.SPEECH_END)) {
                // remove all the Cepstrum from SPEECH_END
                // until we hit a UTTERANCE_END
                cepstrum = readUtil(Signal.UTTERANCE_END);
            }
        }

        getTimer().stop();

        return cepstrum;
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
    private Cepstrum readUtil(Signal signal) throws IOException {
        Cepstrum cepstrum = null;
        do {
            cepstrum = predecessor.getCepstrum();
        } while (cepstrum != null && !cepstrum.getSignal().equals(signal));
        return cepstrum;
    }
}
