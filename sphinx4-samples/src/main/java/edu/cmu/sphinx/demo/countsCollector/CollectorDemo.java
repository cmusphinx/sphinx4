package edu.cmu.sphinx.demo.countsCollector;

import java.io.InputStream;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.api.StreamSpeechRecognizer;
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
				.getResourceAsStream("/edu/cmu/sphinx/demo/countsCollector/BillGates5Mins.wav");
		recognizer.startRecognition(stream, false);

		SpeechResult result;

		Sphinx3Loader loader = (Sphinx3Loader) recognizer.getLoader();
<<<<<<< HEAD
		MllrEstimation me = new MllrEstimation("", 1,
				"/home/bogdanpetcu/mllrmat", false, "", false, loader);

		while ((result = recognizer.getResult()) != null) {
			me.addCounts(result.getResult());
=======

		MllrEstimation me = new MllrEstimation("/home/bogdanpetcu/billgatesopttest", loader);

		while ((result = recognizer.getResult()) != null) {
			me.collect(result.getResult());
>>>>>>> implemented direct counts collecting in MllrEstimation.java and ClustersEstimation.java. This way the program uses less memory than the previous form when the counts were stored before they were used in computing the transform

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

<<<<<<< HEAD
<<<<<<< HEAD
=======
		MllrEstimation me = new MllrEstimation("", 1,
				"/home/bogdanpetcu/mllr_mat_billgates", false, cc.getCounts(),
				"", false, loader);
=======
>>>>>>> implemented direct counts collecting in MllrEstimation.java and ClustersEstimation.java. This way the program uses less memory than the previous form when the counts were stored before they were used in computing the transform

>>>>>>> updated test files and CollectorDemo
		me.estimateMatrices();
		me.createMllrFile();
	}

}
