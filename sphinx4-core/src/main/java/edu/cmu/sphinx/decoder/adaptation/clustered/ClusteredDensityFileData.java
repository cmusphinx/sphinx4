package edu.cmu.sphinx.decoder.adaptation.clustered;

import java.util.ArrayList;
import java.util.Random;

import org.apache.commons.math3.util.FastMath;

import edu.cmu.sphinx.linguist.acoustic.tiedstate.Pool;

/**
 * Used for clustering gaussians. The clustering is performed by euclidean
 * distance criterium. The "k Means" clustering algorithm is used for clustering
 * the gaussians.
 * 
 * @author Bogdan Petcu
 */
public class ClusteredDensityFileData {

	private Pool<float[]> initialData;
	private int numberOfClusters;
	private ArrayList<ArrayList<Integer>> gaussianNumbers;
	private int[] corespondingClass;
	private int numStates;
	private int numGaussiansPerState;

	public ClusteredDensityFileData(Pool<float[]> initialData,
			int numberOfClusters, int numStates, int numGaussiansPerState) {
		this.initialData = initialData;
		this.numberOfClusters = numberOfClusters;
		this.numStates = numStates;
		this.numGaussiansPerState = numGaussiansPerState;
		this.gaussianNumbers = new ArrayList<ArrayList<Integer>>(
				numberOfClusters);
		for (int i = 0; i < numberOfClusters; i++) {
			this.gaussianNumbers.add(new ArrayList<Integer>(
					numGaussiansPerState * numStates));
		}
		this.kMeansClustering(15);
	}

	public ArrayList<ArrayList<Integer>> getGaussianNumbers() {
		return gaussianNumbers;
	}
	
	/**
	 * Used for accessing the index that is specific to a gaussian.
	 * @param gaussian provided in a i * numStates + gaussianIndex form.
	 * @return
	 */
	public int getClassIndex(int gaussian) {
		return corespondingClass[gaussian];
	}

	/**
	 * Computes euclidean distance between 2 n-dimensional points.
	 * 
	 * @param a
	 *            - n-dimensional "a" point
	 * @param b
	 *            - n-dimensional "b" point
	 * @return the euclidean distance between a and b.
	 */
	private float euclidianDistance(float[] a, float[] b) {
		double s = 0, d;

		for (int i = 0; i < a.length; i++) {
			d = a[i] - b[i];
			s += d * d;
		}

		return (float) FastMath.sqrt(s);
	}

	/**
	 * Checks if the two float array have the same components
	 * 
	 * @param a
	 *            - float array a
	 * @param b
	 *            - float array b
	 * @return true if values from a are equal to the ones in b, else false.
	 */
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

	/**
	 * Performs k-means-clustering algorithm for clustering gaussians.
	 * Clustering is done using euclidean distance criterium.
	 * 
	 * @param maxIterations
	 */
	private void kMeansClustering(int maxIterations) {
		ArrayList<float[]> oldCentroids = new ArrayList<float[]>(
				numberOfClusters);
		ArrayList<float[]> centroids = new ArrayList<float[]>(numberOfClusters);
		int numberOfElements = initialData.size(), nrOfIterations = maxIterations, index;
		int[] count = new int[numberOfClusters];
		double distance, min;
		float[] currentValue, centroid;
		float[][][] array = new float[numberOfClusters][numberOfElements][];
		boolean converged = false;
		Random randomGenerator = new Random();

		for (int i = 0; i < numberOfClusters; i++) {
			index = randomGenerator.nextInt(numberOfElements);
			centroids.add(initialData.get(index));
			oldCentroids.add(initialData.get(index));
			count[i] = 0;
		}

		index = 0;

		while (!converged && nrOfIterations > 0) {

			corespondingClass = new int[numStates * numGaussiansPerState];
			array = new float[numberOfClusters][numberOfElements][];
			this.gaussianNumbers = new ArrayList<ArrayList<Integer>>(
					numberOfClusters);
			for (int i = 0; i < numberOfClusters; i++) {
				this.gaussianNumbers.add(new ArrayList<Integer>(
						numGaussiansPerState * numStates));
			}

			for (int i = 0; i < numberOfClusters; i++) {
				oldCentroids.set(i, centroids.get(i));
				count[i] = 0;
			}

			for (int i = 0; i < numStates; i++) {
				for (int j = 0; j < numGaussiansPerState; j++) {
					currentValue = initialData
							.get(i * numGaussiansPerState + j);
					min = this.euclidianDistance(oldCentroids.get(0),
							currentValue);
					index = 0;

					for (int k = 1; k < numberOfClusters; k++) {
						distance = this.euclidianDistance(oldCentroids.get(k),
								currentValue);

						if (distance < min) {
							min = distance;
							index = k;
						}
					}

					array[index][count[index]] = currentValue;
					this.gaussianNumbers.get(index).add(
							i * numGaussiansPerState + j);
					corespondingClass[i * numGaussiansPerState + j] = index;
					count[index]++;
				}
			}

			for (int i = 0; i < numberOfClusters; i++) {
				centroid = new float[initialData.get(0).length];

				if (count[i] > 0) {

					for (int j = 0; j < count[i]; j++) {
						for (int k = 0; k < initialData.get(0).length; k++) {
							centroid[k] += array[i][j][k];
						}
					}

					for (int k = 0; k < initialData.get(0).length; k++) {
						centroid[k] /= count[i];
					}

					centroids.set(i, centroid);
				}
			}

			converged = true;

			for (int i = 0; i < numberOfClusters; i++) {
				converged = converged
						&& (this.isEqual(centroids.get(i), oldCentroids.get(i)));
			}

			nrOfIterations--;
		}
	}

}
