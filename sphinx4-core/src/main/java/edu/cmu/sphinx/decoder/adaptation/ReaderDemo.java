package edu.cmu.sphinx.decoder.adaptation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ReaderDemo {

	public static void main(String[] args) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				System.in));
		System.out.print("Please enter filepath: ");
		String fp = null;
		try {
			fp = reader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}

		CountsReader cd = new CountsReader(fp);
		cd.read();
		System.out.println(cd.getHeader() + "\n");
		System.out.println("n_cb = " + cd.getN_cb() + "\n");
		System.out.println("n_feat = " + cd.getN_feat() + "\n");
		System.out.println("n_density = " + cd.getN_density() + "\n");

		int[] veclen = cd.getVeclen();
		float[][][] wt_mean, wt_var, dnom;
		wt_mean = cd.getWt_mean();
		wt_var = cd.getWt_var();
		dnom = cd.getDnom();

	}

}
