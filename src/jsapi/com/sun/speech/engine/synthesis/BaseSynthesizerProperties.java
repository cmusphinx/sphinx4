/**
 * Copyright 1998-2001 Sun Microsystems, Inc.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package com.sun.speech.engine.synthesis;

import java.beans.PropertyVetoException;

import javax.speech.SpeechError;

import javax.speech.synthesis.Voice;
import javax.speech.synthesis.SynthesizerProperties;

import com.sun.speech.engine.BaseEngineProperties;

/**
 * Supports the JSAPI 1.0 <code>SynthesizerProperties</code> 
 * interface.  The properties of a <code>Synthesizer</code> are:
 *
 * <UL>
 *   <LI>Speaking voice,
 *   <LI>Baseline pitch,
 *   <LI>Pitch range,
 *   <LI>Speaking rate,
 *   <LI>Volume.
 * </UL>
 */
public class BaseSynthesizerProperties extends BaseEngineProperties
    implements SynthesizerProperties {

    /**
     * The default voice.
     */
    protected Voice defaultVoice;

    /**
     * The default pitch.
     */
    protected float defaultPitch;

    /**
     * The default pitch range.
     */
    protected float defaultPitchRange;

    /**
     * The default specking rate.
     */
    protected float defaultSpeakingRate;

    /**
     * The default volume.
     */
    protected float defaultVolume;
    
    /**
     * The current voice.
     */
    protected Voice currentVoice;
    
    /**
     * The current pitch.
     */
    protected float currentPitch;
    
    /**
     * The current pitch range.
     */
    protected float currentPitchRange;
    
    /**
     * The current speaking rate.
     */
    protected float currentSpeakingRate;
    
    /**
     * The current volume.
     */
    protected float currentVolume;

    /**
     * Class constructor.
     */
    public BaseSynthesizerProperties() {
        this.defaultVoice = null;
        this.defaultPitch = 0.0f;
        this.defaultPitchRange = 0.0f;
        this.defaultSpeakingRate = 0.0f;
        this.defaultVolume = 0.0f;
    }
    
    /**
     * Creates a new <code>BaseSynthesizerProperties</code> with the
     * given default values.
     *
     * @param defaultVoice the default voice
     * @param defaultPitch the default pitch
     * @param defaultPitchRange the default pitch range
     * @param defaultSpeakingRate the default speaking rate
     * @param defaultVolume the default volume
     */
    public BaseSynthesizerProperties(Voice defaultVoice,
                                     float defaultPitch,
                                     float defaultPitchRange,
                                     float defaultSpeakingRate,
                                     float defaultVolume) {
        if (defaultVoice != null) {
            this.defaultVoice = (Voice)(defaultVoice.clone());
        } else {
            this.defaultVoice = null;
        }
        
        this.defaultPitch = defaultPitch;
        this.defaultPitchRange = defaultPitchRange;
        this.defaultSpeakingRate = defaultSpeakingRate;
        this.defaultVolume = defaultVolume;

        if (defaultVoice != null) {
            currentVoice = (Voice)(defaultVoice.clone());
        } else {
            currentVoice = null;
        }
    
        currentPitch = defaultPitch;
        currentPitchRange = defaultPitchRange;
        currentSpeakingRate = defaultSpeakingRate;
        currentVolume = defaultVolume;
    }

    /**
     * Resets all properties to their default values.
     */
    public void reset() {
        try {
            setVoice(defaultVoice);
            setVolume(defaultVolume);
            setPitch(defaultPitch);
            setPitchRange(defaultPitchRange);
            setSpeakingRate(defaultSpeakingRate);
        } catch (PropertyVetoException e) {
            throw new SpeechError("Inconsistent default properties");
        }
    }

    /**
     * Gets the current synthesizer voice.
     *
     * @return the current synthesizer voice.
     *
     * @see #setVoice
     */
    public Voice getVoice() {
        return (Voice)(currentVoice.clone());
    }

    /**
     * Sets the current synthesizer voice.
     *
     * @param voice the new voice
     *
     * @see #getVoice
     *
     * @throws PropertyVetoException if the voice cannot be set to
     *   the given value
     */
    public void setVoice(Voice voice)
        throws PropertyVetoException {
        // [[[TODO: Need to check that the voice is legal.]]]
        Voice oldVoice = currentVoice;
        currentVoice = (Voice)(voice.clone());
        postPropertyChangeEvent("Voice", oldVoice, voice);
    }

    /**
     * Gets the baseline pitch for synthesis.
     *
     * @return the baseline pitch in Hertz
     *
     * @see #setPitch
     */
    public float getPitch() {
        return currentPitch;
    }

    /**
     * Sets the baseline pitch for the current synthesis voice.
     *
     * @param hertz the new baseline pitch in Hertz
     *
     * @see #getPitch
     *
     * @throws PropertyVetoException if the baseline pitch cannot be
     *   set to the given value       
     */
    public void setPitch(float hertz)
        throws PropertyVetoException {
        float oldPitch = currentPitch;
        currentPitch = hertz;
        postPropertyChangeEvent("Pitch", oldPitch, hertz);
    }

    /**
     * Gets the pitch range for synthesis.
     *
     * @return the current pitch range in Hertz
     *
     * @see #setPitchRange
     *
     */
    public float getPitchRange() {
        return currentPitchRange;
    }

    /**
     * Sets the pitch range for the current synthesis voice.
     *
     * @param hertz the new range in Hertz
     *
     * @see #getPitchRange
     *
     * @throws PropertyVetoException if the pitch range cannot be set
     *   to the given value
     */
    public void setPitchRange(float hertz)
        throws PropertyVetoException {
        float oldRange = currentPitchRange;
        currentPitchRange = hertz;
        postPropertyChangeEvent("PitchRange", oldRange, hertz);
    }

    /**
     * Gets the current target speaking rate in words per minute.
     *
     * @return the current target speaking rate in words per minute.
     *
     * @see #getSpeakingRate
     */
    public float getSpeakingRate() {
        return currentSpeakingRate;
    }

    /**
     * Sets the target speaking rate in words per minute.
     *
     * @param wpm the new speaking rate in words per minute
     *
     * @throws PropertyVetoException if the speaking rate cannot be
     *   set to the given value
     *
     * @see #getSpeakingRate
     */
    public void setSpeakingRate(float wpm)
        throws PropertyVetoException {
        float oldRate = currentSpeakingRate;
        currentSpeakingRate = wpm;

        postPropertyChangeEvent("SpeakingRate", oldRate, wpm);
    }

    /**
     * Gets the current volume.
     *
     * @return the current volume expressed as a <code>float</code>
     *   0.0 and 1.0, inclusive
     *
     * @see #setVolume
     */
    public float getVolume() {
        return currentVolume;
    }

    /**
     * Sets the volume.
     *
     * @param volume the new volume expressed as a <code>float</code>
     *   0.0 and 1.0, inclusive
     *
     * @see #getVolume
     *
     * @throws PropertyVetoException if the volume cannot be
     *   set to the given value
     */
    public void setVolume(float volume)
        throws PropertyVetoException {
        if (volume > 1.0f)
            volume = 1.0f;
        else if (volume < 0.0f)
            volume = 0.0f;
    
        float oldVolume = currentVolume;
        currentVolume = volume;

        postPropertyChangeEvent("Volume", oldVolume, volume);
    }
}
