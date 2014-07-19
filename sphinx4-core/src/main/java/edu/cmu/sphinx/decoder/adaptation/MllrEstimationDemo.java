package edu.cmu.sphinx.decoder.adaptation;

public class MllrEstimationDemo {

	public static void main(String[] args) throws Exception {
		MllrEstimation me = new MllrEstimation();
		
		me.setLocation("/home/bogdanpetcu/RSoC/adadpt/hub4wsj_sc_8k");
		me.setModel("hub4wsj_sc_8k");
		me.setCountsFilePath("/home/bogdanpetcu/RSoC/adadpt/gauden_counts");
		me.estimateMatrices();
	}

}
