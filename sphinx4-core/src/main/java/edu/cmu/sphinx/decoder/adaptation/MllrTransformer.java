package edu.cmu.sphinx.decoder.adaptation;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import edu.cmu.sphinx.linguist.acoustic.tiedstate.Pool;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Sphinx3Loader;
import edu.cmu.sphinx.util.Utilities;

/**
 * Transforms means using the transformations created with ClustersEstimation.
 * 
 * @author Bogdan Petcu
 */
public class MllrTransformer {

	protected Sphinx3Loader loader;
	private String header;
	protected Pool<float[]> means;
	private int nrOfClusters;
	private float[][][][] As;
	private float[][][] Bs;
	private ClusteredDensityFileData data;

	public MllrTransformer(Sphinx3Loader loader, int nrOfClusters,
			Transform transform) throws Exception {
		this.loader = loader;
		this.means = loader.getMeansPool();
		this.header = "s3\nversion 1.0\nchksum0 no \n      endhdr\n";
		this.nrOfClusters = nrOfClusters;
		this.As = transform.getAs();
		this.Bs = transform.getBs();
		this.data = new ClusteredDensityFileData(loader, nrOfClusters);
	}

	public void setHeader(String header) {
		this.header = header;
	}

	public void transformMean() {
		float[] tmean;
		int stateIndex, gaussianIndex;

		for (int i = 0; i < this.nrOfClusters; i++) {
			for (int j : data.getGaussianNumbers().get(i)) {
				stateIndex = j / loader.getNumGaussiansPerState();
				gaussianIndex = j % loader.getNumGaussiansPerState();

				tmean = new float[loader.getVectorLength()[0]];

				for (int l = 0; l < loader.getVectorLength()[0]; l++) {
					tmean[l] = 0;
					for (int m = 0; m < loader.getVectorLength()[0]; m++) {
						tmean[l] += As[i][0][l][m]
								* loader.getMeansPool()
										.get(stateIndex
												* loader.getNumGaussiansPerState()
												+ gaussianIndex)[m];
					}
					tmean[l] += Bs[i][0][l];
				}

				for (int l = 0; l < loader.getVectorLength()[0]; l++) {
					this.loader.getMeansPool().get(
							stateIndex * loader.getNumGaussiansPerState()
									+ gaussianIndex)[l] = tmean[l];
				}
			}
		}
	}

	/**
	 * Writes the new adapted means to file.
	 * 
	 * @throws IOException
	 */
	public void createNewMeansFile(String path) throws IOException {
		FileOutputStream fp;
		DataOutputStream os;

		fp = new FileOutputStream(path);
		os = new DataOutputStream(fp);

		os.write(this.header.getBytes());

		// byte-order magic
		os.writeInt(1144201745);

		os.writeInt(Utilities.swapInteger(loader.getNumStates()));
		os.writeInt(Utilities.swapInteger(loader.getNumStreams()));
		os.writeInt(Utilities.swapInteger(loader.getNumGaussiansPerState()));

		for (int i = 0; i < loader.getNumStreams(); i++) {
			os.writeInt(Utilities.swapInteger(loader.getVectorLength()[i]));
		}

		os.writeInt(Utilities.swapInteger(loader.getNumGaussiansPerState()
				* loader.getVectorLength()[0] * loader.getNumStates()));

		for (int i = 0; i < loader.getNumStates(); i++) {
			for (int j = 0; j < loader.getNumStreams(); j++) {
				for (int k = 0; k < loader.getNumGaussiansPerState(); k++) {
					for (int l = 0; l < loader.getVectorLength()[0]; l++) {
						os.writeFloat(Utilities.swapFloat(means.get(i
								* loader.getNumStreams()
								* loader.getNumGaussiansPerState() + j
								* loader.getNumGaussiansPerState() + k)[l]));
					}
				}
			}
		}

		os.close();
		fp.close();
	}

}
