package edu.cmu.sphinx.demo.clusteredTransform;

import java.io.InputStream;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.api.StreamSpeechRecognizer;
import edu.cmu.sphinx.decoder.adaptation.CountsCollector;
import edu.cmu.sphinx.decoder.adaptation.DensityFileData;
import edu.cmu.sphinx.decoder.adaptation.clustered.ClusteredDensityFileData;
import edu.cmu.sphinx.decoder.adaptation.clustered.ClustersEstimation;
import edu.cmu.sphinx.decoder.adaptation.clustered.ClustersTransform;
import edu.cmu.sphinx.demo.transcriber.TranscriberDemo;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Sphinx3Loader;
import edu.cmu.sphinx.result.WordResult;

public class ClusteredTransformDemo {

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
				.getResourceAsStream("/edu/cmu/sphinx/demo/countsCollector/BillGates5Mins.wav");
		recognizer.startRecognition(stream, false);

		Sphinx3Loader loader = (Sphinx3Loader) recognizer.getLoader();

		ClusteredDensityFileData cm = new ClusteredDensityFileData(loader.getMeansPool(), 10,
				loader.getNumStates(), loader.getNumGaussiansPerState());

		SpeechResult result;
		ClustersEstimation regTreeEstimation = new ClustersEstimation(1, loader, 10, cm);
		
		while ((result = recognizer.getResult()) != null) {
			regTreeEstimation.collect(result.getResult());

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
		
	
		regTreeEstimation.estimate();
		
		DensityFileData means = new DensityFileData("", -Float.MAX_VALUE,
				loader, false);
		means.getMeansFromLoader();
		
		ClustersTransform rt = new ClustersTransform(means, "/home/bogdanpetcu/todaystest", 10, regTreeEstimation);
		rt.transform();
		rt.writeToFile();
		
	}

}
