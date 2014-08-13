package edu.cmu.sphinx.decoder.adaptation.regtree;

import edu.cmu.sphinx.decoder.adaptation.DensityFileData;
import edu.cmu.sphinx.decoder.adaptation.Transformer;

public class RegTreeTransform extends Transformer {

	private int nrOfClusters;
	RegTreeEstimation estimation;

	public RegTreeTransform(DensityFileData means, String OutputMeanFile,
			int k, RegTreeEstimation estimation) {
		super(means, OutputMeanFile);
		this.nrOfClusters = k;
		this.estimation = estimation;
	}

	protected void transformMean() {
		float[] tmean;
		int stateIndex, gaussianIndex;

		for (int i = 0; i < this.nrOfClusters; i++) {
			for (int j : estimation.getClusteredData().getGaussianNumbers()
					.get(i)) {
				stateIndex = j / means.getNumGaussiansPerState();
				gaussianIndex = j % means.getNumGaussiansPerState();

				tmean = new float[means.getVectorLength()[0]];

				for (int l = 0; l < means.getVectorLength()[0]; l++) {
					tmean[l] = 0;
					for (int m = 0; m < means.getVectorLength()[0]; m++) {
						tmean[l] += estimation.getAs()[i][0][0][l][m]
								* means.getPool()
										.get(stateIndex
												* means.getNumGaussiansPerState()
												+ gaussianIndex)[m];
					}
					tmean[l] += estimation.getBs()[i][0][0][l];
				}

				for (int l = 0; l < means.getVectorLength()[0]; l++) {
					this.means.pool.get(stateIndex
							* means.getNumGaussiansPerState() + gaussianIndex)[l] = tmean[l];
				}
			}
		}
	}
	
}