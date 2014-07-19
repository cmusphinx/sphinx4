package edu.cmu.sphinx.decoder.adaptation;

import java.net.URL;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import edu.cmu.sphinx.linguist.acoustic.tiedstate.Sphinx3Loader;

public class MllrEstimation {

	private String location;
	private String model;
	private String countsFilePath;
	private DensityFileData means;
	private DensityFileData variances;
	private float varFlor;
	private int nMllrClass;
	private int[] cb2mllr;
	private float[][][][] A;
	private float[][][] B;
	private double[][][][][] regL;
	private double[][][][] regR;
	private CountsReader cr;

	public MllrEstimation(String location, String countsFilePath,
			float varFlor, int nMllrClass) {
		super();
		this.location = location;
		this.countsFilePath = countsFilePath;
		this.varFlor = varFlor;
		this.nMllrClass = nMllrClass;
	}

	public MllrEstimation() {
		this.varFlor = (float) 1e-5;
		this.nMllrClass = 1;
	}

	public float[][][][] getA() {
		return A;
	}

	public float[][][] getB() {
		return B;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public void setCountsFilePath(String countsFilePath) {
		this.countsFilePath = countsFilePath;
	}

	public void setVarFlor(float varFlor) {
		this.varFlor = varFlor;
	}

	public void setnMllrClass(int nMllrClass) {
		this.nMllrClass = nMllrClass;
	}

	public void readCounts() throws Exception {
		cr = new CountsReader(this.countsFilePath);
		cr.read();
		this.cb2mllr = new int[cr.getnCb()];
		/*
		 * store useful information
		 */
	}

	public void readMeansAndVariances() throws Exception {
		URL location = new URL("file:" + this.location);
		Sphinx3Loader loader = new Sphinx3Loader(location, this.model, "",
				null, 0, 0, this.varFlor, false);

		this.means = new DensityFileData("means", -Float.MAX_VALUE, loader);
		this.variances = new DensityFileData("variances", this.varFlor, loader);

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

	}

	public void fillRegLowerPart() {
		for (int m = 0; m < nMllrClass; m++) {
			for (int j = 0; j < means.getNumStreams(); j++) {
				for (int l = 0; l < cr.getVeclen()[j]; l++) {
					for (int p = 0; p <= cr.getVeclen()[j]; p++) {
						for (int q = p + 1; q <= cr.getVeclen()[j]; q++) {
							regL[m][j][l][q][p] = regL[m][j][l][p][q];
						}
					}
				}
			}
		}
	}

	public void fillRegMatrices() {
		int mc, len;

		for (int i = 0; i < means.getNumStates(); i++) {
			float[] tmean;
			float wtMeanVar, wtDcountVar, wtDcountVarMean;
			mc = cb2mllr[i];

			if (mc < 0)
				continue;

			for (int j = 0; j < means.getNumStreams(); j++) {
				len = means.getVectorLength()[j];

				for (int k = 0; k < means.getNumGaussiansPerState(); k++) {
					if (cr.getDnom()[i][j][k] > 0.) {
						tmean = means.getPool().get(
								i * means.getNumStates() + k);
						for (int l = 0; l < len; l++) {
							wtMeanVar = cr.getMean()[i][j][k][l]
									* variances.getPool().get(
											i * means.getNumStates() + k)[l];
							wtDcountVar = cr.getDnom()[i][j][k]
									* variances.getPool().get(
											i * means.getNumStates() + k)[l];

							for (int p = 0; p < len; p++) {
								wtDcountVarMean = wtDcountVar * tmean[p];

								for (int q = p; q < len; q++) {
									regL[mc][j][l][p][q] += wtDcountVarMean
											* tmean[q];
								}

								regL[mc][j][l][p][len] += wtDcountVarMean;
								regR[mc][j][l][p] += wtMeanVar * tmean[p];
							}
							regL[mc][j][l][len][len] += wtDcountVar;
							regR[mc][j][l][len] += wtMeanVar;
						}
					}
				}
			}
		}

		fillRegLowerPart();

	}

	public void invertVariances() {
		this.cb2mllr = new int[means.getNumStates()];

		for (int i = 0; i < means.getNumStates(); i++) {
			for (int k = 0; k < means.getNumGaussiansPerState(); k++) {
				for (int l = 0; l < means.getVectorLength()[0]; l++) {
					if (variances.getPool().get(i * means.getNumStates() + k)[l] <= 0.) {
						variances.getPool().get(i * means.getNumStates() + k)[l] = (float) 0.5;
					} else if (variances.getPool().get(
							i * means.getNumStates() + k)[l] < varFlor) {
						variances.getPool().get(i * means.getNumStates() + k)[l] = (float) (1. / varFlor);
					} else {
						variances.getPool().get(i * means.getNumStates() + k)[l] = (float) (1. / variances
								.getPool().get(i * means.getNumStates() + k)[l]);
					}
				}
			}
		}

	}

	public void computeMllr() {
		int len;
		DecompositionSolver solver;
		RealMatrix coef;
		RealVector vect, ABloc;

		this.A = new float[nMllrClass][means.getNumStreams()][][];
		this.B = new float[nMllrClass][means.getNumStreams()][];

		for (int m = 0; m < nMllrClass; m++) {
			for (int i = 0; i < means.getNumStreams(); i++) {

				len = means.getVectorLength()[i];
				this.A[m][i] = new float[len][len];
				this.B[m][i] = new float[len];

				for (int j = 0; j < len; ++j) {
					coef = new Array2DRowRealMatrix(regL[m][i][j], false);
					solver = new LUDecomposition(coef).getSolver();
					vect = new ArrayRealVector(regR[m][i][j], false);
					ABloc = solver.solve(vect);

					for (int k = 0; k < len; ++k) {
						this.A[m][i][j][k] = (float) ABloc.getEntry(k);
					}

					this.B[m][i][j] = (float) ABloc.getEntry(len);

				}

			}
		}
	}

	public void estimateMatrices() throws Exception {
		this.readMeansAndVariances();
		this.readCounts();
		this.invertVariances();

		int len = means.getVectorLength()[0];
		this.regL = new double[nMllrClass][means.getNumStreams()][][][];
		this.regR = new double[nMllrClass][means.getNumStreams()][][];

		for (int i = 0; i < nMllrClass; i++) {
			for (int j = 0; j < means.getNumStreams(); j++) {
				len = means.getVectorLength()[j];
				this.regL[i][j] = new double[len][len + 1][len + 1];
				this.regR[i][j] = new double[len][len + 1];
			}
		}

		this.fillRegMatrices();

		this.computeMllr();
	}

}
