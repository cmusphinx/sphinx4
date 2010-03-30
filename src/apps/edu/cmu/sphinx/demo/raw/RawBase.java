package edu.cmu.sphinx.demo.raw;

import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManagerUtils;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Created by IntelliJ IDEA.
 * User: peter
 * Date: Jan 12, 2010
 * Time: 4:46:46 PM
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
abstract public class RawBase {

    abstract protected CommonConfiguration getConfiguration() throws MalformedURLException, URISyntaxException, ClassNotFoundException;

    protected URL getAudio(String[] args) throws MalformedURLException, ClassNotFoundException {
        if (args.length > 2) {
            throw new Error("USAGE: " + getClass().getSimpleName() + " [<WAV file>]");
        }
        else if(args.length == 2) {
            return new URL(args[1]);
        }
        else {
            return ConfigurationManagerUtils.resourceToURL("resource:/edu/cmu/sphinx/demo/transcriber/10001-90210-01803.wav");
        }
    }

    protected void handleResult(Result result) {
        String resultText = result.getBestResultNoFiller();
        System.out.println(resultText);
    }


    public void run(String[] args) throws MalformedURLException, URISyntaxException, ClassNotFoundException {
        System.out.println("\nRunning " + getClass().getSimpleName() + "...");

        URL audioURL = getAudio(args);

        // get a recognizer configured for the HelloNGram task
        CommonConfiguration config = getConfiguration();


        // allocate the resource necessary for the recognizer
        System.out.println("Loading...");
        Recognizer recognizer = config.getRecognizer();
        recognizer.allocate();

        // configure the audio input for the recognizer
        AudioFileDataSource audioSource = config.getAudioFileDataSource();
        audioSource.setAudioFile(audioURL, null);

        // Loop until last utterance in the audio file has been decoded, in which case the recognizer will return null.
        Result result;
        while ((result = recognizer.recognize()) != null) {
            handleResult(result);
        }
    }

}
