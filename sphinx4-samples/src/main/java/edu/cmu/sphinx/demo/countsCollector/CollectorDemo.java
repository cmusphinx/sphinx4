package edu.cmu.sphinx.demo.countsCollector;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.api.StreamSpeechRecognizer;
import edu.cmu.sphinx.decoder.adaptation.MllrEstimation;
import edu.cmu.sphinx.demo.transcriber.TranscriberDemo;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.result.WordResult;

public class CollectorDemo {

	public static void main(String[] args) throws Exception {

		Configuration configuration = new Configuration();
		LinkedList<Result> results = new LinkedList<Result>();

		configuration
				.setAcousticModelPath("resource:/edu/cmu/sphinx/models/acoustic/wsj");
		configuration
				.setDictionaryPath("resource:/edu/cmu/sphinx/models/acoustic/wsj/dict/cmudict.0.6d");
		configuration
				.setLanguageModelPath("resource:/edu/cmu/sphinx/models/language/en-us.lm.dmp");

		StreamSpeechRecognizer recognizer = new StreamSpeechRecognizer(
				configuration);
		InputStream stream = TranscriberDemo.class
				.getResourceAsStream("/edu/cmu/sphinx/demo/aligner/10001-90210-01803.wav");
		recognizer.startRecognition(stream);

		SpeechResult result;

		while ((result = recognizer.getResult()) != null) {
			results.add(result.getResult());
			System.out.format("Hypothesis: %s\n", result.getHypothesis());

			System.out.println("List of recognized words and their times:");
			for (WordResult r : result.getWords()) {
				System.out.println(r);
			}

			System.out.println("Best 3 hypothesis:");
			for (String s : result.getNbest(3))
				System.out.println(s);

			System.out.println("Lattice contains "
					+ result.getLattice().getNodes().size() + " nodes");
		}

		recognizer.stopRecognition();
		MllrEstimation me = new MllrEstimation("/home/bogdanpetcu/RSoC/wsj",
				"", 1, "/home/bogdanpetcu/mllr_mat", false, results);
		me.estimateMatrices();
		me.createMllrFile();
	}

}