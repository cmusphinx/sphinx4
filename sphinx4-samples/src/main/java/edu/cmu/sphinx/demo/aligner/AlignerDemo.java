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

import java.net.URL;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.SpeechAligner;

import edu.cmu.sphinx.result.WordResult;

/**
 * This class demonstrates how to align audio to existing transcription and
 * receive word timestamps.
 *
 * <br/>
 * In order to initialize the aligner you need to specify several data files
 * which might be downloaded from the CMUSphinx website. There should be an
 * acoustic model for your lanaguage, a dictionary an optional G2P model to
 * convert word strings to pronunciation.
 * <br/>
 * Currently the audio must have specific format (16khz, 16bit, mono), but in
 * the future other formats will be supported.
 * <br/>
 * Text should be a clean text in lower case. It should be cleaned from
 * punctuation marks, numbers and other non-speakable things. In the future
 * automatic cleanup will be supported.
 */
public class AlignerDemo {

    private static final String TEXT =
        "one zero zero zero one nine oh two one oh zero one eight zero three";
    private static final String AUDIO_PATH =
        "file:src/apps/edu/cmu/sphinx/demo/aligner/10001-90210-01803.wav";

    public static void main(String Args[]) throws Exception {
        Configuration config = new Configuration();
        config.setAcousticModelPath("file:models/acoustic/wsj");
        config.setDictionaryPath("file:models/acoustic/wsj/dict/cmudict.0.6d");
        SpeechAligner aligner = new SpeechAligner(config);

        for (WordResult result : aligner.align(new URL(AUDIO_PATH), TEXT))
            System.out.println(result);
    }
}
