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

package edu.cmu.sphinx.frontend.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.sound.sampled.AudioFormat;


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
    private AudioFormat audioFormat;
    

    /**
     * Constructs a default Utterance object.
     *
     * @param name the name of this Utterance, e.g., it can be the
     *    name of the audio file where the audio came from
     * @param format the audio format of this Utterance
     */
    public Utterance(String name, AudioFormat format) {
        this.name = name;
        this.audioFormat = format;
        audioBuffer = new ArrayList();
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
     * Returns the audio format of this Utterance.
     *
     * @return the audio format
     */
    public AudioFormat getAudioFormat() {
        return audioFormat;
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
     * Flattens the data in this Utterance to one flat array.
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
     * Returns the amount of time (in seconds) this Utterance lasts.
     *
     * @return how long is this utterance
     */
    public float getAudioTime() {
        byte[] audio = getAudio();
        return ((float) audio.length) /
            (audioFormat.getSampleRate() * 
             audioFormat.getSampleSizeInBits()/8);
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
