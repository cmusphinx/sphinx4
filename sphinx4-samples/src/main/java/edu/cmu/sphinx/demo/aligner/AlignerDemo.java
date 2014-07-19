/*
 * Copyright 1999-2013 Carnegie Mellon University.
 * Portions Copyright 2004 Sun Microsystems, Inc.
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */
package edu.cmu.sphinx.demo.aligner;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.io.Files.asCharSource;

import java.io.File;
import java.net.URL;

import edu.cmu.sphinx.alignment.SpeechAligner;
import edu.cmu.sphinx.result.WordResult;


/**
 * This class demonstrates how to align audio to existing transcription and receive word timestamps.
 *
 * <br/>
 * In order to initialize the aligner you need to specify several data files which might be
 * downloaded from the CMUSphinx website. There should be an acoustic model for your lanaguage, a
 * dictionary an optional G2P model to convert word strings to pronunciation. <br/>
 * Currently the audio must have specific format (16khz, 16bit, mono), but in the future other
 * formats will be supported. <br/>
 * Text should be a clean text in lower case. It should be cleaned from punctuation marks, numbers
 * and other non-speakable things. In the future automatic cleanup will be supported.
 */
public class AlignerDemo {
    private static final String ACOUSTIC_MODEL_PATH =
            "resource:/edu/cmu/sphinx/models/acoustic/wsj";
    private static final String DICTIONARY_PATH =
            "resource:/edu/cmu/sphinx/models/acoustic/wsj/dict/cmudict.0.6d";
    private static final String TEXT = "one zero zero zero one nine oh two "
            + "one oh zero one eight zero three";

    public static void main(String args[]) throws Exception {
        URL audioUrl;
        String transcript;
        if (args.length > 1) {
            audioUrl = new File(args[0]).toURI().toURL();
            transcript = asCharSource(new File(args[1]), UTF_8).read();
        } else {
            audioUrl = AlignerDemo.class.getResource("10001-90210-01803.wav");
            transcript = TEXT;
        }
        String acousticModelPath =
                (args.length > 2) ? args[2] : ACOUSTIC_MODEL_PATH;
        String dictionaryPath = (args.length > 3) ? args[3] : DICTIONARY_PATH;
        String g2pPath = (args.length > 4) ? args[4] : null;
        SpeechAligner aligner =
                new SpeechAligner(acousticModelPath, dictionaryPath, g2pPath);

        for (WordResult result : aligner.align(audioUrl, transcript)) {
            System.out.format("%-25s [%s]\n",
                              result.getWord().getSpelling(),
                              result.getTimeFrame());
        }
    }
}
