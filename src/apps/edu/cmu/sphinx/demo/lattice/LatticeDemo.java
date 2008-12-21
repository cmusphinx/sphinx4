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

package edu.cmu.sphinx.demo.lattice;

import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Lattice;
import edu.cmu.sphinx.result.LatticeOptimizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.net.URL;

/** A simple Lattice demo showing a simple speech application that generates a Lattice from a recognition result. */
public class LatticeDemo {


    /** Main method for running the Lattice demo. */
    public static void main(String[] args) {
        try {
            URL audioURL;
            if (args.length > 0) {
                audioURL = new File(args[0]).toURI().toURL();
            } else {
                audioURL = LatticeDemo.class.getResource("10001-90210-01803.wav");
            }

            URL url;
            if (args.length > 1) {
                url = new File(args[1]).toURI().toURL();
            } else {
                url = LatticeDemo.class.getResource("config.xml");
            }

            System.out.println("Loading...");

            ConfigurationManager cm = new ConfigurationManager(url);

            Recognizer recognizer = (Recognizer) cm.lookup("recognizer");
            recognizer.allocate();

            AudioInputStream ais = AudioSystem.getAudioInputStream(audioURL);
            StreamDataSource reader =
                    (StreamDataSource) cm.lookup("streamDataSource");
            reader.setInputStream(ais, audioURL.getFile());

            boolean done = false;
            while (!done) {
                /* This method will return when the end of speech
                 * is reached. Note that the endpointer will determine
                 * the end of speech.
                 */
                Result result = recognizer.recognize();

                if (result != null) {
                    Lattice lattice = new Lattice(result);
                    LatticeOptimizer optimizer = new LatticeOptimizer(lattice);
                    optimizer.optimize();
                    lattice.dumpAllPaths();
                    String resultText = result.getBestResultNoFiller();
                    System.out.println("I heard: " + resultText + "\n");
                } else {
                    done = true;
                }
            }
        } catch (IOException e) {
            System.err.println("Problem when loading LatticeDemo: " + e);
            throw new RuntimeException(e);
        } catch (PropertyException e) {
            System.err.println("Problem configuring LatticeDemo: " + e);
            throw new RuntimeException(e);
        } catch (UnsupportedAudioFileException e) {
            System.err.println("Audio file format not supported.");
            throw new RuntimeException(e);
        }
    }


    /** Converts this demo into a unit-test. */
    @Test
    @Ignore(value = "because this test relies on files available only from within CMU")
    public void testLatticeDemo() {
        try {
            main(new String[]{});
        } catch (Throwable t) {
            t.printStackTrace();
            Assert.fail();
        }
    }
}
