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


package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.frontend.util.Util;

import edu.cmu.sphinx.util.SphinxProperties;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;


/**
 * Represents the complete audio data of an utterance.
 */
public class Utterance {

    private String name;
    private List audioBuffer;
    private boolean flattened = false;
    private byte[] flattenedAudio = null;
    private int next = 0;
    private int totalBytes = 0;

    private int bitsPerSample;
    private int sampleRate;
    private int windowShiftInBytes;
    private int windowSizeInBytes;


    /**
     * Constructs a default Utterance object.
     *
     * @param name the name of this Utterance, e.g., it can be the
     *    name of the audio file where the audio came from
     * @param context the context this Utterance is in
     */
    public Utterance(String name, String context) {
        this.name = name;
        audioBuffer = new LinkedList();

        // get the Sphinx properties
        SphinxProperties props = SphinxProperties.getSphinxProperties(context);

        bitsPerSample = props.getInt(FrontEnd.PROP_BITS_PER_SAMPLE,
                                     FrontEnd.PROP_BITS_PER_SAMPLE_DEFAULT);
        sampleRate = props.getInt(FrontEnd.PROP_SAMPLE_RATE,
                                  FrontEnd.PROP_SAMPLE_RATE_DEFAULT);
        
        float windowSizeInMs = props.getFloat
            (Windower.PROP_WINDOW_SIZE_MS, 
             Windower.PROP_WINDOW_SIZE_MS_DEFAULT);
        float windowShiftInMs = props.getFloat
            (Windower.PROP_WINDOW_SHIFT_MS,
             Windower.PROP_WINDOW_SHIFT_MS_DEFAULT);
        
        windowSizeInBytes = Util.getSamplesPerWindow
            (sampleRate, windowSizeInMs) * 2;
        windowShiftInBytes = Util.getSamplesPerShift
            (sampleRate, windowShiftInMs) * 2;
    }


    /**
     * Returns the name of this Utterance.
     *
     * @return the name of this Utterance
     */
    public String getName() {
        return name;
    }


    /**
     * Adds an audio frame into this Utterance.
     *
     * @param audio the audio frame to add
     */
    public void add(byte[] audio) {
        synchronized (audioBuffer) {
            totalBytes += audio.length;
            audioBuffer.add(audio);
            setFlattened(false);
        }
    }


    /**
     * Returns the number of bits per sample.
     *
     * @return the number of bits per sample
     */
    public int getBitsPerSample() {
        return bitsPerSample;
    }


    /**
     * Returns the sample rate of this Utterance.
     *
     * @return the sample rate
     */
    public int getSampleRate() {
	return sampleRate;
    }


    /**
     * Returns the next audio frame.
     *
     * @return the next audio frame
     */
    public byte[] getNext() {
        synchronized (audioBuffer) {
            if (0 <= next && next < audioBuffer.size()) {
                return (byte[]) audioBuffer.get(next++);
            } else {
                return null;
            }
        }
    }


    /**
     * Flattens the Audio objects in this Utterance to one flat array.
     */
    private void flatten() {
        synchronized (audioBuffer) {
	    if (flattenedAudio == null || !isFlattened()) {
		flattenedAudio = new byte[totalBytes];
		int start = 0;
		for (Iterator i = audioBuffer.iterator(); i.hasNext(); ) {
		    byte[] current = (byte[]) i.next();
		    System.arraycopy(current, 0, flattenedAudio, start, 
				     current.length);
		    start += current.length;
		}
		setFlattened(true);
	    }
	}
    }


    /**
     * Returns the complete audio stream of this utterance.
     *
     * @return the complete audio stream
     */
    public byte[] getAudio() {
        flatten();
        return flattenedAudio;
    }


    /**
     * Returns the audio samples of the given feature.
     *
     * @return the audio samples
     *
     * @throws IllegalArgumentException if one or both of the featureIDs
     *    is (are) less than zero
     */
    public byte[] getAudio(int frameNumber) throws IllegalArgumentException {
        return getAudio(frameNumber, frameNumber);
    }


    /**
     * Returns the audio samples of the given series of features. The
     * firstFeatureID must be less than or equal to the lastFeatureID,
     * otherwise an <code>IllegalArgumentException</code> will be
     * thrown.
     *
     * @param firstFeatureID the ID of the first feature
     * @param lastFeatureID the ID of the last feature
     *
     * @throws IllegalArgumentException if the firstFeatureID is greater
     *    than the lastFeatureID
     */
    public byte[] getAudio(int firstFeatureID, int lastFeatureID) throws 
    IllegalArgumentException {

        if (lastFeatureID < firstFeatureID) {
            throw new IllegalArgumentException
                ("lastFeatureID < firstFeatureID");
        }
        if (firstFeatureID < 0) {
            throw new IllegalArgumentException
                ("Invalid firstFeatureID: " + firstFeatureID);
        }
        if (lastFeatureID < 0) {
            throw new IllegalArgumentException
                ("Invalid lastFeatureID: " + lastFeatureID);
        }

        // calculate the total number of bytes for this segment of speech
        int numberBytes = windowSizeInBytes;
        int numberWindows = lastFeatureID - firstFeatureID;
        if (numberWindows > 0) {
            numberBytes += (numberWindows * windowShiftInBytes);
        }

        byte[] audio = new byte[numberBytes];

        flatten();

        int startByte = firstFeatureID * windowShiftInBytes;
        System.arraycopy(flattenedAudio, startByte, audio, 0, audio.length);
        
        return audio;
    }


    /**
     * Returns the amount of time (in seconds) this Utterance lasts
     *
     * @return how long is this utterance
     */
    public float getAudioTime() {
        byte[] audio = getAudio();
        return ((float) audio.length)/((float) sampleRate * (bitsPerSample/8));
    }


    /**
     * Returns true if all the audio data is flattened to one flat
     * audio array.
     *
     * @return true if all the audio data is flattened to one array
     */
    private synchronized boolean isFlattened() {
        return flattened;
    }


    private synchronized void setFlattened(boolean flattened) {
        this.flattened = flattened;
    }
}
