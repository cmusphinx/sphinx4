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

package edu.cmu.sphinx.demo.transcriber;

import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;
import org.junit.Assert;
import org.junit.Test;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/** A simple example that shows how to transcribe a continuous audio file that has multiple utterances in it. */
public class Transcriber {

    private static List<Result> unitTestBuffer = new ArrayList<Result>();


    /** Main method for running the HelloDigits demo. */
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
             * This while loop will terminate after the last utterance
             * in the audio file has been decoded, in which case the
             * recognizer will return null.
             */
                Result result = recognizer.recognize();
                if (result != null) {
                    String resultText = result.getBestResultNoFiller();
                    System.out.println(resultText);
                    unitTestBuffer.add(result);
                } else {
                    done = true;
                }
            }
        } catch (IOException e) {
            System.err.println("Problem when loading Transcriber: " + e);
            throw new RuntimeException(e);
        } catch (PropertyException e) {
            System.err.println("Problem configuring Transcriber: " + e);
            throw new RuntimeException(e);
        } catch (UnsupportedAudioFileException e) {
            System.err.println("Audio file format not supported.");
            throw new RuntimeException(e);
        }
    }


    /** Converts this demo into a unit-test. */
    @Test
    public void testLatticeDemo() {
        try {
            Transcriber.main(new String[]{});
        } catch (Throwable t) {
            Assert.fail();
        }

        List<String> expResults = Arrays.asList("zero zero zero", "two one oh", "zero one eight zero three");
        Assert.assertTrue(unitTestBuffer.size() == expResults.size());
        for (int i = 0; i < expResults.size(); i++) {
            String recogResult = unitTestBuffer.get(i).getBestResultNoFiller();
            Assert.assertEquals(expResults.get(i), recogResult);
        }

        System.out.println("finished");
    }
}
