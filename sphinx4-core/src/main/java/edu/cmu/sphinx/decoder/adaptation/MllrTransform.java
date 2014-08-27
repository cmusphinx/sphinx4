package edu.cmu.sphinx.decoder.adaptation;

/**
 * Class used for transforming means using a MLLR transform generated by
 * MllrEstimation class.
 * 
 * @author Bogdan Petcu
 */
public class MllrTransform extends Transformer {

	private float[][][] A;
	private float[][] B;

	public MllrTransform(DensityFileData means, float[][][] a, float[][] b,
			String outputMeanFile) {
		super(means, outputMeanFile);
		A = a;
		B = b;
	}
	
	protected void transformMean() {
		float[] tmean;

		for (int i = 0; i < means.getNumStates(); i++) {

			for (int j = 0; j < means.getNumStreams(); j++) {
				tmean = new float[means.getVectorLength()[j]];

				for (int k = 0; k < means.getNumGaussiansPerState(); k++) {
					for (int l = 0; l < means.getVectorLength()[j]; l++) {
						tmean[l] = 0;
						for (int m = 0; m < means.getVectorLength()[j]; m++) {
							tmean[l] += A[j][l][m]
									* means.getPool().get(
											i * means.getNumGaussiansPerState()
													+ k)[m];
						}
						tmean[l] += B[j][l];
					}

					for (int l = 0; l < means.getVectorLength()[j]; l++) {
						this.means.pool.get(i * means.getNumGaussiansPerState()
								+ k)[l] = tmean[l];
					}
				}
			}
		}
	}

}
