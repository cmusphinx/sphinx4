/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electronic Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */


package edu.cmu.sphinx.frontend.endpoint;

import edu.cmu.sphinx.frontend.Cepstrum;
import edu.cmu.sphinx.frontend.CepstrumSource;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.Signal;

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

    private static final String PROP_PREFIX 
        = "edu.cmu.sphinx.frontend.endpoint.NonSpeechFilter.";


    /**
     * The SphinxProperty that controls whether to merge discontiguous
     * speech segments in an utterance.
     */
    public static final String PROP_MERGE_SPEECH_SEGMENTS
        = PROP_PREFIX + "mergeSpeechSegments";


    /**
     * The default value for PROP_MERGE_SPEECH_SEGMENTS.
     */
    public static final boolean PROP_MERGE_SPEECH_SEGMENTS_DEFAULT = true;


    /**
     * Controls whether to merge multiple speech segments within an
     * Utterance to one big speech segment, with the boundaries being
     * the start of the first speech segment, and the end of the
     * last speech segment.
     */
    private boolean mergeSpeechSegments;
    private boolean discardMode;

    private CepstrumSource predecessor;
    private List inputBuffer;


    /**
     * Constructs an NonSpeechFilter with the given name, context,
     * and CepstrumSource predecessor.
     *
     * @param name the name of this NonSpeechFilter
     * @param context the context of the SphinxProperties this
     *    NonSpeechFilter uses
     * @param props the SphinxProperties to read properties from
     * @param predecessor the CepstrumSource where this NonSpeechFilter
     *    gets Cepstrum from
     */
    public NonSpeechFilter(String name, String context, SphinxProperties props,
                           CepstrumSource predecessor) throws IOException {
        super(name, context, props);
	this.mergeSpeechSegments = getSphinxProperties().getBoolean
	    (PROP_MERGE_SPEECH_SEGMENTS, PROP_MERGE_SPEECH_SEGMENTS_DEFAULT);
        this.discardMode = true;
        this.predecessor = predecessor;
        this.inputBuffer = new LinkedList();
    }


    /**
     * Sets the predecessor.
     *
     * @param predecessor the predecessor
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
            if (!mergeSpeechSegments) {
                cepstrum = handleNonMergingCepstrum(cepstrum);
            } else {
                cepstrum = handleMergingCepstrum(cepstrum);
            }
        }

        getTimer().stop();

        /*
        if (cepstrum != null) {
            if (cepstrum.hasSignal(Signal.UTTERANCE_START)) {
                System.out.println("NSF: UTTERANCE_START");
            } else if (cepstrum.hasSignal(Signal.UTTERANCE_END)) {
                System.out.println("NSF: UTTERANCE_END");
            }            
        }
        */

        return cepstrum;
    }


    /**
     * Handles the given Cepstrum in the case when mergeSpeechSegment
     * is true.
     */
    private Cepstrum handleMergingCepstrum(Cepstrum cepstrum) throws
    IOException {
        Cepstrum next = cepstrum;

        if (cepstrum.hasSignal(Signal.UTTERANCE_START)) {
            // Read (and discard) all the Cepstrum from UTTERANCE_START
            // until we hit a SPEECH_START. The SPEECH_START is discarded.
            readUntil(Signal.SPEECH_START);
        } else if (cepstrum.getSignal().equals(Signal.SPEECH_END)) {
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
                    next = readCepstrum();
                        
                } else if (last.hasSignal(Signal.UTTERANCE_END)) {
                    next = last;
                }
            }
        }
        return next;
    }


    /**
     * Handles the given Cepstrum in the case when mergeSpeechSegment
     * is false.
     */
    private Cepstrum handleNonMergingCepstrum(Cepstrum cepstrum) throws
    IOException {
        Cepstrum next = cepstrum;
        if (cepstrum != null) {
            if (cepstrum.hasSignal(Signal.SPEECH_START)) {
                // if we hit a SPEECH_START, we will stop discarding
                // Cepstrum, and return an UTTERANCE_START instead
                discardMode = false;
                next = new Cepstrum(Signal.UTTERANCE_START);
            } else if (cepstrum.hasSignal(Signal.SPEECH_END)) {
                // if we hit a SPEECH_END, we will start
                // discarding Cepstrum, and return an UTTERANCE_END instead
                discardMode = true;
                next = new Cepstrum(Signal.UTTERANCE_END);
            } else if (discardMode) {
                next = handleNonMergingCepstrum(readCepstrum());
            }
        }
        return next;
    }


    /**
     * Returns the next Cepstrum, either from the inputBuffer or the
     * predecessor.
     *
     * @return the next available Cepstrum
     */
    private Cepstrum readCepstrum() throws IOException {
        Cepstrum cepstrum = null;
        if (inputBuffer.size() > 0) {
            cepstrum = (Cepstrum) inputBuffer.remove(0);
        } else {
            cepstrum = predecessor.getCepstrum();
        }
        /*
        if (cepstrum != null && cepstrum.getSignal() != null) {
            System.out.println("NSF: incoming: " + 
                               cepstrum.getSignal().toString());
        }
        */
        return cepstrum;
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
