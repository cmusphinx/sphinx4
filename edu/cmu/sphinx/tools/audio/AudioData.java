/*
 * Copyright 1999-2004 Carnegie Mellon University.  
 * Portions Copyright 2002-2004 Sun Microsystems, Inc.  
 * Portions Copyright 2002-2004 Mitsubishi Electronic Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.tools.audio;

import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;

/**
 * Represents a 16bit, SIGNED_PCM, big endian audio clip with a
 * sample rate specified in AudioFormat.
 */
public class AudioData {
    protected AudioFormat format;
    protected short[] shorts;
    protected Vector listeners = new Vector();
    protected int selectionStart = -1;
    protected int selectionEnd = -1;
    
    /**
     * No-arg constructor.  Creates an empty clip at 16kHz sample rate.
     */
    public AudioData() {
	this.format = new AudioFormat(16000f,
				      16,    // sample size in bits
				      1,     // mono
				      true,  // signed
				      true); // big endian
        shorts = null;
    }
    
    /**
     * Creates a new AudioData with the given data and sample rate.
     * Expects the data to be 16bit, big endian, SIGNED_PCM.
     *
     * @param data the audio samples; one sample per element in the
     * array
     * @param sampleRate the sample rate in Hz
     */
    public AudioData(short[] data, float sampleRate) {
        this.shorts = data;
	this.format = new AudioFormat(sampleRate,
				      16,    // sample size in bits
				      1,     // mono
				      true,  // signed
				      true); // big endian
    }

    /**
     * Creates a new AudioData from the given AudioInputStream,
     * converting the data to 16bit, big endian, SIGNED_PCM if
     * needed.
     *
     * @param ais the AudioInputStream
     * @throws IOException if problems happen when reading from ais
     */
    public AudioData(AudioInputStream ais) throws IOException {
        this.shorts = Utils.toSignedPCM(ais);
	this.format = new AudioFormat(ais.getFormat().getSampleRate(),
				      16,    // sample size in bits
				      1,     // mono
				      true,  // signed
				      true); // big endian
    }

    /**
     * Gets the SIGNED_PCM 16 bit big endian audio data.  NOTE:  this
     * the actual array held by this object, so only use it as a
     * reference (i.e., don't modify the contents).
     *
     * @return the SIGNED_PCM 16 bit big endian samples
     */
    public short[] getAudioData() {
	return shorts;
    }

    /**
     * Sets the audio data.
     *
     * @param data the new SIGNED_PCM 16 bit big endian samples
     */
    public void setAudioData(short[] data) {
        this.shorts = data;
        selectionStart = -1;
        selectionEnd = -1;
        fireStateChanged();
    }
    
    /**
     * Gets the audio format.
     *
     * @return the AudioFormat for the data managed by this object
     */
    public AudioFormat getAudioFormat() {
	return format;
    }

    /**
     * Add a ChangeListener.
     *
     * @param listener the listener to add
     */
    public void addChangeListener(ChangeListener listener) {
	listeners.add(listener);
    }

    /**
     * Remove a ChangeListener.
     *
     * @param listener the listener to remove
     */
    public void removeChangeListener(ChangeListener listener) {
	listeners.remove(listener);
    }

    /**
     * Returns the index of the sample representing the start of the
     * selection.  -1 means the very beginning.
     *
     * @return the start of the selection
     * @see #crop
     * @see #getSelectionEnd
     */
    public int getSelectionStart() {
        return selectionStart;
    }
    
    /**
     * Sets the index of the sample of representing the start of the
     * selection.  -1 means the very beginning.
     *
     * @param newStart the new selection start
     * @see #crop
     * @see #setSelectionEnd
     */
    public void setSelectionStart(int newStart) {
        selectionStart = newStart;
        if (selectionEnd != -1) {
            if (selectionEnd < selectionStart) {
                selectionEnd = selectionStart;
            }
        }
        fireStateChanged();
    }
    
    /**
     * Returns the index of the sample representing the end of the
     * selection.  -1 means the very end.
     *
     * @return the end of the selection
     * @see #crop
     * @see #getSelectionStart
     */
    public int getSelectionEnd() {
        return selectionEnd;
    }
    
    /**
     * Sets the index of the sample of representing the end of the
     * selection.  -1 means the very end.
     *
     * @param newStart the new selection end
     * @see #crop
     * @see #setSelectionStart
     */
    public void setSelectionEnd(int newEnd) {
        selectionEnd = newEnd;
        if (selectionEnd != -1) {
            if (selectionStart > selectionEnd) {
                selectionStart = selectionEnd;
            }
        }
        fireStateChanged();
    }

    /**
     * Crops the audio data between the start and end selections.
     * All audio data outside the region will be permanently lost.
     * The selection will be reset to the very beginning and very
     * end of the cropped clip.
     *
     * @see #getSelectionStart
     * @see #getSelectionEnd
     */
    public void crop() {
        selectionStart = Math.max(0, selectionStart);
        if (selectionEnd == -1) {
            selectionEnd = shorts.length;
        }
        short[] newShorts = new short[selectionEnd - selectionStart];
        for (int i = 0; i < (selectionEnd - selectionStart); i++) {
            newShorts[i] = shorts[i + selectionStart];
        }
        selectionStart = -1;
        selectionEnd = -1;
        shorts = newShorts;
        fireStateChanged();
    }
    
    /**
     * Notify all ChangeListeners of a change.
     */
    protected void fireStateChanged() {
        ChangeListener listener;
	ChangeEvent event = new ChangeEvent(this);
	for (int i = 0; i < listeners.size(); i++) {
	    listener = (ChangeListener) listeners.elementAt(i);
	    listener.stateChanged(event);
	}
    }
}
