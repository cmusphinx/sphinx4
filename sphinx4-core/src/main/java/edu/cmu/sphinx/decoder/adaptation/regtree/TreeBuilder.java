package edu.cmu.sphinx.decoder.adaptation.regtree;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.StreamSpeechRecognizer;

public class TreeBuilder {

	private Tree tree;
	
	public Tree getTree(){
		return this.tree;
	}

	public static void main(String[] args) throws IOException {
		Configuration configuration = new Configuration();

		configuration.setAcousticModelPath("/home/bogdanpetcu/RSoC/en-us");
		configuration
				.setDictionaryPath("/home/bogdanpetcu/RSoC/wsj/dict/cmudict.0.6d");
		configuration
				.setLanguageModelPath("/home/bogdanpetcu/RSoC/sphinx4/sphinx4-data/src/main/resources/edu/cmu/sphinx/models/language/en-us.lm.dmp");

		StreamSpeechRecognizer recognizer = new StreamSpeechRecognizer(
				configuration);

		InputStream stream = TreeBuilder.class
				.getResourceAsStream("/home/bogdanpetcu/RSoC/adaptation-test/BillGates_2010/out.wav");
		recognizer.startRecognition(stream, false);
		
		Cluster initialMeans = new Cluster(recognizer.getLoader().getMeansPool());
		ArrayList<Cluster> clusters = initialMeans.kMeansClustering(10, 15);
		System.out.println(clusters.size());
	}
}
