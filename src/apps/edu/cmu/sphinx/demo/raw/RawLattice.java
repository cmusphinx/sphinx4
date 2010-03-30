package edu.cmu.sphinx.demo.raw;

import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Lattice;
import edu.cmu.sphinx.result.LatticeOptimizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManagerUtils;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Created by IntelliJ IDEA.
 * User: peter
 * Date: Jan 12, 2010
 * Time: 4:07:48 PM
 * <p/>
 * Copyright 1999-2004 Carnegie Mellon University.
 * Portions Copyright 2004 Sun Microsystems, Inc.
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * <p/>
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 */
public class RawLattice {

    public static void main(String[] args) throws MalformedURLException, URISyntaxException, ClassNotFoundException {
        new RawLattice().run(args);
    }

    public void run(String[] args) throws MalformedURLException, URISyntaxException, ClassNotFoundException {
        System.out.println("\nRunning " + getClass().getSimpleName() + "...");

        URL audioURL;
        if (args.length > 2) {
            throw new Error("USAGE: " + getClass().getSimpleName() + " [<WAV file>]");
        }
        else if(args.length == 2) {
            audioURL = new URL(args[1]);
        }
        else {
            audioURL = ConfigurationManagerUtils.resourceToURL("resource:/edu/cmu/sphinx/demo/transcriber/10001-90210-01803.wav");
        }

        // get a recognizer configured for the HelloNGram task
        LatticeConfiguration config = new LatticeConfiguration();

        // allocate the resource necessary for the recognizer
        System.out.println("Loading...");
        Recognizer recognizer = config.getRecognizer();
        recognizer.allocate();

        // configure the audio input for the recognizer
        AudioFileDataSource audioSource = config.getAudioFileDataSource();
        audioSource.setAudioFile(audioURL, null);

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
                System.out.println("I heard: " + resultText + '\n');
            } else {
                done = true;
            }
        }

    }
}
