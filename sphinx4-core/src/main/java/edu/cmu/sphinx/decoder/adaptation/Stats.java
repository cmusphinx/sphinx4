package edu.cmu.sphinx.decoder.adaptation;

import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.frontend.FloatData;
import edu.cmu.sphinx.linguist.HMMSearchState;
import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Loader;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Sphinx3Loader;
import edu.cmu.sphinx.linguist.lextree.LexTreeLinguist.LexTreeHMMState;
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
	private Sphinx3Loader loader;
	private float varFlor;
	private LogMath logMath = LogMath.getLogMath();;

	public Stats(Loader loader, int nrOfClusters, ClusteredDensityFileData means)
			throws Exception {
		this.loader = (Sphinx3Loader) loader;
		this.nrOfClusters = nrOfClusters;
		this.means = means;
		this.varFlor = (float) 1e-5;

		if (this.loader == null) {
			throw new Exception("Sphinx3Loader is not set.");
		}

		this.invertVariances();
		this.init();
	}

	private void init() {
		int len = loader.getVectorLength()[0];
		this.regLs = new double[nrOfClusters][][][][];
		this.regRs = new double[nrOfClusters][][][];

		for (int i = 0; i < nrOfClusters; i++) {
			this.regLs[i] = new double[loader.getNumStreams()][][][];
			this.regRs[i] = new double[loader.getNumStreams()][][];

			for (int j = 0; j < loader.getNumStreams(); j++) {
				len = loader.getVectorLength()[j];
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
	private void invertVariances() {

		for (int i = 0; i < loader.getNumStates(); i++) {
			for (int k = 0; k < loader.getNumGaussiansPerState(); k++) {
				for (int l = 0; l < loader.getVectorLength()[0]; l++) {
					if (loader.getVariancePool().get(
							i * loader.getNumGaussiansPerState() + k)[l] <= 0.) {
						this.loader.getVariancePool().get(i
								* loader.getNumGaussiansPerState() + k)[l] = (float) 0.5;
					} else if (loader.getVariancePool().get(
							i * loader.getNumGaussiansPerState() + k)[l] < varFlor) {
						this.loader.getVariancePool().get(i
								* loader.getNumGaussiansPerState() + k)[l] = (float) (1. / varFlor);
					} else {
						this.loader.getVariancePool().get(i
								* loader.getNumGaussiansPerState() + k)[l] = (float) (1. / loader
								.getVariancePool().get(
										i * loader.getNumGaussiansPerState()
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
		LexTreeHMMState state;
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

			state = (LexTreeHMMState) token.getSearchState();
			componentScore = state.calculateComponentScore(feature);
			featureVector = FloatData.toFloatData(feature).getValues();
			mId = (int) state.getHMMState().getMixtureId();
			posteriors = this.computePosterios(componentScore);
			len = loader.getVectorLength()[0];

			for (int i = 0; i < componentScore.length; i++) {
				cluster = means.getClassIndex(mId
						* loader.getNumGaussiansPerState() + i);
				dnom = posteriors[i];
				if (dnom > 0.) {
					tmean = loader.getMeansPool().get(
							mId * loader.getNumGaussiansPerState() + i);

					for (int j = 0; j < featureVector.length; j++) {
						mean = posteriors[i] * featureVector[j];
						wtMeanVar = mean
								* loader.getVariancePool().get(mId
										* loader.getNumGaussiansPerState()
										+ i)[j];
						wtDcountVar = dnom
								* loader.getVariancePool().get(mId
										* loader.getNumGaussiansPerState()
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
			for (int j = 0; j < loader.getNumStreams(); j++) {
				for (int l = 0; l < loader.getVectorLength()[j]; l++) {
					for (int p = 0; p <= loader.getVectorLength()[j]; p++) {
						for (int q = p + 1; q <= loader.getVectorLength()[j]; q++) {
							regLs[i][j][l][q][p] = regLs[i][j][l][p][q];
						}
					}
				}
			}
		}
	}

}