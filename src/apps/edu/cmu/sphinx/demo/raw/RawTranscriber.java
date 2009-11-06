package edu.cmu.sphinx.demo.raw;

import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.demo.raw.TranscriberConfiguration;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * User: peter
 * Date: Nov 2, 2009
 * Time: 2:38:51 PM
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
public class RawTranscriber {

    public static void main(String[] args) throws IOException, UnsupportedAudioFileException {
        run(args);
    }

    public static void run(String[] args) throws MalformedURLException {
        System.out.println("\nRunning RawTranscriber...");

        if( args.length < 1 ) {
            throw new Error( "USAGE: RawTranscriber <sphinx4 root> [<WAV file>]" );
        }

        String root = args[0];

        URL audioURL = (args.length > 1) ?
            new File(args[0]).toURI().toURL() :
            new URL("file:"+root+"/src/apps/edu/cmu/sphinx/demo/transcriber/10001-90210-01803.wav");

        // get a recognizer configurred for the Transcriber task
        TranscriberConfiguration config = new TranscriberConfiguration(root);
        Recognizer recognizer = config.getRecognizer();

        // allocate the resource necessary for the recognizer
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
