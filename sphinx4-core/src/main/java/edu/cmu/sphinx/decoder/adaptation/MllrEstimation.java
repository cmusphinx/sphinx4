package edu.cmu.sphinx.decoder.adaptation;

import java.io.PrintWriter;

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
import edu.cmu.sphinx.linguist.acoustic.tiedstate.MixtureComponent;
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

	private String outputFilePath;
	private float varFlor;
	private int nMllrClass;
	private float[][][] A;
	private float[][] B;
	private double[][][][] regL;
	private double[][][] regR;
	protected Sphinx3Loader s3loader;
	private boolean estimated;
	private LogMath logMath = LogMath.getInstance();
	private Pool<float[]> variancePool;

	public MllrEstimation(String outputFilePath, Loader loader)
			throws Exception {
		super();
		this.varFlor = (float) 1e-5;
		this.nMllrClass = 1;
		this.outputFilePath = outputFilePath;
		this.s3loader = (Sphinx3Loader) loader;
		if (s3loader == null) {
			throw new Exception("Sphinx3Loader is not set.");
		}

		this.variancePool = s3loader.getVariancePool();
		this.invertVariances();
		this.init();
	}

	protected MllrEstimation(Loader loader) throws Exception {
		super();
		this.varFlor = (float) 1e-5;
		this.nMllrClass = 1;
		this.s3loader = (Sphinx3Loader) loader;
		if (s3loader == null) {
			throw new Exception("Sphinx3Loader is not set.");
		}

		this.variancePool = s3loader.getVariancePool();
		this.invertVariances();
	}

	public MllrEstimation() {
		this.varFlor = (float) 1e-5;
		this.nMllrClass = 1;
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

	public void setOutputFilePath(String outputFilePath) {
		this.outputFilePath = outputFilePath;
	}

	public void setVarFlor(float varFlor) {
		this.varFlor = varFlor;
	}

	public void setnMllrClass(int nMllrClass) {
		this.nMllrClass = nMllrClass;
	}

	public void setS3loader(Loader loader) {
		this.s3loader = (Sphinx3Loader) loader;
	}

	/**
	 * Used for verifying if the estimation is fully computed.
	 * 
	 * @return true if the estimation is computed, else false
	 */
	public boolean isComplete() {
		return this.estimated;
	}

	/**
	 * Calculates the scores for each component in the senone.
	 *
	 * @param feature
	 *            the feature to score
	 * @return the LogMath log scores for the feature, one for each component
	 */
	protected float[] calculateComponentScore(FloatData features,
			HMMSearchState state) {
		MixtureComponent[] mc = state.getHMMState().getMixtureComponents();
		float[] mw = state.getHMMState().getLogMixtureWeights();
		float[] featureVector = FloatData.toFloatData(features).getValues();
		float[] logComponentScore = new float[mc.length];

		for (int i = 0; i < mc.length; i++) {
			logComponentScore[i] = mc[i].getScore(featureVector) + mw[i];
		}

		return logComponentScore;
	}

	/**
	 * Computes posterior values for the each component.
	 * 
	 * @param componentScores
	 *            from which the posterior values are computed.
	 * @return posterior values for all components.
	 */
	protected float[] computePosterios(float[] componentScores) {
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
			componentScore = this.calculateComponentScore(feature, state);
			featureVector = FloatData.toFloatData(feature).getValues();
			mixtureId = (int) state.getHMMState().getMixtureId();
			posteriors = this.computePosterios(componentScore);
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
	 * Writes the transformation to file in a format that could further be used
	 * in Sphinx3 and Sphinx4.
	 * 
	 */
	public void createMllrFile() throws Exception {

		if (!this.isComplete()) {
			throw new Exception("Estimation is not computed!");
		}

		PrintWriter writer = new PrintWriter(this.outputFilePath, "UTF-8");

		writer.println(nMllrClass);
		writer.println(s3loader.getNumStreams());

		for (int i = 0; i < s3loader.getNumStreams(); i++) {
			writer.println(s3loader.getVectorLength()[i]);

			for (int j = 0; j < s3loader.getVectorLength()[i]; j++) {
				for (int k = 0; k < s3loader.getVectorLength()[i]; ++k) {
					writer.print(A[i][j][k]);
					writer.print(" ");
				}
				writer.println();
			}

			for (int j = 0; j < s3loader.getVectorLength()[i]; j++) {
				writer.print(B[i][j]);
				writer.print(" ");

			}
			writer.println();

			for (int j = 0; j < s3loader.getVectorLength()[i]; j++) {
				writer.print("1.0 ");

			}
			writer.println();
		}
		writer.close();
	}

	/**
	 * Deploys the whole process of MLLR transform estimation.
	 * 
	 * @throws Exception
	 */
	public void estimateMatrices() throws Exception {
		this.fillRegLowerPart();
		this.computeMllr();
		this.estimated = true;
	}
}
