package edu.cmu.sphinx.decoder.adaptation.regtree;

import java.util.ArrayList;

import edu.cmu.sphinx.decoder.adaptation.Counts;
import edu.cmu.sphinx.decoder.adaptation.MllrEstimation;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Sphinx3Loader;

public class RegTreeEstimation {

	private ClusteredDensityFileData cm;
	private float[][][][][] As;
	private float[][][][] Bs;
	private int k;
	private Counts counts;
	Sphinx3Loader loader;
	private int numStates;
	private int numStreams;
	private int numGaussinsPerState;
	private int[] vectorLength;

	public RegTreeEstimation(Counts counts, ClusteredDensityFileData cm, int k,
			Sphinx3Loader loader) {
		this.counts = counts;
		this.cm = cm;
		this.k = k;
		this.loader = loader;
		this.vectorLength = loader.getVectorLength();
		this.numStates = loader.getNumStates();
		this.numStreams = loader.getNumStreams();
		this.numGaussinsPerState = loader.getNumGaussiansPerState();
		As = new float[k][][][][];
		Bs = new float[k][][][];
	}
	
	public float[][][][][] getAs() {
		return As;
	}

	public float[][][][] getBs() {
		return Bs;
	}

	private Counts getClusterCounts(int k) {
		Counts clusterCounts = new Counts(vectorLength, numStates, numStreams,
				numGaussinsPerState);
		ArrayList<Integer> stateNumbers = cm.getStateNumbers().get(k);
		float[][][][] mean = new float[numStates][numStreams][numGaussinsPerState][vectorLength[0]];
		float[][][] dnom = new float[numStates][numStreams][numGaussinsPerState];

		for (int id : stateNumbers) {
			mean[id] = counts.getMean()[id];
			dnom[id] = counts.getDnom()[id];
		}

		clusterCounts.setDnom(dnom);
		clusterCounts.setMean(mean);

		return clusterCounts;
	}

	public void estimate() throws Exception {
		for (int i = 0; i < k; i++) {
			Counts clusterCounts;
			MllrEstimation estimation;

			clusterCounts = this.getClusterCounts(i);
			estimation = new MllrEstimation("", 1, "", false, clusterCounts,
					"", false, loader);
			estimation.estimateMatrices();
			As[i] = estimation.getA();
			Bs[i] = estimation.getB();
		}
	}

}
