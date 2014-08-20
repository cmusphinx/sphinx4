package edu.cmu.sphinx.decoder.adaptation.clustered;

import edu.cmu.sphinx.linguist.acoustic.tiedstate.Pool;

/**
 * Used for storing a cluster of gaussians.
 * 
 * @author Bogdan Petcu
 */
public class Cluster {

	private Pool<float[]> values;

	public Cluster(Pool<float[]> values) {
		this.values = values;
	}

	public Pool<float[]> getValues() {
		return this.values;
	}

}
