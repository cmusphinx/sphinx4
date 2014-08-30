package edu.cmu.sphinx.decoder.adaptation;

import java.io.PrintWriter;

import edu.cmu.sphinx.linguist.acoustic.tiedstate.Sphinx3Loader;

public class Transform {

	private float[][][] A;
	private float[][] B;
	private Sphinx3Loader loader;

	public Transform(Sphinx3Loader loader) {
		this.loader = loader;
	}
	
	/**
	 * Loads a transform using a MllrEstimation object.
	 * @param estimation used for computing transform.
	 * 
	 */
	public void load(MllrEstimation estimation) throws Exception {
		estimation.perform();
		this.A = estimation.getA();
		this.B = estimation.getB();
	}
	
	/**
	 * Used for access to A matrix.
	 * @return A matrix (representing A from A*x + B = C)
	 */
	public float[][][] getA() throws Exception {
		if (this.A == null) {
			throw new Exception("No transform is loaded.");
		}

		return this.A;
	}

	/**
	 * Used for access to B matrix.
	 * @return B matrix (representing B from A*x + B = C)
	 */
	public float[][] getB() throws Exception {
		if (this.B == null) {
			throw new Exception("No transform is loaded.");
		}

		return this.B;
	}

	/**
	 * Writes the transformation to file in a format that could further be used
	 * in Sphinx3 and Sphinx4.
	 * 
	 */
	public void store(String filePath) throws Exception {
		PrintWriter writer = new PrintWriter(filePath, "UTF-8");

		// nMllrClass
		writer.println("1");
		writer.println(loader.getNumStreams());

		for (int i = 0; i < loader.getNumStreams(); i++) {
			writer.println(loader.getVectorLength()[i]);

			for (int j = 0; j < loader.getVectorLength()[i]; j++) {
				for (int k = 0; k < loader.getVectorLength()[i]; ++k) {
					writer.print(A[i][j][k]);
					writer.print(" ");
				}
				writer.println();
			}

			for (int j = 0; j < loader.getVectorLength()[i]; j++) {
				writer.print(B[i][j]);
				writer.print(" ");

			}
			writer.println();

			for (int j = 0; j < loader.getVectorLength()[i]; j++) {
				writer.print("1.0 ");

			}
			writer.println();
		}
		writer.close();
	}

}
