/*
 * Copyright 1999-2002 Carnegie Mellon University.
 * Portions Copyright 2002 Sun Microsystems, Inc.
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.frontend.endpoint;

import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Integer;

import java.util.LinkedList;

/**
 * Converts a stream of SpeechClassifiedData objects, marked as speech and
 * non-speech, and mark out the regions that are considered speech. This is done
 * by inserting SPEECH_START and SPEECH_END signals into the stream.
 * <p>
 * The algorithm for inserting the two signals is as follows.
 * <p>
 * The algorithm is always in one of two states: 'in-speech' and
 * 'out-of-speech'. If 'out-of-speech', it will read in audio until we hit audio
 * that is speech. If we have read more than 'startSpeech' amount of
 * <i>continuous</i> speech, we consider that speech has started, and insert a
 * SPEECH_START at 'speechLeader' time before speech first started. The state of
 * the algorithm changes to 'in-speech'.
 * <p>
 * Now consider the case when the algorithm is in 'in-speech' state. If it read
 * an audio that is speech, it is scheduled for output. If the audio is
 * non-speech, we read ahead until we have 'endSilence' amount of
 * <i>continuous</i> non-speech. At the point we consider that speech has ended.
 * A SPEECH_END signal is inserted at 'speechTrailer' time after the first
 * non-speech audio. The algorithm returns to 'out-of-speech' state. If any
 * speech audio is encountered in-between, the accounting starts all over again.
 * 
 * While speech audio is processed delay is lowered to some minimal amount. This
 * helps to segment both slow speech with visible delays and fast speech when
 * delays are minimal.
 */
public class SpeechMarker extends BaseDataProcessor {

    /**
     * The property for the minimum amount of time in speech (in milliseconds)
     * to be considered as utterance start.
     */
    @S4Integer(defaultValue = 200)
    public static final String PROP_START_SPEECH = "startSpeech";
    private int startSpeechTime;

    /**
     * The property for the amount of time in silence (in milliseconds) to be
     * considered as utterance end.
     */
    @S4Integer(defaultValue = 200)
    public static final String PROP_END_SILENCE = "endSilence";
    private int endSilenceTime;

    /**
     * The property for the amount of time (in milliseconds) before speech start
     * to be included as speech data.
     */
    @S4Integer(defaultValue = 50)
    public static final String PROP_SPEECH_LEADER = "speechLeader";
    private int speechLeader;

    private LinkedList<Data> inputQueue; // Audio objects are added to the end
    private LinkedList<Data> outputQueue; // Audio objects are added to the end
    private boolean inSpeech;
    private int speechCount;
    private int silenceCount;
    private int startSpeechFrames;
    private int endSilenceFrames;
    private int speechLeaderFrames;

    public SpeechMarker(int startSpeechTime, int endSilenceTime, int speechLeader) {
        initLogger();
        this.startSpeechTime = startSpeechTime;
        this.speechLeader = speechLeader;
        this.endSilenceTime = endSilenceTime;
    }

    public SpeechMarker() {
    }

    @Override
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);

        startSpeechTime = ps.getInt(PROP_START_SPEECH);
        endSilenceTime = ps.getInt(PROP_END_SILENCE);
        speechLeader = ps.getInt(PROP_SPEECH_LEADER);
    }

    /**
     * Initializes this SpeechMarker
     */
    @Override
    public void initialize() {
        super.initialize();
        reset();
    }

    /**
     * Resets this SpeechMarker to a starting state.
     */
    private void reset() {
        inSpeech = false;
        speechCount = 0;
        silenceCount = 0;
        startSpeechFrames = startSpeechTime / 10;
        endSilenceFrames = endSilenceTime / 10;
        speechLeaderFrames = speechLeader / 10;
        this.inputQueue = new LinkedList<Data>();
        this.outputQueue = new LinkedList<Data>();
    }

    /**
     * Returns the next Data object.
     * 
     * @return the next Data object, or null if none available
     * @throws DataProcessingException
     *             if a data processing error occurs
     */
    @Override
    public Data getData() throws DataProcessingException {

        while (outputQueue.isEmpty()) {
            Data data = getPredecessor().getData();

            if (data == null)
                break;

            if (data instanceof DataStartSignal) {
                reset();
                outputQueue.add(data);
                break;
            }

            if (data instanceof DataEndSignal) {
                if (inSpeech) {
                    outputQueue.add(new SpeechEndSignal());
                }
                outputQueue.add(data);
                break;
            }

            if (data instanceof SpeechClassifiedData) {
                SpeechClassifiedData cdata = (SpeechClassifiedData) data;

                if (cdata.isSpeech()) {
                    speechCount++;
                    silenceCount = 0;
                } else {
                    speechCount = 0;
                    silenceCount++;
                }

                if (inSpeech) {
                    outputQueue.add(data);
                } else {
                    inputQueue.add(data);
                    if (inputQueue.size() > startSpeechFrames + speechLeaderFrames) {
                        inputQueue.remove(0);
                    }
                }

                if (!inSpeech && speechCount == startSpeechFrames) {
                    inSpeech = true;
                    outputQueue.add(new SpeechStartSignal(cdata.getCollectTime() - speechLeader - startSpeechFrames));
                    outputQueue.addAll(inputQueue.subList(
                            Math.max(0, inputQueue.size() - startSpeechFrames - speechLeaderFrames), inputQueue.size()));
                    inputQueue.clear();
                }
                if (inSpeech && silenceCount == endSilenceFrames) {
                    inSpeech = false;
                    outputQueue.add(new SpeechEndSignal(cdata.getCollectTime()));
                }
            }
        }

        // If we have something left, return that
        if (!outputQueue.isEmpty()) {
            Data audio = outputQueue.remove(0);
            if (audio instanceof SpeechClassifiedData) {
                SpeechClassifiedData data = (SpeechClassifiedData) audio;
                audio = data.getDoubleData();
            }
            return audio;
        } else {
            return null;
        }

    }

    public boolean inSpeech() {
        return inSpeech;
    }
}
