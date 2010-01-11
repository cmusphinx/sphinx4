package edu.cmu.sphinx.demo.raw;

import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManagerUtils;

import java.net.URISyntaxException;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.File;

/**
 * User: peter
 * Date: Nov 6, 2009
 * Time: 8:49:32 AM
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
public class RawHelloNGram {
    public static void main(String[] args) throws MalformedURLException, URISyntaxException, ClassNotFoundException {
        run(args);
    }

    public static void run(String[] args) throws MalformedURLException, URISyntaxException, ClassNotFoundException {
        System.out.println("\nRunning RawHelloNGram...");

        URL audioURL;
        if (args.length > 2) {
            throw new Error("USAGE: RawTranscriber [<WAV file>]");
        }
        else if(args.length == 2) {
            audioURL = new URL(args[1]);
        }
        else {
            audioURL = ConfigurationManagerUtils.resourceToURL("resource:/edu/cmu/sphinx/demo/transcriber/10001-90210-01803.wav");
        }

        // get a recognizer configurred for the HelloNGram task
        HelloNGramConfiguration config = new HelloNGramConfiguration();

        // allocate the resource necessary for the recognizer
        System.out.println("Loading...");
        Recognizer recognizer = config.getRecognizer();
        recognizer.allocate();

        // configure the audio input for the recognizer
        AudioFileDataSource audioSource = config.getAudioFileDataSource();
        audioSource.setAudioFile(audioURL, null);

        // Loop unitl last utterance in the audio file has been decoded, in which case the recognizer will return null.
        Result result;
        while ((result = recognizer.recognize()) != null) {
            String resultText = result.getBestResultNoFiller();
            System.out.println(resultText);
        }
    }
}
