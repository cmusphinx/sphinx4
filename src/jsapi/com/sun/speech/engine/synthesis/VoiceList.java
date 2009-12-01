/**
 * Copyright 1998-2001 Sun Microsystems, Inc.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package com.sun.speech.engine.synthesis;

import java.util.List;

import javax.speech.synthesis.SynthesizerModeDesc;
import javax.speech.synthesis.Voice;

/**
 * Maintains a list of JSAPI 1.0 <code>Voices</code>.
 */
public class VoiceList {

    /**
     * The list of <code>Voices</code>.
     */
    protected final List<BaseVoice> voiceList;
    
    /**
     * Class constructor.
     */
    public VoiceList() {
        voiceList = new java.util.ArrayList<BaseVoice>();
    }
    
    /**
     * Constructs from the voice list in <code>desc</code>.
     * Requires that all voices in mode desc be instances of
     * <code>BaseVoice</code>.
     *
     * @param desc the <code>SynthesizerModeDesc</code> to get voices from
     */
    public VoiceList(SynthesizerModeDesc desc) {
        voiceList = new java.util.ArrayList<BaseVoice>();
        
        Voice[] v = desc.getVoices();
        
        if (v != null) {
            for (int i = 0; i < v.length; i++) {
                addVoice((BaseVoice)(v[i]));
            }
        }
    }

    /**
     * Adds a voice to the list.
     *
     * @param voice the voice to add
     *
     * @see #removeVoice
     */
    public void addVoice(BaseVoice voice) {
        if (!voiceList.contains(voice)) {
            voiceList.add(voice);
        }
    }

    /**
     * Removes a voice from the list.
     *
     * @param voice the voice to remove
     *
     * @see #addVoice
     */
    public void removeVoice(BaseVoice voice) {
        voiceList.remove(voice);
    }

    /**
     * Gets a voice by its identifier.
     *
     * @param id the voice id
     *
     * @return the voice if it exists; otherwise <code>null</code>
     *
     * @see BaseVoice#getId
     */
    public BaseVoice getVoiceById(String id) {
        for (int i = 0; i < voiceList.size(); i++) {
            BaseVoice bv = voiceList.get(i);
            if (bv.getId().equals(id)) {
                return bv;
            }
        }   
        return null;
    }

    /**
     * Gets the id of a voice.
     *
     * @param voice the voice
     * @param variant the voice variant
     *
     * @return the id of the voice
     */
    public String getVoiceId(Voice voice, int variant) {
        // If voice is a BaseVoice simply return its id.
        //
        if (voice instanceof BaseVoice) {
            BaseVoice bv = (BaseVoice)voice;
            String id = bv.getId();
            if (id != null && id.length() > 0) {
                return id;
            }
        }
        
        // Build a list of indicies of all matching voices.
        // If variant is <= 0 return with first match
        //
        int indexes[] = new int[voiceList.size()];
        int count = 0;
        
        for (int i=0; i<voiceList.size(); i++) {
            BaseVoice bv = voiceList.get(i);
            if (bv.match(voice)) {
                if (variant <= 0) {
                    return bv.getId();
                }
                indexes[count++] = i;
            }
        }
        
        // If no matches, return "".
        if (count == 0) {
            return "";
        }
            
        // Apply modulo to select variant in list
        variant = (variant - 1) % count;
        
        // Return the selected voice id.
        BaseVoice bv = voiceList.get(indexes[variant]);
        return bv.getId();
    }    

    /**
     * Gets id for voice based on parameters provided in JSML.
     * Priority to voice name.  Then try to match age and gender plus
     * variant.
     *
     * @param name the voice name
     * @param gender the gender
     * @param age the age
     * @param variant the variant
     *
     * @return the voice id
     *
     * @see BaseVoice
     * @see Voice
     */
    public String getVoiceId(String name, int gender, int age, int variant) {
        String id;
        
        // Is there a match by voice name?  If yes, return it.
        // Otherwise, ignore name.
        if (name != null && name.length() > 0) {
            id = getVoiceId(new Voice(name,
                                      Voice.GENDER_DONT_CARE,
                                      Voice.AGE_DONT_CARE,
                                      null),
                            0);
            if (id != null && id.length() > 0) {
                return id;
            }
        }
        

        // Try to match gender and age
        id = getVoiceId(new Voice(null,
                                  gender,
                                  age,
                                  null),
                        variant);
        if (id != null && id.length() > 0) {
            return id;
        }

        // Try to match gender and adjoining ages
        int looseAge = age | (age << 1) | (age >> 1);
        id = getVoiceId(new Voice(null,
                                  gender,
                                  looseAge,
                                  null),
                        variant);
        if (id != null && id.length() > 0) {
            return id;
        }

        // Try to match just gender
        id = getVoiceId(new Voice(null,
                                  gender,
                                  Voice.AGE_DONT_CARE,
                                  null),
                        variant);
        if (id != null && id.length() > 0) {
            return id;
        }

        // Failed match
        return "";
    }    
}
