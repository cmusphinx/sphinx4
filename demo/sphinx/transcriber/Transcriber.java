/*
 * Copyright 1999-2004 Carnegie Mellon University.
 * Portions Copyright 2004 Sun Microsystems, Inc.
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */

package demo.sphinx.transcriber;

import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;


/**
 * A simple example that shows how to transcribe a continuous audio file
 * that has multiple utterances in it.
 */
public class Transcriber {

    /**
     * Main method for running the HelloDigits demo.
     */
    public static void main(String[] args) {
        try {
            URL audioURL;
            if (args.length > 0) {
                audioURL = new File(args[0]).toURI().toURL();
            } else {
		audioURL = 
		    Transcriber.class.getResource("10001-90210-01803.wav");
            }

	    URL configURL = Transcriber.class.getResource("config.xml");

            ConfigurationManager cm = new ConfigurationManager(configURL);
	    Recognizer recognizer = (Recognizer) cm.lookup("recognizer");

            /* allocate the resource necessary for the recognizer */
            recognizer.allocate();

	    AudioInputStream ais = AudioSystem.getAudioInputStream(audioURL);
	    StreamDataSource reader = 
		(StreamDataSource) cm.lookup("streamDataSource");
	    reader.setInputStream(ais, audioURL.getFile());
	    
	    boolean done = false;
	    while (!done) {
		/*
		 * This method will return when the end of speech
		 * is reached. Note that the endpointer will determine
		 * the end of speech.
		 */ 
		Result result = recognizer.recognize();
		if (result != null) {
		    String resultText = result.getBestResultNoFiller();
		    System.out.println(resultText);
		} else {
		    done = true;
		}
	    }
        } catch (IOException e) {
            System.err.println("Problem when loading Transcriber: " + e);
            e.printStackTrace();
        } catch (PropertyException e) {
            System.err.println("Problem configuring Transcriber: " + e);
            e.printStackTrace();
        } catch (InstantiationException e) {
            System.err.println("Problem creating Transcriber: " + e);
            e.printStackTrace();
        } catch (UnsupportedAudioFileException e) {
	    System.err.println("Audio file format not supported.");
	    e.printStackTrace();
	}
    }
}
