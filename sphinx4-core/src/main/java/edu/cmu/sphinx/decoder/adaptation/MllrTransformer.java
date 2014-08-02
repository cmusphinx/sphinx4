package edu.cmu.sphinx.decoder.adaptation;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class MllrTransformer {

	private DensityFileData means;
	private String outputMeanFile;
	private float[][][][] A;
	private float[][][] B;
	private String header;

	public MllrTransformer(DensityFileData means, String outputMeanFile,
			float[][][][] a, float[][][] b) {
		super();
		this.means = means;
		this.outputMeanFile = outputMeanFile;
		A = a;
		B = b;
		this.header = "s3" + "\n" + "version 1.0" + "\n" + "chksum0 no" + "\n"
				+ "      endhdr" + "\n";
	}

	public void setHeader(String header) {
		this.header = header;
	}

	private void writeToFile() throws IOException {
		FileOutputStream fp;
		DataOutputStream os;

		fp = new FileOutputStream(this.outputMeanFile);
		os = new DataOutputStream(fp);

		os.writeChars(this.header);
		os.writeInt(means.getNumStates());
		os.writeInt(means.getNumStreams());
		os.writeInt(means.getNumGaussiansPerState());

		for (int i = 0; i < means.getNumStreams(); i++) {
			os.writeInt(means.getVectorLength()[i]);
		}

		for (int i = 0; i < means.getNumStates(); i++) {
			for (int j = 0; j < means.getNumStreams(); j++) {
				for (int k = 0; k < means.getNumGaussiansPerState(); k++) {
					for (int l = 0; l < means.getVectorLength()[0]; l++) {
						os.writeFloat(means.getPool().get(
								i * means.getNumGaussiansPerState() + k)[l]);
					}
				}
			}
		}

		os.close();
		fp.close();

	}

	public void transformMean() {
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

	public void adaptMean() throws IOException {
		this.transformMean();
		this.writeToFile();
	}

	public void transform() throws IOException {
		this.adaptMean();
	}
}
