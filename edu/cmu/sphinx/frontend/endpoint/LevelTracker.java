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
import edu.cmu.sphinx.util.LogMath;

import java.io.IOException;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * Implements a level tracking endpointer invented by Bent Schmidt Nielsen.
 */
public class LevelTracker extends DataProcessor implements AudioEndpointer {

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
     * The SphinxProperty specifying the threshold
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
    private double level;
    private double background;
    private double threshold;
    private int frameLength;
    private AudioSource predecessor;
    private SpeechMarker speechMarker;
    private AudioFilter filter;
    

    /**
     * Constructs an un-initialized LevelTracker.
     */
    public LevelTracker() {}

    /**
     * Initializes an Endpointer with the given name, context,
     * and AudioSource predecessor.
     *
     * @param name the name of this EnergyEndpointer
     * @param context the context of the SphinxProperties this
     *    EnergyEndpointer use
     * @param props the SphinxProperties to read properties from
     * @param predecessor the AudioSource where this EnergyEndpointer
     *    gets Audio from
     *
     * @throws java.io.IOException
     */
    public void initialize(String name, String context, 
                           SphinxProperties props,
                           AudioSource predecessor) throws IOException {
        super.initialize(name, context, props);
        this.predecessor = predecessor;
        reset();
        setProperties();
        speechMarker = new SpeechMarker();
        speechMarker.initialize
            ("SpeechMarker", getContext(), getSphinxProperties(), 
             new SpeechClassifier());
        filter = new AudioFilter
            ("AudioFilter", getContext(), getSphinxProperties(),
             speechMarker);
    }

    /**
     * Sets the properties for this LevelTracker.
     */
    private void setProperties() {
        SphinxProperties props = getSphinxProperties();
        frameLength = props.getInt
            (PROP_FRAME_LENGTH, PROP_FRAME_LENGTH_DEFAULT);
        adjustment = props.getDouble(PROP_ADJUSTMENT, PROP_ADJUSTMENT_DEFAULT);
        threshold = props.getDouble(PROP_THRESHOLD, PROP_THRESHOLD_DEFAULT);
        debug = props.getBoolean(PROP_DEBUG, PROP_DEBUG_DEFAULT);
    }

    /**
     * Resets this LevelTracker to a starting state.
     */
    private void reset() {
        level = 0;
        background = 100;
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
        return filter.getAudio();            
    }

    /**
     * Classifies Audio into either speech or non-speech.
     */
    class SpeechClassifier implements AudioSource {

        List outputQueue = new LinkedList();
        
        /**
         * Returns the logarithm base 10 of the root mean square of the
         * given samples.
         *
         * @param sample the samples
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
            return (LogMath.log10((float)rootMeanSquare) * 20);
        }
        
        /**
         * Classifies the given audio frame as speech or not, and updates
         * the endpointing parameters.
         *
         * @param audio the audio frame
         */
        private void classify(Audio audio) {
            double current = logRootMeanSquare(audio.getSamples());
            // System.out.println("current: " + current);
            level = ((level * averageNumber) + current)/(averageNumber + 1);
            if (current < background) {
                background = current;
            } else {
                background += (current - background) * adjustment;
            }
            if (level < background) {
                level = background;
            }
            boolean isSpeech = (level - background > threshold);
            audio.setSpeech(isSpeech);
            if (debug) {
                String speech = "";
                if (audio.isSpeech()) {
                    speech = "*";
                }
                System.out.println("Bkg: " + background + ", level: " + level +
                                   ", current: " + current + " " + speech);
            }
            outputQueue.add(audio);
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
                Audio audio = predecessor.getAudio();
                if (audio != null) {
                    /*
                    System.out.println("LevelTracker: incoming: " +
                                       audio.getSignal().toString());
                    */
                    audio.setSpeech(false);
                    if (audio.hasContent()) {
                        assert audio.getSamples().length == frameLength;
                        classify(audio);
                    } else {
                        outputQueue.add(audio);
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
    }
}
