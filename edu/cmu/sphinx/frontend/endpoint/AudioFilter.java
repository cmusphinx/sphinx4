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

import edu.cmu.sphinx.frontend.Audio;
import edu.cmu.sphinx.frontend.AudioSource;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.Signal;

import edu.cmu.sphinx.util.SphinxProperties;

import java.io.IOException;

import java.util.*;


/**
 * Given a sequence of Audio, filters out the non-speech regions.
 * The sequence of Audio should have the speech and non-speech regions
 * marked out by the Signal.SPEECH_START and Signal.SPEECH_END, using
 * and endpointer.
 *
 * A sequence of Audio for an Utterance should look like:
 *
 * <pre>
 * UTTERANCE_START (non-speech Audio)
 * SPEECH_START (speech Audio) SPEECH_END (non-speech Audio)
 * UTTERANCE_END
 * </pre>
 * or
 * <pre>
 * UTTERANCE_START (non-speech Audio)
 * SPEECH_START (speech Audio) SPEECH_END (non-speech Audio)
 * SPEECH_START (speech Audio) SPEECH_END (non-speech Audio)
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
 * edu.cmu.sphinx.frontend.AudioFilter.mergeSpeechSegments </pre>
 * is set to true (the default),
 * all the Audio from the first SPEECH_START to the last SPEECH_END
 * will be considered as one Utterance, and enclosed by a pair of
 * UTTERANCE_START and UTTERANCE_END. The first and last non-speech
 * regions, as well as all SPEECH_START and SPEECH_END,
 * will obviously be removed. This gives:
 * <pre>
 * UTTERANCE_START
 * (speech Audio) (non-speech Audio)
 * (speech Audio) (non-speech Audio)
 * ...
 * UTTERANCE_END
 * </pre>
 *
 * <p>On the other hand, if <code>mergeSpeechSegments</code> is set to
 * false, then each:
 * <pre>
 * SPEECH_START (speech Audio) SPEECH_END (non-speech Audio)
 * </pre>
 * will become:
 * <pre>
 * UTTERANCE_START (speech Audio) UTTERANCE_END
 * </pre>
 * that is, the SPEECH_START replaced by UTTERANCE_START, 
 * the SPEECH_END replaced by UTTERANCE_END, and the non-speech
 * region removed. Also, the first UTTERANCE_START and last
 * UTTERANCE_END in the original stream will be removed as well.
 * This will give:
 * <pre>
 * UTTERANCE_START (speech Audio) UTTERANCE_END
 * UTTERANCE_START (speech Audio) UTTERANCE_END
 * ...
 * </pre>
 */
public class AudioFilter extends DataProcessor implements AudioSource {

    private static final String PROP_PREFIX 
        = "edu.cmu.sphinx.frontend.endpoint.AudioFilter.";


    /**
     * The SphinxProperty that controls whether to merge discontiguous
     * speech segments in an utterance.
     */
    public static final String PROP_MERGE_SPEECH_SEGMENTS
        = PROP_PREFIX + "mergeSpeechSegments";


    /**
     * The default value for PROP_MERGE_SPEECH_SEGMENTS.
     */
    public static final boolean PROP_MERGE_SPEECH_SEGMENTS_DEFAULT = false;


    /**
     * Controls whether to merge multiple speech segments within an
     * Utterance to one big speech segment, with the boundaries being
     * the start of the first speech segment, and the end of the
     * last speech segment.
     */
    private boolean mergeSpeechSegments;
    private boolean discardMode;
    private boolean inSpeech;

    private AudioSource predecessor;
    private List inputBuffer;
    private List outputQueue;


    /**
     * Constructs an AudioFilter with the given name, context,
     * and AudioSource predecessor.
     *
     * @param name the name of this AudioFilter
     * @param context the context of the SphinxProperties this
     *    AudioFilter uses
     * @param props the SphinxProperties to read properties from
     * @param predecessor the AudioSource where this AudioFilter
     *    gets Audio from
     */
    public AudioFilter(String name, String context, SphinxProperties props,
                       AudioSource predecessor) throws IOException {
        super(name, context, props);
	this.mergeSpeechSegments = getSphinxProperties().getBoolean
	    (PROP_MERGE_SPEECH_SEGMENTS, PROP_MERGE_SPEECH_SEGMENTS_DEFAULT);
        this.discardMode = true;
        this.inSpeech = false;
        this.predecessor = predecessor;
        this.inputBuffer = new LinkedList();
        this.outputQueue = new LinkedList();
    }


    /**
     * Sets the predecessor.
     *
     * @param predecessor the predecessor
     */
    public void setPredecessor(AudioSource predecessor) {
        this.predecessor = predecessor;
    }


    /**
     * Prints out a message to System.out.
     */
    private void message(String message) {
        System.out.println("AudioFilter: " + message);
    }


    /**
     * Returns the next Audio, which can be either Audio with
     * data, or Audio with an UTTERANCE_START or UTTERANCE_END.
     *
     * @return the next Audio, or null if no Audio is available
     *
     * @throws java.io.IOException if there is error reading the
     *    Audio object
     *
     * @see Audio
     */
    public Audio getAudio() throws IOException {

        if (outputQueue.size() == 0) {
            Audio audio = readAudio();
            
            getTimer().start();
            
            if (audio != null) {
                if (!mergeSpeechSegments) {
                    audio = handleNonMergingAudio(audio);
                } else {
                    audio = handleMergingAudio(audio);
                }
            }
            outputQueue.add(audio);
            
            getTimer().stop();
        }

        if (outputQueue.size() > 0) {
            return (Audio) outputQueue.remove(0);
        } else {
            return null;
        }
    }


