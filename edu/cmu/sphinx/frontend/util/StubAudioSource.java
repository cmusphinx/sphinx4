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

import edu.cmu.sphinx.frontend.Audio;
import edu.cmu.sphinx.frontend.AudioSource;

import java.io.IOException;


/**
 * An AudioSource object that acts as a stub between the real
 * AudioSource and the first processor in the front end. It is 
 * there so that changing the real AudioSource will not require
 * resetting the AudioSource of the first processor.
 *
 * A StubAudioSource is constructed using the real AudioSource, and
 * calling <code>StubAudioSource.getAudio()</code> simply returns
 * <code>realAudioSource.getAudio()</code>.
 * The real AudioSource can be changed by the method 
 * <code>setAudioSource()</code>.
 */
public class StubAudioSource implements AudioSource {
    
    private AudioSource realSource;

    /**
     * Constructs a StubAudioSource with no real AudioSource.
     */
    public StubAudioSource() {};
    
    /**
     * Constructs a StubAudioSource with the given AudioSource.
     *
     * @param audioSource the real AudioSource
     */
    public StubAudioSource(AudioSource audioSource) {
        this.realSource = audioSource;
    }
    
    /**
     * Returns the next Audio object produced by this AudioSource.
     *
     * @return the next available Audio object, returns null if no
     *     Audio object is available
     *
     * @throws java.io.IOException
     */
    public Audio getAudio() throws IOException {
        return realSource.getAudio();
    }
    
    /**
     * Sets the real AudioSource.
     *
     * @param newAudioSource the new AudioSource
     */
    public void setAudioSource(AudioSource newAudioSource) {
        this.realSource = newAudioSource;
    }

    /**
     * Returns a string of the class name for the real audio source.
     *
     * @return a string of the class name for the real audio source.
     */
    public String toString() {
        return "Stub for: " + realSource.getClass().getName();
    }
}



