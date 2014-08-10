package edu.cmu.sphinx.decoder.adaptation;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import edu.cmu.sphinx.util.Utilities;

public class MllrTransformer {

	private DensityFileData means;
	private String outputMeanFile;
	private float[][][][] A;
	private float[][][] B;
	private String header;

	public MllrTransformer(DensityFileData means, float[][][][] a,
			float[][][] b, String outputMeanFile) {
		super();
		this.means = means;
		this.outputMeanFile = outputMeanFile;
		A = a;
		B = b;
		this.header = "s3\nversion 1.0\nchksum0 no \n      endhdr\n";
	}

	public void setHeader(String header) {
		this.header = header;
	}
	
	public void setOutputMeanFile(String outputMeanFile){
		this.outputMeanFile = outputMeanFile;
	}

	public DensityFileData getMeans() {
		return this.means;
	}

	public void writeToFile() throws IOException {
		FileOutputStream fp;
		DataOutputStream os;

		fp = new FileOutputStream(this.outputMeanFile);
		os = new DataOutputStream(fp);

		os.write(this.header.getBytes());

		// byte-order magic
		os.writeInt(1144201745);

		os.writeInt(Utilities.swapInteger(means.getNumStates()));
		os.writeInt(Utilities.swapInteger(means.getNumStreams()));
		os.writeInt(Utilities.swapInteger(means.getNumGaussiansPerState()));

		for (int i = 0; i < means.getNumStreams(); i++) {
			os.writeInt(Utilities.swapInteger(means.getVectorLength()[i]));
		}

		os.writeInt(Utilities.swapInteger(means.getNumGaussiansPerState()
				* means.getVectorLength()[0] * means.getNumStates()));

		for (int i = 0; i < means.getNumStates(); i++) {
			for (int j = 0; j < means.getNumStreams(); j++) {
				for (int k = 0; k < means.getNumGaussiansPerState(); k++) {
					for (int l = 0; l < means.getVectorLength()[0]; l++) {
						os.writeFloat(Utilities.swapFloat(means.getPool().get(
								i * means.getNumStreams()
										* means.getNumGaussiansPerState() + j
										* means.getNumGaussiansPerState() + k)[l]));
					}
				}
			}
		}


		os.close();
		fp.close();
	}

	private void transformMean() {
		float[] tmean;

		for (int i = 0; i < means.getNumStates(); i++) {

			for (int j = 0; j < means.getNumStreams(); j++) {
				tmean = new float[means.getVectorLength()[j]];

				for (int k = 0; k < means.getNumGaussiansPerState(); k++) {
					for (int l = 0; l < means.getVectorLength()[j]; l++) {
						tmean[l] = 0;
						for (int m = 0; m < means.getVectorLength()[j]; m++) {
							tmean[l] += A[0][j][l][m]
									* means.getPool().get(
											i * means.getNumGaussiansPerState()
													+ k)[m];
						}
						tmean[l] += B[0][j][l];
					}

					for (int l = 0; l < means.getVectorLength()[j]; l++) {
						this.means.pool.get(i * means.getNumGaussiansPerState()
								+ k)[l] = tmean[l];
					}
				}
			}
		}
	}

	private void adaptMean() throws IOException {
		this.transformMean();
	}

	public void transform() throws IOException {
		this.adaptMean();
	}
}
