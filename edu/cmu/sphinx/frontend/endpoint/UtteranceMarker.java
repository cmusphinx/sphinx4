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
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.Signal;

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.LogMath;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;


/**
 * Converts a stream of Audio objects, marked as speech and non-speech,
 * into utterances. This is done by inserting UTTERANCE_START and
 * UTTERANCE_END signals into the stream.
 */
public class UtteranceMarker extends DataProcessor implements AudioSource {

    public static final String PROP_PREFIX = 
        "edu.cmu.sphinx.frontend.endpoint.UtteranceMarker.";

    /**
     * The SphinxProperty for the minimum amount of time in speech
     * (in milliseconds) to be considered as utterance start.
     */
    public static final String PROP_START_SPEECH = 
        PROP_PREFIX + "startSpeech";

    /**
     * The default value of PROP_START_SPEECH.
     */
    public static final int PROP_START_SPEECH_DEFAULT = 0;

    /**
     * The SphinxProperty for the amount of time in silence
     * (in milliseconds) to be considered as utterance end.
     */
    public static final String PROP_END_SILENCE = PROP_PREFIX + "endSilence";

    /**
     * The default value of PROP_END_SILENCE.
     */
    public static final int PROP_END_SILENCE_DEFAULT = 0;

    /**
     * The SphinxProperty for the amount of time (in milliseconds)
     * before speech start to be included as speech data.
     */
    public static final String PROP_SPEECH_LEADER = 
        PROP_PREFIX + "speechLeader";

    /**
     * The default value of PROP_SPEECH_LEADER.
     */
    public static final int PROP_SPEECH_LEADER_DEFAULT = 200;

    /**
     * The SphinxProperty for the amount of time (in milliseconds)
     * after speech ends to be included as speech data.
     */
    public static final String PROP_SPEECH_TRAILER = 
        PROP_PREFIX + "speechTrailer";

    /**
     * The default value of PROP_SPEECH_TRAILER.
     */
    public static final int PROP_SPEECH_TRAILER_DEFAULT = 100;


    private AudioSource predecessor;
    private List outputQueue;
    private boolean inUtterance;
    private int startSpeechTime;
    private int endSilenceTime;
    private int speechLeader;
    private int speechTrailer;
    private int sampleRate;


    /**
     * Initializes this UtteranceMarker with the given name, context,
     * and AudioSource predecessor.
     *
     * @param name the name of this UtteranceMarker
     * @param context the context of the SphinxProperties this
     *    UtteranceMarker uses
     * @param props the SphinxProperties to read properties from
     * @param predecessor the AudioSource where this UtteranceMarker
     *    gets Cepstrum from
     *
     * @throws java.io.IOException
     */
    public void initialize(String name, String context, 
                           SphinxProperties props,
                           AudioSource predecessor) throws IOException {
        super.initialize(name, context, props);
        this.predecessor = predecessor;
        this.outputQueue = new ArrayList();
        setProperties();
    }

    /**
     * Sets the properties for this UtteranceMarker.
     */
    private void setProperties() {
        SphinxProperties props = getSphinxProperties();
        startSpeechTime = 
            props.getInt(PROP_START_SPEECH, PROP_START_SPEECH_DEFAULT);
        endSilenceTime = 
            props.getInt(PROP_END_SILENCE, PROP_END_SILENCE_DEFAULT);
        speechLeader =
            props.getInt(PROP_SPEECH_LEADER, PROP_SPEECH_LEADER_DEFAULT);
        speechTrailer =
            props.getInt(PROP_SPEECH_TRAILER, PROP_SPEECH_TRAILER_DEFAULT);
        sampleRate =
            props.getInt(FrontEnd.PROP_SAMPLE_RATE,
                         FrontEnd.PROP_SAMPLE_RATE_DEFAULT);
    }

    /**
     * Resets this UtteranceMarker to a starting state.
     */
    private void reset() {
        inUtterance = false;
    }

    /**
     * Returns the next Audio object.
     *
     * @return the next Audio object, or null if none available
     *
     * @throws java.io.IOException if an error occurred
     *
     * @see Audio
     */
    public Audio getAudio() throws IOException {
        if (outputQueue.size() == 0) {
            if (!inUtterance) {
                readInitialFrames();
            } else {
                Audio audio = predecessor.getAudio();
                outputQueue.add(audio);
                if (!audio.isSpeech()) {
                    inUtterance = !(readEndFrames(audio));
                }
            }
        }
        if (outputQueue.size() > 0) {
            Audio audio = (Audio) outputQueue.remove(0);
            return audio;
        } else {
            return null;
        }
    }

