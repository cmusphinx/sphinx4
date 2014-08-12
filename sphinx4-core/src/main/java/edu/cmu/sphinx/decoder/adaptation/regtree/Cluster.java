package edu.cmu.sphinx.decoder.adaptation.regtree;

import edu.cmu.sphinx.linguist.acoustic.tiedstate.Pool;

public class Cluster {

	private Pool<float[]> values;

	public Cluster(Pool<float[]> values) {
		this.values = values;
	}

	public Pool<float[]> getValues() {
		return this.values;
	}

}
