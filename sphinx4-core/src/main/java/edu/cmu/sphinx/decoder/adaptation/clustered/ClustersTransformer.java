package edu.cmu.sphinx.decoder.adaptation.clustered;

import edu.cmu.sphinx.decoder.adaptation.Transformer;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Sphinx3Loader;

/**
 * Transforms means using the transformations created with ClustersEstimation.
 * 
 * @author Bogdan Petcu
 */
public class ClustersTransformer extends Transformer {

	private int nrOfClusters;
	ClustersEstimation estimation;

	public ClustersTransformer(Sphinx3Loader loader, int nrOfClusters,
			ClustersEstimation estimation) {
		super(loader);
		this.nrOfClusters = nrOfClusters;
		this.estimation = estimation;
	}

	protected void transformMean() {
		float[] tmean;
		int stateIndex, gaussianIndex;

		for (int i = 0; i < this.nrOfClusters; i++) {
			for (int j : estimation.getClusteredData().getGaussianNumbers()
					.get(i)) {
				stateIndex = j / loader.getNumGaussiansPerState();
				gaussianIndex = j % loader.getNumGaussiansPerState();

				tmean = new float[loader.getVectorLength()[0]];

				for (int l = 0; l < loader.getVectorLength()[0]; l++) {
					tmean[l] = 0;
					for (int m = 0; m < loader.getVectorLength()[0]; m++) {
						tmean[l] += estimation.getAs()[i][0][l][m]
								* loader.getMeansPool()
										.get(stateIndex
												* loader.getNumGaussiansPerState()
												+ gaussianIndex)[m];
					}
					tmean[l] += estimation.getBs()[i][0][l];
				}

				for (int l = 0; l < loader.getVectorLength()[0]; l++) {
					this.loader.getMeansPool().get(
							stateIndex * loader.getNumGaussiansPerState()
									+ gaussianIndex)[l] = tmean[l];
				}
			}
		}
	}

}
