/*
 * Copyright 1999-2004 Carnegie Mellon University.
 * Portions Copyright 2004 Sun Microsystems, Inc.
 * Portions Copyright 2004 Mitsubishi Electronic Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */

package demo.sphinx.helloworld;

import edu.cmu.sphinx.frontend.util.Microphone;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;

import java.io.File;
import java.io.IOException;
import java.net.URL;


/**
 * A version of the HelloNGram demo that does not use endpointing.
 * Instead, the user will press ENTER to signal the end of speech.
 */
public class HelloNGram {

    /**
     * Main method for running the HelloNGram demo.
     */
    public static void main(String[] args) {
        try {
            URL url;
            if (args.length > 0) {
                url = new File(args[0]).toURI().toURL();
            } else {
                url = HelloNGram.class.getResource("hellongram.config.xml");
            }

            ConfigurationManager cm = new ConfigurationManager(url);

	    Recognizer recognizer = (Recognizer) cm.lookup("recognizer");
	    final Microphone microphone = 
                (Microphone) cm.lookup("microphone");


            /* allocate the resource necessary for the recognizer */
            recognizer.allocate();

            /*
             * This thread is used to listen for the ENTER key,
             * which when pressed will stop the microphone.
             */
	    Thread t = new Thread() {
		    public void run() {
			try {
			    while (true) {
				if (System.in.read() == 10) {
				    microphone.stopRecording();
				}
			    }			    
			} catch (IOException ioe) {
			    ioe.printStackTrace();
			    System.exit(1);
			}
		    }
		};                    
	    t.start();

	    System.out.println
		("Say any digit(s): e.g. \"two oh oh four\", " +
		 "\"three six five\".");

	    /*
             * The program now enters a loop to keep listening for audio,
             * and decodes the recorded audio.
             */
            while (true) {
		if (microphone.startRecording()) {
                    System.out.println
			("Start speaking. " +
			 "Press ENTER when you finish speaking.");
                    
                    /* 
                     * this method returns when the ENTER key is pressed,
                     * which stops the microphone
                     */
		    Result result = recognizer.recognize();
                    
                    if (result != null) {
                        String resultText = result.getBestResultNoFiller();
                        System.out.println("You said: " + resultText + "\n");
                    } else {
                        System.out.println("I can't hear what you said.\n");
                    }                    
                } else {
                    System.out.println("Cannot start microphone.");
                    recognizer.deallocate();
                    System.exit(1);
                }
            }
        } catch (IOException e) {
            System.err.println("Problem when loading HelloNGram: " + e);
            e.printStackTrace();
        } catch (PropertyException e) {
            System.err.println("Problem configuring HelloNGram: " + e);
            e.printStackTrace();
        } catch (InstantiationException e) {
            System.err.println("Problem creating HelloNGram: " + e);
            e.printStackTrace();
        }
    }
}
