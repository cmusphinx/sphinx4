package edu.cmu.sphinx.demo.Transformer;

import java.io.InputStream;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.api.StreamSpeechRecognizer;
import edu.cmu.sphinx.decoder.adaptation.Stats;
import edu.cmu.sphinx.decoder.adaptation.MllrTransformer;
import edu.cmu.sphinx.decoder.adaptation.Transform;
import edu.cmu.sphinx.demo.transcriber.TranscriberDemo;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Sphinx3Loader;
import edu.cmu.sphinx.result.WordResult;

public class MllrTransformerDemo {

	public static void main(String[] args) throws Exception {

		Configuration configuration = new Configuration();

		configuration.setAcousticModelPath("/home/bogdanpetcu/RSoC/en-us");
		configuration
				.setDictionaryPath("/home/bogdanpetcu/RSoC/wsj/dict/cmudict.0.6d");
		configuration
				.setLanguageModelPath("resource:/edu/cmu/sphinx/models/language/en-us.lm.dmp");

		StreamSpeechRecognizer recognizer = new StreamSpeechRecognizer(
				configuration);

		InputStream stream = TranscriberDemo.class
				.getResourceAsStream("/edu/cmu/sphinx/demo/countsCollector/BillGates5Mins.wav");
		recognizer.startRecognition(stream);

		Sphinx3Loader loader = (Sphinx3Loader) recognizer.getLoader();

		SpeechResult result;
		Stats stats = new Stats(1, loader, 10);

		while ((result = recognizer.getResult()) != null) {
			stats.collect(result.getResult());

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

		Transform transform = new Transform(loader, 10);
		
		transform.update(stats);

		MllrTransformer rt = new MllrTransformer(loader, 10, transform);
		
		rt.applyTransform();
		rt.createNewMeansFile("/home/bogdanpetcu/ClusteredTest");

	}

}
