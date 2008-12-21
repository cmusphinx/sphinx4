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

package edu.cmu.sphinx.demo.wavfile;

import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;


/** A simple Sphinx-4 application that decodes a .WAV file containing connnected-digits audio data. */
public class WavFile {

    public static void main(String[] args) throws MalformedURLException {
        URL audioFileURL;
        URL configURL;

        // use defaults that are loaded from the WavFile.jar or use values provided as arguments to main
        if (args.length == 2) {
            configURL = new File(args[0]).toURI().toURL();
            audioFileURL = new File(args[1]).toURI().toURL();
        } else {
            audioFileURL = WavFile.class.getResource("12345.wav");
            configURL = WavFile.class.getResource("config.xml");
        }

        System.out.println("Loading Recognizer as defined in '" + configURL.toString() + "'...\n");
        ConfigurationManager cm = new ConfigurationManager(configURL);

        // look up the recognizer (which will also lookup all its dependencies
        Recognizer recognizer = (Recognizer) cm.lookup("recognizer");
        recognizer.allocate();

        // configure the audio input for the recognizer
        AudioFileDataSource dataSource = (AudioFileDataSource) cm.lookup("audioFileDataSource");
        dataSource.setAudioFile(audioFileURL, null);

        // decode the audio file.
        System.out.println("Decoding " + audioFileURL);
        Result result = recognizer.recognize();

        System.out.println("Result: " + (result != null ? result.getBestFinalResultNoFiller() : null));
    }
}
