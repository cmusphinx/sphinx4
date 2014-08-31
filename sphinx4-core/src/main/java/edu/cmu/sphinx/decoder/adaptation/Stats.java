package edu.cmu.sphinx.decoder.adaptation;

import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.frontend.FloatData;
import edu.cmu.sphinx.linguist.HMMSearchState;
import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Pool;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Sphinx3Loader;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.LogMath;

/**
 * This class is used for estimating a MLLR transform for each cluster of data.
 * The clustering must be previously performed using
 * ClusteredDensityFileData.java
 * 
 * @author Bogdan Petcu
 */
public class Stats {

	private ClusteredDensityFileData means;
	private double[][][][][] regLs;
	private double[][][][] regRs;
	private int nrOfClusters;
	private Sphinx3Loader s3loader;
	private Pool<float[]> variancePool;
	private float varFlor;
	private LogMath logMath = LogMath.getInstance();

	public Stats(int nMllrClass, Sphinx3Loader loader,
			int nrOfClusters) throws Exception {
		this.s3loader = loader;
		this.nrOfClusters = nrOfClusters;
		this.means = new ClusteredDensityFileData(loader, nrOfClusters);
		this.varFlor = (float) 1e-5;

		if (s3loader == null) {
			throw new Exception("Sphinx3Loader is not set.");
		}

		this.variancePool = s3loader.getVariancePool();
		this.invertVariances();
		this.init();
	}

	private void init() {
		int len = s3loader.getVectorLength()[0];
		this.regLs = new double[nrOfClusters][][][][];
		this.regRs = new double[nrOfClusters][][][];

		for (int i = 0; i < nrOfClusters; i++) {
			this.regLs[i] = new double[s3loader.getNumStreams()][][][];
			this.regRs[i] = new double[s3loader.getNumStreams()][][];

			for (int j = 0; j < s3loader.getNumStreams(); j++) {
				len = s3loader.getVectorLength()[j];
				this.regLs[i][j] = new double[len][len + 1][len + 1];
				this.regRs[i][j] = new double[len][len + 1];
			}
		}
	}

	public ClusteredDensityFileData getClusteredData() {
		return this.means;
	}

	public double[][][][][] getRegLs() {
		return regLs;
	}

	public double[][][][] getRegRs() {
		return regRs;
	}

	/**
	 * Used for inverting variances.
	 */
	protected void invertVariances() {

		for (int i = 0; i < s3loader.getNumStates(); i++) {
			for (int k = 0; k < s3loader.getNumGaussiansPerState(); k++) {
				for (int l = 0; l < s3loader.getVectorLength()[0]; l++) {
					if (s3loader.getVariancePool().get(
							i * s3loader.getNumGaussiansPerState() + k)[l] <= 0.) {
						this.variancePool.get(i
								* s3loader.getNumGaussiansPerState() + k)[l] = (float) 0.5;
					} else if (s3loader.getVariancePool().get(
							i * s3loader.getNumGaussiansPerState() + k)[l] < varFlor) {
						this.variancePool.get(i
								* s3loader.getNumGaussiansPerState() + k)[l] = (float) (1. / varFlor);
					} else {
						this.variancePool.get(i
								* s3loader.getNumGaussiansPerState() + k)[l] = (float) (1. / s3loader
								.getVariancePool().get(
										i * s3loader.getNumGaussiansPerState()
												+ k)[l]);
					}
				}
			}
		}
	}
	
	/**
	 * Computes posterior values for the each component.
	 * 
	 * @param componentScores
	 *            from which the posterior values are computed.
	 * @return posterior values for all components.
	 */
	private float[] computePosterios(float[] componentScores) {
		float max;
		float[] posteriors = componentScores;

		max = posteriors[0];

		for (int i = 1; i < componentScores.length; i++) {
			if (posteriors[i] > max) {
				max = posteriors[i];
			}
		}

		for (int i = 0; i < componentScores.length; i++) {
			posteriors[i] = (float) logMath.logToLinear(posteriors[i] - max);
		}

		return posteriors;
	}

	/**
	 * This method is used for directly collect and use counts. The counts are
	 * collected and stored separately for each cluster.
	 * 
	 * @param result
	 *            Result object to collect counts from.
	 */
	public void collect(Result result) throws Exception {
		Token token = result.getBestToken();
		HMMSearchState state;
		float[] componentScore, featureVector, posteriors, tmean;
		float dnom, wtMeanVar, wtDcountVar, wtDcountVarMean, mean;
		int mId, len, cluster;

		if (token == null)
			throw new Exception("Best token not found!");

		do {
			FloatData feature = (FloatData) token.getData();
			SearchState ss = token.getSearchState();

			if (!(ss instanceof HMMSearchState && ss.isEmitting())) {
				token = token.getPredecessor();
				continue;
			}

			state = (HMMSearchState) token.getSearchState();
			componentScore = state.calculateComponentScore(feature);
			featureVector = FloatData.toFloatData(feature).getValues();
			mId = (int) state.getHMMState().getMixtureId();
			posteriors = this.computePosterios(componentScore);
			len = s3loader.getVectorLength()[0];

			for (int i = 0; i < componentScore.length; i++) {
				cluster = means.getClassIndex(mId
						* s3loader.getNumGaussiansPerState() + i);
				dnom = posteriors[i];
				if (dnom > 0.) {
					tmean = s3loader.getMeansPool().get(
							mId * s3loader.getNumGaussiansPerState() + i);

					for (int j = 0; j < featureVector.length; j++) {
						mean = posteriors[i] * featureVector[j];
						wtMeanVar = mean
								* s3loader
										.getVariancePool()
										.get(mId
												* s3loader
														.getNumGaussiansPerState()
												+ i)[j];
						wtDcountVar = dnom
								* s3loader
										.getVariancePool()
										.get(mId
												* s3loader
														.getNumGaussiansPerState()
												+ i)[j];

						for (int p = 0; p < featureVector.length; p++) {
							wtDcountVarMean = wtDcountVar * tmean[p];

							for (int q = p; q < featureVector.length; q++) {
								regLs[cluster][0][j][p][q] += wtDcountVarMean
										* tmean[q];
							}
							regLs[cluster][0][j][p][len] += wtDcountVarMean;
							regRs[cluster][0][j][p] += wtMeanVar * tmean[p];
						}
						regLs[cluster][0][j][len][len] += wtDcountVar;
						regRs[cluster][0][j][len] += wtMeanVar;

					}
				}
			}

			token = token.getPredecessor();
		} while (token != null);
	}

	/**
	 * Fill lower part of Legetter's set of G matrices.
	 */
	public void fillRegLowerPart() {
		for (int i = 0; i < this.nrOfClusters; i++) {
			for (int j = 0; j < s3loader.getNumStreams(); j++) {
				for (int l = 0; l < s3loader.getVectorLength()[j]; l++) {
					for (int p = 0; p <= s3loader.getVectorLength()[j]; p++) {
						for (int q = p + 1; q <= s3loader.getVectorLength()[j]; q++) {
							regLs[i][j][l][q][p] = regLs[i][j][l][p][q];
						}
					}
				}
			}
		}
	}
}