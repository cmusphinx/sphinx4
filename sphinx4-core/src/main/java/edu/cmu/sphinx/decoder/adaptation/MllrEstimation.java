package edu.cmu.sphinx.decoder.adaptation;

import java.io.PrintWriter;
import java.util.ArrayList;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import edu.cmu.sphinx.linguist.acoustic.tiedstate.Loader;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Sphinx3Loader;
import edu.cmu.sphinx.result.Result;

/**
 * Used for computing a MLLR estimation provided as A and B matrix
 * (representing: A*x + B)
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
	private Counts counts;
<<<<<<< HEAD
	private CountsReader cr;
	private CountsCollector cc;
	private boolean countsFromFile;
	private boolean modelFromFile;
=======
>>>>>>> removed unuseful storage of means and variances
	private Sphinx3Loader s3loader;
	private boolean estimated;
	private boolean classEstimation;
	private ArrayList<Integer> gaussianNumbers;

<<<<<<< HEAD
	public MllrEstimation(String location, int nMllrClass,
			String outputFilePath, boolean countsFromFile,
			String countsFilePath, boolean modelFromFile, Loader loader)
			throws Exception {
=======
	public MllrEstimation(int nMllrClass, String outputFilePath, Counts counts,
			Loader loader) throws Exception {
>>>>>>> removed unuseful storage of means and variances
		super();
		this.varFlor = (float) 1e-5;
		this.nMllrClass = nMllrClass;
		this.outputFilePath = outputFilePath;
		this.s3loader = (Sphinx3Loader) loader;
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
		if (s3loader == null) {
			throw new Exception("Sphinx3Loader is not set.");
		}

>>>>>>> removed unuseful storage of means and variances
	}

	public MllrEstimation() {
		this.varFlor = (float) 1e-5;
		this.nMllrClass = 1;
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
	 * This method is called if the estimation is just for a certain group of
	 * gaussians.
	 */
	public void setClassEstimation(boolean classEstimation,
			ArrayList<Integer> stateNumbers) throws Exception {
		this.classEstimation = classEstimation;
		if (classEstimation
				&& (stateNumbers == null || stateNumbers.size() == 0)) {
			throw new Exception("Empty set of indexes that form the class");
		}
		this.gaussianNumbers = stateNumbers;
	}

	/**
	 * Used for verifying if the estimation is fully computed.
	 * 
	 * @return true if the estimation is computed, else false
	 */
	public boolean isComplete() {
		return this.estimated;
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
	 * Fill Legetter's sets of G and Z matrices.
	 */
	private void fill() {
		int len;

		for (int i = 0; i < s3loader.getNumStates(); i++) {
			float[] tmean;
			float wtMeanVar, wtDcountVar, wtDcountVarMean;

			for (int j = 0; j < s3loader.getNumStreams(); j++) {
				len = s3loader.getVectorLength()[j];

				for (int k = 0; k < s3loader.getNumGaussiansPerState(); k++) {
					if (counts.getDnom()[i][j][k] > 0.) {
						tmean = s3loader.getMeansPool().get(
								i * s3loader.getNumGaussiansPerState() + k);
						for (int l = 0; l < len; l++) {
							wtMeanVar = counts.getMean()[i][j][k][l]
									* s3loader
											.getVariancePool()
											.get(i
													* s3loader
															.getNumGaussiansPerState()
													+ k)[l];
							wtDcountVar = counts.getDnom()[i][j][k]
									* s3loader
											.getVariancePool()
											.get(i
													* s3loader
															.getNumGaussiansPerState()
													+ k)[l];

							for (int p = 0; p < len; p++) {
								wtDcountVarMean = wtDcountVar * tmean[p];

								for (int q = p; q < len; q++) {
									regL[j][l][p][q] += wtDcountVarMean
											* tmean[q];
								}
								regL[j][l][p][len] += wtDcountVarMean;
								regR[j][l][p] += wtMeanVar * tmean[p];
							}
							regL[j][l][len][len] += wtDcountVar;
							regR[j][l][len] += wtMeanVar;
						}
					}
				}
			}
		}
	}

	/**
	 * Fill Legetter's sets of G and Z matrices for a single class of gaussians.
	 */
	private void fillForClass() {
		int len, stateIndex, gaussianIndex;

		for (int gaussianNumber : this.gaussianNumbers) {
			stateIndex = gaussianNumber / s3loader.getNumGaussiansPerState();
			gaussianIndex = gaussianNumber % s3loader.getNumGaussiansPerState();
			float[] tmean;
			float wtMeanVar, wtDcountVar, wtDcountVarMean;

			len = s3loader.getVectorLength()[0];

			if (counts.getDnom()[stateIndex][0][gaussianIndex] > 0.) {
				tmean = s3loader.getMeansPool().get(
						stateIndex * s3loader.getNumGaussiansPerState()
								+ gaussianIndex);
				for (int l = 0; l < len; l++) {
					wtMeanVar = counts.getMean()[stateIndex][0][gaussianIndex][l]
							* s3loader.getVariancePool().get(
									stateIndex
											* s3loader
													.getNumGaussiansPerState()
											+ gaussianIndex)[l];
					wtDcountVar = counts.getDnom()[stateIndex][0][gaussianIndex]
							* s3loader.getVariancePool().get(
									stateIndex
											* s3loader
													.getNumGaussiansPerState()
											+ gaussianIndex)[l];

					for (int p = 0; p < len; p++) {
						wtDcountVarMean = wtDcountVar * tmean[p];

						for (int q = p; q < len; q++) {
							regL[0][l][p][q] += wtDcountVarMean * tmean[q];
						}

						regL[0][l][p][len] += wtDcountVarMean;
						regR[0][l][p] += wtMeanVar * tmean[p];
					}
					regL[0][l][len][len] += wtDcountVar;
					regR[0][l][len] += wtMeanVar;
				}
			}

		}
	}

	private void fillRegMatrices() {
		if (!this.classEstimation) {
			this.fill();
		} else {
			this.fillForClass();
		}

		fillRegLowerPart();
	}

	/**
	 * Used for inverting variances.
	 */
	private void invertVariances() {

		for (int i = 0; i < s3loader.getNumStates(); i++) {
			for (int k = 0; k < s3loader.getNumGaussiansPerState(); k++) {
				for (int l = 0; l < s3loader.getVectorLength()[0]; l++) {
					if (s3loader.getVariancePool().get(
							i * s3loader.getNumGaussiansPerState() + k)[l] <= 0.) {
						s3loader.getVariancePool().get(
								i * s3loader.getNumGaussiansPerState() + k)[l] = (float) 0.5;
					} else if (s3loader.getVariancePool().get(
							i * s3loader.getNumGaussiansPerState() + k)[l] < varFlor) {
						s3loader.getVariancePool().get(
								i * s3loader.getNumGaussiansPerState() + k)[l] = (float) (1. / varFlor);
					} else {
						s3loader.getVariancePool().get(
								i * s3loader.getNumGaussiansPerState() + k)[l] = (float) (1. / s3loader
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

	/**
	 * Writes the transformation to file.
	 * 
	 * @throws Exception
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
	 * 
	 * @throws Exception
	 */
>>>>>>> added javadocs
	public void estimateMatrices() throws Exception {

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
		this.computeMllr();
		this.estimated = true;
	}
}
