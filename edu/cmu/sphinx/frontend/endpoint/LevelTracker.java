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

import java.util.LinkedList;
import java.util.List;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.SphinxProperties;


/**
 * Implements a level tracking endpointer invented by Bent Schmidt Nielsen.
 *
 * <p>This endpointer is composed of three main steps.
 * <ol>
 * <li>classification of audio into speech and non-speech
 * <li>inserting SPEECH_START and SPEECH_END signals around speech
 * <li>removing non-speech regions
 * </ol>
 *
 * <p>The first step, classification of audio into speech and non-speech,
 * uses Bent Schmidt Nielsen's algorithm. Each time audio comes in, 
 * the average signal level and the background noise level are updated,
 * using the signal level of the current audio. If the average signal
 * level is greater than the background noise level by a certain
 * threshold value (configurable), then the current audio is marked
 * as speech. Otherwise, it is marked as non-speech.
 *
 * <p>The second and third step of this endpointer are documented in the
 * classes SpeechMarker and DataFilter.
 *
 * @see SpeechMarker
 */
public class LevelTracker extends BaseDataProcessor {

    /**
     * Prefix for the SphinxProperties of this class.
     */
    public static final String PROP_PREFIX = 
        "edu.cmu.sphinx.frontend.endpoint.LevelTracker.";

    /**
     * The SphinxProperty specifying the endpointing frame length.
     * This is the number of samples in each endpointed frame.
     */
    public static final String PROP_FRAME_LENGTH = PROP_PREFIX + "frameLength";

    /**
     * The default value of PROP_FRAME_LENGTH.
     */
    public static final int PROP_FRAME_LENGTH_DEFAULT = 160;

    /**
     * The SphinxProperty specifying the minimum signal level used
     * to update the background signal level.
     */
    public static final String PROP_MIN_SIGNAL = PROP_PREFIX + "minSignal";

    /**
     * The default value of PROP_MIN_SIGNAL.
     */
    public static final double PROP_MIN_SIGNAL_DEFAULT = 0;

    /**
     * The SphinxProperty specifying the threshold. If the current signal
     * level is greater than the background level by this threshold,
     * then the current signal is marked as speech.
     */
    public static final String PROP_THRESHOLD = PROP_PREFIX + "threshold";

    /**
     * The default value of PROP_THRESHOLD.
     */
    public static final double PROP_THRESHOLD_DEFAULT = 10;

    /**
     * The SphinxProperty specifying the adjustment.
     */
    public static final String PROP_ADJUSTMENT = PROP_PREFIX + "adjustment";

    /**
     * The default value of PROP_ADJUSTMENT_DEFAULT.
     */
    public static final double PROP_ADJUSTMENT_DEFAULT = 0.003;

    /**
     * The SphinxProperty specifying whether to print debug messages.
     */
    public static final String PROP_DEBUG = PROP_PREFIX + "debug";

    /**
     * The default value of PROP_DEBUG.
     */
    public static final boolean PROP_DEBUG_DEFAULT = false;
    

    private boolean debug;
    private double averageNumber = 1;
    private double adjustment;
    private double level;               // average signal level
    private double background;          // background signal level
    private double minSignal;           // minimum valid signal level
    private double threshold;
    private int frameLength;
    private SpeechClassifier classifier;
    private SpeechMarker speechMarker;
    private NonSpeechDataFilter filter;
    

    /**
     * Constructs an un-initialized LevelTracker.
     */
    public LevelTracker() {}

    /**
     * Initializes this LevelTracker endpointer with the given name, context,
     * and DataProcessor predecessor.
     *
     * @param name        the name of this LevelTracker, if this is null,
     *                    the name "LevelTracker" will be given by default
     * @param frontEnd    the context of the SphinxProperties this
     *                    LevelTracker use
     * @param props       the SphinxProperties to read properties from
     * @param predecessor the DataProcessor where this LevelTracker
     *                    gets Data from
     */
    public void initialize(String name, String frontEnd, 
                           SphinxProperties props, DataProcessor predecessor) {
        super.initialize((name == null ? "LevelTracker" : name),
                         frontEnd, props, predecessor);
        reset();
        setProperties(props);
        classifier = new SpeechClassifier();
        classifier.initialize
            ("SpeechClassifier", frontEnd, props, predecessor);
        speechMarker = new SpeechMarker();
        speechMarker.initialize
            ("SpeechMarker", frontEnd, props, classifier);
        filter = new NonSpeechDataFilter();
        filter.initialize
            ("NonSpeechDataFilter", frontEnd, props, speechMarker);
    }

