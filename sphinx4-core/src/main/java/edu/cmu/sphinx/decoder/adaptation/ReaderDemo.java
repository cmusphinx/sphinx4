package edu.cmu.sphinx.decoder.adaptation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Checks basic utility of CountsReader class
 */
public class ReaderDemo {

	public static void main(String[] args) throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				System.in));
		System.out.print("Please enter filepath: ");
		String fp = null;

		try {
			fp = reader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		CountsReader cr;
		Counts cd;
		int nCb, nFeat, nDensity;
		int[] veclen;
		float[][][][] mean, variance;
		float[][][] dnom;

		cr = new CountsReader(fp);
		cr.read();
		cd = cr.getCounts();
		nCb = cd.getnCb();
		nFeat = cd.getnFeat();
		nDensity = cd.getnDensity();

		System.out.println(cd.getHeader() + "\n");
		System.out.println("n_cb = " + nCb + "\n");
		System.out.println("n_feat = " + nFeat + "\n");
		System.out.println("n_density = " + nDensity + "\n");

		veclen = cd.getVeclen();
		mean = cd.getMean();
		variance = cd.getVariance();
		dnom = cd.getDnom();

		System.out.println(mean[0][0][0][0]);
		System.out.println(mean[nCb - 1][nFeat - 1][nDensity - 1][cd
				.getVeclen()[0] - 1]);
		System.out.println(variance[0][0][0][0]);
		System.out.println(variance[nCb - 1][nFeat - 1][nDensity - 1][cd
				.getVeclen()[0] - 1]);
		System.out.println(dnom[0][0][0]);
		System.out.println(dnom[nCb - 1][nFeat - 1][nDensity - 1]);

	}

}
