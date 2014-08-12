package edu.cmu.sphinx.decoder.adaptation.regtree;

import edu.cmu.sphinx.decoder.adaptation.DensityFileData;
import edu.cmu.sphinx.decoder.adaptation.Transformer;

public class RegTreeTransform extends Transformer {

	private int k;
	RegTreeEstimation estimation;

	public RegTreeTransform(DensityFileData means, String OutputMeanFile,
			int k, RegTreeEstimation estimation) {
		super(means, OutputMeanFile);
		this.k = k;
		this.estimation = estimation;
	}

	protected void transformMean() {
		float[] tmean;

		for (int i = 0; i < this.k; i++) {
			for (int j : estimation.getClusteredData().getStateNumbers().get(i)) {

				for (int k = 0; k < means.getNumStreams(); k++) {
					tmean = new float[means.getVectorLength()[k]];

					for (int l = 0; l < means.getNumGaussiansPerState(); l++) {
						for (int m = 0; m < means.getVectorLength()[k]; m++) {
							tmean[m] = 0;
							for (int n = 0; n < means.getVectorLength()[k]; n++) {
								tmean[m] += estimation.getAs()[i][0][k][m][n]
										* means.getPool()
												.get(j
														* means.getNumGaussiansPerState()
														+ l)[n];
							}
							tmean[m] += estimation.getBs()[i][0][k][m];
						}

						for (int m = 0; m < means.getVectorLength()[k]; m++) {
							this.means.pool.get(j
									* means.getNumGaussiansPerState() + l)[m] = tmean[m];
						}
					}
				}
			}
		}
	}

}
