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
public class LevelTracker extends DataProcessor implements AudioSource {

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


    private double averageNumber;
    private double adjustment;
    private double level;
    private double background;
    private double threshold;
    private int frameLength;
    private int lastAudioStart;
    private Audio lastAudio;
    private AudioSource predecessor;


    /**
     * Initializes an Endpointer with the given name, context,
     * and CepstrumSource predecessor.
     *
     * @param name the name of this EnergyEndpointer
     * @param context the context of the SphinxProperties this
     *    EnergyEndpointer uses
     * @param props the SphinxProperties to read properties from
     * @param predecessor the CepstrumSource where this EnergyEndpointer
     *    gets Cepstrum from
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
    }

    /**
     * Sets the properties for this LevelTracker.
     */
    private void setProperties() {
        SphinxProperties props = getSphinxProperties();
        adjustment = props.getDouble(PROP_ADJUSTMENT, PROP_ADJUSTMENT_DEFAULT);
        threshold = props.getDouble(PROP_THRESHOLD, PROP_THRESHOLD_DEFAULT);
    }        

    /**
     * Resets this LevelTracker to a starting state.
     */
    private void reset() {
        level = 0;
        background = 100;
        lastAudio = null;
        lastAudioStart = 0;
    }

    /**
     * Returns the logarithm base 10 of the root mean square of the
     * given samples.
     *
     * @param sample the samples
     *
     * @return the calculated log root mean square in log 10
     */
    private static double logRootMeanSquare(double[] samples) {
        double sumOfSquares = 0.0f;
        for (int i = 0; i < samples.length; i++) {
            double sample = samples[i];
            sumOfSquares += sample * sample;
        }
        double rootMeanSquare = Math.sqrt((double)sumOfSquares/samples.length);
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
        Audio audio = predecessor.getAudio();
        if (audio != null) {
            if (audio.hasContent()) {
                processIncomingAudio(audio);
            }
        }
        return audio;
    }

    /**
     * Perform endpointing on the given Audio object.
     *
     * @param audio the Audio object to endpoint
     */
    private void processIncomingAudio(Audio audio) throws IOException {

        int dataLength = 0;
        
        // include any residual samples from the last Audio object
        boolean hasResidualAudio =
            (lastAudio != null && 
             lastAudioStart < lastAudio.getSamples().length);

        if (hasResidualAudio) {
            dataLength += (lastAudio.getSamples().length - lastAudioStart);
        }

        List audioList = new LinkedList();
        double[] data = audio.getSamples();
        dataLength += data.length;
        audioList.add(audio);

        Audio signalFrame = null;

        if (dataLength < frameLength) {
            // make sure we at least have one frame's worth of data
            while (dataLength < frameLength) {
                Audio nextAudio = predecessor.getAudio();
                if (nextAudio != null) {
                    if (audio.hasContent()) {
                        audioList.add(nextAudio);
                        dataLength += audio.getSamples().length;
                    } else {
                        signalFrame = nextAudio;
                        break;
                    }
                }
            }            
        }

        // allocate for the next frame
        if (dataLength >= frameLength) {
            data = new double[frameLength];
        } else {
            data = new double[dataLength];
        }
        
        int start = 0;
        // copy residual data from last Audio
        if (hasResidualAudio) {
            int residualLength =
                (lastAudio.getSamples().length - lastAudioStart);
            System.arraycopy(lastAudio.getSamples(), lastAudioStart,
                             data, start, residualLength);
            start += residualLength;                             
        }

        // copy Audio data
        Audio thisAudio = null;
        int length = 0;

        for (Iterator i = audioList.iterator(); 
             i.hasNext() && start < data.length; ) {
            thisAudio = (Audio) i.next();
            i.remove();
            length = thisAudio.getSamples().length;
            int capacity = (data.length - start);
            if (length > capacity) {
                length = capacity;
            }
            System.arraycopy(thisAudio.getSamples(), 0,
                             data, start, length);
            start += length;
        }

        // the first audio frame
        Audio newAudio = new Audio(data);
        classify(newAudio);

        assert (audioList.size() == 0);

        start = length;
        while ((start + frameLength) < thisAudio.getSamples().length) {
            data = new double[frameLength];
            System.arraycopy(thisAudio.getSamples(), start,
                             data, 0, data.length);
            start += frameLength;
            newAudio = new Audio(data);
            classify(newAudio);
        }

        lastAudio = thisAudio;
        lastAudioStart = start;
    }
}
