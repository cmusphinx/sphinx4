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

import edu.cmu.sphinx.result.Lattice;
import edu.cmu.sphinx.result.LatticeOptimizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.result.Sausage;
import edu.cmu.sphinx.result.SausageMaker;

import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class SausageDumpTest {

    /**
     * Main method for running the LatticeDumpTest demo.
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

            /* allocate the resource necessary for the recognizer */
            recognizer.allocate();

	    StreamDataSource reader
		= (StreamDataSource) cm.lookup("streamDataSource");

            /* set the stream data source to read from the audio file */
	    reader.setInputStream
		(AudioSystem.getAudioInputStream(audioFileURL),
		 audioFileURL.getFile());

            /* decode the audio file */
            Result result = recognizer.recognize();
            
            /* print out the results */
            if (result != null) {
                System.out.println
		    ("\nRESULT: " +result.getBestFinalResultNoFiller() + "\n");
                Lattice lattice = new Lattice(result);
		LatticeOptimizer lo = new LatticeOptimizer(lattice);
		lo.optimize();
                lattice.dumpAISee("lattice.gdl", "lattice");

		float lmw = (float) 7.0;
		try {
		    lmw = cm.getPropertySheet("lexTreeLinguist").getFloat
			("languageWeight", (float) 7.0);
		} catch (PropertyException e1) {
		    System.err.println("couldn't retrieve language weight, using default " + lmw);
		}
		lattice.computeNodePosteriors(lmw);
		try {
		    SausageMaker sm = new SausageMaker(lattice);
		    Sausage sausage = sm.makeSausage();
		    System.out.println
			("best sausage hypothesis: \""
			 + sausage.getBestHypothesisString() + "\"");
		    sausage.dumpAISee("sausage.gdl", "sausage decode");
		    System.out.println("dumped sausage");
		} catch (Exception e) {
		    System.err.println("error making sausage");
		    e.printStackTrace();
		}
            } else {
                System.out.println("Result: null\n");
            }
        } catch (IOException e) {
            System.err.println("Problem when loading LatticeDumpTest: " + e);
            e.printStackTrace();
        } catch (PropertyException e) {
            System.err.println("Problem configuring LatticeDumpTest: " + e);
            e.printStackTrace();
        } catch (InstantiationException e) {
            System.err.println("Problem creating LatticeDumpTest: " + e);
            e.printStackTrace();
        } catch (UnsupportedAudioFileException e) {
            System.err.println("Audio file format not supported: " + e);
            e.printStackTrace();
        }
    }
}
