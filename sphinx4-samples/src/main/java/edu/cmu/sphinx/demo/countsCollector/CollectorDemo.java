package edu.cmu.sphinx.demo.countsCollector;

import java.io.InputStream;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.api.StreamSpeechRecognizer;
import edu.cmu.sphinx.decoder.adaptation.CountsCollector;
import edu.cmu.sphinx.decoder.adaptation.MllrEstimation;
import edu.cmu.sphinx.demo.transcriber.TranscriberDemo;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Sphinx3Loader;
import edu.cmu.sphinx.result.WordResult;

public class CollectorDemo {

	public static void main(String[] args) throws Exception {

		Configuration configuration = new Configuration();

		configuration.setAcousticModelPath("/home/bogdanpetcu/RSoC/en-us");
		configuration
				.setDictionaryPath("resource:/edu/cmu/sphinx/models/acoustic/wsj/dict/cmudict.0.6d");
		configuration
				.setLanguageModelPath("resource:/edu/cmu/sphinx/models/language/en-us.lm.dmp");

		StreamSpeechRecognizer recognizer = new StreamSpeechRecognizer(
				configuration);
		InputStream stream = TranscriberDemo.class
				.getResourceAsStream("/edu/cmu/sphinx/demo/countsCollector/billgates5Mins.wav");
		recognizer.startRecognition(stream, false);

		SpeechResult result;

		Sphinx3Loader loader = (Sphinx3Loader) recognizer.getLoader();

		CountsCollector cc = new CountsCollector(loader.getVectorLength(),
				loader.getNumStates(), loader.getNumStreams(),
				loader.getNumGaussiansPerState());

		while ((result = recognizer.getResult()) != null) {
			cc.collect(result.getResult());

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

		MllrEstimation me = new MllrEstimation("", 1,
				"/home/bogdanpetcu/mllr_mat_billgates", false, cc.getCounts(),
				"", false, loader);

		me.estimateMatrices();
		me.createMllrFile();
	}

}
