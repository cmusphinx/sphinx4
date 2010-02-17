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
package edu.cmu.sphinx.result.test;

import edu.cmu.sphinx.frontend.util.StreamDataSource;

import edu.cmu.sphinx.recognizer.Recognizer;

import edu.cmu.sphinx.result.ConfidenceResult;
import edu.cmu.sphinx.result.ConfidenceScorer;
import edu.cmu.sphinx.result.Path;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.result.WordResult;

import edu.cmu.sphinx.util.props.ConfigurationManager;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.Test;

public class MAPConfidenceTest {

	@Test
	public void testMAPConfidence ()
			throws UnsupportedAudioFileException, IOException {
		URL audioFileURL = new File("src/test/edu/cmu/sphinx/result/test/green.wav")
				.toURI().toURL();
		URL configURL = new File("src/test/edu/cmu/sphinx/result/test/config.xml")
				.toURI().toURL();

		ConfigurationManager cm = new ConfigurationManager(configURL);
		Recognizer recognizer = (Recognizer) cm.lookup("recognizer");

		recognizer.allocate();

		StreamDataSource reader = (StreamDataSource) cm
				.lookup("streamDataSource");

		reader.setInputStream(AudioSystem.getAudioInputStream(audioFileURL),
				audioFileURL.getFile());

		Result result = recognizer.recognize();

		if (result != null) {
			ConfidenceScorer cs = (ConfidenceScorer) cm
					.lookup("confidenceScorer");
			ConfidenceResult cr = cs.score(result);
			Path best = cr.getBestHypothesis();

			/* print out confidence of individual words in the best path */
			WordResult[] words = best.getWords();
			for (WordResult wr : words) {
				System.out.println(wr.getPronunciation().getWord()
						.getSpelling());
				System.out.println("   (confidence: "
						+ wr.getLogMath().logToLinear(
								(float) wr.getConfidence()) + ')');
			}

			System.out.println();

			/* confidence of the best path */
			System.out.println(best.getTranscription());
			System.out.println("   (confidence: "
					+ best.getLogMath().logToLinear(
							(float) best.getConfidence()) + ')');
		}
	}
}
