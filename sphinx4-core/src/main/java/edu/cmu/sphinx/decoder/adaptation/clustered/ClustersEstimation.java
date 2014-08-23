package edu.cmu.sphinx.decoder.adaptation.clustered;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import edu.cmu.sphinx.decoder.adaptation.MllrEstimation;
import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.frontend.FloatData;
import edu.cmu.sphinx.linguist.HMMSearchState;
import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Loader;
import edu.cmu.sphinx.result.Result;

/**
 * This class is used for estimating a MLLR transform for each cluster of data.
 * The clustering must be previously performed using
 * ClusteredDensityFileData.java
 * 
 * @author Bogdan Petcu
 */
public class ClustersEstimation extends MllrEstimation {

	private ClusteredDensityFileData cd;
	private double[][][][][] regLs;
	private double[][][][] regRs;
	private float[][][][] As;
	private float[][][] Bs;
	protected int nrOfClusters;

	public ClustersEstimation(int nMllrClass, Loader loader, int nrOfClusters,
			ClusteredDensityFileData cd) throws Exception {
		super(loader);
		this.nrOfClusters = nrOfClusters;
		this.cd = cd;
		this.invertVariances();
		this.init();
	}

	private void init() {
		int len = s3loader.getVectorLength()[0];
		this.regLs = new double[nrOfClusters][][][][];
		this.regRs = new double[nrOfClusters][][][];

		As = new float[nrOfClusters][][][];
		Bs = new float[nrOfClusters][][];

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
		return this.cd;
	}

	public float[][][][] getAs() {
		return this.As;
	}

	public float[][][] getBs() {
		return this.Bs;
	}

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
			componentScore = this.calculateComponentScore(feature, state);
			featureVector = FloatData.toFloatData(feature).getValues();
			mId = (int) state.getHMMState().getMixtureId();
			posteriors = this.computePosterios(componentScore);
			len = s3loader.getVectorLength()[0];

			for (int i = 0; i < componentScore.length; i++) {
				cluster = cd.getClassIndex(mId
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

	private void fillRegLowerPart() {
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

	private void computeMllrTransforms() {
		int len;
		DecompositionSolver solver;
		RealMatrix coef;
		RealVector vect, ABloc;

		for (int c = 0; c < nrOfClusters; c++) {
			this.As[c] = new float[s3loader.getNumStreams()][][];
			this.Bs[c] = new float[s3loader.getNumStreams()][];

			for (int i = 0; i < s3loader.getNumStreams(); i++) {
				len = s3loader.getVectorLength()[i];
				this.As[c][i] = new float[len][len];
				this.Bs[c][i] = new float[len];

				for (int j = 0; j < len; ++j) {
					coef = new Array2DRowRealMatrix(regLs[c][i][j], false);
					solver = new LUDecomposition(coef).getSolver();
					vect = new ArrayRealVector(regRs[c][i][j], false);
					ABloc = solver.solve(vect);

					for (int k = 0; k < len; ++k) {
						this.As[c][i][j][k] = (float) ABloc.getEntry(k);
					}

					this.Bs[c][i][j] = (float) ABloc.getEntry(len);
				}
			}
		}
	}

	public void estimate() {
		this.fillRegLowerPart();
		this.computeMllrTransforms();
	}

}