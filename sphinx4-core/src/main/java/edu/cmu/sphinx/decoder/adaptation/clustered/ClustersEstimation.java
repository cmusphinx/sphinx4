package edu.cmu.sphinx.decoder.adaptation.clustered;

import java.util.ArrayList;

import edu.cmu.sphinx.decoder.adaptation.Counts;
import edu.cmu.sphinx.decoder.adaptation.MllrEstimation;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Sphinx3Loader;

public class ClustersEstimation {

	private ClusteredDensityFileData cm;
	private float[][][][] As;
	private float[][][] Bs;
	private int k;
	private Counts counts;
	Sphinx3Loader loader;
	private int numStates;
	private int numStreams;
	private int numGaussinsPerState;
	private int[] vectorLength;

	public ClustersEstimation(Counts counts, ClusteredDensityFileData cm,
			int k, Sphinx3Loader loader) {
		this.counts = counts;
		this.cm = cm;
		this.k = k;
		this.loader = loader;
		this.vectorLength = loader.getVectorLength();
		this.numStates = loader.getNumStates();
		this.numStreams = loader.getNumStreams();
		this.numGaussinsPerState = loader.getNumGaussiansPerState();
		As = new float[k][][][];
		Bs = new float[k][][];
	}

	public float[][][][] getAs() {
		return As;
	}

	public float[][][] getBs() {
		return Bs;
	}

	public ClusteredDensityFileData getClusteredData() {
		return this.cm;
	}

	private Counts getClusterCounts(int k) {
		Counts clusterCounts = new Counts(vectorLength, numStates, numStreams,
				numGaussinsPerState);
		ArrayList<Integer> gaussianNumbers = cm.getGaussianNumbers().get(k);
		float[][][][] mean = new float[numStates][numStreams][numGaussinsPerState][vectorLength[0]];
		float[][][] dnom = new float[numStates][numStreams][numGaussinsPerState];
		int stateIndex, gaussianIndex;

		for (int id : gaussianNumbers) {
			stateIndex = id / numGaussinsPerState;
			gaussianIndex = id % numGaussinsPerState;
			mean[stateIndex][0][gaussianIndex] = counts.getMean()[stateIndex][0][gaussianIndex];
			dnom[stateIndex][0][gaussianIndex] = counts.getDnom()[stateIndex][0][gaussianIndex];
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
			estimation.setClassEstimation(true, this.cm.getGaussianNumbers()
					.get(i));
			estimation.estimateMatrices();
			As[i] = estimation.getA();
			Bs[i] = estimation.getB();
		}
	}

}
