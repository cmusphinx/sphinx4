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

package tests.result.lattice;

import edu.cmu.sphinx.frontend.util.StreamDataSource;

import edu.cmu.sphinx.recognizer.Recognizer;

import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.result.Lattice;
import edu.cmu.sphinx.result.LatticeOptimizer;

import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;


/**
 * Compares the lattices generated when the LexTreeLinguist flag
 * 'keepAllTokens' is turned on/off.
 */
public class LatticeCompTest {

    /**
     * Main method for running the LatticeCompTest demo.
     */
    public static void main(String[] args) {
        try {
            
            URL audioFileURL;
            
            if (args.length > 0) {
                audioFileURL = new File(args[0]).toURI().toURL();
            } else {
                audioFileURL = new File("green.wav").toURI().toURL();
            }

            URL configURL = new File("./config.xml").toURI().toURL();

            System.out.println("Loading Recognizer...\n");

            ConfigurationManager cm = new ConfigurationManager(configURL);

	    Recognizer recognizer = (Recognizer) cm.lookup("recognizer");

	    StreamDataSource reader
                = (StreamDataSource) cm.lookup("streamDataSource");

            AudioInputStream ais 
                = AudioSystem.getAudioInputStream(audioFileURL);

            /* set the stream data source to read from the audio file */
            reader.setInputStream(ais, audioFileURL.getFile());

            /* allocate the resource necessary for the recognizer */
            recognizer.allocate();

            /* decode the audio file */
            Result result = recognizer.recognize();

            /* print out the results */
            System.out.println("\nRESULT: " + 
                               result.getBestFinalResultNoFiller() + "\n");
            Lattice lattice = new Lattice(result);
            lattice.dumpAISee("lattice.gdl", "lattice");
            
            /* recognizer by keeping all tokens */

            URL allConfigURL = new File("./all.config.xml").toURI().toURL();

            System.out.println("Loading Recognizer...\n");

            ConfigurationManager allCM
                = new ConfigurationManager(allConfigURL);

            Recognizer allRecognizer = (Recognizer) allCM.lookup("recognizer");

            allRecognizer.allocate();

            StreamDataSource allReader 
                = (StreamDataSource) allCM.lookup("streamDataSource");
            
            allReader.setInputStream
                (AudioSystem.getAudioInputStream(audioFileURL),
                 audioFileURL.getFile());
            
            Result allResult = allRecognizer.recognize();

            System.out.println("\nRESULT: " +
                               allResult.getBestFinalResultNoFiller() + "\n");
            Lattice allLattice = new Lattice(allResult);
            allLattice.dumpAISee("allLattice.gdl", "All Lattice");
            
            if (lattice.isEquivalent(allLattice)) {
                System.out.println
                    ("The two lattices are equivalent. TEST PASSED.");
            } else {
                System.out.println
                    ("The two lattices are inequivalent. TEST FAILED.");
            }
        } catch (IOException e) {
            System.err.println("Problem when loading LatticeCompTest: " + e);
            e.printStackTrace();
        } catch (PropertyException e) {
            System.err.println("Problem configuring LatticeCompTest: " + e);
            e.printStackTrace();
        } catch (InstantiationException e) {
            System.err.println("Problem creating LatticeCompTest: " + e);
            e.printStackTrace();
        } catch (UnsupportedAudioFileException e) {
            System.err.println("Audio file format not supported: " + e);
            e.printStackTrace();
        }
    }
}
