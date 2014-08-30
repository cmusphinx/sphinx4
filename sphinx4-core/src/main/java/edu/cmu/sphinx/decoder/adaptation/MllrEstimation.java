package edu.cmu.sphinx.decoder.adaptation;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.frontend.FloatData;
import edu.cmu.sphinx.linguist.HMMSearchState;
import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Loader;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Pool;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Sphinx3Loader;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.LogMath;

/**
 * Used for computing a MLLR estimation that will be provided as A and B matrix
 * (representing: A*x + B = C)
 * 
 * @author Bogdan Petcu
 */
public class MllrEstimation {

	private float varFlor;
	private float[][][] A;
	private float[][] B;
	private double[][][][] regL;
	private double[][][] regR;
	protected Sphinx3Loader s3loader;
	private static LogMath logMath = LogMath.getInstance();
	private Pool<float[]> variancePool;

	public MllrEstimation(Loader loader)
			throws Exception {
		super();
		this.varFlor = (float) 1e-5;
		this.s3loader = (Sphinx3Loader) loader;
		
		if (s3loader == null) {
			throw new Exception("Sphinx3Loader is not set.");
		}

		this.variancePool = s3loader.getVariancePool();
		this.invertVariances();
		this.init();
	}

	public MllrEstimation() {
		this.varFlor = (float) 1e-5;
	}

	private void init() {
		int len = s3loader.getVectorLength()[0];
		this.regL = new double[s3loader.getNumStreams()][][][];
		this.regR = new double[s3loader.getNumStreams()][][];

		for (int i = 0; i < s3loader.getNumStreams(); i++) {
			len = s3loader.getVectorLength()[i];
			this.regL[i] = new double[len][len + 1][len + 1];
			this.regR[i] = new double[len][len + 1];
		}
	}

	public float[][][] getA() {
		return A;
	}

	public float[][] getB() {
		return B;
	}

	public void setVarFlor(float varFlor) {
		this.varFlor = varFlor;
	}

	public void setS3loader(Loader loader) {
		this.s3loader = (Sphinx3Loader) loader;
	}

	/**
	 * Computes posterior values for the each component.
	 * 
	 * @param componentScores
	 *            from which the posterior values are computed.
	 * @return posterior values for all components.
	 */
	public static float[] computePosterios(float[] componentScores) {
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
	 * Fill lower part of Legetter's set of G matrices.
	 */
	private void fillRegLowerPart() {
		for (int j = 0; j < s3loader.getNumStreams(); j++) {
			for (int l = 0; l < s3loader.getVectorLength()[j]; l++) {
				for (int p = 0; p <= s3loader.getVectorLength()[j]; p++) {
					for (int q = p + 1; q <= s3loader.getVectorLength()[j]; q++) {
						regL[j][l][q][p] = regL[j][l][p][q];
					}
				}
			}
		}
	}

	/**
	 * This method is used for directly collect and use counts in order to
	 * compute a MLLR Estimation
	 * 
	 * @param result
	 *            Result object to collect counts from.
	 */
	public void collect(Result result) throws Exception {
		Token token = result.getBestToken();
		HMMSearchState state;
		float[] componentScore, featureVector, posteriors, tmean;
		float dnom, wtMeanVar, wtDcountVar, wtDcountVarMean, mean;
		int mixtureId, len;

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
			mixtureId = (int) state.getHMMState().getMixtureId();
			posteriors = computePosterios(componentScore);
			len = s3loader.getVectorLength()[0];

			for (int i = 0; i < componentScore.length; i++) {
				dnom = posteriors[i];
				if (dnom > 0.) {
					tmean = s3loader.getMeansPool().get(
							mixtureId * s3loader.getNumGaussiansPerState() + i);

					for (int j = 0; j < featureVector.length; j++) {
						mean = posteriors[i] * featureVector[j];
						wtMeanVar = mean
								* s3loader
										.getVariancePool()
										.get(mixtureId
												* s3loader
														.getNumGaussiansPerState()
												+ i)[j];
						wtDcountVar = dnom
								* s3loader
										.getVariancePool()
										.get(mixtureId
												* s3loader
														.getNumGaussiansPerState()
												+ i)[j];
						for (int p = 0; p < featureVector.length; p++) {
							wtDcountVarMean = wtDcountVar * tmean[p];

							for (int q = p; q < featureVector.length; q++) {
								regL[0][j][p][q] += wtDcountVarMean * tmean[q];
							}
							regL[0][j][p][len] += wtDcountVarMean;
							regR[0][j][p] += wtMeanVar * tmean[p];
						}
						regL[0][j][len][len] += wtDcountVar;
						regR[0][j][len] += wtMeanVar;
					}
				}
			}

			token = token.getPredecessor();
		} while (token != null);
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
	 * Used for computing the actual transformation (A and B matrices).
	 */
	private void computeMllr() {
		int len;
		DecompositionSolver solver;
		RealMatrix coef;
		RealVector vect, ABloc;

		this.A = new float[s3loader.getNumStreams()][][];
		this.B = new float[s3loader.getNumStreams()][];

		for (int i = 0; i < s3loader.getNumStreams(); i++) {
			len = s3loader.getVectorLength()[i];
			this.A[i] = new float[len][len];
			this.B[i] = new float[len];

			for (int j = 0; j < len; ++j) {
				coef = new Array2DRowRealMatrix(regL[i][j], false);
				solver = new LUDecomposition(coef).getSolver();
				vect = new ArrayRealVector(regR[i][j], false);
				ABloc = solver.solve(vect);

				for (int k = 0; k < len; ++k) {
					this.A[i][j][k] = (float) ABloc.getEntry(k);
				}

				this.B[i][j] = (float) ABloc.getEntry(len);
			}
		}
	}



	/**
	 * Deploys the whole process of MLLR transform estimation.
	 */
	public void perform() throws Exception {
		this.fillRegLowerPart();
		this.computeMllr();
	}
}
