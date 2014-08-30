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
<<<<<<< HEAD
=======
import edu.cmu.sphinx.util.LogMath;
>>>>>>> implemented direct counts collecting in MllrEstimation.java and ClustersEstimation.java. This way the program uses less memory than the previous form when the counts were stored before they were used in computing the transform

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
<<<<<<< HEAD
	private Counts counts;
<<<<<<< HEAD
	private CountsReader cr;
	private CountsCollector cc;
	private boolean countsFromFile;
	private boolean modelFromFile;
=======
>>>>>>> removed unuseful storage of means and variances
	private Sphinx3Loader s3loader;
=======
	protected Sphinx3Loader s3loader;
<<<<<<< HEAD
>>>>>>> implemented direct counts collecting in MllrEstimation.java and ClustersEstimation.java. This way the program uses less memory than the previous form when the counts were stored before they were used in computing the transform
	private boolean estimated;
	private LogMath logMath = LogMath.getInstance();
	private Pool<float[]> variancePool;

<<<<<<< HEAD
<<<<<<< HEAD
	public MllrEstimation(String location, int nMllrClass,
			String outputFilePath, boolean countsFromFile,
			String countsFilePath, boolean modelFromFile, Loader loader)
			throws Exception {
=======
	public MllrEstimation(int nMllrClass, String outputFilePath, Counts counts,
			Loader loader) throws Exception {
>>>>>>> removed unuseful storage of means and variances
=======
	public MllrEstimation(String outputFilePath, Loader loader)
=======
	private static LogMath logMath = LogMath.getInstance();
	private Pool<float[]> variancePool;

	public MllrEstimation(Loader loader)
>>>>>>> Added transform object and made necessary modifications. Isolated the object from its process of construction.
			throws Exception {
>>>>>>> implemented direct counts collecting in MllrEstimation.java and ClustersEstimation.java. This way the program uses less memory than the previous form when the counts were stored before they were used in computing the transform
		super();
		this.varFlor = (float) 1e-5;
		this.s3loader = (Sphinx3Loader) loader;
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
		this.init();
	}

	/**
	 * Reads Means and Variances and also counts if it's requested to read them
	 * from file.
	 * 
	 * @throws Exception
	 */
	public void init() throws Exception {
		this.readMeansAndVariances();
<<<<<<< HEAD
		this.readCounts();
=======
		if (countsFromFile) {
			this.readCountsFromFile();
		}
>>>>>>> changed clustering so it clusters gaussians by gaussian index, not state index
=======
		this.counts = counts;
=======
>>>>>>> implemented direct counts collecting in MllrEstimation.java and ClustersEstimation.java. This way the program uses less memory than the previous form when the counts were stored before they were used in computing the transform
=======
		
>>>>>>> Added transform object and made necessary modifications. Isolated the object from its process of construction.
		if (s3loader == null) {
			throw new Exception("Sphinx3Loader is not set.");
		}

<<<<<<< HEAD
>>>>>>> removed unuseful storage of means and variances
=======
		this.variancePool = s3loader.getVariancePool();
		this.invertVariances();
		this.init();
	}

<<<<<<< HEAD
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
>>>>>>> implemented direct counts collecting in MllrEstimation.java and ClustersEstimation.java. This way the program uses less memory than the previous form when the counts were stored before they were used in computing the transform
	}

=======
>>>>>>> Added transform object and made necessary modifications. Isolated the object from its process of construction.
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

<<<<<<< HEAD
<<<<<<< HEAD
	public void readCounts() throws Exception {
		if (this.countsFromFile) {
=======
=======
	/**
<<<<<<< HEAD
	 * Reads counts from file if called.
	 * 
	 * @throws Exception
	 */
>>>>>>> added javadocs
	private void readCountsFromFile() throws Exception {
<<<<<<< HEAD
>>>>>>> implemented mllr estimation per class
			cr = new CountsReader(this.countsFilePath);
			cr.read();
			this.counts = cr.getCounts();
			this.cb2mllr = new int[counts.getnCb()];
		} else {
			cc = new CountsCollector(means.getVectorLength(),
					means.getNumStates(), means.getNumStreams(),
					means.getNumGaussiansPerState());
		}
=======
		cr = new CountsReader(this.countsFilePath);
		cr.read();
		this.counts = cr.getCounts();
<<<<<<< HEAD
<<<<<<< HEAD
		this.cb2mllr = new int[counts.getnCb()];
>>>>>>> changed clustering so it clusters gaussians by gaussian index, not state index
=======
		this.cb2mllr = new int[counts.getNumStates()];
>>>>>>> fixed some naming and formating issues in decoder.adaptation package
=======
>>>>>>> removed unused dimension of A and B matrices
	}

	/**
	 * Reads means and variances. These are from a provided file or from the
	 * provided Sphinx3Loader.
	 * 
	 * @throws Exception
	 */
	private void readMeansAndVariances() throws Exception {

		if (modelFromFile) {
			URL location = new URL("file:" + this.location);
			Sphinx3Loader loader = new Sphinx3Loader(location, this.model, "",
					null, 0, 0, this.varFlor, false);

			this.means = new DensityFileData("means", -Float.MAX_VALUE, loader,
					true);
			this.variances = new DensityFileData("variances", this.varFlor,
					loader, true);

			if (means.getNumStates() != variances.getNumStates()
					|| means.getNumStreams() != variances.getNumStreams()
					|| means.getNumGaussiansPerState() != variances
							.getNumGaussiansPerState()) {
				throw new Exception("Dimensions mismatch!");
			}

			for (int i = 0; i < means.getNumStreams(); i++) {
				if (means.getVectorLength()[i] != variances.getVectorLength()[i]) {
					throw new Exception(
							"Mismatch between vector length of some stream(s) and prior length");
				}
			}
		} else {
			if (s3loader == null) {
				throw new Exception("Sphinx3Loader is not set.");
			}

			this.means = new DensityFileData();
			this.variances = new DensityFileData();
			this.means.setPool(s3loader.getMeansPool());
			this.means.setNumGaussiansPerState(s3loader
					.getNumGaussiansPerState());
			this.means.setNumStreams(s3loader.getNumStreams());
			this.means.setNumStates(s3loader.getNumStates());
			this.means.setVectorLength(s3loader.getVectorLength());
			this.variances.setPool(s3loader.getVariancePool());
			this.variances.setNumGaussiansPerState(s3loader
					.getNumGaussiansPerState());
			this.variances.setNumStreams(s3loader.getNumStreams());
			this.variances.setNumStates(s3loader.getNumStates());
			this.variances.setVectorLength(s3loader.getVectorLength());
		}
	}

	/**
=======
>>>>>>> removed unuseful storage of means and variances
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
<<<<<<< HEAD
	
	public void addCounts(Result result) throws Exception{
		cc.collect(result);
	}
=======
>>>>>>> changed clustering so it clusters gaussians by gaussian index, not state index



<<<<<<< HEAD
<<<<<<< HEAD
	public void estimateMatrices() {
		if(!countsFromFile){
			this.counts = cc.getCounts();
		}
		
=======
=======
	/**
	 * Deploys the whole process of MLLR transform estimation.
	 */
<<<<<<< HEAD
>>>>>>> added javadocs
	public void estimateMatrices() throws Exception {
<<<<<<< HEAD

>>>>>>> changed clustering so it clusters gaussians by gaussian index, not state index
		this.invertVariances();

		int len = s3loader.getVectorLength()[0];
		this.regL = new double[s3loader.getNumStreams()][][][];
		this.regR = new double[s3loader.getNumStreams()][][];

		for (int i = 0; i < s3loader.getNumStreams(); i++) {
			len = s3loader.getVectorLength()[i];
			this.regL[i] = new double[len][len + 1][len + 1];
			this.regR[i] = new double[len][len + 1];
		}

		this.fillRegMatrices();
=======
=======
	public void perform() throws Exception {
>>>>>>> Added transform object and made necessary modifications. Isolated the object from its process of construction.
		this.fillRegLowerPart();
>>>>>>> implemented direct counts collecting in MllrEstimation.java and ClustersEstimation.java. This way the program uses less memory than the previous form when the counts were stored before they were used in computing the transform
		this.computeMllr();
	}
}