    /**
     * Handles the given Audio in the case when mergeSpeechSegment
     * is true.
     */
    private Audio handleMergingAudio(Audio audio) throws IOException {
        Audio next = audio;

        if (audio.hasSignal(Signal.UTTERANCE_START)) {
            
            // Read (and discard) all the Audio from UTTERANCE_START
            // until we hit a SPEECH_START. The SPEECH_START is discarded.
            List audioList = readUntil(Signal.SPEECH_START,
                                       Signal.UTTERANCE_END);
            Audio last = (Audio) audioList.get(audioList.size() - 1);
            if (last != null) {
                if (last.hasSignal(Signal.UTTERANCE_END)) {
                    outputQueue.add(audio);
                    next = last;
                }
            }
        } else if (audio.getSignal().equals(Signal.SPEECH_END)) {
            // read (and discard) all the Audio from SPEECH_END
            // until we hit a UTTERANCE_END
            List audioList = readUntil(Signal.SPEECH_START,
                                       Signal.UTTERANCE_END);
            Audio last = (Audio) audioList.get(audioList.size() - 1);
            if (last != null) {
                if (last.hasSignal(Signal.SPEECH_START)) {
                    // first remove the SPEECH_START, then add
                    // all the Audio to the inputBuffer
                    
                    audioList.remove(last);
                    inputBuffer.addAll(audioList);                    
                    next = readAudio();
                        
                } else if (last.hasSignal(Signal.UTTERANCE_END)) {
                    // System.out.println("Last is UTTERANCE_END");
                    next = last;
                }
            }
        }
        return next;
    }


    /**
     * Handles the given Audio in the case when mergeSpeechSegment
     * is false.
     */
    private Audio handleNonMergingAudio(Audio audio) throws
    IOException {
        Audio next = audio;
        if (audio != null) {
            if (audio.hasSignal(Signal.SPEECH_START)) {
                if (inSpeech) {
                    // Normally, we should not be encounter a SPEECH_START
                    // if we are inSpeech. This is error-handling code.
                    message("ALERT: getting a SPEECH_START while "+
                            "in speech, removing it.");
                    do {
                        next = readAudio();
                    } while (next != null &&
                             next.hasSignal(Signal.SPEECH_START));
                    if (next != null) {
                        next = handleNonMergingAudio(next);
                    }
                } else {
                    // if we hit a SPEECH_START, we will stop discarding
                    // Audio, and return an UTTERANCE_START instead
                    inSpeech = true;
                    discardMode = false;
                    next = new Audio
                        (Signal.UTTERANCE_START,
                         audio.getCollectTime(),
                         audio.getFirstSampleNumber());
                }
            } else if (audio.hasSignal(Signal.SPEECH_END)) {
                if (!inSpeech) {
                    // Normally, we should not get a SPEECH_END
                    // if we are not inSpeech. This is error-handling code.
                    message("ALERT: getting a SPEECH_END while "+
                            "not in speech, removing it.");
                    do {
                        next = readAudio();
                    } while (next != null &&
                             next.hasSignal(Signal.SPEECH_END));
                    if (next != null) {
                        next = handleNonMergingAudio(next);
                    }
                } else {
                    // if we hit a SPEECH_END, we will start
                    // discarding Audio, and return an UTTERANCE_END instead
                    inSpeech = false;
                    discardMode = true;
                    next = new Audio
                        (Signal.UTTERANCE_END,
                         audio.getCollectTime(),
                         audio.getFirstSampleNumber());
                }
            } else if (discardMode) {
                while (next != null && 
                       next.getSignal() != Signal.SPEECH_START &&
                       next.getSignal() != Signal.SPEECH_END) {
                    next = readAudio();
                }
                next = handleNonMergingAudio(next);
            }
        }
        return next;
    }


    /**
     * Returns the next Audio, either from the inputBuffer or the
     * predecessor.
     *
     * @return the next available Audio
     */
    private Audio readAudio() throws IOException {
        Audio audio = null;
        if (inputBuffer.size() > 0) {
            audio = (Audio) inputBuffer.remove(0);
        } else {
            audio = predecessor.getAudio();
            if (audio != null) {
                String speech = "";
                if (audio.isSpeech()) { 
                    speech = " *";
                }
                // message("incoming: " + audio.getSignal().toString() + speech);
            }
        }
        return audio;
    }


    /**
     * Remove from the end of this Queue all the Audio,
     * until we hit a Audio with the given Signal, which will
     * also be removed.
     *
     * @param signal where we should stop removing
     *
     * @return a list of all Audio removed, including the Signal
     */
    private List readUntil(Signal signal) throws IOException {
        return readUntil(signal, null);
    }


    /**
     * Read until we hit a Audio of the two given Signal types.
     *
     * @param signal1 the first Signal type
     * @param signal2 the second Signal type
     *
     * @return a list of all the Audio read,
     *    including the last Audio with the Signal
     */
    private List readUntil(Signal signal1, Signal signal2) throws
    IOException {
        List audioList = new LinkedList();
        Audio audio = null;
        do {
            audio = readAudio();
            if (audio != null) {
                audioList.add(audio);
            }
        } while (audio != null &&
                 !audio.hasSignal(signal1) &&
                 !audio.hasSignal(signal2));
        return audioList;
    }
}
