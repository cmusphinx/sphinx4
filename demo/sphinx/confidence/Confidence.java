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
package demo.sphinx.confidence;

import edu.cmu.sphinx.frontend.util.Microphone;

import edu.cmu.sphinx.recognizer.Recognizer;

import edu.cmu.sphinx.result.ConfidenceResult;
import edu.cmu.sphinx.result.ConfidenceScorer;
import edu.cmu.sphinx.result.Lattice;
import edu.cmu.sphinx.result.LatticeOptimizer;
import edu.cmu.sphinx.result.MAPConfidenceScorer;
import edu.cmu.sphinx.result.Path;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.result.WordResult;

import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;

import java.io.File;
import java.io.IOException;

import java.net.URL;

import java.text.DecimalFormat;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class Confidence {

    private static DecimalFormat format = new DecimalFormat("#.#####");

    /**
     * Main method for running the Confidence demo.
     */
    public static void main(String[] args) {
        try {            
            URL configURL;

            if (args.length > 0) {
                configURL = new File(args[0]).toURI().toURL();
            } else {
                configURL = Confidence.class.getResource("config.xml");
            }

            System.out.println("Loading Recognizer...\n");

            ConfigurationManager cm = new ConfigurationManager(configURL);
	    Recognizer recognizer = (Recognizer) cm.lookup("recognizer");

            /* allocate the resource necessary for the recognizer */
            recognizer.allocate();

            printInstructions();

	    Microphone microphone = (Microphone) cm.lookup("microphone");

            if (microphone.startRecording()) {

                while (true) {

                    System.out.println
                        ("Start speaking. Press Ctrl-C to quit.\n");

                    /* decode the audio file */
                    Result result = recognizer.recognize();
                    
                    if (result != null) {

                        ConfidenceScorer cs = (ConfidenceScorer) cm.lookup
                            ("confidenceScorer");
                        ConfidenceResult cr = cs.score(result);
                        Path best = cr.getBestHypothesis();

                        /* confidence of the best path */
                        System.out.println(best.getTranscription());
                        System.out.println
                            ("     (confidence: " +
                             format.format(best.getLogMath().logToLinear
                                           ((float)best.getConfidence()))
                             + ")");
                        System.out.println();
                        
                        /*
                         * print out confidence of individual words 
                         * in the best path
                         */
                        WordResult[] words = best.getWords();
                        for (int i = 0; i < words.length; i++) {
                            WordResult wr = (WordResult) words[i];
                            printWordConfidence(wr);
                        }
                        System.out.println();
                    }
                }
            } else {
                System.out.println("Cannot start microphone.");
                recognizer.deallocate();
                System.exit(1);
            }            
        } catch (IOException e) {
            System.err.println("Problem when loading Confidence: " + e);
            e.printStackTrace();
        } catch (PropertyException e) {
            System.err.println("Problem configuring Confidence: " + e);
            e.printStackTrace();
        } catch (InstantiationException e) {
            System.err.println("Problem creating Confidence: " + e);
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
             "Refer to the file confidence.test for a complete list.\n");
    }

    /**
     * Prints out the word and its confidence score.
     */
    private static void printWordConfidence(WordResult wr) {
        String word = wr.getPronunciation().getWord().getSpelling();

        System.out.print(word);

        /* pad spaces between the word and its score */
        int entirePadLength = 10;
        if (word.length() < entirePadLength) {
            for (int i = word.length(); i < entirePadLength; i++) {
                System.out.print(" ");
            }
        }

        System.out.println
            (" (confidence: " +
             format.format
             (wr.getLogMath().logToLinear((float)wr.getConfidence())) + ")");
    }
}
