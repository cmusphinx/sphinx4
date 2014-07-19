package edu.cmu.sphinx.decoder.adaptation;

public class MllrEstimationDemo {

	public static void main(String[] args) throws Exception {
		MllrEstimation me1 = new MllrEstimation();

		me1.setLocation("/home/bogdanpetcu/RSoC/en-us");
		me1.setModel("en-us");
		me1.setCountsFilePath("/home/bogdanpetcu/RSoC/data/BillGates_2010/unadapted/gauden_counts");
		me1.setOutputFilePath("/home/bogdanpetcu/RSoC/data/BillGates_2010/adapted/mllrMatrix");
		me1.estimateMatrices();
	
//		MllrEstimation me2 = new MllrEstimation();
//
//		me2.setLocation("/home/bogdanpetcu/RSoC/data/EricMead_2009/adapted/hub4wsj_sc_8k");
//		me2.setModel("hub4wsj_sc_8k");
//		me2.setCountsFilePath("/home/bogdanpetcu/RSoC/data/EricMead_2009/unadapted/gauden_counts");
//		me2.setOutputFileName("mllrMatrix");
//		me2.estimateMatrices();
//
//		MllrEstimation me3 = new MllrEstimation();
//
//		me3.setLocation("/home/bogdanpetcu/RSoC/data/GaryFlake_2010/adapted/hub4wsj_sc_8k");
//		me3.setModel("hub4wsj_sc_8k");
//		me3.setCountsFilePath("/home/bogdanpetcu/RSoC/data/GaryFlake_2010/unadapted/gauden_counts");
//		me3.setOutputFileName("mllrMatrix");
//		me3.estimateMatrices();
//
//		MllrEstimation me4 = new MllrEstimation();
//
//		me4.setLocation("/home/bogdanpetcu/RSoC/data/JaneMcGonigal_2010/adapted/hub4wsj_sc_8k");
//		me4.setModel("hub4wsj_sc_8k");
//		me4.setCountsFilePath("/home/bogdanpetcu/RSoC/data/JaneMcGonigal_2010/unadapted/gauden_counts");
//		me4.setOutputFileName("mllrMatrix");
//		me4.estimateMatrices();

	
	}

}
