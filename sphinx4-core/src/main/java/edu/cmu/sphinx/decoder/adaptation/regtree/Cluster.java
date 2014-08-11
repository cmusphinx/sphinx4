package edu.cmu.sphinx.decoder.adaptation.regtree;

import java.util.ArrayList;
import java.util.Random;

import org.apache.commons.math3.util.FastMath;

import edu.cmu.sphinx.linguist.acoustic.tiedstate.Pool;

public class Cluster {

	private Pool<float[]> values;

	public Cluster(Pool<float[]> values) {
		this.values = values;
	}

	public Pool<float[]> getValues() {
		return this.values;
	}

	private float euclidianDistance(float[] a, float[] b) {
		double s = 0, d;

		for (int i = 0; i < a.length; i++) {
			d = a[i] - b[i];
			s += d * d;
		}

		return (float) FastMath.sqrt(s);
	}

	private boolean isEqual(float[] a, float[] b) {
		if (a.length != b.length) {
			return false;
		}

		for (int i = 0; i < a.length; i++) {
			if (a[i] != b[i]) {
				return false;
			}
		}

		return true;
	}

	public ArrayList<Cluster> kMeansClustering(int nrOfClusters,
			int maxIterations) {
		ArrayList<Cluster> clusters = new ArrayList<Cluster>(nrOfClusters);
		ArrayList<float[]> oldCentroids = new ArrayList<float[]>(nrOfClusters);
		ArrayList<float[]> centroids = new ArrayList<float[]>(nrOfClusters);
		int numberOfElements = values.size(), nrOfIterations = maxIterations, index;
		int[] count = new int[nrOfClusters];
		double distance, min;
		float[] currentValue, centroid;
		float[][][] array = new float[nrOfClusters][numberOfElements][];
		boolean converged = false;
		Random randomGenerator = new Random();

		for (int i = 0; i < nrOfClusters; i++) {
			index = randomGenerator.nextInt(numberOfElements);
			centroids.add(values.get(index));
			oldCentroids.add(values.get(index));
			count[i] = 0;
		}

		index = 0;

		while (!converged && nrOfIterations > 0) {

			array = new float[nrOfClusters][numberOfElements][];

			for (int i = 0; i < nrOfClusters; i++) {
				oldCentroids.set(i, centroids.get(i));
				count[i] = 0;
			}

			for (int i = 0; i < numberOfElements; i++) {
				currentValue = values.get(i);
				min = this.euclidianDistance(oldCentroids.get(0), currentValue);
				index = 0;

				for (int j = 1; j < nrOfClusters; j++) {
					distance = this.euclidianDistance(oldCentroids.get(j),
							currentValue);

					if (distance < min) {
						min = distance;
						index = j;
					}

				}

				array[index][count[index]] = currentValue;
				count[index]++;

			}

			for (int i = 0; i < nrOfClusters; i++) {
				centroid = new float[values.get(0).length];

				if (count[i] > 0) {

					for (int j = 0; j < count[i]; j++) {
						for (int k = 0; k < values.get(0).length; k++) {
							centroid[k] += array[i][j][k];
						}
					}

					for (int k = 0; k < values.get(0).length; k++) {
						centroid[k] /= count[i];
					}

					centroids.set(i, centroid);
				}
			}

			converged = true;

			for (int i = 0; i < nrOfClusters; i++) {
				converged = converged
						&& (this.isEqual(centroids.get(i), oldCentroids.get(i)));
			}

			nrOfIterations--;
		}

		for (int i = 0; i < nrOfClusters; i++) {
			Pool<float[]> values = new Pool<float[]>("values");

			for (int j = 0; j < count[i]; j++) {
				values.put(j, array[i][j]);
			}

			clusters.add(new Cluster(values));
		}

		return clusters;
	}
}