    /**
     * Returns the amount of audio data in milliseconds in the 
     * given Audio object.
     *
     * @param audio the Audio object
     *
     * @return the amount of audio data in milliseconds
     */
    public int getAudioTime(Audio audio) {
        return (int) (audio.getSamples().length * 1000.0f / sampleRate);
    }
        
    /**
     * Read the starting frames until the utterance has started.
     */
    private void readInitialFrames() throws IOException {
        while (!inUtterance) {
            Audio audio = predecessor.getAudio();
            outputQueue.add(audio);
            if (audio.isSpeech()) {
                boolean speechStarted = handleFirstSpeech(audio);
                if (speechStarted) {
                    addUtteranceStart();
                    inUtterance = true;
                    break;
                }
            } else {
                outputQueue.add(audio);
            }
        }
    }

    /**
     * Handles an Audio object that can possibly be the first in
     * an utterance.
     *
     * @param audio the Audio to handle
     *
     * @return true if utterance/speech has started for real, false otherwise
     */
    private boolean handleFirstSpeech(Audio audio) throws IOException {
        int speechTime = getAudioTime(audio);
        while (speechTime < startSpeechTime) {
            Audio next = predecessor.getAudio();
            outputQueue.add(next);
            if (!next.isSpeech()) {
                return false;
            } else {
                speechTime += getAudioTime(audio);
            }
        }
        return true;
    }

    /**
     * Backtrack from the current position to add an UTTERANCE_START Signal
     * to the outputQueue.
     */
    private void addUtteranceStart() {
        int silenceLength = 0;
        ListIterator i = outputQueue.listIterator(outputQueue.size()-1);

        // backtrack until we have 'speechLeader' amount of non-speech
        while (silenceLength < speechLeader && i.hasPrevious()) {
            Audio current = (Audio) i.previous();
            if (current.hasContent()) {
                if (current.isSpeech()) {
                    silenceLength = 0;
                } else {
                    silenceLength += getAudioTime(current);
                }
            } else if (current.hasSignal(Signal.UTTERANCE_START)) {
                throw new Error("Too many UTTERANCE_START");
            } else if (current.hasSignal(Signal.UTTERANCE_END)) {
                i.next(); // skip the UTTERANCE_END
                break;
            }
        }

        // add the UTTERANCE_START
        i.add(new Audio(Signal.UTTERANCE_START));
    }

    /**
     * Given a non-speech frame, try to read more non-speech frames
     * until we think its the end of utterance.
     *
     * @param audio a non-speech frame
     *
     * @return true if speech has really ended, false if speech
     *    has not ended
     */
    private boolean readEndFrames(Audio audio) throws IOException {
        ListIterator i = outputQueue.listIterator(outputQueue.size()-1);

        int silenceLength = getAudioTime(audio);

        // read ahead until we have 'endSilenceTime' amount of silence
        while (silenceLength < endSilenceTime) {
            Audio next = predecessor.getAudio();
            outputQueue.add(next);
            if (next.isSpeech()) {
                // if speech is detected again, we're still in
                // an utterance
                return false;
            } else {
                silenceLength += getAudioTime(next);
            }
        }

        boolean utteranceEndAdded = false;

        // read ahead util we have 'speechTrailer' amount of silence
        while (silenceLength < speechTrailer) {
            Audio next = predecessor.getAudio();
            if (next.isSpeech()) {
                outputQueue.add(new Audio(Signal.UTTERANCE_END));
                outputQueue.add(next);
                utteranceEndAdded = true;
                break;
            } else {
                silenceLength += getAudioTime(next);
                outputQueue.add(next);
            }
        }

        if (!utteranceEndAdded) {
            // iterator from the end of speech to add UTTERANCE_END
            silenceLength = 0;
            while (silenceLength < speechTrailer && i.hasNext()) {
                Audio next = (Audio) i.next();
                assert !next.isSpeech();
                silenceLength += getAudioTime(next);
            }
            outputQueue.add(new Audio(Signal.UTTERANCE_END));
        }

        return true;
    }

}
