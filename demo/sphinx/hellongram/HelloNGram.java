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

package demo.sphinx.hellongram;

import edu.cmu.sphinx.frontend.util.Microphone;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;

import java.io.File;
import java.io.IOException;
import java.net.URL;


/**
 * A simple HelloNGram demo showing a simple speech application 
 * built using Sphinx-4. This application uses the Sphinx-4 endpointer,
 * which automatically segments incoming audio into utterances and silences.
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

            System.out.println("Loading...");

            ConfigurationManager cm = new ConfigurationManager(url);

	    Recognizer recognizer = (Recognizer) cm.lookup("recognizer");
	    Microphone microphone = (Microphone) cm.lookup("microphone");

            /* allocate the resource necessary for the recognizer */
            recognizer.allocate();

            printInstructions();

            /* the microphone will keep recording until the program exits */
	    if (microphone.startRecording()) {

		while (true) {
		    System.out.println
			("Start speaking. Press Ctrl-C to quit.\n");

                    /*
                     * This method will return when the end of speech
                     * is reached. Note that the endpointer will determine
                     * the end of speech.
                     */ 
		    Result result = recognizer.recognize();
		    
		    if (result != null) {
			String resultText = result.getBestResultNoFiller();
			System.out.println("You said: " + resultText + "\n");
		    } else {
			System.out.println("I can't hear what you said.\n");
		    }
		}
	    } else {
		System.out.println("Cannot start microphone.");
		recognizer.deallocate();
		System.exit(1);
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

    /**
     * Prints out what to say for this demo.
     */
    private static void printInstructions() {
        System.out.println
            ("Sample sentences:\n" +
             "\n" +
             "the green one right in the middle\n" +
             "the purple one on the lower right side\n" +
             "the closest purple one on the far left side\n" +
             "the only one left on the left\n" +
             "\n" +
             "Refer to the file hellongram.test for a complete list.\n");
    }
}
