/**
 * Copyright 2001 Sun Microsystems, Inc.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */

package demo.jsapi.cardmatch;

import java.io.File;

import java.util.Locale;

import javax.speech.Central;
import javax.speech.Engine;
import javax.speech.EngineStateError;
import javax.speech.synthesis.Synthesizer;
import javax.speech.synthesis.SynthesizerModeDesc;
import javax.speech.synthesis.SynthesizerProperties;
import javax.speech.synthesis.Voice;

import com.sun.speech.freetts.util.Utilities;


/**
 * Manages TTS output
 */
public class CardMatchVoice {

    private Synthesizer synthesizer;
    
    public CardMatchVoice() throws Exception {
	try {
            String synthesizerName = Utilities.getProperty
                ("synthesizerName",
                 "Unlimited domain FreeTTS Speech Synthesizer from Sun Labs");

	    // Create a new SynthesizerModeDesc that will match the FreeTTS
	    // Synthesizer.
	    SynthesizerModeDesc desc = new SynthesizerModeDesc
		(synthesizerName,
		 null,
		 Locale.US,
		 Boolean.FALSE,         // running?
		 null);                 // voice

	    synthesizer = Central.createSynthesizer(desc);

	    if (synthesizer == null) {
		String message = "Can't find synthesizer.\n" +
		    "Make sure that there is a \"speech.properties\" file " +
		    "at either of these locations: \n";
		message += "user.home    : " + 
		    System.getProperty("user.home") + "\n";
		message += "java.home/lib: " + System.getProperty("java.home")
		    + File.separator + "lib\n";
		
		System.err.println(message);
                throw new Exception("JSAPI Synthesizer cannot be constructed");
	    }

            // create the voice
            String voiceName = System.getProperty("voiceName", "kevin16");
	    Voice voice = new Voice
                (voiceName, Voice.GENDER_DONT_CARE, Voice.AGE_DONT_CARE, null);

	    // get it ready to speak
	    synthesizer.allocate();
	    synthesizer.resume();
            synthesizer.getSynthesizerProperties().setVoice(voice);

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Unable to initialize CardMatchVoice");
        }
    }

    /**
     * Speaks the given string of text.
     *
     * @param text the text to speak
     */
    public void speak(String text) {
        if (synthesizer != null) {
            try {
                synthesizer.speakPlainText(text, null);
            } catch (EngineStateError ese) {
                ese.printStackTrace();
            }
        } else {
            System.out.println("CardMatchVoice unavailable");
        }
    }

    /**
     * Shuts down this CardMatchVoice.
     */
    public void close() {
        if (synthesizer != null) {
            try {
                synthesizer.deallocate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
