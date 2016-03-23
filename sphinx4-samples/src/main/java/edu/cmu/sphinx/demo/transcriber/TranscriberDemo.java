/*
 * Copyright 1999-2013 Carnegie Mellon University.
 * Portions Copyright 2004 Sun Microsystems, Inc.
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 */

package edu.cmu.sphinx.demo.transcriber;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.api.StreamSpeechRecognizer;
import edu.cmu.sphinx.decoder.adaptation.Stats;
import edu.cmu.sphinx.decoder.adaptation.Transform;
import edu.cmu.sphinx.result.WordResult;

import static java.lang.System.out;

/**
 * A simple example that shows how to transcribe a continuous audio file that
 * has multiple utterances in it.
 */
public class TranscriberDemo {

    public static void main(String[] args) throws Exception {
        out.println("Loading models...");

        Configuration conf = new Configuration();

        // Load model from the jar
        conf.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");

        // You can also load model from folder
        // conf.setAcousticModelPath("file:en-us");

        conf.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
        conf.setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin");

        StreamSpeechRecognizer recognizer = new StreamSpeechRecognizer(conf);
        String sample_10001_90210_01803_wav = "/edu/cmu/sphinx/demo/aligner/10001-90210-01803.wav";
        String localFilename = System.getProperty("localFilename", "");
        InputStream stream  = localFilename.isEmpty() ?
            getResourceAsStream(sample_10001_90210_01803_wav) : Files.newInputStream(Paths.get(localFilename));
        stream.skip(44);

        // Simple recognition with generic model
        recognizer.startRecognition(stream);
        SpeechResult result;
        while ((result = recognizer.getResult()) != null) {
            out.format("Hypothesis1: %s\n", result.getHypothesis());
            out.println("List of recognized words and their times:");
            for (WordResult r : result.getWords()) {
                out.println(r);
            }
            out.println("Best 3 hypothesis:");
            for (String s : result.getNbest(3))
                out.println(s);

        }
        recognizer.stopRecognition();

        // Live adaptation to speaker with speaker profiles

        stream = getResourceAsStream(sample_10001_90210_01803_wav);
        stream.skip(44);

        // Stats class is used to collect speaker-specific data
        Stats stats = recognizer.createStats(1);
        recognizer.startRecognition(stream);
        while ((result = recognizer.getResult()) != null) {
            out.format("Stats: %s\n", result.getHypothesis());
            stats.collect(result);
        }
        recognizer.stopRecognition();

        // Transform represents the speech profile
        Transform transform = stats.createTransform();
        recognizer.setTransform(transform);

        // Decode again with updated transform
        stream = getResourceAsStream(sample_10001_90210_01803_wav);
        stream.skip(44);
        recognizer.startRecognition(stream);
        while ((result = recognizer.getResult()) != null) {
            out.format("Hypothesis2: %s\n", result.getHypothesis());
        }
        recognizer.stopRecognition();

    }

    private static InputStream getResourceAsStream(String resource) {
        return TranscriberDemo.class.getResourceAsStream(resource);
    }
}
