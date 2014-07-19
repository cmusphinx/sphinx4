package edu.cmu.sphinx.decoder.adaptation;

public class MllrEstimationDemo {

	public static void main(String[] args) throws Exception {
		MllrEstimation me1 = new MllrEstimation();

		me1.setLocation("/home/bogdanpetcu/RSoC/en-us");
		me1.setModel("en-us");
		me1.setCountsFilePath("/home/bogdanpetcu/RSoC/data/BillGates_2010/unadapted/gauden_counts");
		me1.setOutputFilePath("/home/bogdanpetcu/RSoC/data/BillGates_2010/adapted/mllrMatrix");
		me1.estimateMatrices();
	
		MllrEstimation me2 = new MllrEstimation();

		me2.setLocation("/home/bogdanpetcu/RSoC/en-us");
		me2.setModel("en-us");
		me2.setCountsFilePath("/home/bogdanpetcu/RSoC/data/EricMead_2009/unadapted/gauden_counts");
		me2.setOutputFilePath("/home/bogdanpetcu/RSoC/data/EricMead_2009/adapted/mllrMatrix");
		me2.estimateMatrices();
		
		MllrEstimation me3 = new MllrEstimation();

		me3.setLocation("/home/bogdanpetcu/RSoC/en-us");
		me3.setModel("en-us");
		me3.setCountsFilePath("/home/bogdanpetcu/RSoC/data/GaryFlake_2010/unadapted/gauden_counts");
		me3.setOutputFilePath("/home/bogdanpetcu/RSoC/data/GaryFlake_2010/adapted/mllrMatrix");
		me3.estimateMatrices();

		MllrEstimation me4 = new MllrEstimation();

		me4.setLocation("/home/bogdanpetcu/RSoC/en-us");
		me4.setModel("en-us");
		me4.setCountsFilePath("/home/bogdanpetcu/RSoC/data/JaneMcGonigal_2010/unadaped/gauden_counts");
		me4.setOutputFilePath("/home/bogdanpetcu/RSoC/data/JaneMcGonigal_2010/adapted/mllrMatrix");
		me4.estimateMatrices();

	
	}

}
