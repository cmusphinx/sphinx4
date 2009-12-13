/**
 * Copyright 1998-2001 Sun Microsystems, Inc.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package com.sun.speech.engine.synthesis;

import javax.speech.synthesis.Voice;

/**
 * Extends the JSAPI 1.0 <code>Voice</code> class to encapsulate
 * engine-specific data.
 */
public class BaseVoice extends Voice {
    /**
     * The id of the voice
     */
    protected String voiceId;

    /**
     * The default pitch
     */
    protected float defaultPitch;

    /**
     * The default pitch range
     */
    protected float defaultPitchRange;

    /**
     * The default speaking rate
     */
    protected float defaultSpeakingRate;

    /**
     * The default volume
     */
    protected float defaultVolume;

    /**
     * Class constructor.  The age and gender parameters are defined in
     * <code>Voice</code>.
     *
     * @param id the id
     * @param name the name
     * @param gender the gender
     * @param age the age
     * @param style the style
     * @param pitch the baseline pitch in Hertz
     * @param pitchRange the pitch range in Hertz
     * @param speakingRate the speaking rate in words per minute
     * @param volume the volume expressed between 0.0 and 1.0,
     *   inclusive
     */ 
    public BaseVoice(String id,
                     String name,
                     int gender,
                     int age,
                     String style,
                     float pitch,
                     float pitchRange,
                     float speakingRate,
                     float volume) {
        super(name, gender, age, style);   
        this.voiceId = id;
        defaultPitch = pitch;
        defaultPitchRange = pitchRange;
        defaultSpeakingRate = speakingRate;
        defaultVolume = volume;
    }

    /**
     * Gets the id for this voice.  Should be unique for a
     * synthesizer.
     *
     * @return the id for this voice
     *
     * @see #setId
     */
    public String getId() {
        return voiceId;
    }

    /**
     * Sets the id for this voice.
     *
     * @param id the new id
     *
     * @see #getId
     */
    public void setId(String id) {
        voiceId = id;
    }

    /**
     * Gets the pitch for this voice
     * @return the pitch
     */
    public float getPitch() {
	return defaultPitch;
    }

    /**
     * Gets the pitch range for this voice
     * @return the pitch range
     */
    public float getPitchRange() {
	return defaultPitchRange;
    }

    /**
     * Gets the speaking rate for this voice
     * @return the speaking rate
     */
    public float getSpeakingRate() {
	return defaultSpeakingRate;
    }

    /**
     * Gets the volume for this voice
     * @return the volume
     */
    public float getVolume() {
	return defaultVolume;
    }

    /**
     * Creates a copy of this voice.
     *
     * @return a clone of this voice
     */
    public Object clone() {
        return super.clone();
    }

    /**
     * Converts a Voice to a printable string.
     *
     */
    public String toString() {
        StringBuilder buf = new StringBuilder();

        if (getName() != null) {
            buf.append(getName());
        }
        buf.append(":");

        // Note: doesn't handle compound choices.
        // e.g. GENDER_MALE | GENDER_FEMALE
        int gender = getGender();
        if (gender == Voice.GENDER_DONT_CARE) {
            buf.append("GENDER_DONT_CARE");
        } else if ((gender & Voice.GENDER_MALE) != 0) {
            buf.append("GENDER_MALE");
        } else if ((gender & Voice.GENDER_FEMALE) != 0) {
            buf.append("GENDER_FEMALE");
        } else if ((gender & Voice.GENDER_NEUTRAL) != 0) {
            buf.append("GENDER_NEUTRAL");
        }
        buf.append(":");

        // Note: doesn't handle compound choices.
        // e.g. AGE_CHILD | AGE_TEENAGER
        int age = getAge();
        if (age == Voice.AGE_DONT_CARE) {
            buf.append("AGE_DONT_CARE");
        } else if ((age & Voice.AGE_CHILD) != 0) {
            buf.append("AGE_CHILD");
        } else if ((age & Voice.AGE_TEENAGER) != 0) {
            buf.append("AGE_TEENAGER");
        } else if ((age & Voice.AGE_YOUNGER_ADULT) != 0) {
            buf.append("AGE_YOUNGER_ADULT");
        } else if ((age & Voice.AGE_MIDDLE_ADULT) != 0) {
            buf.append("AGE_MIDDLE_ADULT");
        } else if ((age & Voice.AGE_OLDER_ADULT) != 0) {
            buf.append("AGE_OLDER_ADULT");
        }
        buf.append(":");

        if (getStyle() != null) {
            buf.append(getStyle());
        }

        return buf.toString();
    }
}