    /**
     * Sets the properties for this LevelTracker.
     *
     * @param props the SphinxProperties to use
     */
    private void setProperties(SphinxProperties props) {

        frameLength = props.getInt
            (getName(), PROP_FRAME_LENGTH, PROP_FRAME_LENGTH_DEFAULT);

        adjustment = props.getDouble
            (getName(), PROP_ADJUSTMENT, PROP_ADJUSTMENT_DEFAULT);

        threshold = props.getDouble
            (getName(), PROP_THRESHOLD, PROP_THRESHOLD_DEFAULT);

        minSignal = props.getDouble
            (getName(), PROP_MIN_SIGNAL, PROP_MIN_SIGNAL_DEFAULT);

        debug = props.getBoolean
            (getName(), PROP_DEBUG, PROP_DEBUG_DEFAULT);
    }

    /**
     * Resets this LevelTracker to a starting state.
     */
    private void reset() {
        level = 0;
        background = 100;
    }

    /**
     * Sets the predecessor DataProcessor.
     *
     * @param predecessor the new predecessor of this LevelTracker
     */
    public void setPredecessor(DataProcessor predecessor) {
        super.setPredecessor(predecessor);
        classifier.setPredecessor(predecessor);
    }

    /**
     * Returns the next Data object, which are already class[Aified
     * as speech or non-speech.
     *
     * @return the next Data object, or null if none available
     *
     * @throws DataProcessingException if a data processing error occurred
     *
     * @see Data
     */
    public Data getData() throws DataProcessingException {
        return filter.getData();            
    }

    /**
     * Classifies Data into either speech or non-speech.
     */
    class SpeechClassifier extends BaseDataProcessor {

        List outputQueue = new LinkedList();
        
        /**
         * Initializes this DataProcessor.
         *
         * @param name         the name of this DataProcessor, if this is null,
         *                     the name "SpeechClassifier" is given by default
         * @param pipelineName the name of the front-end pipeline this
         *                     DataProcessor is in
         * @param props        the SphinxProperties to use
         * @param predecessor the predecessor of this DataProcessor
         */
        public void initialize(String name, String frontEndName,
                               SphinxProperties props,
                               DataProcessor predecessor) {
            super.initialize((name == null ? "SpeechClassifier" : name),
                             frontEndName, props, predecessor);
        }

        /**
         * Returns the logarithm base 10 of the root mean square of the
         * given samples.
         *
         * @param samples the samples
         *
         * @return the calculated log root mean square in log 10
         */
        private double logRootMeanSquare(double[] samples) {
            assert samples.length > 0;
            double sumOfSquares = 0.0f;
            for (int i = 0; i < samples.length; i++) {
                double sample = samples[i];
                sumOfSquares += sample * sample;
            }
            double rootMeanSquare = Math.sqrt
                ((double)sumOfSquares/samples.length);
            rootMeanSquare = Math.max(rootMeanSquare, 1);
            return (LogMath.log10((float)rootMeanSquare) * 20);
        }
        
        /**
         * Classifies the given audio frame as speech or not, and updates
         * the endpointing parameters.
         *
         * @param audio the audio frame
         */
        private void classify(DoubleData audio) {
            double current = logRootMeanSquare(audio.getValues());
            // System.out.println("current: " + current);
            boolean isSpeech = false;
            if (current >= minSignal) {
                level = ((level*averageNumber) + current)/(averageNumber + 1);
                if (current < background) {
                    background = current;
                } else {
                    background += (current - background) * adjustment;
                }
                if (level < background) {
                    level = background;
                }
                isSpeech = (level - background > threshold);
            }
            SpeechClassifiedData labeledAudio
                = new SpeechClassifiedData(audio, isSpeech);
            if (debug) {
                String speech = "";
                if (labeledAudio.isSpeech()) {
                    speech = "*";
                }
                System.out.println("Bkg: " + background + ", level: " + level +
                                   ", current: " + current + " " + speech);
            }
            outputQueue.add(labeledAudio);
        }
        
        /**
         * Returns the next Data object.
         *
         * @return the next Data object, or null if none available
         *
         * @throws DataProcessingException if a data processing error occurs
         */
        public Data getData() throws DataProcessingException {
            if (outputQueue.size() == 0) {
                Data audio = getPredecessor().getData();
                if (audio != null) {
                    if (audio instanceof DoubleData) {
                        DoubleData data = (DoubleData) audio;
                        if (data.getValues().length > frameLength) {
                            throw new Error
                                ("Size of each audio frame should be <= " +
                                 frameLength + ". If you have 2 bytes per " +
                                 "sample, you should set the byte per read to "
                                 + frameLength*2);
                        }
                        classify(data);
                    } else {
                        outputQueue.add(audio);
                    }
                }
            }
            if (outputQueue.size() > 0) {
                Data audio = (Data) outputQueue.remove(0);
                return audio;
            } else {
                return null;
            }
        }
    }
}
