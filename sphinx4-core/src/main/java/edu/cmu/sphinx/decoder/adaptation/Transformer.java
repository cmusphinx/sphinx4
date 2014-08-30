package edu.cmu.sphinx.decoder.adaptation;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import edu.cmu.sphinx.linguist.acoustic.tiedstate.Pool;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Sphinx3Loader;
import edu.cmu.sphinx.util.Utilities;

/**
 * Base class for transforming acoustic models. This class is extended by
 * MllrTransform and ClustersTransfrom, each implementing a different way
 * transformMean method.
 * 
 * @author Bogdan Petcu
 */
public abstract class Transformer {

	protected Sphinx3Loader loader;
	private String header;
	protected Pool<float[]> means;

	public Transformer(Sphinx3Loader loader) {
		super();
		this.loader = loader;
		this.means = loader.getMeansPool();
		this.header = "s3\nversion 1.0\nchksum0 no \n      endhdr\n";
	}

	public void setHeader(String header) {
		this.header = header;
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

	/**
	 * Transforms the means using provided A and B matrices and stores them in
	 * "means" field.
	 */
	protected abstract void transformMean() throws Exception;

	/**
	 * Adapts the means.
	 */
	private void adaptMean() throws Exception {
		this.transformMean();
	}

	/**
	 * Transforms the acoustic model
	 * 
	 * @throws Exception
	 */
	public void applyTransform() throws Exception {
		this.adaptMean();
		// TODO: Variance adaptation
	}

}
