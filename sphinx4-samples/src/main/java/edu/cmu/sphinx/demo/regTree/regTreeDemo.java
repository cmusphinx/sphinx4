package edu.cmu.sphinx.demo.regTree;

import java.io.InputStream;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.api.StreamSpeechRecognizer;
import edu.cmu.sphinx.decoder.adaptation.CountsCollector;
import edu.cmu.sphinx.decoder.adaptation.regtree.ClusteredDensityFileData;
import edu.cmu.sphinx.decoder.adaptation.regtree.RegTreeEstimation;
import edu.cmu.sphinx.demo.transcriber.TranscriberDemo;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Sphinx3Loader;
import edu.cmu.sphinx.result.WordResult;

public class regTreeDemo {

	public static void main(String[] args) throws Exception {

		Configuration configuration = new Configuration();

		configuration.setAcousticModelPath("/home/bogdanpetcu/RSoC/en-us");
		configuration
				.setDictionaryPath("/home/bogdanpetcu/RSoC/wsj/dict/cmudict.0.6d");
		configuration
				.setLanguageModelPath("/home/bogdanpetcu/RSoC/sphinx4/sphinx4-data/src/main/resources/edu/cmu/sphinx/models/language/en-us.lm.dmp");

		StreamSpeechRecognizer recognizer = new StreamSpeechRecognizer(
				configuration);

		InputStream stream = TranscriberDemo.class
				.getResourceAsStream("/edu/cmu/sphinx/demo/countsCollector/BillGates_2010_554.62_564.32.wav");
		recognizer.startRecognition(stream, false);

		Sphinx3Loader loader = (Sphinx3Loader) recognizer.getLoader();

		ClusteredDensityFileData cm = new ClusteredDensityFileData(loader.getMeansPool(), 10,
				loader.getNumStates(), loader.getNumGaussiansPerState());

		CountsCollector cc = new CountsCollector(loader.getVectorLength(),
				loader.getNumStates(), loader.getNumStreams(),
				loader.getNumGaussiansPerState());

		SpeechResult result;

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
		
		RegTreeEstimation regTreeEstimation = new RegTreeEstimation(cc.getCounts(), cm, 10, loader);
		regTreeEstimation.estimate();
	
	}

}
